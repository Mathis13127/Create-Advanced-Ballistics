package com.pixel.create_ordnance.foundation.gui;

import java.util.ArrayList;
import java.util.List;

import com.pixel.create_ordnance.config.OrdnanceCommonConfig.ThrustType;
import com.pixel.create_ordnance.config.OrdnanceConfigs;
import com.pixel.create_ordnance.mechanics.physics.core.PhysicsConstants;
import com.pixel.create_ordnance.mechanics.physics.core.PhysicsRegistry;
import com.pixel.create_ordnance.mechanics.scanning.CurrentProjectileManager;
import com.pixel.create_ordnance.mechanics.scanning.ProjectileAnalyzer;
import com.pixel.create_ordnance.mechanics.scanning.ProjectileStats;
import com.pixel.create_ordnance.mechanics.scanning.ScrapResult;

import com.simibubi.create.content.equipment.goggles.GogglesItem;
import com.simibubi.create.foundation.gui.RemovedGuiUtils;
import com.simibubi.create.infrastructure.config.AllConfigs;
import com.simibubi.create.infrastructure.config.CClient;

import net.createmod.catnip.gui.element.BoxElement;
import net.createmod.catnip.theme.Color;

import net.minecraft.ChatFormatting;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;

import net.minecraft.world.level.block.Blocks;

public class GogglesProjectileOverlay {

