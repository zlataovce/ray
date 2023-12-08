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

package me.kcra.ray.bukkit;

import me.kcra.ray.TransformerRegistry;
import me.kcra.ray.bukkit.plugin.BukkitPluginDescription;
import me.kcra.ray.plugin.PluginDescription;
import me.kcra.ray.transform.Transformer;
import org.bukkit.Bukkit;
import org.bukkit.UnsafeValues;
import org.bukkit.plugin.PluginDescriptionFile;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@SuppressWarnings("deprecation") // UnsafeValues is ok
public class BukkitTransformerRegistry implements TransformerRegistry, Transformer {
    private static final Object UNSAFE;
    private static final Class<?> CMN_CLASS; // CraftMagicNumbers
    private static final Field INSTANCE_FIELD; // CraftMagicNumbers.INSTANCE
    private final List<Transformer> transformers = new ArrayList<>();

    static {
        Object theUnsafe = null;
        try {
            // test if it has everything we need
            sun.misc.Unsafe.class.getDeclaredMethod("putObject", Object.class, long.class, Object.class);
            sun.misc.Unsafe.class.getDeclaredMethod("staticFieldBase", Field.class);
            sun.misc.Unsafe.class.getDeclaredMethod("staticFieldOffset", Field.class);

            final Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);

            theUnsafe = unsafeField.get(null);
        } catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException ignored) {
        }
        UNSAFE = theUnsafe;

        CMN_CLASS = Bukkit.getUnsafe().getClass();

        Field instanceField = null;
        try {
            instanceField = CMN_CLASS.getDeclaredField("INSTANCE");
        } catch (NoSuchFieldException ignored) {
        }
        INSTANCE_FIELD = instanceField;
    }

    public BukkitTransformerRegistry() {
        if (INSTANCE_FIELD == null) {
            throw new UnsupportedOperationException("CraftMagicNumbers#INSTANCE not found");
        }

        final Object proxy = Proxy.newProxyInstance(
                CMN_CLASS.getClassLoader(),
                new Class<?>[]{UnsafeValues.class},
                new ProxyHandler(this, Bukkit.getUnsafe())
        );

        try {
            if ((INSTANCE_FIELD.getModifiers() & Modifier.FINAL) != 0) {
                if (UNSAFE == null) {
                    throw new UnsupportedOperationException("sun.misc.Unsafe is not available");
                }

                final sun.misc.Unsafe theUnsafe = (sun.misc.Unsafe) UNSAFE;
                theUnsafe.putObject(
                        theUnsafe.staticFieldBase(INSTANCE_FIELD),
                        theUnsafe.staticFieldOffset(INSTANCE_FIELD),
                        proxy
                );
            } else {
                INSTANCE_FIELD.setAccessible(true);
                INSTANCE_FIELD.set(null, proxy);
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to inject UnsafeValues proxy", e);
        }
    }

    @Override
    public synchronized void clear() {
        this.transformers.clear();
    }

    @Override
    public synchronized void register(@NotNull Transformer transformer) {
        this.transformers.add(transformer);
    }

    @Override
    public synchronized void registerAll(@NotNull Iterable<Transformer> transformers) {
        for (final Transformer transformer : transformers) {
            this.transformers.add(transformer);
        }
    }

    @Override
    public synchronized byte @NotNull [] transform(@NotNull PluginDescription desc, @NotNull String path, byte @NotNull [] data) {
        for (final Transformer transformer : this.transformers) {
            data = transformer.transform(desc, path, data);
        }

        return data;
    }

    @Override
    public @NotNull Iterator<Transformer> iterator() {
        return this.transformers.iterator();
    }

    public static class ProxyHandler implements InvocationHandler {
        protected final Transformer transformer;
        protected final UnsafeValues delegate;

        public ProxyHandler(@NotNull Transformer transformer, @NotNull UnsafeValues delegate) {
            this.transformer = transformer;
            this.delegate = delegate;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            final Object result = method.invoke(this.delegate, args);
            if (method.getName().equals("processClass") && args.length == 3) {
                final String path = (String) args[1];

                if (!path.startsWith("me/kcra/ray")) { // don't transform classes of this plugin
                    return this.transformer.transform(
                            new BukkitPluginDescription((PluginDescriptionFile) args[0]),
                            path,
                            (byte[]) result
                    );
                }
            }

            return result;
        }
    }
}
