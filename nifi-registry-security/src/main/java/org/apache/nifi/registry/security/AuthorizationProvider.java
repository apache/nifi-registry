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
package org.apache.nifi.registry.security;

import org.apache.nifi.registry.properties.NiFiRegistryProperties;
import org.apache.nifi.registry.user.generated.User;
import org.apache.nifi.registry.user.generated.Users;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AuthorizationProvider {

    private static final String USERS_XSD = "/users.xsd";
    private static final String JAXB_GENERATED_PATH = "org.apache.nifi.registry.user.generated";
    private static final JAXBContext JAXB_CONTEXT = initializeJaxbContext();

    /**
     * Load the JAXBContext.
     */
    private static JAXBContext initializeJaxbContext() {
        try {
            return JAXBContext.newInstance(JAXB_GENERATED_PATH, AuthorizationProvider.class.getClassLoader());
        } catch (JAXBException e) {
            throw new RuntimeException("Unable to create JAXBContext.");
        }
    }

    private final List<String> authorizedUsers;

    public AuthorizationProvider(final NiFiRegistryProperties properties) {
        final File usersFile = properties.getAuthorizedUsersFile();
        final List<String> userList = new ArrayList<>();

        // load the users from the specified file
        if (usersFile != null && usersFile.exists()) {
            try {
                // find the schema
                final SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
                final Schema schema = schemaFactory.newSchema(AuthorizationProvider.class.getResource(USERS_XSD));

                // attempt to unmarshal
                final Unmarshaller unmarshaller = JAXB_CONTEXT.createUnmarshaller();
                unmarshaller.setSchema(schema);
                final JAXBElement<Users> element = unmarshaller.unmarshal(new StreamSource(usersFile), Users.class);
                final Users users = element.getValue();

                // add each users dn
                for (final User user : users.getUser()) {
                    userList.add(user.getDn());
                }
            } catch (SAXException | JAXBException e) {
                throw new RuntimeException("Unable to read the authorized useres file: " + e, e);
            }
        }

        authorizedUsers = Collections.unmodifiableList(userList);
    }

    public boolean canAccess(final String dn) {
        return authorizedUsers.contains(dn);
    }
}
