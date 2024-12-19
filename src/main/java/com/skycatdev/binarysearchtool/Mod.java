package com.skycatdev.binarysearchtool;

import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

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
    public static final HashMap<String, Function<Set<String>, Set<String>>> DEPENDENCY_OVERRIDES = new HashMap<>();

    static {
        ID_OVERRIDES.put("owo-sentinel", Set.of("owo-sentinel"));
    }

    public Mod(String name, String mainId, Set<String> ids, Set<String> dependencies, String filename) {
        this.name = name;
        this.mainId = mainId;
        this.ids = ID_OVERRIDES.getOrDefault(mainId, ids);
        this.dependencies = DEPENDENCY_OVERRIDES.getOrDefault(mainId, Function.identity()).apply(dependencies);
        this.filename = filename;
    }

    /**
     * Loads dependency overrides from a {@code fabric_loader_dependencies.json} file.
     * @param overrideFile The file containing the overrides. Will do nothing if the file does not exist.
     */
    public static void loadDependencyOverrides(File overrideFile) throws JsonIOException, JsonSyntaxException, IOException {
        if (overrideFile.exists()) {
            DEPENDENCY_OVERRIDES.clear();
            try (FileReader fileReader = new FileReader(overrideFile)) {
                JsonObject depOverrides = JsonParser.parseReader(fileReader).getAsJsonObject();
                if (depOverrides.getAsJsonPrimitive("version").getAsInt() != 1) {
                    throw new IOException("Dependency override version was not 1, we don't know how to parse that!"); // TODO: Better exception type
                }
                JsonObject overrides = depOverrides.getAsJsonObject("overrides");
                for (String modId : overrides.keySet()) {
                    JsonObject override = overrides.get(modId).getAsJsonObject();
                    if (override.has("depends")) {
                        JsonObject depsJson = override.getAsJsonObject("depends");
                        Set<String> newDeps = new HashSet<>(depsJson.keySet());
                        DEPENDENCY_OVERRIDES.put(modId, (originalDeps) -> newDeps);
                    } else {
                        Function<Set<String>, Set<String>> function = Function.identity();
                        if (override.has("+depends")) {
                            JsonObject plusDepsJson = override.getAsJsonObject("+depends");
                            function = function.andThen((originalDeps) -> {
                                originalDeps.addAll(plusDepsJson.keySet());
                                return originalDeps;
                            });
                        }
                        if (override.has("-depends")) {
                            JsonObject minusDepsJson = override.getAsJsonObject("-depends");
                            function = function.andThen((originalDeps) -> {
                                originalDeps.removeAll(minusDepsJson.keySet());
                                return originalDeps;
                            });
                        }
                        DEPENDENCY_OVERRIDES.put(modId, function);
                    }
                }
            } catch (FileNotFoundException e) {
                // TODO
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Mod otherMod) {
            return name.equals(otherMod.name) &&
                   mainId.equals(otherMod.mainId) &&
                   ids.equals(otherMod.ids) &&
                   dependencies.equals(otherMod.dependencies) &&
                   filename.equals(otherMod.filename);
        }
        return false;
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
