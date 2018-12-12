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
package org.apache.nifi.registry.bundle.extract.nar.docs;

import org.apache.nifi.registry.bundle.extract.BundleException;
import org.apache.nifi.registry.bundle.model.ExtensionDetails;
import org.apache.nifi.registry.bundle.model.ExtensionType;
import org.apache.nifi.registry.bundle.model.ProvidedServiceApiDetails;
import org.apache.nifi.registry.bundle.model.RestrictionDetails;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

/**
 * SAX implementation of ExtensionDocsParser.
 */
public class SAXExtensionDocsParser implements ExtensionDocsParser {

    @Override
    public ExtensionDocs parse(final InputStream inputStream) {
        final SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);
        try {
            final SAXParser saxParser = spf.newSAXParser();
            final ExtensionDocsHandler handler = new ExtensionDocsHandler();

            final XMLReader xmlReader = saxParser.getXMLReader();
            xmlReader.setContentHandler(handler);
            xmlReader.parse(new InputSource(inputStream));

            return handler.getExtensionDocs();
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new BundleException("Unable to parse extension docs due to: " + e.getMessage(), e);
        }
    }

    /**
     * ContentHandler for the SAX events.
     */
    private static class ExtensionDocsHandler extends DefaultHandler {

        private ExtensionDocsElement currElement;
        private Stack<String> elementStack = new Stack<>();

        private StringBuilder buffer = new StringBuilder();
        private ExtensionDetails.Builder extensionDetailsBuilder;
        private ProvidedServiceApiDetails.Builder providedApiBuilder;
        private RestrictionDetails.Builder restrictionBuilder;

        private String nifiApiVersion;
        private Set<ExtensionDetails> extensionDetails = new HashSet<>();

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            elementStack.push(localName);

            currElement = ExtensionDocsElement.fromElementName(localName);
            switch(currElement) {
                case EXTENSION:
                    extensionDetailsBuilder = new ExtensionDetails.Builder();
                    break;
                case SERVICE:
                    providedApiBuilder = new ProvidedServiceApiDetails.Builder();
                    break;
                case RESTRICTION:
                    restrictionBuilder = new RestrictionDetails.Builder();
                    break;
                default:
                    break;
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            elementStack.pop();
            final String prevElementName = elementStack.isEmpty() ? "" : elementStack.peek();

            currElement = ExtensionDocsElement.fromElementName(localName);
            switch(currElement) {
                case NIFI_API_VERSION:
                    nifiApiVersion = buffer.toString();
                    break;
                case NAME:
                    if (ExtensionDocsElement.EXTENSION.getElementName().equals(prevElementName)) {
                        extensionDetailsBuilder.name(buffer.toString().trim());
                    }
                    break;
                case DESCRIPTION:
                    if (ExtensionDocsElement.EXTENSION.getElementName().equals(prevElementName)) {
                        extensionDetailsBuilder.description(buffer.toString().trim());
                    }
                    break;
                case TYPE:
                    if (ExtensionDocsElement.EXTENSION.getElementName().equals(prevElementName)) {
                        final ExtensionType type = ExtensionType.valueOf(buffer.toString().trim());
                        extensionDetailsBuilder.type(type);
                    }
                    break;
                case TAG:
                    if (ExtensionDocsElement.TAGS.getElementName().equals(prevElementName)) {
                        extensionDetailsBuilder.addTag(buffer.toString().trim());
                    }
                    break;
                case GENERAL_RESTRICTION_EXPLANATION:
                    final String explanation = buffer.toString().trim();
                    if (!explanation.isEmpty()) {
                        extensionDetailsBuilder.generalRestriction(explanation);
                    }
                    break;
                case RESTRICTION_REQUIRED_PERMISSION:
                    if (ExtensionDocsElement.RESTRICTION.getElementName().equals(prevElementName)) {
                        restrictionBuilder.requiredPermission(buffer.toString().trim());
                    }
                    break;
                case RESTRICTION_EXPLANATION:
                    if (ExtensionDocsElement.RESTRICTION.getElementName().equals(prevElementName)) {
                        restrictionBuilder.explanation(buffer.toString().trim());
                    }
                    break;
                case RESTRICTION:
                    final RestrictionDetails restrictionDetails = restrictionBuilder.build();
                    extensionDetailsBuilder.addRestriction(restrictionDetails);
                    restrictionBuilder = null;
                    break;
                case SERVICE_CLASS_NAME:
                    if (ExtensionDocsElement.SERVICE.getElementName().equals(prevElementName)) {
                        providedApiBuilder.className(buffer.toString().trim());
                    }
                    break;
                case SERVICE_GROUP_ID:
                    if (ExtensionDocsElement.SERVICE.getElementName().equals(prevElementName)) {
                        providedApiBuilder.groupId(buffer.toString().trim());
                    }
                    break;
                case SERVICE_ARTIFACT_ID:
                    if (ExtensionDocsElement.SERVICE.getElementName().equals(prevElementName)) {
                        providedApiBuilder.artifactId(buffer.toString().trim());
                    }
                    break;
                case SERVICE_VERSION:
                    if (ExtensionDocsElement.SERVICE.getElementName().equals(prevElementName)) {
                        providedApiBuilder.version(buffer.toString().trim());
                    }
                    break;
                case SERVICE:
                    final ProvidedServiceApiDetails providedServiceApi = providedApiBuilder.build();
                    extensionDetailsBuilder.addProvidedServiceApi(providedServiceApi);
                    providedApiBuilder = null;
                    break;
                case EXTENSION:
                    final ExtensionDetails details = extensionDetailsBuilder.build();
                    extensionDetails.add(details);
                    extensionDetailsBuilder = null;
                default:
                    break;
            }

            currElement = null;
            buffer = new StringBuilder();
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            if (currElement == null || currElement == ExtensionDocsElement.OTHER) {
                return;
            }

            for(int i=start; i < (start+length); i++) {
                buffer.append(ch[i]);
            }
        }

        public ExtensionDocs getExtensionDocs() {
            return new ExtensionDocs(nifiApiVersion, extensionDetails);
        }
    }

}
