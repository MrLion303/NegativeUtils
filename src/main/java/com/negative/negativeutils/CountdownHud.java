package com.negative.negativeutils;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = "negativeutils",
        value = Dist.CLIENT
)
public class CountdownHud {

    private CountdownHud() {
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay()
                != VanillaGuiOverlay.BOSS_EVENT_PROGRESS.type()) {
            return;
        }

        if (!CountdownClientData.isConfigured()) {
            return;
        }

        long remainingMillis = CountdownClientData.getRemainingMillis();

        if (remainingMillis <= 0) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null) {
            return;
        }

        long totalSeconds = (remainingMillis + 999) / 1000;
        long days = totalSeconds / 86_400;
        long hours = (totalSeconds % 86_400) / 3_600;
        long minutes = (totalSeconds % 3_600) / 60;
        long seconds = totalSeconds % 60;

        String countdownText;

        if (days > 0) {
            countdownText = String.format(
                    "%d:%02d:%02d:%02d",
                    days,
                    hours,
                    minutes,
                    seconds
            );
        } else {
            countdownText = String.format(
                    "%02d:%02d:%02d",
                    hours,
                    minutes,
                    seconds
            );
        }

        String customText = CountdownClientData.getDisplayText();
        String position = CountdownClientData.getDisplayPosition();
        int color = CountdownClientData.getDisplayColor();

        int screenWidth = event.getWindow().getGuiScaledWidth();
        int screenHeight = event.getWindow().getGuiScaledHeight();
        var graphics = event.getGuiGraphics();

        Component timerComponent = Component.literal(countdownText);
        Component textComponent = Component.literal(customText);

        int timerY;
        int textY;

        switch (position) {
            case "ACTIONBAR" -> {
                timerY = screenHeight - 48;
                textY = timerY + 12;
            }
            case "SCOREBOARD" -> {
                timerY = screenHeight / 2 - 12;
                textY = timerY + 12;

                int rightX = screenWidth - 6
                        - minecraft.font.width(timerComponent);

                graphics.drawString(
                        minecraft.font,
                        timerComponent,
                        rightX,
                        timerY,
                        color,
                        false
                );

                if (!customText.isBlank()) {
                    int textRightX = screenWidth - 6
                            - minecraft.font.width(textComponent);

                    graphics.drawString(
                            minecraft.font,
                            textComponent,
                            textRightX,
                            textY,
                            color,
                            false
                    );
                }
                return;
            }
            case "TITLE" -> {
                timerY = screenHeight / 2 - 15;
                textY = timerY + 13;
            }
            case "BOSSBAR" -> {
                timerY = 10;
                textY = timerY + 13;
            }
            default -> {
                timerY = 10;
                textY = timerY + 13;
            }
        }

        graphics.drawCenteredString(
                minecraft.font,
                timerComponent,
                screenWidth / 2,
                timerY,
                color
        );

        if (!customText.isBlank()) {
            graphics.drawCenteredString(
                    minecraft.font,
                    textComponent,
                    screenWidth / 2,
                    textY,
                    color
            );
        }
    }
}