package com.pixel.create_ordnance.foundation.gui;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.pixel.create_ordnance.CreateOrdnance;
import com.pixel.create_ordnance.config.OrdnanceConfigs;
import com.pixel.create_ordnance.registry.ModBlocks;

import net.createmod.catnip.config.ui.BaseConfigScreen;
import net.createmod.catnip.gui.ScreenOpener;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.resources.language.I18n;

import net.minecraft.network.chat.CommonComponents;

import net.minecraft.world.item.ItemStack;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

import org.apache.commons.lang3.mutable.MutableObject;

public class OpenOrdnanceMenuButton extends Button {

    public OpenOrdnanceMenuButton(int x, int y) {
        super(x, y, 20, 20, CommonComponents.EMPTY, OpenOrdnanceMenuButton::click, DEFAULT_NARRATION);
    }

    @Override
    public void renderString(GuiGraphics graphics, Font pFont, int pColor) {
        ItemStack icon = new ItemStack(ModBlocks.DEBUG_TAIL.get()); // Using Debug Tail as icon
        graphics.renderItem(icon, getX() + 2, getY() + 2);
    }

    public static void click(Button b) {
        ScreenOpener.open(new BaseConfigScreen(Minecraft.getInstance().screen, CreateOrdnance.MODID));
    }

    public record SingleMenuRow(String leftTextKey, String rightTextKey) {
        public SingleMenuRow(String centerTextKey) {
            this(centerTextKey, centerTextKey);
        }
    }

    public static class MenuRows {
        public static final MenuRows MAIN_MENU = new MenuRows(Arrays.asList(
                new SingleMenuRow("menu.singleplayer"),
                new SingleMenuRow("menu.multiplayer"),
                new SingleMenuRow("fml.menu.mods", "menu.online"), // Row 3
                new SingleMenuRow("narrator.button.language", "narrator.button.accessibility")));

        public static final MenuRows INGAME_MENU = new MenuRows(Arrays.asList(
                new SingleMenuRow("menu.returnToGame"),
                new SingleMenuRow("gui.advancements", "gui.stats"),
                new SingleMenuRow("menu.sendFeedback", "menu.reportBugs"), // Row 3
                new SingleMenuRow("menu.options", "menu.shareToLan"),
                new SingleMenuRow("menu.returnToMenu")));

        protected final List<String> leftTextKeys, rightTextKeys;

        public MenuRows(List<SingleMenuRow> rows) {
            leftTextKeys = rows.stream().map(SingleMenuRow::leftTextKey).collect(Collectors.toList());
            rightTextKeys = rows.stream().map(SingleMenuRow::rightTextKey).collect(Collectors.toList());
        }
    }

    @EventBusSubscriber(value = Dist.CLIENT, modid = CreateOrdnance.MODID)
    public static class OpenConfigButtonHandler {

        @SubscribeEvent
        public static void onGuiInit(ScreenEvent.Init.Post event) {
            Screen screen = event.getScreen();

            MenuRows menu;
            int rowIdx;
            int offsetX;

            if (screen instanceof TitleScreen) {
                if (!OrdnanceConfigs.CLIENT.ui.quickConfigButton.showConfigInTitleScreen.get())
                    return;
                menu = MenuRows.MAIN_MENU;
                // Create uses Row 2 by default. We want Row 3 (Mod/Realms row)
                rowIdx = 3;
                offsetX = OrdnanceConfigs.CLIENT.ui.quickConfigButton.titleScreenOffsetX.get();
                // We need to pass the specific offset Y too
            } else if (screen instanceof PauseScreen) {
                if (!OrdnanceConfigs.CLIENT.ui.quickConfigButton.showConfigInPauseMenu.get())
                    return;
                menu = MenuRows.INGAME_MENU;
                // Create uses Row 2 by default. We want Row 3 (Feedback/Bugs row)
                rowIdx = 3;
                offsetX = OrdnanceConfigs.CLIENT.ui.quickConfigButton.pauseMenuOffsetX.get();
            } else {
                return;
            }

            if (rowIdx == 0)
                return;

            boolean onLeft = offsetX < 0;
            // Lookup target message strings from translation keys
            // STRICT LOGIC: Select usage based on side.
            // If onLeft (Negative), target Left button. If Right (Positive), target Right
            // button.
            String targetKey = (onLeft ? menu.leftTextKeys : menu.rightTextKeys).get(rowIdx - 1);
            String targetMessage = I18n.get(targetKey);

            int offsetX_ = offsetX;
            int offsetY_ = (screen instanceof TitleScreen)
                    ? OrdnanceConfigs.CLIENT.ui.quickConfigButton.titleScreenOffsetY.get()
                    : OrdnanceConfigs.CLIENT.ui.quickConfigButton.pauseMenuOffsetY.get();

            MutableObject<GuiEventListener> toAdd = new MutableObject<>(null);

            event.getListenersList()
                    .stream()
                    .filter(w -> w instanceof AbstractWidget)
                    .map(w -> (AbstractWidget) w)
                    .filter(w -> {
                        String msg = w.getMessage().getString();
                        return msg.equals(targetMessage);
                    })
                    .findFirst()
                    .ifPresent(w -> toAdd.setValue(
                            new OpenOrdnanceMenuButton(
                                    w.getX() + offsetX_ + (onLeft ? -20 : w.getWidth()),
                                    w.getY() + offsetY_)));

            if (toAdd.getValue() != null)
                event.addListener(toAdd.getValue());
        }
    }
}