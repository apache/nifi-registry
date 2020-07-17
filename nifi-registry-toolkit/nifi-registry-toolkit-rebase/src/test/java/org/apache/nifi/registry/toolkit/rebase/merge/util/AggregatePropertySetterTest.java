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
import org.apache.nifi.registry.flow.diff.DifferenceType;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class AggregatePropertySetterTest {
    private DifferenceType differenceType;
    private PropertySetter<VersionedComponent, String> propertySetter1;
    private PropertySetter<VersionedComponent, String> propertySetter2;
    private AggregatePropertySetter<String> aggregatePropertySetter;

    @SuppressWarnings("unchecked")
    @Before
    public void setup() {
        propertySetter1 = mock(PropertySetter.class);
        propertySetter2 = mock(PropertySetter.class);
        differenceType = DifferenceType.COMMENTS_CHANGED;
        aggregatePropertySetter = new AggregatePropertySetter<>(differenceType, Arrays.asList(propertySetter1, propertySetter2));
    }

    @Test
    public void testAggregatePropertySetterFirstSetter() {
        VersionedComponent component = mock(VersionedComponent.class);
        String testString = "test-string";

        when(propertySetter1.accepts(component)).thenReturn(true);

        aggregatePropertySetter.accept(component, testString);
        verify(propertySetter1).accept(component, testString);
        verifyZeroInteractions(propertySetter2);
    }

    @Test
    public void testAggregatePropertySetterSecondSetter() {
        VersionedComponent component = mock(VersionedComponent.class);
        String testString = "test-string";

        when(propertySetter1.accepts(component)).thenReturn(false);
        when(propertySetter2.accepts(component)).thenReturn(true);

        aggregatePropertySetter.accept(component, testString);
        verify(propertySetter1).accepts(component);
        verify(propertySetter2).accept(component, testString);
        verifyNoMoreInteractions(propertySetter1);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testAggregatePropertySetterIncompatibleSetters() {
        VersionedComponent component = mock(VersionedComponent.class);
        String testString = "test-string";

        when(propertySetter1.accepts(component)).thenReturn(false);
        when(propertySetter2.accepts(component)).thenReturn(false);

        aggregatePropertySetter.accept(component, testString);
    }
}
