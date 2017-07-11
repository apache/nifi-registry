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

import java.io.IOException;
import java.io.PrintWriter;
import java.security.cert.X509Certificate;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthorizedUserFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(AuthorizedUserFilter.class);
    private final AuthorizationProvider provider;

    public AuthorizedUserFilter(final AuthorizationProvider provider) {
        this.provider = provider;
    }

    @Override
    public void init(FilterConfig fc) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        final HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        final HttpServletResponse httpServletResponse = (HttpServletResponse) response;

        if (request.isSecure()) {
            final String dn = getDn(httpServletRequest);

            // if the user has a certificate, extract the dn and see if they can access
            if (dn != null && provider.canAccess(dn)) {
                chain.doFilter(request, response);
            } else {
                // set the response status
                httpServletResponse.setContentType("text/plain");
                httpServletResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);

                // write the response message
                PrintWriter out = httpServletResponse.getWriter();
                out.println("Access is denied.");

                // log the failure
                logger.info(String.format(String.format("User <%s> is not authorized.", dn)));
            }
        } else {
            chain.doFilter(request, response);
        }
    }

    private String getDn(final HttpServletRequest request) {
        X509Certificate[] certs = (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");
        if (certs != null && certs.length > 0) {
            return certs[0].getSubjectDN().getName().trim();
        } else {
            return null;
        }
    }

    @Override
    public void destroy() {
    }

}
