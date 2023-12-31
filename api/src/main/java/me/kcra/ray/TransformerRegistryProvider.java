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

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

@ApiStatus.Internal
public final class TransformerRegistryProvider {
    private static TransformerRegistry INSTANCE = null;

    public static @NotNull Optional<TransformerRegistry> get() {
        return Optional.ofNullable(INSTANCE);
    }

    public static void register(@NotNull TransformerRegistry registry) {
        INSTANCE = Objects.requireNonNull(registry, "registry");
    }

    public static void unregister() {
        INSTANCE = null;
    }
}
