package com.negative.negativeutils;

import java.lang.reflect.Method;

import net.minecraftforge.fml.ModList;

/**
 * Integración opcional con VFXShaderMod. Se consulta su estado público para
 * no forzar primera persona mientras ese mod controla una cámara cinematográfica.
 */
public final class VfxCameraCompatibility {
    private static final String VFX_MOD_ID = "vfxshadersmod";
    private static final String CAMERA_CONTROLLER =
            "com.mrlion303.vfxshadersmod.client.ClientVfxCameraController";
    private static final Method IS_CAMERA_OVERRIDE_ACTIVE = findCameraStateMethod();

    private VfxCameraCompatibility() {}

    private static Method findCameraStateMethod() {
        if (!ModList.get().isLoaded(VFX_MOD_ID)) return null;
        try {
            Class<?> controller = Class.forName(
                    CAMERA_CONTROLLER,
                    false,
                    VfxCameraCompatibility.class.getClassLoader()
            );
            return controller.getMethod("isCameraOverrideActive");
        } catch (ReflectiveOperationException | LinkageError exception) {
            return null;
        }
    }

    public static boolean isCameraOverrideActive() {
        if (IS_CAMERA_OVERRIDE_ACTIVE == null) return false;
        try {
            return Boolean.TRUE.equals(IS_CAMERA_OVERRIDE_ACTIVE.invoke(null));
        } catch (ReflectiveOperationException | LinkageError exception) {
            return false;
        }
    }
}
