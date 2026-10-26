package dev.fan4.compat.chesttracker;

import java.util.*;

/** The screens (by mod) that Chest Tracker's button should not appear on. Plain data, so tests and the real-jar check can read it without Fabric. */
public final class ScreenList {
    private ScreenList() {}
    /** Mod id to the screen classes (by name; each mod's own, not remapped) that cannot hold items in the world. Abstract bases cover their subclasses. */
    public static final Map<String,List<String>> SCREENS=new LinkedHashMap<>();
    static {
        SCREENS.put("etched",List.of("EtchingScreen","JukeboxSettingsScreen","RadioScreen","BoomboxScreen","AlbumCoverScreen").stream().map(n->"gg.moonflower.etched.client.screen."+n).toList());
        SCREENS.put("create",List.of(
            "content.equipment.blueprint.BlueprintScreen","content.logistics.factoryBoard.FactoryPanelSetItemScreen","content.logistics.filter.AbstractFilterScreen",
            "content.logistics.redstoneRequester.RedstoneRequesterScreen","content.logistics.stockTicker.StockKeeperCategoryScreen","content.logistics.stockTicker.StockKeeperRequestScreen",
            "content.redstone.link.controller.LinkedControllerScreen","content.trains.schedule.ScheduleScreen").stream().map(n->"com.simibubi.create."+n).toList());
        SCREENS.put("uncraftingtable76",List.of("net.pitan76.uncraftingtable.client.UncraftingScreen"));
        SCREENS.put("accessories",List.of("AccessoriesScreen","AccessoriesExperimentalScreen").stream().map(n->"io.wispforest.accessories.client.gui."+n).toList());
        SCREENS.put("aether",List.of("AltarScreen","LoreBookScreen").stream().map(n->"com.aetherteam.aether.client.gui.screen.inventory."+n).toList());
        SCREENS.put("pointblank",List.of("com.vicmatskiv.pointblank.client.gui.AttachmentManagerScreen"));
        SCREENS.put("the_bumblezone",List.of("BuzzingBriefcaseScreen","CrystallineFlowerScreen").stream().map(n->"com.telepathicgrunt.the_bumblezone.client.screens."+n).toList());
        SCREENS.put("waystones",List.of("AdminSelectionScreen","ManageWaystoneGroupsScreen","ManageWaystonesScreen","PersonalWaystoneSettingsScreen","SharestoneSelectionScreen",
            "WaystoneEditScreen","WaystoneGroupEditScreen","WaystoneModifierScreen","WaystoneSelectionScreen").stream().map(n->"net.blay09.mods.waystones.client.gui.screen."+n).toList());
    }
}
