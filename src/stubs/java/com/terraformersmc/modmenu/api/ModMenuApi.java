package com.terraformersmc.modmenu.api;

/** Compile-time copy of the one Mod Menu 11 method used; never packaged. */
public interface ModMenuApi {
    default ConfigScreenFactory<?> getModConfigScreenFactory(){return parent->parent;}
}
