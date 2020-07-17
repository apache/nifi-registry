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

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.nifi.registry.flow.VersionedComponent;
import org.apache.nifi.registry.flow.diff.DifferenceType;

import java.io.IOException;
import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collector;

import static org.apache.nifi.registry.toolkit.rebase.RecursiveRebase.OBJECT_MAPPER;

/**
 * Contains logic to apply a property difference to a VersionedComponent.
 */
public class PropertySetter<T, U> implements BiConsumer<VersionedComponent, Object> {
    private final DifferenceType differenceType;
    private final Class<T> componentType;
    private final Function<Object, U> argConverter;
    private final BiConsumer<T, U> setter;

    public static <T extends VersionedComponent> PropertySetter<T, String> createStringSetter(DifferenceType differenceType, Class<T> componentType, BiConsumer<T, String> setter) {
        return new PropertySetter<>(differenceType, componentType, String.class::cast, setter);
    }

    public static <T extends VersionedComponent, U extends Number> PropertySetter<T, U> createNumberSetter(DifferenceType differenceType, Class<T> componentType, BiConsumer<T, U> setter,
                                                                                                           Function<Number, U> numberConverter) {
        return new PropertySetter<>(differenceType, componentType, o -> numberConverter.apply((Number) o), setter);
    }

    @SuppressWarnings("unchecked")
    public static <T extends VersionedComponent, U extends Collection<String>> PropertySetter<T, U> createStringCollectionSetter(DifferenceType differenceType, Class<T> componentType,
                                                                                                                                 BiConsumer<T, U> setter, Collector<String, ?, U> collector) {
        return new PropertySetter<>(differenceType, componentType, o -> (U) ((Collection) o).stream().map(String.class::cast).collect(collector), setter);
    }

    /**
     * Initial parse doesn't know what type to deserialize since from and to are of type Object, this allows a reparse of the value with known type.
     */
    public static <T, U> PropertySetter<T, U> createReparseSetter(DifferenceType differenceType, Class<T> componentType, TypeReference<U> argType, BiConsumer<T, U> setter) {
        return new PropertySetter<>(differenceType, componentType, o -> {
            try {
                return OBJECT_MAPPER.readValue(OBJECT_MAPPER.writeValueAsString(o), argType);
            } catch (IOException e) {
                throw new RuntimeException("Unable to parse " + o + " as " + argType + ".");
            }
        }, setter);
    }

    /**
     * Initial parse doesn't know what type to deserialize since from and to are of type Object, this allows a reparse of the value with known type.
     */
    public static <T, U> PropertySetter<T, U> createReparseSetter(DifferenceType differenceType, Class<T> componentType, Class<U> argType, BiConsumer<T, U> setter) {
        return new PropertySetter<>(differenceType, componentType, o -> {
            try {
                return OBJECT_MAPPER.readValue(OBJECT_MAPPER.writeValueAsString(o), argType);
            } catch (IOException e) {
                throw new RuntimeException("Unable to parse " + o + " as " + argType + ".");
            }
        }, setter);
    }

    public PropertySetter(DifferenceType differenceType, Class<T> componentType, Function<Object, U> argConverter, BiConsumer<T, U> setter) {
        this.differenceType = differenceType;
        this.componentType = componentType;
        this.argConverter = argConverter;
        this.setter = setter;
    }


    public boolean accepts(VersionedComponent versionedComponent) {
        return componentType.isAssignableFrom(versionedComponent.getClass());
    }

    @SuppressWarnings("unchecked")
    @Override
    public void accept(VersionedComponent t, Object o) {
        if (!accepts(t)) {
            throw new UnsupportedOperationException("Cannot handle " + differenceType + " for component " + t);
        }
        if (o == null) {
            setter.accept((T) t, null);
            return;
        }
        try {
            setter.accept((T) t, argConverter.apply(o));
        } catch (Exception e) {
            throw new IllegalArgumentException(PropertySetter.class.getSimpleName() + " for " + differenceType + " cannot handle argument " + o + " of type " + o.getClass(), e);
        }
    }

    public Pair<DifferenceType, BiConsumer<VersionedComponent, Object>> toPair() {
        return Pair.of(differenceType, this);
    }
}
