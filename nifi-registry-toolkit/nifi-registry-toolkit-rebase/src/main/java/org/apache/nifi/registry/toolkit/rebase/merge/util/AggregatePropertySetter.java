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

import org.apache.commons.lang3.tuple.Pair;
import org.apache.nifi.registry.flow.VersionedComponent;
import org.apache.nifi.registry.flow.diff.DifferenceType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Setter for a DifferenceType that could apply to several distinct component types.
 */
public class AggregatePropertySetter<U> implements BiConsumer<VersionedComponent, Object> {
    private final DifferenceType differenceType;
    private final List<PropertySetter<? extends VersionedComponent, U>> delegates;

    public AggregatePropertySetter(DifferenceType differenceType, List<PropertySetter<? extends VersionedComponent, U>> delegates) {
        this.differenceType = differenceType;
        this.delegates = delegates;
    }

    @Override
    public void accept(VersionedComponent versionedComponent, Object o) {
        for (PropertySetter delegate : delegates) {
            if (delegate.accepts(versionedComponent)) {
                delegate.accept(versionedComponent, o);
                return;
            }
        }

        throw new UnsupportedOperationException("Cannot handle " + differenceType + " for component " + versionedComponent);
    }

    public Pair<DifferenceType, BiConsumer<VersionedComponent, Object>> toPair() {
        return Pair.of(differenceType, this);
    }

    public static class Builder<U> {
        private final DifferenceType differenceType;
        private final Function<Object, U> argConverter;
        private final List<PropertySetter<? extends VersionedComponent, U>> delegates;

        public static Builder<String> createStringBuilder(DifferenceType differenceType) {
            return new Builder<>(differenceType, String.class::cast);
        }

        public static <U extends Number> Builder<U> createNumberBuilder(DifferenceType differenceType, Function<Number, U> numberConverter) {
            return new Builder<>(differenceType, o -> numberConverter.apply((Number) o));
        }

        public Builder(DifferenceType differenceType, Function<Object, U> argConverter) {
            this.differenceType = differenceType;
            this.argConverter = argConverter;
            this.delegates = new ArrayList<>();
        }

        public <T extends VersionedComponent> Builder<U> addDelegate(Class<T> accepts, BiConsumer<T, U> consumer) {
            delegates.add(new PropertySetter<>(differenceType, accepts, argConverter, consumer));
            return this;
        }

        public AggregatePropertySetter<U> build() {
            return new AggregatePropertySetter<>(differenceType, Collections.unmodifiableList(new ArrayList<>(delegates)));
        }
    }
}
