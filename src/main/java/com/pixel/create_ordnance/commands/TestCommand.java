package com.pixel.create_ordnance.commands;

import com.mojang.brigadier.builder.ArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class TestCommand {

        public static ArgumentBuilder<CommandSourceStack, ?> register() {
                return Commands.literal("test")
                                .requires(source -> source.hasPermission(2));
        }

}