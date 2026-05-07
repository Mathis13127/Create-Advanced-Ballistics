package com.pixel.create_ordnance.commands;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import com.pixel.create_ordnance.config.OrdnanceConfigs;

import net.createmod.catnip.config.ConfigBase;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * In-game command handler for reading and modifying Ordnance configuration at runtime.
 *
 * <p>Registered under {@code /ordnance config}. Supports:
 * <ul>
 *   <li>{@code /ordnance config} — dumps all current config values.</li>
 *   <li>{@code /ordnance config <path>} — reads a single config value.</li>
 *   <li>{@code /ordnance config <path> <value>} — sets a config value (with tab-completion).</li>
 * </ul>
 *
 * <p>Uses reflection-based traversal of the Create {@code ConfigBase} hierarchy to
 * discover and suggest config keys dynamically. Changes trigger a
 * {@link com.pixel.create_ordnance.api.events.ProjectileConfigReloadEvent}.</p>
 *
 * @see OrdnanceConfigs
 */
public class ConfigCommand {

    public static ArgumentBuilder<CommandSourceStack, ?> register() {
        return Commands.literal("config")
                .executes(ConfigCommand::dumpAllConfigs)
                .then(Commands.argument("path", StringArgumentType.string())
                        .suggests((ctx, builder) -> OrdnanceCommandUtils.suggestSubstring(getConfigKeys(), builder))
                        .executes(ConfigCommand::handleConfigAction)
                        .then(Commands.argument("value", StringArgumentType.greedyString())
                                .suggests(ConfigCommand::configValueSuggestions)
                                .executes(ConfigCommand::setConfigValue)));
    }

    private static int dumpAllConfigs(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        source.sendSuccess(() -> Component.literal("§6=== Ordnance Configuration Dump ===§r"), false);

        ConfigVisitor dumper = (obj, path, field) -> {
            if (field == null) {
                source.sendSuccess(() -> Component.literal("§e[" + path + "]§r"), false);
            } else {
                String displayVal = getConfigValueDisplay(obj);
                if (displayVal != null) {
                    source.sendSuccess(() -> Component.literal(" - §b" + path + "§r: §a" + displayVal), false);
                }
            }
        };

        walkConfig(OrdnanceConfigs.COMMON, "Common", dumper);
        walkConfig(OrdnanceConfigs.SERVER, "Server", dumper);
        walkConfig(OrdnanceConfigs.CLIENT, "Client", dumper);
        return 1;
    }

    private static int handleConfigAction(CommandContext<CommandSourceStack> ctx) {
        String path = StringArgumentType.getString(ctx, "path");
        CommandSourceStack source = ctx.getSource();

        Object configObject = resolveConfigObject(path);
        if (configObject == null) {
            source.sendFailure(Component.literal("Config key not found: " + path));
            return 0;
        }

        if (configObject instanceof ConfigBase) {
            source.sendSuccess(() -> Component.literal("§6=== Dump of " + path + " ===§r"), false);

            ConfigVisitor dumper = (obj, p, field) -> {
                if (field != null) {
                    String displayVal = getConfigValueDisplay(obj);
                    if (displayVal != null) {
                        source.sendSuccess(() -> Component.literal(" - §b" + p + "§r: §a" + displayVal), false);
                    }
                } else if (!p.equals(path)) {
                    source.sendSuccess(() -> Component.literal("§e[" + p + "]§r"), false);
                }
            };

            walkConfig(configObject, path, dumper);
            return 1;
        }

        if (isConfigValue(configObject.getClass())) {
            String display = getConfigValueDisplay(configObject);
            if (display != null) {
                StringBuilder info = new StringBuilder("§6" + path + " §r: §a" + display + " §7(");

                if (configObject instanceof ConfigBase.ConfigBool) {
                    info.append("§b[true/false]");
                } else if (configObject instanceof ConfigBase.ConfigFloat f) {
                    double[] bounds = findBounds(f);
                    if (bounds != null)
                        info.append("§b[").append(bounds[0]).append(" - ").append(bounds[1]).append("]");
                } else if (configObject instanceof ConfigBase.ConfigInt i) {
                    double[] bounds = findBounds(i);
                    if (bounds != null)
                        info.append("§b[").append((int) bounds[0]).append(" - ").append((int) bounds[1]).append("]");
                } else if (configObject instanceof ConfigBase.ConfigEnum<?> e) {
                    Object[] constants = getEnumConstants(e);
                    if (constants != null) {
                        info.append("§b[");
                        for (int k = 0; k < constants.length; k++) {
                            info.append(constants[k].toString().toLowerCase());
                            if (k < constants.length - 1) info.append("/");
                        }
                        info.append("]");
                    }
                }
                info.append(")");

                source.sendSuccess(() -> Component.literal(info.toString()), false);
                return 1;
            }
        }

        source.sendFailure(Component.literal("Unknown config type for " + path));
        return 0;
    }

