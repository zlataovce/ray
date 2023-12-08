# ray

proof-of-concept extensible bytecode transformation for a block game

## Technical notes

Currently, the only supported platform is Bukkit.

Spigot already transforms plugins' bytecode in the name of backwards compatibility, so why not take advantage of this
for something more mischievous? In post-flattening Bukkit (`1.13+`), plugins have their bytecode transformed with a
public "API" (`UnsafeValues#processClass`, formerly just hidden in the `Commodore` class). This "API" is implemented by
the `CraftMagicNumbers` singleton, with its instance being stored in its `public static final UnsafeValues INSTANCE`
field. You should already know where this is going.

You can replace the singleton instance with `sun.misc.Unsafe` for your own `UnsafeValues` implementation, which allows
you to transform every plugin class that's loaded after yours. Now, I know that using Java's Unsafe is very bad, which
is why I've also made the [`bukkit-agent`](./bukkit-agent) module, which is a premain Java agent that strips the final
modifier from the field, so it can be accessed and replaced with plain Reflection. Unlimited power!

## Usage

The reference implementation can be found in the [`bukkit`](./bukkit) module, build it with `./gradlew :ray-bukkit:shadowJar`
(output is in `bukkit/build/libs/ray-bukkit-<version>-all.jar`, pay attention to the JAR classifier).

The plugin discovers `me.kcra.ray.transform.Transformer` (from the [`api`](./api) module) services in JARs in the
`plugins/ray/transformers` directory, these are then added to the class transformation pipeline.

A reference implementation for a transformer JAR can be found in the [`transform/reobf`](./transform/reobf) module,
this one reads the `/META-INF/mappings/reobf.tiny` resource, which modern Paper installations bundle to deobfuscate
stacktraces, and reobfuscates Mojang-mapped plugins using it. You can test this with any dev-classified JAR from a
[`paperweight-userdev`](https://github.com/PaperMC/paperweight-test-plugin)-based plugin.

## Licensing

This project is licensed under the [Apache License, Version 2.0](./LICENSE).
