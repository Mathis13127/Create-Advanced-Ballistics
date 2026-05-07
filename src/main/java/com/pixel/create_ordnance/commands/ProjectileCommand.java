package com.pixel.create_ordnance.commands;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import com.pixel.create_ordnance.CreateOrdnance;
import com.pixel.create_ordnance.api.cgs.CGSType;
import com.pixel.create_ordnance.api.cgs.ICGSController;
import com.pixel.create_ordnance.content.entity.ProjectileEntity;
import com.pixel.create_ordnance.mechanics.cgs.CGSManager;
import com.pixel.create_ordnance.mechanics.cgs.CommandCGSController;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;

/**
 * Command handler for {@code /prj-lib projectile cgs}.
 * <p>
 * Provides debug-level control over projectile thrust vectoring via the CGS API.
 * Values are <b>held</b> until explicitly cleared.
 * <p>
 * <b>Syntax:</b>
 * <pre>
 * /prj-lib projectile cgs &lt;targets&gt; pitch &lt;-1.0..1.0&gt;
 * /prj-lib projectile cgs &lt;targets&gt; yaw   &lt;-1.0..1.0&gt;
 * /prj-lib projectile cgs &lt;targets&gt; roll  &lt;-1.0..1.0&gt;
 * /prj-lib projectile cgs &lt;targets&gt; thrust &lt;0..100&gt;
 * /prj-lib projectile cgs &lt;targets&gt; clear
 * /prj-lib projectile cgs &lt;targets&gt; get
 * </pre>
 */
public class ProjectileCommand {

