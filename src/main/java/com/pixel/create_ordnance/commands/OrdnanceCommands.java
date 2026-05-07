package com.pixel.create_ordnance.commands;

import com.mojang.brigadier.CommandDispatcher;

import com.pixel.create_ordnance.CreateOrdnance;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = CreateOrdnance.MODID)
public class OrdnanceCommands {

        @SubscribeEvent
        public static void onRegisterCommands(RegisterCommandsEvent event) {
                new OrdnanceCommands(event.getDispatcher());
        }

        public OrdnanceCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
                dispatcher.register(Commands.literal("prj-lib")
                                // Help command removed as per user request
                                .then(RegistryCommand.register())
                                .then(TestCommand.register())
                                .then(AttractionCommand.register())
                                .then(ConfigCommand.register())
                                .then(ParticleCommand.register())
                                .then(BlackBoxCommand.register())
                                .then(ProjectileCommand.register()));
        }
}