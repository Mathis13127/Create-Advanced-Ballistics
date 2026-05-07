package com.pixel.create_ordnance.content.items;

import java.util.List;

import net.createmod.catnip.gui.element.ScreenElement;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Interface for tool modes used by Ordnance debug items.
 *
 * <p>Implementations provide a display name, icon, and description list
 * shown in the Create-style radial menu when the player scrolls modes.</p>
 *
 * @see TransformerMode
 * @see OrdnanceDebuggerItem
 */
public interface IOrdnanceToolMode {
    MutableComponent getDisplayName();

    ScreenElement getIcon();

    List<Component> getDescription();
}