package com.pixel.create_ordnance.mixin.compat.vista;

import java.util.List;
import java.util.Set;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import net.neoforged.fml.loading.LoadingModList;

/**
 * Mixin config plugin that prevents Vista-related mixins from loading
 * when Vista is not installed.
 * <p>
 * Without this plugin, the Mixin framework would attempt to load mixin
 * classes that import Vista types (e.g., {@code BroadcastManager},
 * {@code ViewFinderBlockEntity}), causing {@code NoClassDefFoundError}
 * even with {@code required: false} in the mixin JSON.
 * <p>
 * This plugin is loaded very early by the Mixin framework — before mods
 * are initialized. Only {@link LoadingModList} is available at this point.
 * <p>
 * <b>IMPORTANT:</b> This class must NOT import any Vista classes.
 */
public class VistaCompatMixinPlugin implements IMixinConfigPlugin {

    private static boolean vistaPresent = false;

    @Override
    public void onLoad(String mixinPackage) {
        vistaPresent = LoadingModList.get().getModFileById("vista") != null;
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    /**
     * Called for each mixin class before it is loaded.
     * Returns {@code false} to prevent loading if Vista is absent.
     */
    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return vistaPresent;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null; // use default list from JSON
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
