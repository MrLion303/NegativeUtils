package com.negative.negativeutils.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Sobrescribe el icono de micrófono del HUD de Simple Voice Chat mientras
 * el servidor mantiene activo el mute global.
 *
 * Se dibuja después del HUD de SVC y reutiliza su textura, posición y escala.
 * No modifica el ajuste local de micrófono guardado por el cliente.
 */
@Mod.EventBusSubscriber(
        modid = "negativeutils",
        bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT
)
public final class VoicechatMuteHudOverlay {
    private static final ResourceLocation HOTBAR_OVERLAY = new ResourceLocation("hotbar");
    private static final ResourceLocation MUTED_MICROPHONE =
            new ResourceLocation("voicechat", "textures/icons/microphone_off.png");

    private VoicechatMuteHudOverlay() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void afterHotbar(RenderGuiOverlayEvent.Post event) {
        if (!HOTBAR_OVERLAY.equals(event.getOverlay().id())) {
            return;
        }
        if (!VoicechatMuteClientState.isGloballyMuted()
                || !ModList.get().isLoaded("voicechat")) {
            return;
        }

        Object config = getClientConfig();
        if (config == null
                || readBoolean(config, "hideIcons", false)
                || !readBoolean(config, "showHudIcons", true)) {
            return;
        }

        // El icono de desconectado o de voz totalmente desactivada tiene prioridad.
        if (isVoicechatDisconnectedOrDisabled()) {
            return;
        }

        int posX = readInt(config, "hudIconPosX", 0);
        int posY = readInt(config, "hudIconPosY", 0);
        float scale = (float) readDouble(config, "hudIconScale", 1.0D);

        Minecraft minecraft = Minecraft.getInstance();
        var graphics = event.getGuiGraphics();
        graphics.pose().pushPose();

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        if (posX < 0) {
            graphics.pose().translate(minecraft.getWindow().getGuiScaledWidth(), 0.0D, 0.0D);
        }
        if (posY < 0) {
            graphics.pose().translate(0.0D, minecraft.getWindow().getGuiScaledHeight(), 0.0D);
        }

        graphics.pose().translate(posX, posY, 0.0D);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.blit(
                MUTED_MICROPHONE,
                posX < 0 ? -16 : 0,
                posY < 0 ? -16 : 0,
                0,
                0,
                16,
                16,
                16,
                16
        );

        graphics.pose().popPose();
    }

    private static Object getClientConfig() {
        try {
            Class<?> voicechatClient = Class.forName(
                    "de.maxhenkel.voicechat.VoicechatClient",
                    false,
                    VoicechatMuteHudOverlay.class.getClassLoader()
            );
            Field field = voicechatClient.getField("CLIENT_CONFIG");
            return field.get(null);
        } catch (ReflectiveOperationException | LinkageError exception) {
            return null;
        }
    }

    private static Object readConfigEntry(Object config, String name) {
        try {
            Field field = config.getClass().getField(name);
            Object entry = field.get(config);
            Method get = entry.getClass().getMethod("get");
            return get.invoke(entry);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return null;
        }
    }

    private static boolean readBoolean(Object config, String name, boolean fallback) {
        Object value = readConfigEntry(config, name);
        return value instanceof Boolean result ? result : fallback;
    }

    private static int readInt(Object config, String name, int fallback) {
        Object value = readConfigEntry(config, name);
        return value instanceof Number number ? number.intValue() : fallback;
    }

    private static double readDouble(Object config, String name, double fallback) {
        Object value = readConfigEntry(config, name);
        return value instanceof Number number ? number.doubleValue() : fallback;
    }

    private static boolean isVoicechatDisconnectedOrDisabled() {
        try {
            Class<?> clientManager = Class.forName(
                    "de.maxhenkel.voicechat.voice.client.ClientManager",
                    false,
                    VoicechatMuteHudOverlay.class.getClassLoader()
            );
            Object stateManager = clientManager.getMethod("getPlayerStateManager").invoke(null);
            boolean disconnected = (boolean) stateManager.getClass()
                    .getMethod("isDisconnected")
                    .invoke(stateManager);
            boolean disabled = (boolean) stateManager.getClass()
                    .getMethod("isDisabled")
                    .invoke(stateManager);
            return disconnected || disabled;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            // Si no se puede consultar el estado interno, no ocultamos el indicador global.
            return false;
        }
    }
}