    private static int setConfigValue(CommandContext<CommandSourceStack> ctx) {
        String path = StringArgumentType.getString(ctx, "path");
        String valueStr = StringArgumentType.getString(ctx, "value");
        CommandSourceStack source = ctx.getSource();

        Object configValue = resolveConfigObject(path);
        if (configValue == null) {
            source.sendFailure(Component.literal("Config key not found: " + path));
            return 0;
        }

        try {
            if (configValue instanceof ConfigBase.ConfigBool b) {
                boolean inputVal = Boolean.parseBoolean(valueStr);
                b.set(inputVal);
                source.sendSuccess(
                        () -> Component.literal("§a[Ordnance] Successfully set " + path + " to §e" + inputVal),
                        true);
            } else if (configValue instanceof ConfigBase.ConfigFloat f) {
                float inputVal = Float.parseFloat(valueStr);

                double[] bounds = findBounds(f);
                if (bounds != null) {
                    double min = bounds[0];
                    double max = bounds[1];
                    if (inputVal < min || inputVal > max) {
                        source.sendFailure(
                                Component.literal("§c[Ordnance] Value for " + path + " is out of bounds! §7(Min: "
                                        + min + ", Max: " + max + ")"));
                        return 0;
                    }
                }

                f.set((double) inputVal);
                source.sendSuccess(
                        () -> Component.literal("§a[Ordnance] Successfully set " + path + " to §e" + inputVal),
                        true);
            } else if (configValue instanceof ConfigBase.ConfigInt i) {
                int inputVal = Integer.parseInt(valueStr);

                double[] bounds = findBounds(i);
                if (bounds != null) {
                    double min = bounds[0];
                    double max = bounds[1];
                    if (inputVal < min || inputVal > max) {
                        source.sendFailure(
                                Component.literal("§c[Ordnance] Value for " + path + " is out of bounds! §7(Min: "
                                        + (int) min + ", Max: " + (int) max + ")"));
                        return 0;
                    }
                }

                i.set(inputVal);
                source.sendSuccess(
                        () -> Component.literal("§a[Ordnance] Successfully set " + path + " to §e" + inputVal),
                        true);
            } else if (configValue instanceof ConfigBase.ConfigEnum e) {
                Object[] constants = getEnumConstants(e);
                if (constants != null) {
                    Object match = null;
                    for (Object c : constants) {
                        if (c.toString().equalsIgnoreCase(valueStr)) {
                            match = c;
                            break;
                        }
                    }
                    if (match != null) {
                        final Object finalMatch = match;
                        e.set(match);
                        source.sendSuccess(
                                () -> Component.literal("§a[Ordnance] Successfully set " + path + " to §e"
                                        + finalMatch.toString().toLowerCase()),
                                true);
                    } else {
                        source.sendFailure(Component.literal("Invalid enum value: " + valueStr));
                        return 0;
                    }
                }
            } else {
                source.sendFailure(Component.literal("Unsupported config type."));
                return 0;
            }
            return 1;
        } catch (NumberFormatException e) {
            source.sendFailure(Component.literal("Invalid format for " + path + ": " + valueStr));
            return 0;
        }
    }

    private static double[] findBounds(Object configObject) {
        try {
            Field providerField = null;
            Class<?> cValueClass = configObject.getClass();
            while (cValueClass != null && !cValueClass.getSimpleName().equals("CValue")) {
                cValueClass = cValueClass.getSuperclass();
            }
            if (cValueClass == null)
                return null;

            providerField = cValueClass.getDeclaredField("provider");
            providerField.setAccessible(true);
            Object provider = providerField.get(configObject);
            if (provider == null)
                return null;

            Object target = provider;
            try {
                Field nested = provider.getClass().getDeclaredField("arg$3");
                nested.setAccessible(true);
                Object val = nested.get(provider);
                if (val != null && !val.getClass().isPrimitive()) {
                    target = val;
                }
            } catch (Exception ignored) {
            }

            Double min = null;
            Double max = null;

            try {
                Field minF = target.getClass().getDeclaredField("arg$3");
                minF.setAccessible(true);
                min = ((Number) minF.get(target)).doubleValue();

                Field maxF = target.getClass().getDeclaredField("arg$4");
                maxF.setAccessible(true);
                max = ((Number) maxF.get(target)).doubleValue();
            } catch (Exception ignored) {
            }

            if (min != null && max != null) {
                return new double[] { min, max };
            }
        } catch (Exception e) {
        }
        return null;
    }

    @FunctionalInterface
    private interface ConfigVisitor {
        void visit(Object configValue, String path, Field field);
    }

