package com.negative.negativeutils;

import java.util.List;
import java.util.Set;

public final class EncyclopediaClientData {
    private static List<EncyclopediaData.Entry> entries = List.of();
    private static Set<String> discoveries = Set.of();
    private static boolean canEdit;

    private EncyclopediaClientData() {
    }

    public static void setEntries(
            List<EncyclopediaData.Entry> newEntries,
            boolean operator
    ) {
        entries = List.copyOf(newEntries);
        canEdit = operator;
    }

    public static List<EncyclopediaData.Entry> getEntries() {
        return entries;
    }

    public static boolean canEdit() {
        return canEdit;
    }

    public static void setDiscoveries(Set<String> newDiscoveries) {
        discoveries = Set.copyOf(newDiscoveries);
    }

    public static boolean hasDiscovered(EncyclopediaData.Entry entry) {
        return discoveries.contains(PlayerDiscoveryData.key(entry));
    }
}