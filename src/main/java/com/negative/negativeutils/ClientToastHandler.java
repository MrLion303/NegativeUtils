package com.negative.negativeutils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;

public final class ClientToastHandler {
    private ClientToastHandler() {
    }

    public static void showDiscovery(String message) {
        SystemToast.add(
                Minecraft.getInstance().getToasts(),
                SystemToast.SystemToastIds.TUTORIAL_HINT,
                Component.literal("Nuevo descubrimiento"),
                Component.literal(message)
        );
    }
}