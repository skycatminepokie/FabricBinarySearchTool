package com.skycatdev.binarysearchtool;

import java.io.File;
import java.nio.file.Path;
import java.util.Set;

/**
 *
 * @param name The name of the mod, as defined by the fmj. Uses the modid as backup.
 * @param ids The modid of the mod and all jijed mods.
 * @param dependencies The dependencies listed by the mod.
 * @param filename The name of the mod file, not including extensions.
 */
public record Mod(String name, Set<String> ids, Set<String> dependencies, String filename) {
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean tryDisable(Path modFolder) {
        File disabledMod = modFolder.resolve(filename + ".jar.disabled").toFile();
        if (disabledMod.exists()) {
            return true; // Already disabled
        }
        return modFolder.resolve(filename + ".jar").toFile().renameTo(disabledMod);
    }

    public boolean tryEnable(Path modFolder) {
        File enabledMod = modFolder.resolve(filename + ".jar").toFile();
        if (enabledMod.exists()) {
            return true; // Already enabled
        }
        return modFolder.resolve(filename + ".jar.disabled").toFile().renameTo(enabledMod);
    }
}
