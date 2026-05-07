package com.pixel.create_ordnance.compat;

import java.util.Optional;
import java.util.function.Supplier;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.LoadingModList;

/**
 * Central registry of optional mod integrations.
 * <p>
 * Pattern borrowed from Create: classes in {@code compat/<modname>/} are
 * <b>never</b> touched by the classloader unless the corresponding mod is
 * present, thanks to the double-{@link Supplier} trick used by
 * {@link #executeIfInstalled} and {@link #runIfInstalled}.
 */
public enum Mods {

    VISTA("vista"),
    AAA_PARTICLES("aaa_particles");

    private final String id;

    Mods(String id) {
        this.id = id;
    }

    /** @return the mod id */
    public String id() {
        return id;
    }

    public ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(id, path);
    }

    /** @return whether this mod is present in the current load */
    public boolean isLoaded() {
        return LoadingModList.get().getModFileById(id) != null;
    }

    /**
     * Run code only if this mod is installed.
     * The double-Supplier ensures the inner class is never loaded
     * (and its imports never resolved) when the mod is absent.
     *
     * @return {@link Optional#empty()} if mod absent, otherwise the result
     */
    public <T> Optional<T> runIfInstalled(Supplier<Supplier<T>> toRun) {
        if (isLoaded())
            return Optional.ofNullable(toRun.get().get());
        return Optional.empty();
    }

    /**
     * Execute code only if this mod is installed.
     * Same classloader-safety as {@link #runIfInstalled}.
     */
    public void executeIfInstalled(Supplier<Runnable> toExecute) {
        if (isLoaded()) {
            toExecute.get().run();
        }
    }
}
