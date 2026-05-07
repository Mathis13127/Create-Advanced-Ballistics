package com.pixel.create_ordnance.commands;

import java.util.List;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import com.pixel.create_ordnance.CreateOrdnance;

import dev.architectury.networking.NetworkManager;

import mod.chloeprime.aaaparticles.api.common.ParticleEmitterInfo;
import mod.chloeprime.aaaparticles.common.network.S2CAddParticle;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.Vec2Argument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;

import net.minecraft.network.chat.Component;

import net.minecraft.resources.ResourceLocation;

import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class ParticleCommand {

    public static ArgumentBuilder<CommandSourceStack, ?> register() {
        return Commands.literal("particle")
                .then(Commands.argument("effect", ResourceLocationArgument.id())
                        .suggests((ctx, builder) -> {
                            return OrdnanceCommandUtils.suggestResourceSubstring(
                                    List.of(ResourceLocation.fromNamespaceAndPath(CreateOrdnance.MODID, "explosion_test")),
                                    builder);
                        })
                        .executes(ctx -> spawnParticle(ctx, false, false))
                        .then(Commands.argument("position", Vec3Argument.vec3())
                                .executes(ctx -> spawnParticle(ctx, true, false))
                                .then(Commands.argument("rotation", Vec2Argument.vec2())
                                        .executes(ctx -> spawnParticle(ctx, true, true)))));
    }

    private static int spawnParticle(CommandContext<CommandSourceStack> ctx, boolean hasPos, boolean hasRot)
    {
        CommandSourceStack source = ctx.getSource();
        var player = source.getPlayer();

        ResourceLocation effekLoc = ResourceLocationArgument.getId(ctx, "effect");

        Vec3 pos;
        Vec2 rot;

        // Validate that the effect exists using ClassLoader (checks JAR/Classpath)
        String assetPath = "assets/" + effekLoc.getNamespace() + "/effeks/" + effekLoc.getPath() + ".efkefc";
        if (ParticleCommand.class.getClassLoader().getResource(assetPath) == null)
        {
            source.sendFailure(Component.literal("§c[Ordnance] Particle effect not found: " + effekLoc));
            return 0;
        }

        if (hasPos)
        {
            pos = Vec3Argument.getVec3(ctx, "position");
        }
        else
        {
            if (player == null)
                return 0;
            pos = player.getEyePosition();
        }

        if (hasRot)
        {
            rot = Vec2Argument.getVec2(ctx, "rotation");
        }
        else
        {
            if (player == null)
                return 0;
            rot = new Vec2(player.getXRot(), player.getYRot());
        }

        float finalRotX = (float) Math.toRadians(rot.x);
        float finalRotY = (float) Math.toRadians(-rot.y);

        var level = source.getLevel();
        var info = ParticleEmitterInfo.create(level, effekLoc);
        info.position(pos);
        info.rotation(finalRotX, finalRotY, 0);
        info.scale(1.0f);

        if (info instanceof S2CAddParticle packet)
        {
            NetworkManager.sendToPlayers(source.getServer().getPlayerList().getPlayers(), packet);
            source.sendSuccess(() -> Component.literal("§a[Ordnance] Spawning " + effekLoc), true);
        }
        else
        {
            source.sendFailure(Component.literal("Failed to create particle packet."));
        }
        return 1;
    }
}