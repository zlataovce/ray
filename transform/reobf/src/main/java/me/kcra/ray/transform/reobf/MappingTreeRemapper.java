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

import net.fabricmc.mappingio.tree.MappingTreeView;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.commons.Remapper;

import java.util.Objects;

public class MappingTreeRemapper extends Remapper {
    private final MappingTreeView tree;
    private final int srcId, dstId;

    public MappingTreeRemapper(@NotNull MappingTreeView tree, @NotNull String srcNs, @NotNull String dstNs) {
        this.tree = tree;
        this.srcId = tree.getNamespaceId(srcNs);
        this.dstId = tree.getNamespaceId(dstNs);
    }

    @Override
    public String map(String internalName) {
        return tree.mapClassName(internalName, srcId, dstId);
    }

    @Override
    public String mapMethodName(String owner, String name, String descriptor) {
        if ("<init>".equals(name) || "<clinit>".equals(name)) {
            return name; // don't remap special method names
        }

        final MappingTreeView.ClassMappingView ownerClass = tree.getClass(owner, srcId);
        if (ownerClass == null) {
            return name;
        }

        final MappingTreeView.MethodMappingView method = ownerClass.getMethod(name, descriptor, srcId);
        return method != null ? Objects.requireNonNullElse(method.getName(dstId), name) : name;
    }

    @Override
    public String mapFieldName(String owner, String name, String descriptor) {
        final MappingTreeView.ClassMappingView ownerClass = tree.getClass(owner, srcId);
        if (ownerClass == null) {
            return name;
        }

        final MappingTreeView.FieldMappingView field = ownerClass.getField(name, descriptor, srcId);
        return field != null ? Objects.requireNonNullElse(field.getName(dstId), name) : name;
    }

    @Override
    public String mapRecordComponentName(String owner, String name, String descriptor) {
        return mapFieldName(owner, name, descriptor);
    }
}