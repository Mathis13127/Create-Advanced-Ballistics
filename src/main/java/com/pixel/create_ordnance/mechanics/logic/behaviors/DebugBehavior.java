package com.pixel.create_ordnance.mechanics.logic.behaviors;

import com.pixel.create_ordnance.mechanics.logic.LogicContext;
import com.pixel.create_ordnance.mechanics.logic.ProjectileBehavior;
import com.pixel.create_ordnance.mechanics.logic.ProjectileLogic;

/**
 * Debug behavior that broadcasts messages during each lifecycle phase.
 *
 * <p>Reads parameters:
 * <ul>
 *   <li>{@code "setup_message"} — message shown on setup</li>
 *   <li>{@code "tick_message"} — message shown each tick</li>
 *   <li>{@code "trigger_message"} — message shown on trigger</li>
 * </ul>
 */
public class DebugBehavior implements ProjectileBehavior {

    @Override
    public void onSetup(LogicContext ctx) {
        ProjectileLogic.broadcast(ctx, ctx.getString("setup_message"));
    }

    @Override
    public void onTick(LogicContext ctx) {
        ProjectileLogic.broadcast(ctx, ctx.getString("tick_message"));
    }

    @Override
    public void onTrigger(LogicContext ctx) {
        ProjectileLogic.broadcast(ctx, ctx.getString("trigger_message"));
    }

    @Override
    public String getBehaviorId() {
        return "debug";
    }
}
