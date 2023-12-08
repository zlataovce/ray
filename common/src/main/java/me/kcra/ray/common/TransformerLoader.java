/*
 * This file is part of ray, licensed under the Apache License, Version 2.0 (the "License").
 *
 * Copyright (c) 2023 Matous Kucera
 *
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.kcra.ray.common;

import me.kcra.ray.transform.Transformer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.io.Closeable;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public class TransformerLoader implements Closeable {
    private final List<URLClassLoader> loaders = Collections.synchronizedList(new ArrayList<>());
    private final ClassLoader loader;

    public TransformerLoader() {
        this(TransformerLoader.class.getClassLoader());
    }

    public TransformerLoader(@NotNull ClassLoader loader) {
        this.loader = Objects.requireNonNull(loader, "loader");
    }

    public @NotNull @Unmodifiable List<Transformer> load(@NotNull Path file) {
        final List<Transformer> transformers = new ArrayList<>();
        try {
            final URLClassLoader loader = new URLClassLoader(new URL[]{file.toUri().toURL()}, this.loader);
            this.loaders.add(loader);

            for (final Transformer transformer : ServiceLoader.load(Transformer.class, loader)) {
                transformers.add(transformer);
            }
        } catch (MalformedURLException ignored) {
        }

        return Collections.unmodifiableList(transformers);
    }

    public @NotNull @Unmodifiable List<Transformer> loadAll(@NotNull Path dir) throws IOException {
        try (final Stream<Path> s = Files.walk(dir)) {
            return s.filter(p -> Files.isRegularFile(p) && p.toString().endsWith(".jar"))
                    .flatMap(p -> load(p).stream())
                    .toList();
        }
    }

    public @NotNull ClassLoader loader() {
        return this.loader;
    }

    @Override
    public void close() throws IOException {
        for (final URLClassLoader loader : this.loaders) {
            loader.close();
        }
    }
}