        public static void renderOverlay(GuiGraphics graphics) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player == null || mc.level == null)
                        return;

                // 1. Check Goggles
                if (!GogglesItem.isWearingGoggles(mc.player))
                        return;

                // 2. Check Active Projectile (Set by OrdnanceClientEvents)
                ScrapResult result = CurrentProjectileManager.getActiveProjectile();

                if (result == null)
                        return;

                // 3. PERFORM ANALYSIS (V3.5)
                ProjectileStats stats = ProjectileAnalyzer
                                .analyze(result);

                // 4. Render Tooltip
                List<Component> tooltip = new ArrayList<>();

                tooltip.add(Component.literal("Projectile Stats").withStyle(ChatFormatting.GOLD));

                // --- PROJECTILE TYPE CLASSIFICATION (V18) ---
                String typeStr = "Unknown";
                ChatFormatting typeColor = ChatFormatting.WHITE;

                if (stats.thrustType() == ThrustType.PROPELLER) {
                        typeStr = "Torpedo";
                        typeColor = ChatFormatting.AQUA;
                } else if (stats
                                .thrustType() == ThrustType.AFTERBURNER) {
                        if (stats.totalFuel() > 0) {
                                typeStr = "Missile";
                                typeColor = ChatFormatting.GOLD;
                        } else {
                                typeStr = "Bomb (Boosted)";
                                typeColor = ChatFormatting.RED;
                        }
                } else if (stats.thrustType() == ThrustType.NONE) {
                        typeStr = "Bomb";
                        typeColor = ChatFormatting.RED;
                }

                tooltip.add(Component.literal("Type: ")
                                .append(Component.literal(typeStr).withStyle(typeColor,
                                                ChatFormatting.BOLD)));
                tooltip.add(Component.literal(""));

                // Basic Stats
                tooltip.add(Component.literal("Mass: ")
                                .append(Component.literal(String.format("%.2f kg", stats.totalMass()))
                                                .withStyle(ChatFormatting.WHITE)));
                tooltip.add(Component.literal("Stability: ")
                                .append(Component.literal(String.format("%.2f", stats.totalStability()))
                                                .withStyle(ChatFormatting.WHITE)));
                tooltip.add(Component.literal("Components: ").append(Component
                                .literal(String.valueOf(result.blockIds().size()))
                                .withStyle(ChatFormatting.WHITE)));

                // Propulsion Stats
                tooltip.add(Component.literal(""));

                // Propulsion Type Styling
                boolean isNone = stats
                                .thrustType() == ThrustType.NONE;
                MutableComponent thrustComp = Component.literal(stats.thrustType().name());
                if (isNone) {
                        thrustComp.withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
                } else {
                        thrustComp.withStyle(ChatFormatting.AQUA);
                }
                tooltip.add(Component.literal("Propulsion: ").append(thrustComp));

                // Fuel Styling
                boolean noFuel = stats.totalFuel() <= 0;
                MutableComponent fuelText = Component
                                .literal(String.format("%.0f kL", stats.totalFuel()));

                if (noFuel) {
                        fuelText.withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
                } else {
                        fuelText.withStyle(ChatFormatting.WHITE);

                        // Flight Time Estimation (V15)
                        if (stats.totalFuelConsumption() > 0) {
                                double totalTicks = stats.totalFuel() / stats.totalFuelConsumption();
                                int totalSeconds = (int) (totalTicks / 20);
                                int minutes = totalSeconds / 60;
                                int seconds = totalSeconds % 60;

                                String timeStr;
                                if (minutes > 0) {
                                        timeStr = String.format(" (~%dm %ds)", minutes, seconds);
                                } else {
                                        timeStr = String.format(" (~%ds)", seconds);
                                }
                                fuelText.append(Component.literal(timeStr)
                                                .withStyle(ChatFormatting.GRAY));
                        }
                }
                tooltip.add(Component.literal("Fuel: ").append(fuelText));

                // Buoyancy Ratio = (Total Volume * Water Density) / Total Weight
                double totalVolume = stats.volumeFront() + stats.volumeBack();
                var waterProfile = PhysicsRegistry
                                .get(Blocks.WATER.defaultBlockState());
                if (waterProfile == null) {
                        throw new IllegalStateException(
                                        "[Ordnance] Water is not registered in PhysicsRegistry! This is a fatal data error.");
                }
                double waterDensity = waterProfile.density();
                double buoyancyMultiplier = PhysicsConstants.getGlobalBuoyancyMultiplier();
                double buoyancyRatio = (stats.totalMass() > 0)
                                ? (totalVolume * waterDensity * buoyancyMultiplier) / stats.totalMass()
                                : 0;

                // Flight Characteristics
                if (!isNone) {
                        // Use pre-computed terminal velocity from the analyzer
                        double estTerminalVel = stats.estimatedTerminalVelocity();

                        tooltip.add(Component.literal("Est. Terminal Velocity: ")
                                        .append(Component.literal(String.format("%.1f b/s", estTerminalVel))
                                                        .withStyle(ChatFormatting.WHITE)));

                        // Weight Ratio = (Effective Thrust) / (Force of Gravity)
                        double gravityStrength = PhysicsConstants.getGravityStrength();
                        double thrustMultiplier = 1.0; // Thrust multiplier will come from ThrustModule
                        double effectiveThrust = stats.baseThrust() * thrustMultiplier;
                        double weightRatio = (stats.totalMass() > 0)
                                        ? (effectiveThrust / (stats.totalMass() * gravityStrength))
                                        : 0;

                        ChatFormatting wrColor = (weightRatio >= 1.0) ? ChatFormatting.GREEN
                                        : ChatFormatting.RED;
                        tooltip.add(Component.literal("Weight Ratio: ")
                                        .append(Component.literal(String.format("%.2f", weightRatio))
                                                        .withStyle(wrColor)));

                        tooltip.add(Component.literal("Max Nozzle Tilt: ").append(Component
                                        .literal(String.format("%.1f", stats.maxNozzleTilt()))
                                        .withStyle(ChatFormatting.WHITE)));
                }

                ChatFormatting brColor = (buoyancyRatio >= 1.0) ? ChatFormatting.GREEN
                                : ChatFormatting.RED;
                tooltip.add(Component.literal("Buoyancy Ratio: ")
                                .append(Component.literal(String.format("%.2f", buoyancyRatio))
                                                .withStyle(brColor)));

                // --- STABILITY STATUS (V16) ---
                tooltip.add(Component.literal(""));
                double margin = stats.centerOfMass().y - stats.centerOfPressure().y;
                MutableComponent stabilityStatus;

                double stableMargin = OrdnanceConfigs.CLIENT.gogglesStats.stabilityThresholds.stableMargin
                                .get();
                double veryStableMargin = OrdnanceConfigs.CLIENT.gogglesStats.stabilityThresholds.veryStableMargin
                                .get();

                if (margin < 0) {
                        stabilityStatus = Component.literal(" [!] UNSTABLE")
                                        .withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
                } else if (margin < stableMargin) {
                        stabilityStatus = Component.literal(" [+] Stable")
                                        .withStyle(ChatFormatting.YELLOW);
                } else if (margin < veryStableMargin) {
                        stabilityStatus = Component.literal(" [++] Very Stable")
                                        .withStyle(ChatFormatting.GREEN);
                } else {
                        stabilityStatus = Component.literal(" [+++] Extremely Stable")
                                        .withStyle(ChatFormatting.DARK_GREEN,
                                                        ChatFormatting.BOLD);
                }

                tooltip.add(Component.literal("Status: ").append(stabilityStatus));

                renderCreateStyleTooltip(graphics, tooltip);
        }

        private static void renderCreateStyleTooltip(GuiGraphics graphics, List<Component> tooltip) {
                Minecraft mc = Minecraft.getInstance();
                int width = graphics.guiWidth();
                int height = graphics.guiHeight();

                CClient cfg = AllConfigs.client();
                int posX = width / 2 + cfg.overlayOffsetX.get();
                int posY = height / 2 + cfg.overlayOffsetY.get();

                // Calculate dimensions
                int tooltipTextWidth = 0;
                for (FormattedText line : tooltip) {
                        int w = mc.font.width(line);
                        if (w > tooltipTextWidth)
                                tooltipTextWidth = w;
                }

                int tooltipHeight = 10 + (tooltip.size() * 10);

                // Clamp to screen
                posX = Math.min(posX, width - tooltipTextWidth - 20);
                posY = Math.min(posY, height - tooltipHeight - 20);

                // Draw Background
                Color colorBackground = BoxElement.COLOR_VANILLA_BACKGROUND.scaleAlpha(.75f);
                Color colorBorderTop = BoxElement.COLOR_VANILLA_BORDER.getFirst().copy();
                Color colorBorderBot = BoxElement.COLOR_VANILLA_BORDER.getSecond().copy();

                RemovedGuiUtils.drawHoveringText(graphics, tooltip, posX, posY, width, height, -1,
                                colorBackground.getRGB(), colorBorderTop.getRGB(), colorBorderBot.getRGB(), mc.font);
        }
}