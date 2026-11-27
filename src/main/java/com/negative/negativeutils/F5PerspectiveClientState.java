package com.negative.negativeutils;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;

/** Estado local de la restricción de perspectiva recibida desde el servidor. */
public final class F5PerspectiveClientState {
    private static boolean disabled;
    private static boolean forceFirstPerson;

    private F5PerspectiveClientState() {}

    public static void apply(boolean isDisabled) {
        disabled = isDisabled;
        forceFirstPerson = !isDisabled;
    }

    public static void tick(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.level == null) return;

        // VFXShaderMod debe conservar el control mientras ejecuta una cámara propia.
        if (VfxCameraCompatibility.isCameraOverrideActive()) return;

        if (forceFirstPerson) {
            minecraft.options.setCameraType(CameraType.FIRST_PERSON);
            forceFirstPerson = false;
        }

        // No se intercepta F5: se corrige la perspectiva al final del tick del cliente.
        if (disabled && minecraft.options.getCameraType() != CameraType.FIRST_PERSON) {
            minecraft.options.setCameraType(CameraType.FIRST_PERSON);
        }
    }

    public static void reset() {
        disabled = false;
        forceFirstPerson = false;
    }
}
