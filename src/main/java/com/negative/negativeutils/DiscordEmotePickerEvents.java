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
        modid = NegativeUtilsMod.MOD_ID,
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class DiscordEmotePickerEvents {
    private static final Map<Screen, Button> CHAT_BUTTONS = new WeakHashMap<>();
    private static final Map<Screen, DiscordEmoteOverlay> OVERLAYS = new WeakHashMap<>();
    private static final Map<Screen, EditBox> CHAT_INPUTS = new WeakHashMap<>();

    private DiscordEmotePickerEvents() {
    }

    @SubscribeEvent
    public static void onChatScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof ChatScreen)) return;

        EditBox chatInput = event.getListenersList().stream()
                .filter(EditBox.class::isInstance)
                .map(EditBox.class::cast)
                .findFirst()
                .orElse(null);
        if (chatInput == null) return;

        Screen screen = event.getScreen();
        CHAT_INPUTS.put(screen, chatInput);

        Button button = new Button(
                Button.builder(
                        Component.literal("Emotes"),
                        ignored -> DiscordEmoteClientData.ensureEmoteFontLoaded(() ->
                                OVERLAYS.computeIfAbsent(screen, ignoredScreen -> new DiscordEmoteOverlay()))
                ).bounds(
                        screen.width - 72,
                        screen.height - 42,
                        64,
                        20
                ).tooltip(
                        Tooltip.create(Component.literal("Mostrar emotes de Discord"))
                )
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
                        ? 0xB0182028
                        : 0x90101820;
                graphics.fill(getX(), getY(), getX() + width, getY() + height, background);
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
        CHAT_BUTTONS.put(screen, button);
    }

    @SubscribeEvent
    public static void onChatScreenRender(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof ChatScreen)) return;
        DiscordEmoteOverlay overlay = OVERLAYS.get(event.getScreen());
        EditBox chatInput = CHAT_INPUTS.get(event.getScreen());
        if (overlay != null && chatInput != null) {
            overlay.render(event.getGuiGraphics(), event.getScreen().width, event.getScreen().height,
                    event.getMouseX(), event.getMouseY());
        }
    }

    @SubscribeEvent
    public static void onChatScreenClick(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!(event.getScreen() instanceof ChatScreen)) return;
        DiscordEmoteOverlay overlay = OVERLAYS.get(event.getScreen());
        EditBox chatInput = CHAT_INPUTS.get(event.getScreen());
        if (overlay == null || chatInput == null || event.getButton() != 0) return;

        if (overlay.contains(event.getMouseX(), event.getMouseY(),
                event.getScreen().width, event.getScreen().height)) {
            overlay.click(event.getMouseX(), event.getMouseY(),
                    event.getScreen().width, event.getScreen().height, chatInput);
            event.setCanceled(true);
        } else {
            OVERLAYS.remove(event.getScreen());
        }
    }

    @SubscribeEvent
    public static void onChatScreenScroll(ScreenEvent.MouseScrolled.Pre event) {
        if (!(event.getScreen() instanceof ChatScreen)) return;
        DiscordEmoteOverlay overlay = OVERLAYS.get(event.getScreen());
        if (overlay == null) return;
        if (overlay.contains(event.getMouseX(), event.getMouseY(),
                event.getScreen().width, event.getScreen().height)) {
            overlay.scroll(event.getScrollDelta());
            event.setCanceled(true);
        }
    }
}
