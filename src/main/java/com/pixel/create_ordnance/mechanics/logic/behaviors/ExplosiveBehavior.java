package com.pixel.create_ordnance.mechanics.logic.behaviors;

import com.pixel.create_ordnance.mechanics.logic.LogicContext;
import com.pixel.create_ordnance.mechanics.logic.ProjectileBehavior;
import com.pixel.create_ordnance.mechanics.logic.ProjectileLogic;

/**
 * Behavior for explosive payloads.
 * On trigger, creates an explosion at the component's position.
 *
 * <p>Reads the {@code "explosion_power"} parameter from the {@link LogicContext}.
 */
public class ExplosiveBehavior implements ProjectileBehavior {

    @Override
    public void onTrigger(LogicContext ctx) {
        if (ctx.level() == null || ctx.position() == null)
            return;

        double power = ctx.getDouble("explosion_power");
        ProjectileLogic.explode(ctx.level(), ctx.projectile(), ctx.position(), (float) power);
    }

    @Override
    public String getBehaviorId() {
        return "explosive";
    }
}
