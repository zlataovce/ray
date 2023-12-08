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

package me.kcra.ray;

import me.kcra.ray.transform.Transformer;
import org.jetbrains.annotations.NotNull;

public interface TransformerRegistry extends Iterable<Transformer> {
    static @NotNull TransformerRegistry platform() {
        return TransformerRegistryProvider.get()
                .orElseThrow(() -> new RuntimeException("No registry loaded, unsupported platform?"));
    }

    void clear();
    void register(@NotNull Transformer transformer);
    default void registerAll(@NotNull Iterable<Transformer> transformers) {
        for (final Transformer transformer : transformers) {
            this.register(transformer);
        }
    }
}
