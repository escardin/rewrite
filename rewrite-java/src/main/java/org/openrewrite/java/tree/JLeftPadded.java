/*
 * Copyright 2020 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.tree;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.With;
import lombok.experimental.FieldDefaults;
import org.openrewrite.marker.Markable;
import org.openrewrite.marker.Markers;

import java.util.function.Function;

/**
 * A Java element that could have space preceding some delimiter.
 * For example an array dimension could have space before the opening
 * bracket, and the containing {@link #elem} could have a prefix that occurs
 * after the bracket.
 *
 * @param <T>
 */
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Data
public class JLeftPadded<T> implements Markable {
    @With
    Space before;

    @With
    T elem;

    @With
    Markers markers;

    public JLeftPadded<T> map(Function<T, T> map) {
        return withElem(map.apply(elem));
    }

    public enum Location {
        ASSIGNMENT,
        BINARY_OPERATOR,
        EXTENDS,
        FIELD_ACCESS_NAME,
        MEMBER_REFERENCE,
        TERNARY_TRUE,
        TERNARY_FALSE,
        TRY_FINALLY,
        UNARY_OPERATOR,
        VARIABLE_INITIALIZER,
        WHILE_CONDITION
    }
}