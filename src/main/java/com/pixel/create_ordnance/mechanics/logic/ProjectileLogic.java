package com.pixel.create_ordnance.mechanics.logic;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Utility class providing shared logic methods for projectile behaviors.
 *
 * <p>Contains reusable functions like {@link #broadcast(LogicContext, String)}
 * and {@link #explode(Level, Entity, Vec3, float)} that are called by
 * {@link ProjectileBehavior} implementations.</p>
 *
 * @see ProjectileBehavior
 */
public class ProjectileLogic {

    /**
     * Broadcasts a chat message to nearby players (within 10 blocks).
     * If the projectile entity is a player, sends directly to them instead.
     *
     * @param ctx        the logic context (level, position, entity)
     * @param msgContent the message text to broadcast
     */
    public static void broadcast(LogicContext ctx, String msgContent) {
        if (ctx.level() == null || ctx.position() == null) {
            System.err
                    .println("[ProjectileLogic] WARNING: broadcast() with NULL context. Logic: " + msgContent);
            return;
        }
        Component msg = Component.literal("§6[Ordnance] " + (msgContent != null ? msgContent : "Logic Executed."));
        if (ctx.projectile() instanceof Player player) {
            player.displayClientMessage(msg, false);
        } else {
            ctx.level().players().stream()
                    .filter(p -> p.distanceToSqr(ctx.position()) < 100)
                    .forEach(p -> p.displayClientMessage(msg, false));
        }
    }

    /**
     * Standardized explosion utility that ignores fluid resistance.
     * Use this for all projectile-related explosions to ensure they work in
     * water/lava.
     */
    public static void explode(Level level, Entity source, Vec3 pos, float power) {
        if (level == null || pos == null)
            return;

        level.explode(source, null, ProjectileExplosionCalculator.INSTANCE,
                pos.x, pos.y, pos.z, power, true, Level.ExplosionInteraction.BLOCK);
    }
}
