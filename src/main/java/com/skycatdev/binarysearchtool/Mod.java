package com.skycatdev.binarysearchtool;

/**
 *
 * @param id The modid of the mod.
 * @param dependencies The dependencies listed by the mod.
 * @param filename The name of the mod file, not including extensions.
 */
public record Mod(String id, String[] dependencies, String filename) {
}
