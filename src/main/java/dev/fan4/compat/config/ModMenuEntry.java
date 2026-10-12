package dev.fan4.compat.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.class_437;

/** Mod Menu's config button. Fabric only loads this entrypoint when Mod Menu asks for it. */
public final class ModMenuEntry implements ModMenuApi {
    public ConfigScreenFactory<?> getModConfigScreenFactory(){return parent->(class_437)ConfigScreen.open(parent);}
}