    public static ArgumentBuilder<CommandSourceStack, ?> register() {
        return Commands.literal("projectile")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("cgs")
                        .then(Commands.argument("targets", EntityArgument.entities())
                                .then(registerCgsProperty("pitch"))
                                .then(registerCgsProperty("yaw"))
                                .then(registerCgsProperty("roll"))
                                .then(registerCgsProperty("thrust"))
                                .then(Commands.literal("clear")
                                        .executes(ProjectileCommand::runCgsClear))
                                .then(Commands.literal("get")
                                        .executes(ProjectileCommand::runCgsGet))));
    }

    // =========================================================
    // PROPERTY SET
    // =========================================================

    private static ArgumentBuilder<CommandSourceStack, ?> registerCgsProperty(String property) {
        // pitch/yaw/roll: -1.0 to 1.0 (proportion of maxNozzleTilt)
        // thrust: 0 to 100 (percentage)
        float min = property.equals("thrust") ? 0.0f : -1.0f;
        float max = property.equals("thrust") ? 100.0f : 1.0f;

        return Commands.literal(property)
                .then(Commands.argument("value", FloatArgumentType.floatArg(min, max))
                        .executes(ctx -> runCgsSet(ctx, property)));
    }

    private static int runCgsSet(CommandContext<CommandSourceStack> ctx, String property)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        float value = FloatArgumentType.getFloat(ctx, "value");
        var targets = EntityArgument.getEntities(ctx, "targets");
        int count = 0;

        for (var entity : targets) {
            if (entity instanceof ProjectileEntity projectile) {
                CommandCGSController controller = getOrCreateController(projectile);
                controller.setProperty(property, value);
                count++;

                CreateOrdnance.LOGGER.info(
                        "[CGS Command] {} = {} on entity {} (UUID: {})",
                        property, value, projectile.getId(), projectile.getUUID());
            }
        }

        if (count == 0) {
            ctx.getSource().sendFailure(Component.literal(
                    "\u00a7c[CGS] No ProjectileEntity found in targets."));
            return 0;
        }

        String unit = property.equals("thrust") ? "%" : "";
        String msg = String.format(
                "\u00a7a[CGS] \u00a7e%s\u00a7a = \u00a7e%.2f%s\u00a7a on \u00a7e%d\u00a7a projectile(s). \u00a77[HELD until clear]",
                property, value, unit, count);
        ctx.getSource().sendSuccess(() -> Component.literal(msg), true);
        return count;
    }

    // =========================================================
    // CLEAR
    // =========================================================

    private static int runCgsClear(CommandContext<CommandSourceStack> ctx)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var targets = EntityArgument.getEntities(ctx, "targets");
        int count = 0;

        for (var entity : targets) {
            if (entity instanceof ProjectileEntity projectile) {
                CGSManager manager = CGSManager.getInstance();
                ICGSController existing = manager.getController(projectile.getUUID(), CGSType.THRUST_VECTOR);

                if (existing instanceof CommandCGSController cmd) {
                    cmd.clear();
                    count++;

                    CreateOrdnance.LOGGER.info(
                            "[CGS Command] Cleared demands on entity {} (UUID: {})",
                            projectile.getId(), projectile.getUUID());
                }
            }
        }

        if (count == 0) {
            ctx.getSource().sendFailure(Component.literal(
                    "\u00a7c[CGS] No active command controller found on targets."));
            return 0;
        }

        String msg = String.format(
                "\u00a7a[CGS] Cleared all demands on \u00a7e%d\u00a7a projectile(s). \u00a77[NEUTRAL: 0/0/0/100%%]",
                count);
        ctx.getSource().sendSuccess(() -> Component.literal(msg), true);
        return count;
    }

    // =========================================================
    // GET (read current state)
    // =========================================================

    private static int runCgsGet(CommandContext<CommandSourceStack> ctx)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var targets = EntityArgument.getEntities(ctx, "targets");
        int count = 0;

        for (var entity : targets) {
            if (entity instanceof ProjectileEntity projectile) {
                CGSManager manager = CGSManager.getInstance();
                ICGSController existing = manager.getController(projectile.getUUID(), CGSType.THRUST_VECTOR);

                // Read ACTUAL entity data (what physics actually sees)
                float actualPitch = projectile.getPitchDemand();
                float actualYaw = projectile.getYawDemand();
                float actualRoll = projectile.getRollDemand();
                float actualThrottle = projectile.getThrottle();
                double maxTilt = projectile.getMaxNozzleTilt();

                String controllerInfo;
                if (existing instanceof CommandCGSController cmd) {
                    controllerInfo = cmd.hasActiveCommand()
                            ? String.format("\u00a7aACTIVE \u00a77(cmd: P=%.2f Y=%.2f R=%.2f T=%.0f%%)",
                                    cmd.getPitch(), cmd.getYaw(), cmd.getRoll(), cmd.getThrottleValue() * 100)
                            : "\u00a77INACTIVE (cleared)";
                } else if (existing != null) {
                    controllerInfo = "\u00a7d" + existing.getId() + " \u00a77(non-command)";
                } else {
                    controllerInfo = "\u00a78NONE";
                }

                String msg = String.format(
                        "\u00a76[CGS] Entity #%d:\n"
                                + "\u00a77  Controller: %s\n"
                                + "\u00a77  Entity demands: P=\u00a7e%.3f\u00a77 Y=\u00a7e%.3f\u00a77 R=\u00a7e%.3f\u00a77 T=\u00a7e%.0f%%\n"
                                + "\u00a77  maxNozzleTilt: \u00a7e%.1f\u00a77\u00b0",
                        projectile.getId(), controllerInfo,
                        actualPitch, actualYaw, actualRoll, actualThrottle * 100,
                        maxTilt);
                ctx.getSource().sendSuccess(() -> Component.literal(msg), false);
                count++;
            }
        }

        if (count == 0) {
            ctx.getSource().sendFailure(Component.literal(
                    "\u00a7c[CGS] No ProjectileEntity found in targets."));
        }
        return count;
    }

    // =========================================================
    // HELPER
    // =========================================================

    private static CommandCGSController getOrCreateController(ProjectileEntity entity) {
        CGSManager manager = CGSManager.getInstance();
        ICGSController existing = manager.getController(entity.getUUID(), CGSType.THRUST_VECTOR);

        if (existing instanceof CommandCGSController cmd) {
            return cmd;
        }

        // Attach a new CommandCGSController (replaces any non-command controller)
        CommandCGSController controller = new CommandCGSController();
        manager.attach(entity, controller);

        CreateOrdnance.LOGGER.info(
                "[CGS Command] Created new CommandCGSController for entity {} (UUID: {})",
                entity.getId(), entity.getUUID());
        return controller;
    }
}
