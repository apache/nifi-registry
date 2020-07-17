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
package org.apache.nifi.registry.toolkit.rebase.merge.util;

import org.apache.nifi.registry.flow.VersionedComponent;
import org.apache.nifi.registry.flow.VersionedConnection;
import org.apache.nifi.registry.flow.VersionedProcessor;
import org.apache.nifi.registry.flow.diff.DifferenceType;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

public class PropertySetterTest {

    @Test
    public void testStringSetter() {
        BiConsumer<VersionedComponent, String> setter = mock(BiConsumer.class);

        PropertySetter<VersionedComponent, String> propertySetter = PropertySetter.createStringSetter(DifferenceType.COMMENTS_CHANGED, VersionedComponent.class, setter);

        VersionedComponent component = mock(VersionedComponent.class);
        String testString = "test-string";

        assertTrue(propertySetter.accepts(component));

        propertySetter.accept(component, testString);

        verify(setter).accept(component, testString);
    }

    @Test
    public void testStringSetterWrongType() {
        BiConsumer<VersionedConnection, String> setter = mock(BiConsumer.class);

        PropertySetter<VersionedConnection, String> propertySetter = PropertySetter.createStringSetter(DifferenceType.COMMENTS_CHANGED, VersionedConnection.class, setter);

        VersionedComponent component = mock(VersionedComponent.class);
        String testString = "test-string";

        assertFalse(propertySetter.accepts(component));

        try {
            propertySetter.accept(component, testString);
            fail("Expected unsupported operation exception.");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        verifyZeroInteractions(setter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testStringSetterIncompatibleValue() {
        BiConsumer<VersionedComponent, String> setter = mock(BiConsumer.class);

        PropertySetter<VersionedComponent, String> propertySetter = PropertySetter.createStringSetter(DifferenceType.COMMENTS_CHANGED, VersionedComponent.class, setter);

        VersionedComponent component = mock(VersionedComponent.class);

        assertTrue(propertySetter.accepts(component));

        propertySetter.accept(component, 1);
    }

    @Test
    public void testStringSetterNull() {
        BiConsumer<VersionedComponent, String> setter = mock(BiConsumer.class);

        PropertySetter<VersionedComponent, String> propertySetter = PropertySetter.createStringSetter(DifferenceType.COMMENTS_CHANGED, VersionedComponent.class, setter);

        VersionedComponent component = mock(VersionedComponent.class);

        assertTrue(propertySetter.accepts(component));

        propertySetter.accept(component, null);

        verify(setter).accept(component, null);
    }

    @Test
    public void testNumberConversion() {
        BiConsumer<VersionedProcessor, Long> setter = mock(BiConsumer.class);

        PropertySetter<VersionedProcessor, Long> propertySetter = PropertySetter.createNumberSetter(DifferenceType.RUN_DURATION_CHANGED, VersionedProcessor.class, setter, Number::longValue);

        VersionedProcessor component = mock(VersionedProcessor.class);

        assertTrue(propertySetter.accepts(component));

        propertySetter.accept(component, 1F);

        verify(setter).accept(eq(component), eq(Long.valueOf(1)));
    }

    @Test
    public void testStringCollectionConversion() {
        BiConsumer<VersionedConnection, List<String>> setter = mock(BiConsumer.class);

        PropertySetter<VersionedConnection, List<String>> propertySetter = PropertySetter.createStringCollectionSetter(DifferenceType.PRIORITIZERS_CHANGED,
                VersionedConnection.class, setter, Collectors.toList());

        List<String> strings = new ArrayList<>(Arrays.asList("1", "2", "3"));
        Set<String> prioritizers = new LinkedHashSet<>(strings);

        VersionedConnection component = mock(VersionedConnection.class);

        assertTrue(propertySetter.accepts(component));

        propertySetter.accept(component, prioritizers);

        verify(setter).accept(eq(component), eq(strings));
    }
}
