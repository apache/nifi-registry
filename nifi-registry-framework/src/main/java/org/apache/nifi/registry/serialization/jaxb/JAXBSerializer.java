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
package org.apache.nifi.registry.serialization.jaxb;

import org.apache.nifi.registry.serialization.SerializationException;
import org.apache.nifi.registry.serialization.Serializer;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A Serializer that uses JAXB for serializing/deserializing.
 */
public class JAXBSerializer<T> implements Serializer<T> {

    private final JAXBContext jaxbContext;

    /**
     * Load the JAXBContext.
     */
    public JAXBSerializer(final Class<T> clazz) {
        try {
            this.jaxbContext = JAXBContext.newInstance(clazz);
        } catch (JAXBException e) {
            throw new RuntimeException("Unable to create JAXBContext: " + e.getMessage(), e);
        }
    }

    @Override
    public void serialize(final T t, final OutputStream out) throws SerializationException {
        if (t == null) {
            throw new IllegalArgumentException("The object to serialize cannot be null");
        }

        if (out == null) {
            throw new IllegalArgumentException("OutputStream cannot be null");
        }

        try {
            final Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.marshal(t, out);
        } catch (JAXBException e) {
            throw new SerializationException("Unable to serialize object", e);
        }
    }

    @Override
    public T deserialize(final InputStream input) throws SerializationException {
        if (input == null) {
            throw new IllegalArgumentException("InputStream cannot be null");
        }

        try {
            final Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            return (T) unmarshaller.unmarshal(input);
        } catch (JAXBException e) {
            throw new SerializationException("Unable to deserialize object", e);
        }
    }

}
