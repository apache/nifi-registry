/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.nifi.registry.security.authorization.file;

import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.registry.properties.NiFiRegistryProperties;
import org.apache.nifi.registry.properties.util.IdentityMapping;
import org.apache.nifi.registry.properties.util.IdentityMappingUtil;
import org.apache.nifi.registry.security.authorization.AccessPolicy;
import org.apache.nifi.registry.security.authorization.AccessPolicyProviderInitializationContext;
import org.apache.nifi.registry.security.authorization.AuthorizerConfigurationContext;
import org.apache.nifi.registry.security.authorization.ConfigurableAccessPolicyProvider;
import org.apache.nifi.registry.security.authorization.RequestAction;
import org.apache.nifi.registry.security.authorization.User;
import org.apache.nifi.registry.security.authorization.UserGroupProvider;
import org.apache.nifi.registry.security.authorization.UserGroupProviderLookup;
import org.apache.nifi.registry.security.authorization.annotation.AuthorizerContext;
import org.apache.nifi.registry.security.authorization.exception.AuthorizationAccessException;
import org.apache.nifi.registry.security.authorization.exception.UninheritableAuthorizationsException;
import org.apache.nifi.registry.security.authorization.file.generated.Authorizations;
import org.apache.nifi.registry.security.authorization.file.generated.Policies;
import org.apache.nifi.registry.security.authorization.file.generated.Policy;
import org.apache.nifi.registry.security.exception.SecurityProviderCreationException;
import org.apache.nifi.registry.security.exception.SecurityProviderDestructionException;
import org.apache.nifi.registry.util.PropertyValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileAccessPolicyProvider implements ConfigurableAccessPolicyProvider {

    private static final Logger logger = LoggerFactory.getLogger(FileAccessPolicyProvider.class);

    private static final String AUTHORIZATIONS_XSD = "/authorizations.xsd";
    private static final String JAXB_AUTHORIZATIONS_PATH = "org.apache.nifi.registry.security.authorization.file.generated";

    private static final JAXBContext JAXB_AUTHORIZATIONS_CONTEXT = initializeJaxbContext(JAXB_AUTHORIZATIONS_PATH);

    /**
     * Load the JAXBContext.
     */
    private static JAXBContext initializeJaxbContext(final String contextPath) {
        try {
            return JAXBContext.newInstance(contextPath, FileAuthorizer.class.getClassLoader());
        } catch (JAXBException e) {
            throw new RuntimeException("Unable to create JAXBContext.");
        }
    }

    private static final DocumentBuilderFactory DOCUMENT_BUILDER_FACTORY = DocumentBuilderFactory.newInstance();
    private static final XMLOutputFactory XML_OUTPUT_FACTORY = XMLOutputFactory.newInstance();

    private static final String POLICY_ELEMENT = "policy";
    private static final String POLICY_USER_ELEMENT = "policyUser";
    private static final String POLICY_GROUP_ELEMENT = "policyGroup";
    private static final String IDENTIFIER_ATTR = "identifier";
    private static final String RESOURCE_ATTR = "resource";
    private static final String ACTIONS_ATTR = "actions";

    /* These codes must match the enumeration values set in authorizations.xsd */
    static final String READ_CODE = "R";
    static final String WRITE_CODE = "W";
    static final String DELETE_CODE = "D";

    /*  TODO - move this somewhere into nifi-registry-security-framework so it can be applied to any ConfigurableAccessPolicyProvider
     *  (and also gets us away from requiring magic strings here) */
    private static final ResourceActionPair[] INITIAL_ADMIN_ACCESS_POLICIES = {
            new ResourceActionPair("/tenants", READ_CODE),
            new ResourceActionPair("/tenants", WRITE_CODE),
            new ResourceActionPair("/tenants", DELETE_CODE),
            new ResourceActionPair("/policies", READ_CODE),
            new ResourceActionPair("/policies", WRITE_CODE),
            new ResourceActionPair("/policies", DELETE_CODE),
            new ResourceActionPair("/buckets", READ_CODE),
            new ResourceActionPair("/buckets", WRITE_CODE),
            new ResourceActionPair("/buckets", DELETE_CODE),
            new ResourceActionPair("/actuator", READ_CODE),
            new ResourceActionPair("/actuator", WRITE_CODE),
            new ResourceActionPair("/actuator", DELETE_CODE),
            new ResourceActionPair("/swagger", READ_CODE),
            new ResourceActionPair("/swagger", WRITE_CODE),
            new ResourceActionPair("/swagger", DELETE_CODE),
            new ResourceActionPair("/proxy", WRITE_CODE)
    };

    /*  TODO - move this somewhere into nifi-registry-security-framework so it can be applied to any ConfigurableAccessPolicyProvider
     *  (and also gets us away from requiring magic strings here) */
    private static final ResourceActionPair[] NIFI_ACCESS_POLICIES = {
            new ResourceActionPair("/buckets", READ_CODE),
            new ResourceActionPair("/proxy", WRITE_CODE)
    };

    static final String PROP_NIFI_IDENTITY_PREFIX = "NiFi Identity ";
    static final String PROP_USER_GROUP_PROVIDER = "User Group Provider";
    static final String PROP_AUTHORIZATIONS_FILE = "Authorizations File";
    static final String PROP_INITIAL_ADMIN_IDENTITY = "Initial Admin Identity";
    static final Pattern NIFI_IDENTITY_PATTERN = Pattern.compile(PROP_NIFI_IDENTITY_PREFIX + "\\S+");

    private Schema authorizationsSchema;
    private NiFiRegistryProperties properties;
    private File authorizationsFile;
    private String initialAdminIdentity;
    private Set<String> nifiIdentities;
    private List<IdentityMapping> identityMappings;

    private UserGroupProvider userGroupProvider;
    private UserGroupProviderLookup userGroupProviderLookup;
    private final AtomicReference<AuthorizationsHolder> authorizationsHolder = new AtomicReference<>();

    @Override
    public void initialize(AccessPolicyProviderInitializationContext initializationContext) throws SecurityProviderCreationException {
        userGroupProviderLookup = initializationContext.getUserGroupProviderLookup();

        try {
            final SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            authorizationsSchema = schemaFactory.newSchema(FileAuthorizer.class.getResource(AUTHORIZATIONS_XSD));
        } catch (Exception e) {
            throw new SecurityProviderCreationException(e);
        }
    }

    @Override
    public void onConfigured(AuthorizerConfigurationContext configurationContext) throws SecurityProviderCreationException {
        try {
            final PropertyValue userGroupProviderIdentifier = configurationContext.getProperty(PROP_USER_GROUP_PROVIDER);
            if (!userGroupProviderIdentifier.isSet()) {
                throw new SecurityProviderCreationException("The user group provider must be specified.");
            }

            userGroupProvider = userGroupProviderLookup.getUserGroupProvider(userGroupProviderIdentifier.getValue());
            if (userGroupProvider == null) {
                throw new SecurityProviderCreationException("Unable to locate user group provider with identifier " + userGroupProviderIdentifier.getValue());
            }

            final PropertyValue authorizationsPath = configurationContext.getProperty(PROP_AUTHORIZATIONS_FILE);
            if (StringUtils.isBlank(authorizationsPath.getValue())) {
                throw new SecurityProviderCreationException("The authorizations file must be specified.");
            }

            // get the authorizations file and ensure it exists
            authorizationsFile = new File(authorizationsPath.getValue());
            if (!authorizationsFile.exists()) {
                logger.info("Creating new authorizations file at {}", new Object[] {authorizationsFile.getAbsolutePath()});
                saveAuthorizations(new Authorizations());
            }

            // extract the identity mappings from nifi-registry.properties if any are provided
            identityMappings = Collections.unmodifiableList(IdentityMappingUtil.getIdentityMappings(properties));

            // get the value of the initial admin identity
            final PropertyValue initialAdminIdentityProp = configurationContext.getProperty(PROP_INITIAL_ADMIN_IDENTITY);
            initialAdminIdentity = initialAdminIdentityProp.isSet() ? IdentityMappingUtil.mapIdentity(initialAdminIdentityProp.getValue(), identityMappings) : null;

            // extract any nifi identities
            nifiIdentities = new HashSet<>();
            for (Map.Entry<String,String> entry : configurationContext.getProperties().entrySet()) {
                Matcher matcher = NIFI_IDENTITY_PATTERN.matcher(entry.getKey());
                if (matcher.matches() && !StringUtils.isBlank(entry.getValue())) {
                    nifiIdentities.add(IdentityMappingUtil.mapIdentity(entry.getValue(), identityMappings));
                }
            }

            // load the authorizations
            load();

            logger.info(String.format("Authorizations file loaded at %s", new Date().toString()));
        } catch (SecurityProviderCreationException | JAXBException | IllegalStateException | SAXException e) {
            throw new SecurityProviderCreationException(e);
        }
    }

    @Override
    public UserGroupProvider getUserGroupProvider() {
        return userGroupProvider;
    }

    @Override
    public Set<AccessPolicy> getAccessPolicies() throws AuthorizationAccessException {
        return authorizationsHolder.get().getAllPolicies();
    }

    @Override
    public synchronized AccessPolicy addAccessPolicy(AccessPolicy accessPolicy) throws AuthorizationAccessException {
        if (accessPolicy == null) {
            throw new IllegalArgumentException("AccessPolicy cannot be null");
        }

        // create the new JAXB Policy
        final Policy policy = createJAXBPolicy(accessPolicy);

        // add the new Policy to the top-level list of policies
        final AuthorizationsHolder holder = authorizationsHolder.get();
        final Authorizations authorizations = holder.getAuthorizations();
        authorizations.getPolicies().getPolicy().add(policy);

        saveAndRefreshHolder(authorizations);

        return authorizationsHolder.get().getPoliciesById().get(accessPolicy.getIdentifier());
    }

    @Override
    public AccessPolicy getAccessPolicy(String identifier) throws AuthorizationAccessException {
        if (identifier == null) {
            return null;
        }

        final AuthorizationsHolder holder = authorizationsHolder.get();
        return holder.getPoliciesById().get(identifier);
    }

    @Override
    public AccessPolicy getAccessPolicy(String resourceIdentifier, RequestAction action) throws AuthorizationAccessException {
        return authorizationsHolder.get().getAccessPolicy(resourceIdentifier, action);
    }

    @Override
    public synchronized AccessPolicy updateAccessPolicy(AccessPolicy accessPolicy) throws AuthorizationAccessException {
        if (accessPolicy == null) {
            throw new IllegalArgumentException("AccessPolicy cannot be null");
        }

        final AuthorizationsHolder holder = this.authorizationsHolder.get();
        final Authorizations authorizations = holder.getAuthorizations();

        // try to find an existing Authorization that matches the policy id
        Policy updatePolicy = null;
        for (Policy policy : authorizations.getPolicies().getPolicy()) {
            if (policy.getIdentifier().equals(accessPolicy.getIdentifier())) {
                updatePolicy = policy;
                break;
            }
        }

        // no matching Policy so return null
        if (updatePolicy == null) {
            return null;
        }

        // update the Policy, save, reload, and return
        transferUsersAndGroups(accessPolicy, updatePolicy);
        saveAndRefreshHolder(authorizations);

        return this.authorizationsHolder.get().getPoliciesById().get(accessPolicy.getIdentifier());
    }

    @Override
    public synchronized AccessPolicy deleteAccessPolicy(AccessPolicy accessPolicy) throws AuthorizationAccessException {
        if (accessPolicy == null) {
            throw new IllegalArgumentException("AccessPolicy cannot be null");
        }

        return deleteAccessPolicy(accessPolicy.getIdentifier());
    }

    @Override
    public synchronized AccessPolicy deleteAccessPolicy(String accessPolicyIdentifer) throws AuthorizationAccessException {
        if (accessPolicyIdentifer == null) {
            throw new IllegalArgumentException("Access policy identifier cannot be null");
        }

        final AuthorizationsHolder holder = this.authorizationsHolder.get();
        AccessPolicy deletedPolicy = holder.getPoliciesById().get(accessPolicyIdentifer);
        if (deletedPolicy == null) {
            return null;
        }

        // find the matching Policy and remove it
        final Authorizations authorizations = holder.getAuthorizations();
        Iterator<Policy> policyIter = authorizations.getPolicies().getPolicy().iterator();
        while (policyIter.hasNext()) {
            final Policy policy = policyIter.next();
            if (policy.getIdentifier().equals(accessPolicyIdentifer)) {
                policyIter.remove();
                break;
            }
        }

        saveAndRefreshHolder(authorizations);
        return deletedPolicy;
    }

    AuthorizationsHolder getAuthorizationsHolder() {
        return authorizationsHolder.get();
    }

    @AuthorizerContext
    public void setNiFiProperties(NiFiRegistryProperties properties) {
        this.properties = properties;
    }

    @Override
    public synchronized void inheritFingerprint(String fingerprint) throws AuthorizationAccessException {
        parsePolicies(fingerprint).forEach(policy -> addAccessPolicy(policy));
    }

    @Override
    public void checkInheritability(String proposedFingerprint) throws AuthorizationAccessException, UninheritableAuthorizationsException {
        try {
            // ensure we can understand the proposed fingerprint
            parsePolicies(proposedFingerprint);
        } catch (final AuthorizationAccessException e) {
            throw new UninheritableAuthorizationsException("Unable to parse the proposed fingerprint: " + e);
        }

        // ensure we are in a proper state to inherit the fingerprint
        if (!getAccessPolicies().isEmpty()) {
            throw new UninheritableAuthorizationsException("Proposed fingerprint is not inheritable because the current access policies is not empty.");
        }
    }

    @Override
    public String getFingerprint() throws AuthorizationAccessException {
        final List<AccessPolicy> policies = new ArrayList<>(getAccessPolicies());
        Collections.sort(policies, Comparator.comparing(AccessPolicy::getIdentifier));

        XMLStreamWriter writer = null;
        final StringWriter out = new StringWriter();
        try {
            writer = XML_OUTPUT_FACTORY.createXMLStreamWriter(out);
            writer.writeStartDocument();
            writer.writeStartElement("accessPolicies");

            for (AccessPolicy policy : policies) {
                writePolicy(writer, policy);
            }

            writer.writeEndElement();
            writer.writeEndDocument();
            writer.flush();
        } catch (XMLStreamException e) {
            throw new AuthorizationAccessException("Unable to generate fingerprint", e);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (XMLStreamException e) {
                    // nothing to do here
                }
            }
        }

        return out.toString();
    }

    private List<AccessPolicy> parsePolicies(final String fingerprint) {
        final List<AccessPolicy> policies = new ArrayList<>();

        final byte[] fingerprintBytes = fingerprint.getBytes(StandardCharsets.UTF_8);
        try (final ByteArrayInputStream in = new ByteArrayInputStream(fingerprintBytes)) {
            final DocumentBuilder docBuilder = DOCUMENT_BUILDER_FACTORY.newDocumentBuilder();
            final Document document = docBuilder.parse(in);
            final Element rootElement = document.getDocumentElement();

            // parse all the policies and add them to the current access policy provider
            NodeList policyNodes = rootElement.getElementsByTagName(POLICY_ELEMENT);
            for (int i = 0; i < policyNodes.getLength(); i++) {
                Node policyNode = policyNodes.item(i);
                policies.add(parsePolicy((Element) policyNode));
            }
        } catch (SAXException | ParserConfigurationException | IOException e) {
            throw new AuthorizationAccessException("Unable to parse fingerprint", e);
        }

        return policies;
    }

    private AccessPolicy parsePolicy(final Element element) {
        final AccessPolicy.Builder builder = new AccessPolicy.Builder()
                .identifier(element.getAttribute(IDENTIFIER_ATTR))
                .resource(element.getAttribute(RESOURCE_ATTR));

        final String actions = element.getAttribute(ACTIONS_ATTR);
        if (actions.equals(RequestAction.READ.name())) {
            builder.action(RequestAction.READ);
        } else if (actions.equals(RequestAction.WRITE.name())) {
            builder.action(RequestAction.WRITE);
        } else if (actions.equals(RequestAction.DELETE.name())) {
            builder.action(RequestAction.DELETE);
        } else {
            throw new IllegalStateException("Unknown Policy Action: " + actions);
        }

        NodeList policyUsers = element.getElementsByTagName(POLICY_USER_ELEMENT);
        for (int i=0; i < policyUsers.getLength(); i++) {
            Element policyUserNode = (Element) policyUsers.item(i);
            builder.addUser(policyUserNode.getAttribute(IDENTIFIER_ATTR));
        }

        NodeList policyGroups = element.getElementsByTagName(POLICY_GROUP_ELEMENT);
        for (int i=0; i < policyGroups.getLength(); i++) {
            Element policyGroupNode = (Element) policyGroups.item(i);
            builder.addGroup(policyGroupNode.getAttribute(IDENTIFIER_ATTR));
        }

        return builder.build();
    }

    private void writePolicy(final XMLStreamWriter writer, final AccessPolicy policy) throws XMLStreamException {
        // sort the users for the policy
        List<String> policyUsers = new ArrayList<>(policy.getUsers());
        Collections.sort(policyUsers);

        // sort the groups for this policy
        List<String> policyGroups = new ArrayList<>(policy.getGroups());
        Collections.sort(policyGroups);

        writer.writeStartElement(POLICY_ELEMENT);
        writer.writeAttribute(IDENTIFIER_ATTR, policy.getIdentifier());
        writer.writeAttribute(RESOURCE_ATTR, policy.getResource());
        writer.writeAttribute(ACTIONS_ATTR, policy.getAction().name());

        for (String policyUser : policyUsers) {
            writer.writeStartElement(POLICY_USER_ELEMENT);
            writer.writeAttribute(IDENTIFIER_ATTR, policyUser);
            writer.writeEndElement();
        }

        for (String policyGroup : policyGroups) {
            writer.writeStartElement(POLICY_GROUP_ELEMENT);
            writer.writeAttribute(IDENTIFIER_ATTR, policyGroup);
            writer.writeEndElement();
        }

        writer.writeEndElement();
    }

    /**
     * Loads the authorizations file and populates the AuthorizationsHolder, only called during start-up.
     *
     * @throws JAXBException            Unable to reload the authorized users file
     */
    private synchronized void load() throws JAXBException, SAXException {
        // attempt to unmarshal
        final Authorizations authorizations = unmarshallAuthorizations();
        if (authorizations.getPolicies() == null) {
            authorizations.setPolicies(new Policies());
        }

        final AuthorizationsHolder authorizationsHolder = new AuthorizationsHolder(authorizations);
        final boolean emptyAuthorizations = authorizationsHolder.getAllPolicies().isEmpty();
        final boolean hasInitialAdminIdentity = (initialAdminIdentity != null && !StringUtils.isBlank(initialAdminIdentity));
        final boolean hasNiFiIdentities = (nifiIdentities != null && !nifiIdentities.isEmpty());

        // if we are starting fresh then we might need to populate an initial admin
        if (emptyAuthorizations) {
            if (hasInitialAdminIdentity) {
               logger.info("Populating authorizations for Initial Admin: " + initialAdminIdentity);
               populateInitialAdmin(authorizations);
            }

            if (hasNiFiIdentities) {
                logger.info("Populating proxy authorizations for NiFi clients: [{}]", StringUtils.join(nifiIdentities, ";"));
                populateNiFiIdentities(authorizations);
            }

            saveAndRefreshHolder(authorizations);
        } else {
            this.authorizationsHolder.set(authorizationsHolder);
        }
    }

    private void saveAuthorizations(final Authorizations authorizations) throws JAXBException {
        final Marshaller marshaller = JAXB_AUTHORIZATIONS_CONTEXT.createMarshaller();
        marshaller.setSchema(authorizationsSchema);
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.marshal(authorizations, authorizationsFile);
    }

    private Authorizations unmarshallAuthorizations() throws JAXBException {
        final Unmarshaller unmarshaller = JAXB_AUTHORIZATIONS_CONTEXT.createUnmarshaller();
        unmarshaller.setSchema(authorizationsSchema);

        final JAXBElement<Authorizations> element = unmarshaller.unmarshal(new StreamSource(authorizationsFile), Authorizations.class);
        return element.getValue();
    }

    /**
     *  Creates the initial admin user and sets policies managing buckets, users, and policies.
     *
     *  TODO - move this somewhere into nifi-registry-security-framework so it can be applied to any ConfigurableAccessPolicyProvider
     */
    private void populateInitialAdmin(final Authorizations authorizations) {
        final User initialAdmin = userGroupProvider.getUserByIdentity(initialAdminIdentity);
        if (initialAdmin == null) {
            throw new SecurityProviderCreationException("Unable to locate initial admin " + initialAdminIdentity + " to seed policies");
        }

        for (ResourceActionPair resourceAction : INITIAL_ADMIN_ACCESS_POLICIES) {
            addUserToAccessPolicy(authorizations, resourceAction.resource, initialAdmin.getIdentifier(), resourceAction.actionCode);
        }
    }

    /**
     * Creates a user for each NiFi client and gives each one write permission to /proxy.
     *
     * @param authorizations the overall authorizations
     */
    private void populateNiFiIdentities(Authorizations authorizations) {
        for (String nifiIdentity : nifiIdentities) {
            final User nifiUser = userGroupProvider.getUserByIdentity(nifiIdentity);
            if (nifiUser == null) {
                throw new SecurityProviderCreationException("Unable to locate node " + nifiIdentity + " to seed policies.");
            }

            // grant access to the resources needed for initial nifi-proxy identities
            for (ResourceActionPair resourceAction : NIFI_ACCESS_POLICIES) {
                addUserToAccessPolicy(authorizations, resourceAction.resource, nifiUser.getIdentifier(), resourceAction.actionCode);
            }
        }
    }


    /**
     * Creates and adds an access policy for the given resource, identity, and actions to the specified authorizations.
     *
     * @param authorizations the Authorizations instance to add the policy to
     * @param resource the resource for the policy
     * @param userIdentifier the identifier for the user to add to the policy
     * @param action the action for the policy
     */
    private void addUserToAccessPolicy(final Authorizations authorizations, final String resource, final String userIdentifier, final String action) {
        // first try to find an existing policy for the given resource and action
        Policy foundPolicy = null;
        for (Policy policy : authorizations.getPolicies().getPolicy()) {
            if (policy.getResource().equals(resource) && policy.getAction().equals(action)) {
                foundPolicy = policy;
                break;
            }
        }

        if (foundPolicy == null) {
            // if we didn't find an existing policy create a new one
            final String uuidSeed = resource + action;

            final AccessPolicy.Builder builder = new AccessPolicy.Builder()
                    .identifierGenerateFromSeed(uuidSeed)
                    .resource(resource)
                    .addUser(userIdentifier);

            if (action.equals(READ_CODE)) {
                builder.action(RequestAction.READ);
            } else if (action.equals(WRITE_CODE)) {
                builder.action(RequestAction.WRITE);
            } else if (action.equals(DELETE_CODE)) {
                builder.action(RequestAction.DELETE);
            } else {
                throw new IllegalStateException("Unknown Policy Action: " + action);
            }

            final AccessPolicy accessPolicy = builder.build();
            final Policy jaxbPolicy = createJAXBPolicy(accessPolicy);
            authorizations.getPolicies().getPolicy().add(jaxbPolicy);
        } else {
            // otherwise add the user to the existing policy
            Policy.User policyUser = new Policy.User();
            policyUser.setIdentifier(userIdentifier);
            foundPolicy.getUser().add(policyUser);
        }
    }

    private Policy createJAXBPolicy(final AccessPolicy accessPolicy) {
        final Policy policy = new Policy();
        policy.setIdentifier(accessPolicy.getIdentifier());
        policy.setResource(accessPolicy.getResource());

        switch (accessPolicy.getAction()) {
            case READ:
                policy.setAction(READ_CODE);
                break;
            case WRITE:
                policy.setAction(WRITE_CODE);
                break;
            case DELETE:
                policy.setAction(DELETE_CODE);
                break;
            default:
                break;
        }

        transferUsersAndGroups(accessPolicy, policy);
        return policy;
    }

    /**
     * Sets the given Policy to the state of the provided AccessPolicy. Users and Groups will be cleared and
     * set to match the AccessPolicy, the resource and action will be set to match the AccessPolicy.
     *
     * Does not set the identifier.
     *
     * @param accessPolicy the AccessPolicy to transfer state from
     * @param policy the Policy to transfer state to
     */
    private void transferUsersAndGroups(AccessPolicy accessPolicy, Policy policy) {
        // add users to the policy
        policy.getUser().clear();
        for (String userIdentifier : accessPolicy.getUsers()) {
            Policy.User policyUser = new Policy.User();
            policyUser.setIdentifier(userIdentifier);
            policy.getUser().add(policyUser);
        }

        // add groups to the policy
        policy.getGroup().clear();
        for (String groupIdentifier : accessPolicy.getGroups()) {
            Policy.Group policyGroup = new Policy.Group();
            policyGroup.setIdentifier(groupIdentifier);
            policy.getGroup().add(policyGroup);
        }
    }

    /**
     * Adds the given user identifier to the policy if it doesn't already exist.
     *
     * @param userIdentifier a user identifier
     * @param policy a policy to add the user to
     */
    private void addUserToPolicy(final String userIdentifier, final Policy policy) {
        // determine if the user already exists in the policy
        boolean userExists = false;
        for (Policy.User policyUser : policy.getUser()) {
            if (policyUser.getIdentifier().equals(userIdentifier)) {
                userExists = true;
                break;
            }
        }

        // add the user to the policy if doesn't already exist
        if (!userExists) {
            Policy.User policyUser = new Policy.User();
            policyUser.setIdentifier(userIdentifier);
            policy.getUser().add(policyUser);
        }
    }

    /**
     * Adds the given group identifier to the policy if it doesn't already exist.
     *
     * @param groupIdentifier a group identifier
     * @param policy a policy to add the user to
     */
    private void addGroupToPolicy(final String groupIdentifier, final Policy policy) {
        // determine if the group already exists in the policy
        boolean groupExists = false;
        for (Policy.Group policyGroup : policy.getGroup()) {
            if (policyGroup.getIdentifier().equals(groupIdentifier)) {
                groupExists = true;
                break;
            }
        }

        // add the group to the policy if doesn't already exist
        if (!groupExists) {
            Policy.Group policyGroup = new Policy.Group();
            policyGroup.setIdentifier(groupIdentifier);
            policy.getGroup().add(policyGroup);
        }
    }

    /**
     * Finds the Policy matching the resource and action, or creates a new one and adds it to the list of policies.
     *
     * @param policies the policies to search through
     * @param seedIdentity the seedIdentity to use when creating identifiers for new policies
     * @param resource the resource for the policy
     * @param action the action string for the police (R or RW)
     * @return the matching policy or a new policy
     */
    private Policy getOrCreatePolicy(final List<Policy> policies, final String seedIdentity, final String resource, final String action) {
        Policy foundPolicy = null;

        // try to find a policy with the same resource and actions
        for (Policy policy : policies) {
            if (policy.getResource().equals(resource) && policy.getAction().equals(action)) {
                foundPolicy = policy;
                break;
            }
        }

        // if a matching policy wasn't found then create one
        if (foundPolicy == null) {
            final String uuidSeed = resource + action + seedIdentity;
            final String policyIdentifier = IdentifierUtil.getIdentifier(uuidSeed);

            foundPolicy = new Policy();
            foundPolicy.setIdentifier(policyIdentifier);
            foundPolicy.setResource(resource);
            foundPolicy.setAction(action);

            policies.add(foundPolicy);
        }

        return foundPolicy;
    }

    /**
     * Saves the Authorizations instance by marshalling to a file, then re-populates the
     * in-memory data structures and sets the new holder.
     *
     * Synchronized to ensure only one thread writes the file at a time.
     *
     * @param authorizations the authorizations to save and populate from
     * @throws AuthorizationAccessException if an error occurs saving the authorizations
     */
    private synchronized void saveAndRefreshHolder(final Authorizations authorizations) throws AuthorizationAccessException {
        try {
            saveAuthorizations(authorizations);

            this.authorizationsHolder.set(new AuthorizationsHolder(authorizations));
        } catch (JAXBException e) {
            throw new AuthorizationAccessException("Unable to save Authorizations", e);
        }
    }

    @Override
    public void preDestruction() throws SecurityProviderDestructionException {
    }

    private static class ResourceActionPair {
        public String resource;
        public String actionCode;
        public ResourceActionPair(String resource, String actionCode) {
            this.resource = resource;
            this.actionCode = actionCode;
        }
    }
}
