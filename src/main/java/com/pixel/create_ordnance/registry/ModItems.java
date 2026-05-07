package com.pixel.create_ordnance.registry;

import com.pixel.create_ordnance.CreateOrdnance;
import com.pixel.create_ordnance.content.items.OrdnanceDebuggerItem;

import com.simibubi.create.foundation.data.CreateRegistrate;

import com.tterrag.registrate.util.entry.ItemEntry;

/**
 * Deferred item registration for CreateOrdnance.
 *
 * <p>Uses Create's {@link CreateRegistrate Registrate}
 * to register items. Currently holds only the
 * {@link OrdnanceDebuggerItem} used
 * for testing projectile launch and scan.</p>
 */
public class ModItems {

    private static final CreateRegistrate REGISTRATE = CreateOrdnance.REGISTRATE;

    public static final ItemEntry<OrdnanceDebuggerItem> ORDNANCE_DEBUGGER = REGISTRATE
            .item("ordnance_debugger", OrdnanceDebuggerItem::new)
            .register();

    public static void register() {
    }
}
