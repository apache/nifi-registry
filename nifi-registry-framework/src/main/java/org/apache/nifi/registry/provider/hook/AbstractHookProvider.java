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
package org.apache.nifi.registry.provider.hook;

import org.apache.nifi.registry.hook.Event;
import org.apache.nifi.registry.hook.EventHookException;
import org.apache.nifi.registry.hook.EventHookProvider;
import org.apache.nifi.registry.hook.EventType;
import org.apache.nifi.registry.provider.ProviderConfigurationContext;
import org.apache.nifi.registry.provider.ProviderCreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * An AbstractHookProvider class that serves as a place to keep common Event Hook implementation details.
 */
public abstract class AbstractHookProvider
        implements EventHookProvider {

    static final Logger LOGGER = LoggerFactory.getLogger(AbstractHookProvider.class);

    static final String EVENT_WHITELIST = "Event Whitelist";
    protected List<EventType> whiteListEvents = null;

    @Override
    public void handle(Event event) throws EventHookException {
        // Abstract event handling logic can be implemented here.
    }

    @Override
    public void onConfigured(ProviderConfigurationContext configurationContext) throws ProviderCreationException {

    }

    /**
     * Determines if the Event should be handled or ignored based on the user configured Event Whitelist property
     *
     * @param event
     *  Event that has been passed to the provider by the framework.
     *
     * @return
     *  True if the event should be handled by this provider and false otherwise.
     */
    public boolean handleEvent(Event event) {
        if (!CollectionUtils.isEmpty(whiteListEvents)) {
            if (!whiteListEvents.contains(event.getEventType())) {
                return true;
            }
        }

        return false;
    }
}
