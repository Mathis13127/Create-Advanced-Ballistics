package com.pixel.create_ordnance.content.items;

import java.util.List;

import com.simibubi.create.foundation.gui.AllIcons;

import net.createmod.catnip.gui.element.ScreenElement;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Available modes for the {@link OrdnanceDebuggerItem}.
 *
 * <ul>
 *   <li>{@link #LAUNCH} — scan and launch a projectile from the targeted block.</li>
 *   <li>{@link #RESPAWN} — respawn the last projectile at its original position.</li>
 *   <li>{@link #RELAUNCH_RANDOM} — re-launch with randomized velocity jitter.</li>
 *   <li>{@link #REDSTONE_BLOCK} — place/remove a redstone block (for testing triggers).</li>
 *   <li>{@link #DUMB_SCAN} — perform a structural scan without launching.</li>
 * </ul>
 *
 * @see IOrdnanceToolMode
 */
public enum TransformerMode implements IOrdnanceToolMode {

    LAUNCH(AllIcons.I_MTD_REPLAY),
    RESPAWN(AllIcons.I_REFRESH),
    RELAUNCH_RANDOM(AllIcons.I_DICE),
    REDSTONE_BLOCK(AllIcons.I_FX_SURFACE_ON),
    DUMB_SCAN(AllIcons.I_CONFIG_OPEN);

    private final AllIcons icon;

    TransformerMode(AllIcons icon) {
        this.icon = icon;
    }

    @Override
    public MutableComponent getDisplayName() {
        return Component.translatable("gui.create_ordnance.tool.mode." + name().toLowerCase());
    }

    @Override
    public ScreenElement getIcon() {
        return icon;
    }

    @Override
    public List<Component> getDescription() {
        return List.of(
                Component.translatable("gui.create_ordnance.tool.mode." + name().toLowerCase() + ".desc"));
    }
}