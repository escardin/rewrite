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
package org.openrewrite.java.search

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser

interface FindInheritedFieldTest {

    @Test
    fun findInheritedField(jp: JavaParser) {
        val a = """
            import java.util.*;
            public class A {
               protected List list;
               private Set set;
            }
        """

        val b = jp.parse("public class B extends A { }", a)[0]

        assertThat(FindInheritedField.find(b.classes[0], "java.util.List").firstOrNull()?.name)
            .isEqualTo("list")

        // the Set field is not considered to be inherited because it is private
        assertThat(FindInheritedField.find(b.classes[0], "java.util.Set")).isEmpty()
    }

    @Test
    fun findArrayOfType(jp: JavaParser) {
        val a = """
            public class A {
               String[] s;
            }
        """

        val b = jp.parse("public class B extends A { }", a)[0]

        assertThat(FindInheritedField.find(b.classes[0], "java.lang.String").firstOrNull()?.name)
            .isEqualTo("s")

        assertThat(FindInheritedField.find(b.classes[0], "java.util.Set")).isEmpty()
    }
}