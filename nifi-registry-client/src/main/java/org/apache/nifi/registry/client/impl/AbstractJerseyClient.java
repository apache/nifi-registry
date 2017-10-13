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
package org.apache.nifi.registry.client.impl;

import org.apache.nifi.registry.client.NiFiRegistryException;

import java.io.IOException;

/**
 * Base class for the client operations to share exception handling.
 */
public class AbstractJerseyClient {

    /**
     * Executes the given action and returns the result.
     *
     * @param action the action to execute
     * @param errorMessage the message to use if a NiFiRegistryException is thrown
     * @param <T> the return type of the action
     * @return the result of the action
     * @throws NiFiRegistryException if any exception other than IOException is encountered
     * @throws IOException if an I/O error occurs communicating with the registry
     */
    protected <T> T executeAction(final String errorMessage, final NiFiRegistryAction<T> action) throws NiFiRegistryException, IOException {
        try {
            return action.execute();
        } catch (final Exception e) {
            final Throwable ioeCause = getIOExceptionCause(e);
            if (ioeCause == null) {
                throw new NiFiRegistryException(errorMessage, e);
            } else {
                throw (IOException) ioeCause;
            }
        }
    }

    /**
     * An action to execute with the given return type.
     *
     * @param <T> the return type of the action
     */
    protected interface NiFiRegistryAction<T> {

        T execute();

    }

    /**
     * @param e an exception that was encountered interacting with the registry
     * @return the IOException that caused this exception, or null if the an IOException did not cause this exception
     */
    protected Throwable getIOExceptionCause(final Throwable e) {
        if (e == null) {
            return null;
        }

        if (e instanceof IOException) {
            return e;
        }

        return getIOExceptionCause(e.getCause());
    }

}
