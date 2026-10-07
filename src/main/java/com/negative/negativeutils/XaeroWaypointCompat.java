package com.negative.negativeutils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = NegativeUtilsMod.MOD_ID, value = Dist.CLIENT)
public final class XaeroWaypointCompat {
    private static final String MOD_NAME = "NegativeUtils";
    private static final String MANAGER = "xaero.common.minimap.waypoints.WaypointsManager";
    private static final String WAYPOINT = "xaero.common.minimap.waypoints.Waypoint";

    private XaeroWaypointCompat() {}

    public static boolean isAvailable() {
        return ModList.get().isLoaded("xaerominimap");
    }

    public static void sync() {
        if (!isAvailable()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        try {
            Class<?> manager = Class.forName(MANAGER);
            Method getCustom = manager.getMethod("getCustomWaypoints", String.class);
            Object table = getCustom.invoke(null, MOD_NAME);
            if (!(table instanceof Collection<?>)) return;
            @SuppressWarnings("rawtypes") Collection collection = (Collection) table;

            collection.clear();
            String dimension = mc.level.dimension().location().toString();
            for (WaypointSavedData.Waypoint wp : WaypointClientData.getWaypoints()) {
                if (!wp.visible() || !wp.dimension().equals(dimension)) continue;

                Vec3 position = WaypointClientData.resolvePosition(wp);
                if (position == null) continue;

                Class<?> waypointClass = Class.forName(WAYPOINT);
                Constructor<?> constructor = waypointClass.getConstructor(
                        int.class, int.class, int.class, String.class, String.class, int.class);

                Object xaeroWaypoint = constructor.newInstance(
                        (int) Math.floor(position.x),
                        (int) Math.floor(position.y),
                        (int) Math.floor(position.z),
                        wp.name(),
                        wp.icon().isEmpty() ? "◆" : wp.icon().substring(0, 1),
                        toXaeroColor(wp.color()));

                collection.add(xaeroWaypoint);
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Xaero es opcional y sus internals pueden cambiar entre versiones.
        }
    }

    private static int toXaeroColor(int rgb) {
        int[][] palette = {
                {0x000000, 0}, {0x0000AA, 1}, {0x00AA00, 2}, {0x00AAAA, 3},
                {0xAA0000, 4}, {0xAA00AA, 5}, {0xFFAA00, 6}, {0xAAAAAA, 7},
                {0x555555, 8}, {0x5555FF, 9}, {0x55FF55, 10}, {0x55FFFF, 11},
                {0xFF5555, 12}, {0xFF55FF, 13}, {0xFFFF55, 14}, {0xFFFFFF, 15}
        };
        int best = 15;
        long bestDistance = Long.MAX_VALUE;
        int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
        for (int[] entry : palette) {
            int pr = (entry[0] >> 16) & 255, pg = (entry[0] >> 8) & 255, pb = entry[0] & 255;
            long distance = (long)(r - pr) * (r - pr) + (long)(g - pg) * (g - pg) + (long)(b - pb) * (b - pb);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = entry[1];
            }
        }
        return best;
    }
}
