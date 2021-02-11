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

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;
import org.openrewrite.maven.internal.MavenPomDownloader;
import org.openrewrite.maven.internal.MavenMetadata;
import org.openrewrite.maven.internal.RawMaven;
import org.openrewrite.maven.internal.RawRepositories;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.maven.tree.Pom;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Callable;

public class InMemoryMavenPomCache implements MavenPomCache {
    private final Map<String, Optional<RawMaven>> pomCache = new HashMap<>();
    private final Map<GroupArtifactRepository, Optional<MavenMetadata>> mavenMetadataCache = new HashMap<>();
    private final Map<Pom.Repository, Optional<Pom.Repository>> normalizedRepositoryUrls = new HashMap<>();
    private final Set<String> unresolvablePoms = new HashSet<>();

    private final CacheResult<RawMaven> UNAVAILABLE_POM = new CacheResult<>(CacheResult.State.Unavailable, null);
    private final CacheResult<MavenMetadata> UNAVAILABLE_METADATA = new CacheResult<>(CacheResult.State.Unavailable, null);
    private final CacheResult<Pom.Repository> UNAVAILABLE_REPOSITORY = new CacheResult<>(CacheResult.State.Unavailable, null);

    public InMemoryMavenPomCache() {
        Metrics.gaugeMapSize("rewrite.maven.cache.size", Tags.of("type", "inmem", "content", "poms"), pomCache);
        Metrics.gaugeMapSize("rewrite.maven.cache.size", Tags.of("type", "inmem", "content", "metadata"), mavenMetadataCache);
        Metrics.gaugeMapSize("rewrite.maven.cache.size", Tags.of("type", "inmem", "content", "repository urls"), normalizedRepositoryUrls);
        fillUnresolvablePoms();
    }

    private void fillUnresolvablePoms() {
        new BufferedReader(new InputStreamReader(MavenPomDownloader.class.getResourceAsStream("/unresolvable.txt"), StandardCharsets.UTF_8))
                .lines()
                .filter(line -> !line.isEmpty())
                .forEach(unresolvablePoms::add);
    }

    @Override
    public CacheResult<MavenMetadata> computeMavenMetadata(URI repo, String groupId, String artifactId, Callable<MavenMetadata> orElseGet) throws Exception {
        GroupArtifactRepository gar = new GroupArtifactRepository(repo, new GroupArtifact(groupId, artifactId));
        Optional<MavenMetadata> rawMavenMetadata = mavenMetadataCache.get(gar);

        //noinspection OptionalAssignedToNull
        if (rawMavenMetadata == null) {
            try {
                MavenMetadata metadata = orElseGet.call();
                mavenMetadataCache.put(gar, Optional.ofNullable(metadata));
                return new CacheResult<>(CacheResult.State.Updated, metadata);
            } catch (Exception e) {
                mavenMetadataCache.put(gar, Optional.empty());
                throw e;
            }
        }

        return rawMavenMetadata
                .map(metadata -> new CacheResult<>(CacheResult.State.Cached, metadata))
                .orElse(UNAVAILABLE_METADATA);
    }

    @Override
    public CacheResult<RawMaven> computeMaven(URI repo, String groupId, String artifactId, String version,
                                              Callable<RawMaven> orElseGet) throws Exception {

        //There are a few exceptional artifacts that will never be resolved by the repositories. This will always
        //result in an Unavailable response from the cache.
        String artifactCoordinates = groupId + ':' + artifactId + ':' + version;
        if (unresolvablePoms.contains(artifactCoordinates)) {
            return UNAVAILABLE_POM;
        }

        String cacheKey = repo.toString() + ":" + artifactCoordinates;
        Optional<RawMaven> rawMaven = pomCache.get(cacheKey);

        //noinspection OptionalAssignedToNull
        if (rawMaven == null) {
            try {
                RawMaven maven = orElseGet.call();
                pomCache.put(cacheKey, Optional.ofNullable(maven));
                return new CacheResult<>(CacheResult.State.Updated, maven);
            } catch (Exception e) {
                pomCache.put(cacheKey, Optional.empty());
                throw e;
            }
        }

        return rawMaven
                .map(pom -> new CacheResult<>(CacheResult.State.Cached, pom))
                .orElse(UNAVAILABLE_POM);
    }

    @Override
    public CacheResult<Pom.Repository> computeRepository(Pom.Repository repository,
                                                                     Callable<Pom.Repository> orElseGet) throws Exception {
        Optional<Pom.Repository> normalizedRepository = normalizedRepositoryUrls.get(repository);

        //noinspection OptionalAssignedToNull
        if (normalizedRepository == null) {
            try {
                Pom.Repository repo = orElseGet.call();
                normalizedRepositoryUrls.put(repository, Optional.ofNullable(repo));
                return new CacheResult<>(CacheResult.State.Updated, repo);
            } catch (Exception e) {
                normalizedRepositoryUrls.put(repository, Optional.empty());
                throw e;
            }
        }

        return normalizedRepository
                .map(pom -> new CacheResult<>(CacheResult.State.Cached, pom))
                .orElse(UNAVAILABLE_REPOSITORY);
    }
}
