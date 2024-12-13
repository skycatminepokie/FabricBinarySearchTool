package com.skycatdev.binarysearchtool;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Set;

/**
 * @param name         The name of the mod, as defined by the fmj. Uses the modid as backup.
 * @param mainId       The modid of the mod
 * @param ids          The modid of the mod and all jijed mods.
 * @param dependencies The dependencies listed by the mod.
 * @param filename     The name of the mod file, not including extensions.
 */
public record Mod(String name, String mainId, Set<String> ids, Set<String> dependencies, String filename) {
    /**
     * Main id -> Full provides set
     */
    public static final HashMap<String, Set<String>> ID_OVERRIDES = new HashMap<>();

    static {
        ID_OVERRIDES.put("owo-sentinel", Set.of("owo-sentinel"));
    }

    public Mod(String name, String mainId, Set<String> ids, Set<String> dependencies, String filename) {
        this.name = name;
        this.mainId = mainId;
        this.ids = ID_OVERRIDES.getOrDefault(mainId, ids);
        this.dependencies = dependencies;
        this.filename = filename;
    }

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
