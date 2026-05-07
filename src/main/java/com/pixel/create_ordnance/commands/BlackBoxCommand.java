package com.pixel.create_ordnance.commands;

import java.io.File;
import java.io.FileWriter;
import java.util.UUID;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import com.pixel.create_ordnance.config.OrdnanceConfigs;
import com.pixel.create_ordnance.mechanics.physics.logic.BlackBox;
import com.pixel.create_ordnance.mechanics.physics.logic.SimulationEngine;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class BlackBoxCommand {

    public static ArgumentBuilder<CommandSourceStack, ?> register() {
        return Commands.literal("blackbox")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("copy-last-projectile-debugged")
                        .executes(BlackBoxCommand::runExport));
    }

    private static int runExport(CommandContext<CommandSourceStack> ctx) {
        try {
            if (!OrdnanceConfigs.COMMON.divers.debugMode.get()) {
                ctx.getSource().sendFailure(Component.literal("§cDebug Mode is disabled in config."));
                return 0;
            }

            // Get BlackBox from SimulationEngine
            BlackBox blackBox = SimulationEngine
                    .getInstance().getBlackBox();

            if (blackBox.getHistory().isEmpty()) {
                ctx.getSource().sendFailure(
                        Component.literal("§cBlackBox is empty. Fire a projectile first!"));
                return 0;
            }

            // Build JSON
            String fullJson = blackBox.toJson();

            // Write to file: run/logs/projectile_debug/Blackbox_projectile_lastest.json
            File outputDir = new File("logs/projectile_debug");
            outputDir.mkdirs();

            int tickCount = blackBox.getHistory().size();

            File outFile = new File(outputDir, "Blackbox_projectile_lastest.json");
            try (FileWriter fw = new FileWriter(outFile, false)) {
                fw.write(fullJson);
            }
            String path = outFile.getAbsolutePath();
            UUID focus = blackBox.getFocusedUUID();
            String focusStr = (focus != null) ? focus.toString() : "Manual Scan (No Entity)";

            ctx.getSource().sendSuccess(() -> Component.literal(
                    "§a[BlackBox] Exported " + tickCount + " ticks for focus: §e" + focusStr
                            + " §a→ " + path),
                    false);

            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("§cError exporting BlackBox: " + e.getMessage()));
            return 0;
        }
    }
}