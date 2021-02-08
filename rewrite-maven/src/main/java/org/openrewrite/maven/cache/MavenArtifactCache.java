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
package org.openrewrite.maven.cache;

import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.tree.Pom;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Supplier;

public interface MavenArtifactCache {
    MavenArtifactCache NOOP = new MavenArtifactCache() {
        @Override
        public Path getArtifact(Pom.Dependency dependency) {
            return null;
        }

        @Override
        @Nullable
        public Path putArtifact(Pom.Dependency dependency, InputStream is, Consumer<Throwable> onError) {
            try {
                is.close();
            } catch (IOException e) {
                onError.accept(e);
            }
            return null;
        }
    };

    @Nullable
    Path getArtifact(Pom.Dependency dependency);

    @Nullable
    Path putArtifact(Pom.Dependency dependency, InputStream is, Consumer<Throwable> onError);

    @Nullable
    default Path computeArtifact(Pom.Dependency dependency, Callable<@Nullable InputStream> orElseGet,
                                 Consumer<Throwable> onError) {
        Path artifact = getArtifact(dependency);
        if (artifact == null) {
            try (InputStream is = orElseGet.call()) {
                if (is != null) {
                    artifact = putArtifact(dependency, is, onError);
                }
            } catch (Exception e) {
                onError.accept(e);
            }
        }
        return artifact;
    }

    default MavenArtifactCache orElse(MavenArtifactCache other) {
        MavenArtifactCache me = this;
        return new MavenArtifactCache() {
            @Override
            @Nullable
            public Path getArtifact(Pom.Dependency dependency) {
                Path artifact = me.getArtifact(dependency);
                return artifact == null ? other.getArtifact(dependency) : artifact;
            }

            @Override
            @Nullable
            public Path putArtifact(Pom.Dependency dependency, InputStream is, Consumer<Throwable> onError) {
                Path artifact = me.putArtifact(dependency, is, onError);
                return artifact == null ? other.putArtifact(dependency, is, onError) : artifact;
            }
        };
    }
}