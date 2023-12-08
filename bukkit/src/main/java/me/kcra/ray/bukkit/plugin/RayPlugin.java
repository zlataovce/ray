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

package me.kcra.ray.bukkit.plugin;

import me.kcra.ray.TransformerRegistry;
import me.kcra.ray.TransformerRegistryProvider;
import me.kcra.ray.bukkit.BukkitTransformerRegistry;
import me.kcra.ray.common.TransformerLoader;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class RayPlugin extends JavaPlugin {
    private final TransformerRegistry registry = new BukkitTransformerRegistry();
    private final TransformerLoader loader = new TransformerLoader(getClass().getClassLoader());

    public RayPlugin() {
        TransformerRegistryProvider.register(this.registry);

        final Path transformersFolder = this.getDataFolder().toPath().resolve("transformers");
        try {
            Files.createDirectories(transformersFolder);
            this.registry.registerAll(this.loader.loadAll(transformersFolder));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load transformers", e);
        }
    }

    @Override
    public void onDisable() {
        TransformerRegistryProvider.unregister();

        this.registry.clear();
        try {
            this.loader.close();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to close transformer loader", e);
        }
    }
}
