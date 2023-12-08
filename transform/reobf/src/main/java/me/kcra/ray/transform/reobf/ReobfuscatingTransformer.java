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

package me.kcra.ray.transform.reobf;

import me.kcra.ray.plugin.PluginDescription;
import me.kcra.ray.transform.Transformer;
import net.fabricmc.mappingio.MappedElementKind;
import net.fabricmc.mappingio.adapter.MappingSourceNsSwitch;
import net.fabricmc.mappingio.format.tiny.Tiny2FileReader;
import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import net.fabricmc.mappingio.tree.VisitableMappingTree;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ReobfuscatingTransformer implements Transformer {
    public static final String MOJANG_PLUS_YARN_NAMESPACE = "mojang+yarn";
    public static final String SPIGOT_NAMESPACE = "spigot";

    private final Remapper remapper;

    public ReobfuscatingTransformer() {
        Class<?> bukkitClass;
        try {
            bukkitClass = Class.forName("org.bukkit.Bukkit");
        } catch (ClassNotFoundException ignored) {
            throw new UnsupportedOperationException("Could not find org.bukkit.Bukkit");
        }

        final InputStream is = bukkitClass.getResourceAsStream("/META-INF/mappings/reobf.tiny");
        if (is == null) {
            throw new UnsupportedOperationException("Could not find embedded mapping file");
        }

        final MemoryMappingTree tree = new MemoryMappingTree();
        try {
            Tiny2FileReader.read(
                    new InputStreamReader(is, StandardCharsets.UTF_8),
                    new MappingSourceNsSwitch(tree, SPIGOT_NAMESPACE)
            );
            is.close();

            this.populateHierarchy(tree, MOJANG_PLUS_YARN_NAMESPACE);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        this.remapper = new MappingTreeRemapper(tree, MOJANG_PLUS_YARN_NAMESPACE, SPIGOT_NAMESPACE);
    }

    public ReobfuscatingTransformer(@NotNull Remapper remapper) {
        this.remapper = remapper;
    }

    @Override
    public byte @NotNull [] transform(@NotNull PluginDescription desc, @NotNull String path, byte @NotNull [] data) {
        final ClassReader r = new ClassReader(data);
        final ClassWriter w = new ClassWriter(r, 0);

        r.accept(new ClassRemapper(w, this.remapper), 0);
        return w.toByteArray();
    }

    protected void populateHierarchy(@NotNull VisitableMappingTree tree, @NotNull String namespace) throws IOException {
        final int nsId = tree.getNamespaceId(namespace);
        if (nsId == MappingTree.NULL_NAMESPACE_ID) {
            throw new IllegalArgumentException("Namespace not found");
        }

        if (tree.visitHeader()) {
            tree.visitNamespaces(tree.getSrcNamespace(), List.of(namespace));
        }

        if (tree.visitContent()) {
            do {
                final Set<Class<?>> processed = new HashSet<>();
                for (final MappingTree.ClassMapping clm : List.copyOf(tree.getClasses())) {
                    try {
                        final Class<?> clazz = Class.forName(
                                fromInternalName(clm.getSrcName()),
                                false,
                                this.getClass().getClassLoader()
                        );
                        if (processed.contains(clazz)) continue;

                        final List<Class<?>> superTypes = collectSuperTypes(clazz);
                        if (superTypes.size() <= 1) { // has no super types
                            processed.add(clazz);
                            continue;
                        }

                        final Map<NameDescriptorPair, String> mapped = new HashMap<>();
                        for (final Class<?> superType : superTypes) {
                            final String superName = toInternalName(superType.getName());
                            final MappingTree.ClassMapping superClm = tree.getClass(superName);
                            if (superClm == null) continue;

                            if (processed.add(superType) && tree.visitClass(superName)) {
                                for (final Map.Entry<NameDescriptorPair, String> entry : mapped.entrySet()) {
                                    final NameDescriptorPair pair = entry.getKey();
                                    if (tree.visitMethod(pair.name(), pair.desc())) {
                                        final String mappedName = entry.getValue();
                                        if (mappedName != null) {
                                            tree.visitDstName(MappedElementKind.METHOD, 0, mappedName);
                                        }
                                    }
                                }
                            }

                            for (final Method method : superType.getDeclaredMethods()) {
                                if (
                                        (method.getModifiers() & Modifier.PRIVATE) != 0
                                                || (method.getModifiers() & Modifier.FINAL) != 0
                                                || (method.getModifiers() & Modifier.STATIC) != 0
                                ) {
                                    continue; // exclude private, final and static members
                                }

                                final String methodName = toInternalName(method.getName());
                                final String methodDesc = Type.getMethodDescriptor(method);

                                final MappingTree.MethodMapping mtm = superClm.getMethod(methodName, methodDesc);
                                if (mtm == null) continue;

                                mapped.computeIfAbsent(new NameDescriptorPair(methodName, methodDesc), (k) -> mtm.getDstName(nsId));
                            }
                        }
                    } catch (ClassNotFoundException ignored) {
                    }
                }
            } while (!tree.visitEnd());
        }
    }

    protected List<Class<?>> collectSuperTypes(Class<?> clazz) {
        final List<Class<?>> superTypes = new ArrayList<>();

        do {
            superTypes.add(clazz);
        } while ((clazz = clazz.getSuperclass()) != null && clazz != Object.class);
        Collections.reverse(superTypes); // highest parent goes first

        return superTypes;
    }

    private static String toInternalName(@NotNull String s) {
        return s.replace('.', '/');
    }

    private static String fromInternalName(@NotNull String s) {
        return s.replace('/', '.');
    }

    private record NameDescriptorPair(@NotNull String name, @NotNull String desc) {
    }
}
