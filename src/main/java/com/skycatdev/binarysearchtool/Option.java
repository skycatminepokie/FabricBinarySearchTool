package com.skycatdev.binarysearchtool;

import org.jetbrains.annotations.Nullable;

public record Option(String name, @Nullable Runnable callback) {
}