    private static void walkConfig(Object configObject, String prefix, ConfigVisitor visitor) {
        if (configObject == null)
            return;
        visitor.visit(configObject, prefix, null);

        for (Field field : configObject.getClass().getDeclaredFields()) {
            try {
                field.setAccessible(true);
                Class<?> type = field.getType();
                Object value = field.get(configObject);
                String currentPath = prefix + "." + field.getName();

                if (ConfigBase.class.isAssignableFrom(type)) {
                    walkConfig(value, currentPath, visitor);
                } else if (isConfigValue(type)) {
                    visitor.visit(value, currentPath, field);
                }
            } catch (Exception e) {
            }
        }
    }

    private static List<String> getConfigKeys() {
        List<String> keys = new ArrayList<>();
        ConfigVisitor collector = (obj, path, field) -> keys.add(path);
        walkConfig(OrdnanceConfigs.COMMON, "Common", collector);
        walkConfig(OrdnanceConfigs.SERVER, "Server", collector);
        walkConfig(OrdnanceConfigs.CLIENT, "Client", collector);
        return keys;
    }

    private static boolean isConfigValue(Class<?> type) {
        return ConfigBase.ConfigBool.class.isAssignableFrom(type) ||
                ConfigBase.ConfigFloat.class.isAssignableFrom(type) ||
                ConfigBase.ConfigInt.class.isAssignableFrom(type) ||
                ConfigBase.ConfigEnum.class.isAssignableFrom(type);
    }

    private static String getConfigValueDisplay(Object configValue) {
        if (configValue instanceof ConfigBase.ConfigBool b)
            return String.valueOf(b.get());
        if (configValue instanceof ConfigBase.ConfigFloat f)
            return String.valueOf(f.get());
        if (configValue instanceof ConfigBase.ConfigInt i)
            return String.valueOf(i.get());
        if (configValue instanceof ConfigBase.ConfigEnum<?> e)
            return e.get().toString().toLowerCase();
        return null;
    }

    private static Object[] getEnumConstants(ConfigBase.ConfigEnum<?> configEnum) {
        try {
            Object current = configEnum.get();
            if (current != null) {
                return current.getClass().getEnumConstants();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static CompletableFuture<Suggestions> configValueSuggestions(CommandContext<CommandSourceStack> ctx,
            SuggestionsBuilder builder) {
        try {
            String path = StringArgumentType.getString(ctx, "path");
            Object configValue = resolveConfigObject(path);
            if (configValue instanceof ConfigBase.ConfigBool) {
                builder.suggest("true", Component.literal("§7Set to §atrue"));
                builder.suggest("false", Component.literal("§7Set to §cfalse"));
                return builder.buildFuture();
            } else if (configValue instanceof ConfigBase.ConfigFloat f) {
                double[] bounds = findBounds(f);
                String current = String.valueOf(f.get());
                if (bounds != null) {
                    builder.suggest(current, Component
                            .literal("§7Current Value (Range: §e" + bounds[0] + " §r- §e" + bounds[1] + "§7)"));
                } else {
                    builder.suggest(current, Component.literal("§7Current Value (Float)"));
                }
                return builder.buildFuture();
            } else if (configValue instanceof ConfigBase.ConfigInt i) {
                double[] bounds = findBounds(i);
                String current = String.valueOf(i.get());
                if (bounds != null) {
                    builder.suggest(current, Component.literal(
                            "§7Current Value (Range: §e" + (int) bounds[0] + " §r- §e" + (int) bounds[1] + "§7)"));
                } else {
                    builder.suggest(current, Component.literal("§7Current Value (Integer)"));
                }
                return builder.buildFuture();
            } else if (configValue instanceof ConfigBase.ConfigEnum<?> e) {
                Object[] constants = getEnumConstants(e);
                if (constants != null) {
                    for (Object c : constants) {
                        builder.suggest(c.toString().toLowerCase(),
                                Component.literal("§7Select §b" + c.toString().toLowerCase()));
                    }
                }
                return builder.buildFuture();
            }
        } catch (IllegalArgumentException e) {
        }
        return Suggestions.empty();
    }

    private static Object resolveConfigObject(String path) {
        String[] parts = path.split("\\.");
        if (parts.length < 1)
            return null;

        Object current = switch (parts[0].toLowerCase()) {
            case "common" -> OrdnanceConfigs.COMMON;
            case "server" -> OrdnanceConfigs.SERVER;
            case "client" -> OrdnanceConfigs.CLIENT;
            default -> null;
        };

        if (current == null)
            return null;

        for (int i = 1; i < parts.length; i++) {
            try {
                String target = parts[i];
                Field foundField = null;

                for (Field f : current.getClass().getDeclaredFields()) {
                    if (f.getName().equalsIgnoreCase(target)) {
                        foundField = f;
                        break;
                    }
                }

                if (foundField == null)
                    return null;

                foundField.setAccessible(true);
                current = foundField.get(current);
                if (current == null)
                    return null;
            } catch (Exception e) {
                return null;
            }
        }
        return current;
    }
}