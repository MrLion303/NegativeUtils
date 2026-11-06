package com.negative.negativeutils;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Carga las imágenes de holograma desde el paquete de recursos del cliente.
 * Se admiten assets/contadores/<nombre>.png y assets/contadores/textures/<nombre>.png.
 */
public final class CountdownHologramTextures {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Map<String, TextureInfo> LOADED = new HashMap<>();
    private static final Map<String, Long> MISSING_UNTIL = new HashMap<>();
    private static final long MISSING_RETRY_MILLIS = 2_000L;
    private static ResourceManager lastResourceManager;

    private CountdownHologramTextures() {
    }

    public static TextureInfo get(String imageName) {
        String cleanName = CountdownSavedData.sanitizeHologramImage(imageName);
        if (cleanName == null || cleanName.isBlank()) {
            return null;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ResourceManager manager = minecraft.getResourceManager();

        // Un resource manager nuevo significa que Minecraft recargó los paquetes.
        // Las ausencias previas deben volver a comprobarse después de esa recarga.
        if (lastResourceManager != manager) {
            lastResourceManager = manager;
            MISSING_UNTIL.clear();
        }

        TextureInfo cached = LOADED.get(cleanName);
        if (cached != null) {
            return cached;
        }

        long now = System.currentTimeMillis();
        Long retryAt = MISSING_UNTIL.get(cleanName);
        if (retryAt != null && now < retryAt) {
            return null;
        }

        ResourceLocation sourceLocation =
                ResourceLocation.fromNamespaceAndPath("contadores", cleanName);
        Optional<Resource> resource = manager.getResource(sourceLocation);

        // Compatibilidad con paquetes que guardan las imágenes bajo textures/,
        // que es la convención habitual de otros recursos de Minecraft.
        if (resource.isEmpty()) {
            sourceLocation = ResourceLocation.fromNamespaceAndPath(
                    "contadores", "textures/" + cleanName
            );
            resource = manager.getResource(sourceLocation);
        }

        if (resource.isEmpty()) {
            MISSING_UNTIL.put(cleanName, now + MISSING_RETRY_MILLIS);
            LOGGER.warn(
                    "No se encontró la imagen {}. Se buscó en assets/contadores/{} y assets/contadores/textures/{}.",
                    cleanName, cleanName, cleanName
            );
            return null;
        }

        try (InputStream stream = resource.get().open()) {
            NativeImage image = NativeImage.read(stream);
            int width = image.getWidth();
            int height = image.getHeight();
            if (width <= 0 || height <= 0 || width > 4096 || height > 4096) {
                image.close();
                MISSING_UNTIL.put(cleanName, now + MISSING_RETRY_MILLIS);
                LOGGER.warn(
                        "La imagen del holograma {} tiene dimensiones no válidas (máximo 4096x4096).",
                        cleanName
                );
                return null;
            }

            ResourceLocation textureLocation = ResourceLocation.fromNamespaceAndPath(
                    "negativeutils", "countdown_holograms/" + cleanName
            );
            DynamicTexture dynamicTexture = new DynamicTexture(image);
            minecraft.getTextureManager().register(textureLocation, dynamicTexture);

            TextureInfo info = new TextureInfo(textureLocation, width, height);
            LOADED.put(cleanName, info);
            MISSING_UNTIL.remove(cleanName);
            LOGGER.info(
                    "Imagen de holograma cargada: {} ({}x{}) desde {}.",
                    cleanName, width, height, sourceLocation
            );
            return info;
        } catch (Exception exception) {
            MISSING_UNTIL.put(cleanName, now + MISSING_RETRY_MILLIS);
            LOGGER.warn(
                    "No se pudo cargar la imagen del holograma {} desde {}.",
                    cleanName, sourceLocation, exception
            );
            return null;
        }
    }

    public record TextureInfo(ResourceLocation location, int width, int height) {
    }
}
