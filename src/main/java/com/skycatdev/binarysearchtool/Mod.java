package com.skycatdev.binarysearchtool;

import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Set;

/**
 *
 * @param id The modid of the mod.
 * @param dependencies The dependencies listed by the mod.
 * @param filename The name of the mod file, not including extensions.
 */
public record Mod(@Nullable String name, Set<String> ids, Set<String> dependencies, String filename) {
    public boolean tryDisable(Path modFolder) {
        return modFolder.resolve(filename + ".jar").toFile().renameTo(modFolder.resolve(filename + ".jar.disabled").toFile());
    }

    public boolean tryEnable(Path modFolder) {
        return modFolder.resolve(filename + ".jar.disabled").toFile().renameTo(modFolder.resolve(filename + ".jar").toFile());
    }
}
