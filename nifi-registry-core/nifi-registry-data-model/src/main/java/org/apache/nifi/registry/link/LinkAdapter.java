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
package org.apache.nifi.registry.link;

import javax.ws.rs.core.Link;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.namespace.QName;
import java.util.Map;

/**
 * This class is a modified version of Jersey's Link.JaxbAdapter that adds protection against nulls.
 */
public class LinkAdapter extends XmlAdapter<Link.JaxbLink, Link> {

    /**
     * Convert a {@link Link.JaxbLink} into a {@link Link}.
     *
     * @param v instance of type {@link Link.JaxbLink}.
     * @return mapped instance of type {@link Link.JaxbLink}
     */
    @Override
    public Link unmarshal(Link.JaxbLink v) {
        if (v == null) {
            return null;
        }

        Link.Builder lb = Link.fromUri(v.getUri());
        for (Map.Entry<QName, Object> e : v.getParams().entrySet()) {
            lb.param(e.getKey().getLocalPart(), e.getValue().toString());
        }
        return lb.build();
    }

    /**
     * Convert a {@link Link} into a {@link Link.JaxbLink}.
     *
     * @param v instance of type {@link Link}.
     * @return mapped instance of type {@link Link.JaxbLink}.
     */
    @Override
    public Link.JaxbLink marshal(Link v) {
        if (v == null) {
           return null;
        }

        Link.JaxbLink jl = new Link.JaxbLink(v.getUri());
        for (Map.Entry<String, String> e : v.getParams().entrySet()) {
            final String name = e.getKey();
            jl.getParams().put(new QName("", name), e.getValue());
        }
        return jl;
    }
}
