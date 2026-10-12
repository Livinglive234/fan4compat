package com.terraformersmc.modmenu.api;

import net.minecraft.class_437;

/** Compile-time copy of Mod Menu 11's signature, checked against the real jar by configNativeCheck; never packaged. */
@FunctionalInterface
public interface ConfigScreenFactory<S extends class_437> {
    S create(class_437 parent);
}
