package com.negative.negativeutils;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = EnciclopediaMod.MOD_ID,
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class DiscordEmotePickerEvents {
    private static final Map<Screen, Button> CHAT_BUTTONS =
            new WeakHashMap<>();

    private DiscordEmotePickerEvents() {
    }

    @SubscribeEvent
    public static void onChatScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof ChatScreen chatScreen)) {
            return;
        }

        EditBox chatInput = event.getListenersList().stream()
                .filter(EditBox.class::isInstance)
                .map(EditBox.class::cast)
                .findFirst()
                .orElse(null);
        if (chatInput == null) {
            return;
        }

        Button button = new Button(
                Button.builder(
                                Component.literal("Emotes"),
                                ignored -> DiscordEmoteClientData
                                        .ensureEmoteFontLoaded(() ->
                                                Minecraft.getInstance().setScreen(
                                                        new DiscordEmotePickerScreen(
                                                                chatInput.getValue()
                                                        )
                                                )
                                        )
                )
                        .bounds(
                                event.getScreen().width - 72,
                                event.getScreen().height - 42,
                                64,
                                20
                        )
                        .tooltip(Tooltip.create(
                                Component.literal("Insertar un emote de Discord")
                        ))
        ) {
            @Override
            public ComponentPath nextFocusPath(FocusNavigationEvent event) {
                return null;
            }

            @Override
            protected void renderWidget(
                    net.minecraft.client.gui.GuiGraphics graphics,
                    int mouseX,
                    int mouseY,
                    float partialTick
            ) {
                int background = isHoveredOrFocused()
                        ? 0xA0000000
                        : 0x80000000;
                graphics.fill(
                        getX(),
                        getY(),
                        getX() + width,
                        getY() + height,
                        background
                );
                graphics.drawCenteredString(
                        Minecraft.getInstance().font,
                        getMessage(),
                        getX() + width / 2,
                        getY() + (height - 8) / 2,
                        0xFFFFFFFF
                );
            }
        };
        event.addListener(button);
        CHAT_BUTTONS.put(chatScreen, button);
    }

    @SubscribeEvent
    public static void onChatScreenRender(ScreenEvent.Render.Post event) {
        Button button = CHAT_BUTTONS.get(event.getScreen());
        if (button != null) {
            button.render(
                    event.getGuiGraphics(),
                    event.getMouseX(),
                    event.getMouseY(),
                    event.getPartialTick()
            );
        }
    }
}
