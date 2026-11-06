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
 * Carga las imágenes desde assets/contadores/<nombre>.png del paquete de recursos activo.
 */
public final class CountdownHologramTextures {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Map<String, TextureInfo> LOADED = new HashMap<>();
    private static final Set<String> MISSING = new HashSet<>();

    private CountdownHologramTextures() {
    }

    public static TextureInfo get(String imageName) {
        String cleanName = CountdownSavedData.sanitizeHologramImage(imageName);
        if (cleanName == null || cleanName.isBlank()) {
            return null;
        }

        TextureInfo cached = LOADED.get(cleanName);
        if (cached != null) {
            return cached;
        }
        if (MISSING.contains(cleanName)) {
            return null;
        }

        ResourceManager manager = Minecraft.getInstance().getResourceManager();
        ResourceLocation sourceLocation = ResourceLocation.fromNamespaceAndPath("contadores", cleanName);
        Optional<Resource> resource = manager.getResource(sourceLocation);
        if (resource.isEmpty()) {
            MISSING.add(cleanName);
            LOGGER.warn("No se encontró la imagen {}. Debe existir en assets/contadores/{} dentro del paquete de recursos.",
                    cleanName, cleanName);
            return null;
        }

        try (InputStream stream = resource.get().open()) {
            NativeImage image = NativeImage.read(stream);
            int width = image.getWidth();
            int height = image.getHeight();
            if (width <= 0 || height <= 0 || width > 4096 || height > 4096) {
                image.close();
                MISSING.add(cleanName);
                LOGGER.warn("La imagen del holograma {} tiene dimensiones no válidas (máximo 4096x4096).", cleanName);
                return null;
            }

            ResourceLocation textureLocation = ResourceLocation.fromNamespaceAndPath(
                    "negativeutils", "countdown_holograms/" + cleanName
            );
            Minecraft.getInstance().getTextureManager().register(
                    textureLocation, new DynamicTexture(image)
            );

            TextureInfo info = new TextureInfo(textureLocation, width, height);
            LOADED.put(cleanName, info);
            return info;
        } catch (Exception exception) {
            MISSING.add(cleanName);
            LOGGER.warn("No se pudo cargar la imagen del holograma {} desde assets/contadores/{}.",
                    cleanName, cleanName, exception);
            return null;
        }
    }

    public record TextureInfo(ResourceLocation location, int width, int height) {
    }
}
