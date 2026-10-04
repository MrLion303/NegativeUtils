package com.negative.negativeutils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

public class EncyclopediaScreen extends Screen {
    private static final int ENTRY_WIDTH = 140;
    private static final int ENTRY_HEIGHT = 22;
    private static final int ENTRY_GAP = 6;
    private static final int ROWS_PER_COLUMN = 10;
    private static final int MAX_COLUMNS_PER_PAGE = 6;
    private static final int EXTRA_GAP_AFTER_THIRD_COLUMN = 14;

    private static boolean adminTabActive;
    private static int temporaryPageCount = 1;

    private static final Map<String, List<EncyclopediaData.Entry>>
            temporaryTestEntries = new HashMap<>();

    private static final Set<String> locallyDeletedEntryKeys = new HashSet<>();

    private Category selectedCategory = Category.ITEM;
    private EncyclopediaData.Entry selectedEntry;
    private boolean adminButtonShown;
    private int currentPage;
    private int rowsPerColumn = ROWS_PER_COLUMN;
    private int columnsPerPage;
    private int pageCount;
    private List<EncyclopediaData.Entry> visibleEntries = List.of();

    private enum Category {
        ITEM("Item"),
        BLOQUE("Bloque"),
        MOB("Mob");

        private final String label;

        Category(String label) {
            this.label = label;
        }
    }

    public EncyclopediaScreen() {
        super(Component.literal("negativeutils"));
    }

    private boolean showAdminButtons() {
        return adminButtonShown && adminTabActive;
    }

    private String getTemporaryPageKey() {
        return selectedCategory.name() + ":" + currentPage;
    }

    private String getEntryKey(EncyclopediaData.Entry entry) {
        return entry.category().name() + ":" + entry.targetId();
    }

    private boolean isTemporaryTest(EncyclopediaData.Entry entry) {
        for (List<EncyclopediaData.Entry> entries : temporaryTestEntries.values()) {
            if (entries.contains(entry)) {
                return true;
            }
        }

        return false;
    }

    private void deleteEntry(EncyclopediaData.Entry entry) {
        if (isTemporaryTest(entry)) {
            for (List<EncyclopediaData.Entry> entries : temporaryTestEntries.values()) {
                entries.remove(entry);
            }
        } else {
            locallyDeletedEntryKeys.add(getEntryKey(entry));
            EncyclopediaNetwork.deleteEntry(entry);
        }

        selectedEntry = null;
        this.clearWidgets();
        this.init();
    }

    @Override
    protected void init() {
        adminButtonShown = EncyclopediaClientData.canEdit();

        if (!adminButtonShown) {
            adminTabActive = false;
        }

        if (Minecraft.getInstance().getConnection() != null) {
            EncyclopediaNetwork.requestEntries();
        }

        int categoryButtonWidth = 80;
        int categoryGap = 6;
        int categoryTotalWidth = categoryButtonWidth * 3 + categoryGap * 2;
        int categoryStartX = (this.width - categoryTotalWidth) / 2;

        for (int i = 0; i < Category.values().length; i++) {
            Category category = Category.values()[i];
            int x = categoryStartX + i * (categoryButtonWidth + categoryGap);

            String label = category == selectedCategory
                    ? "> " + category.label
                    : category.label;

            this.addRenderableWidget(
                    Button.builder(
                            Component.literal(label),
                            button -> {
                                selectedCategory = category;
                                selectedEntry = null;
                                currentPage = 0;
                                this.clearWidgets();
                                this.init();
                            }
                    )
                    .bounds(x, 55, categoryButtonWidth, 20)
                    .build()
            );
        }

        if (adminButtonShown) {
            this.addRenderableWidget(
                    Button.builder(
                            Component.literal(
                                    adminTabActive ? "Admin: ON" : "Administrar"
                            ),
                            button -> {
                                adminTabActive = !adminTabActive;

                                if (!adminTabActive) {
                                    selectedEntry = null;
                                }

                                this.clearWidgets();
                                this.init();
                            }
                    )
                    .bounds(this.width - 110, 10, 100, 20)
                    .build()
            );
        }

        if (showAdminButtons()) {
            this.addRenderableWidget(
                    Button.builder(
                            Component.literal("Nueva entrada"),
                            button -> Minecraft.getInstance().setScreen(
                                    new EncyclopediaAdminScreen()
                            )
                    )
                    .bounds(this.width - 215, 10, 100, 20)
                    .build()
            );
        }

        if (selectedEntry == null) {
            createEntryButtons();

            if (showAdminButtons()) {
                this.addRenderableWidget(
                        Button.builder(
                                Component.literal("Nueva página"),
                                button -> {
                                    temporaryPageCount =
                                            Math.max(temporaryPageCount, pageCount) + 1;
                                    currentPage = temporaryPageCount - 1;
                                    this.clearWidgets();
                                    this.init();
                                }
                        )
                        .bounds(10, this.height - 35, 100, 20)
                        .build()
                );

                this.addRenderableWidget(
                        Button.builder(
                                Component.literal("Crear test"),
                                button -> {
                                    EncyclopediaData.Category testCategory =
                                            switch (selectedCategory) {
                                                case ITEM ->
                                                        EncyclopediaData.Category.ITEM;
                                                case BLOQUE ->
                                                        EncyclopediaData.Category.BLOCK;
                                                case MOB ->
                                                        EncyclopediaData.Category.MOB;
                                            };

                                    EncyclopediaData.Entry testEntry =
                                            new EncyclopediaData.Entry(
                                                    "minecraft:barrier",
                                                    testCategory,
                                                    "test",
                                                    "test"
                                            );

                                    temporaryTestEntries
                                            .computeIfAbsent(
                                                    getTemporaryPageKey(),
                                                    key -> new ArrayList<>()
                                            )
                                            .add(testEntry);

                                    this.clearWidgets();
                                    this.init();
                                }
                        )
                        .bounds(120, this.height - 35, 100, 20)
                        .build()
                );
            }
        } else {
            this.addRenderableWidget(
                    Button.builder(
                            Component.literal("Volver"),
                            button -> {
                                selectedEntry = null;
                                this.clearWidgets();
                                this.init();
                            }
                    )
                    .bounds(this.width / 2 - 105, this.height - 35, 100, 20)
                    .build()
            );
        }

        this.addRenderableWidget(
                Button.builder(
                        Component.literal("Cerrar"),
                        button -> this.onClose()
                )
                .bounds(this.width / 2 + 5, this.height - 35, 100, 20)
                .build()
        );
    }

    private void createEntryButtons() {
        rowsPerColumn = ROWS_PER_COLUMN;

        int usableWidth = this.width - 50;
        columnsPerPage = 0;

        for (int candidateColumns = 1;
                candidateColumns <= MAX_COLUMNS_PER_PAGE;
                candidateColumns++) {

            int requiredWidth =
                    candidateColumns * ENTRY_WIDTH
                    + (candidateColumns - 1) * ENTRY_GAP
                    + (candidateColumns > 3
                            ? EXTRA_GAP_AFTER_THIRD_COLUMN
                            : 0);

            if (requiredWidth > usableWidth) {
                break;
            }

            columnsPerPage = candidateColumns;
        }

        columnsPerPage = Math.max(1, columnsPerPage);

        List<EncyclopediaData.Entry> categoryEntries = getCategoryEntries();
        int pageSize = rowsPerColumn * columnsPerPage;

        int pagesForEntries = Math.max(
                1,
                (categoryEntries.size() + pageSize - 1) / pageSize
        );

        pageCount = Math.max(pagesForEntries, temporaryPageCount);
        currentPage = Math.max(0, Math.min(currentPage, pageCount - 1));

        int start = currentPage * pageSize;
        int end = Math.min(start + pageSize, categoryEntries.size());

        List<EncyclopediaData.Entry> pageEntries = new ArrayList<>();

        if (start < categoryEntries.size()) {
            pageEntries.addAll(categoryEntries.subList(start, end));
        }

        pageEntries.addAll(
                temporaryTestEntries.getOrDefault(
                        getTemporaryPageKey(),
                        List.of()
                )
        );

        visibleEntries = pageEntries;

        for (int i = 0; i < visibleEntries.size(); i++) {
            EncyclopediaData.Entry entry = visibleEntries.get(i);
            int column = i / rowsPerColumn;
            int row = i % rowsPerColumn;
            int x = getEntryX(column);
            int y = 120 + row * (ENTRY_HEIGHT + ENTRY_GAP);

            boolean canOpen = isTemporaryTest(entry)
                    || EncyclopediaClientData.hasDiscovered(entry);

            Button entryButton = Button.builder(
                    Component.literal(""),
                    button -> {
                        if (canOpen) {
                            selectedEntry = entry;
                            this.clearWidgets();
                            this.init();
                        }
                    }
            )
            .bounds(x, y, ENTRY_WIDTH - 24, ENTRY_HEIGHT)
            .build();

            entryButton.active = canOpen;
            entryButton.setAlpha(0.0F);
            this.addRenderableWidget(entryButton);

            if (showAdminButtons()) {
                this.addRenderableWidget(
                        Button.builder(
                                Component.literal("X"),
                                button -> deleteEntry(entry)
                        )
                        .bounds(x + ENTRY_WIDTH - 20, y, 20, ENTRY_HEIGHT)
                        .build()
                );
            }
        }

        if (pageCount > 1) {
            this.addRenderableWidget(
                    Button.builder(
                            Component.literal("Anterior"),
                            button -> {
                                currentPage--;
                                this.clearWidgets();
                                this.init();
                            }
                    )
                    .bounds(this.width / 2 - 90, this.height - 65, 85, 20)
                    .build()
            );

            this.addRenderableWidget(
                    Button.builder(
                            Component.literal("Siguiente"),
                            button -> {
                                currentPage++;
                                this.clearWidgets();
                                this.init();
                            }
                    )
                    .bounds(this.width / 2 + 5, this.height - 65, 85, 20)
                    .build()
            );
        }
    }

    private int getEntryX(int column) {
        return 25
                + column * (ENTRY_WIDTH + ENTRY_GAP)
                + (column >= 3 ? EXTRA_GAP_AFTER_THIRD_COLUMN : 0);
    }

    private List<EncyclopediaData.Entry> getCategoryEntries() {
        List<EncyclopediaData.Entry> result = new ArrayList<>();
        Set<String> keysStillOnServer = new HashSet<>();

        for (EncyclopediaData.Entry entry : EncyclopediaClientData.getEntries()) {
            String key = getEntryKey(entry);
            keysStillOnServer.add(key);

            if (belongsToSelectedCategory(entry)
                    && !locallyDeletedEntryKeys.contains(key)) {
                result.add(entry);
            }
        }

        locallyDeletedEntryKeys.removeIf(key -> !keysStillOnServer.contains(key));

        return result;
    }

    @Override
    public void tick() {
        super.tick();

        if (adminButtonShown != EncyclopediaClientData.canEdit()) {
            this.clearWidgets();
            this.init();
        }
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        this.renderBackground(graphics);
        NegativeUtilsGuiStyle.renderFrame(graphics, width, height);

        super.render(graphics, mouseX, mouseY, partialTick);

        Component screenTitle = selectedEntry == null
                ? Component.literal("negativeutils")
                : getEntryName(selectedEntry);

        graphics.drawCenteredString(
                this.font,
                screenTitle,
                this.width / 2,
                25,
                0xFFFFFF
        );

        if (selectedEntry == null) {
            renderEntryList(graphics);

            if (pageCount > 1) {
                graphics.drawCenteredString(
                        this.font,
                        "Página " + (currentPage + 1) + " / " + pageCount,
                        this.width / 2,
                        this.height - 88,
                        0xFFFFFF
                );
            }
        } else {
            renderEntryDetails(graphics, mouseX, mouseY);
        }
    }

    private void renderEntryList(GuiGraphics graphics) {
        graphics.drawCenteredString(
                this.font,
                "Categoría: " + selectedCategory.label,
                this.width / 2,
                90,
                0xFFFF55
        );

        if (visibleEntries.isEmpty()) {
            graphics.drawCenteredString(
                    this.font,
                    "Todavía no hay entradas en esta categoría.",
                    this.width / 2,
                    130,
                    0xAAAAAA
            );
            return;
        }

        for (int i = 0; i < visibleEntries.size(); i++) {
            EncyclopediaData.Entry entry = visibleEntries.get(i);
            int column = i / rowsPerColumn;
            int row = i % rowsPerColumn;
            int x = getEntryX(column);
            int y = 120 + row * (ENTRY_HEIGHT + ENTRY_GAP);

            boolean revealed = isTemporaryTest(entry)
                    || EncyclopediaClientData.hasDiscovered(entry);

            Component name = isTemporaryTest(entry)
                    ? Component.literal("test")
                    : revealed
                            ? getEntryName(entry)
                            : Component.literal("???");

            if (revealed) {
                ItemStack icon = getEntryIcon(entry);
                if (!icon.isEmpty()) {
                    graphics.renderItem(icon, x + 5, y + 3);
                }
            }

            graphics.drawString(
                    this.font,
                    name,
                    x + 28,
                    y + 7,
                    revealed ? 0xFFFFFF : 0xAAAAAA
            );
        }
    }

    private void renderEntryDetails(
            GuiGraphics graphics,
            int mouseX,
            int mouseY
    ) {
        graphics.drawCenteredString(
                this.font,
                getEntryName(selectedEntry),
                this.width / 2,
                90,
                0xFFFFFF
        );

        int previewX = this.width / 2 - 130;

        if (selectedEntry.category() == EncyclopediaData.Category.MOB
                && !isTemporaryTest(selectedEntry)) {
            renderMobPreview(graphics, selectedEntry, mouseX, mouseY);
        } else {
            ItemStack icon = getEntryIcon(selectedEntry);

            if (!icon.isEmpty()) {
                graphics.pose().pushPose();
                graphics.pose().translate(previewX, 145, 0);
                graphics.pose().scale(4.0F, 4.0F, 1.0F);
                graphics.renderItem(icon, 0, 0);
                graphics.pose().popPose();
            }
        }

        int textX = this.width / 2 + 25;
        int textY = 130;
        int textWidth = this.width / 2 - 55;

        graphics.drawString(this.font, "Descripción:", textX, textY, 0xFFFF55);

        List<net.minecraft.util.FormattedCharSequence> lines =
                this.font.split(
                        Component.literal(selectedEntry.description()),
                        textWidth
                );

        for (int i = 0; i < lines.size(); i++) {
            graphics.drawString(
                    this.font,
                    lines.get(i),
                    textX,
                    textY + 18 + i * 12,
                    0xFFFFFF
            );
        }
    }

    private void renderMobPreview(
        GuiGraphics graphics,
        EncyclopediaData.Entry entry,
        int mouseX,
        int mouseY
) {
    if (Minecraft.getInstance().level == null) {
        return;
    }

    ResourceLocation id = ResourceLocation.tryParse(entry.targetId());
    if (id == null) {
        return;
    }

    EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(id);
    if (type == null) {
        return;
    }

    Entity entity = type.create(Minecraft.getInstance().level);

    if (entity instanceof LivingEntity livingEntity) {
        float mouseOffsetX =
                (this.width / 2.0F - mouseX) * 0.45F;
        float mouseOffsetY =
                (180.0F - mouseY) * 0.45F;

        InventoryScreen.renderEntityInInventoryFollowsMouse(
                graphics,
                this.width / 2 - 85,
                245,
                70,
                mouseOffsetX,
                mouseOffsetY,
                livingEntity
        );
    }
}

    private boolean belongsToSelectedCategory(
            EncyclopediaData.Entry entry
    ) {
        return switch (selectedCategory) {
            case ITEM -> entry.category() == EncyclopediaData.Category.ITEM;
            case BLOQUE -> entry.category() == EncyclopediaData.Category.BLOCK;
            case MOB -> entry.category() == EncyclopediaData.Category.MOB;
        };
    }

    private Component getEntryName(EncyclopediaData.Entry entry) {
        if (isTemporaryTest(entry)) {
            return Component.literal("test");
        }

        ResourceLocation id = ResourceLocation.tryParse(entry.targetId());

        if (id == null) {
            return Component.literal(entry.targetId());
        }

        return switch (entry.category()) {
            case ITEM -> {
                var item = ForgeRegistries.ITEMS.getValue(id);
                yield item == null
                        ? Component.literal(entry.targetId())
                        : item.getDescription();
            }
            case BLOCK -> {
                Block block = ForgeRegistries.BLOCKS.getValue(id);
                yield block == null
                        ? Component.literal(entry.targetId())
                        : block.getName();
            }
            case MOB -> {
                var type = ForgeRegistries.ENTITY_TYPES.getValue(id);
                yield type == null
                        ? Component.literal(entry.targetId())
                        : type.getDescription();
            }
        };
    }

    private ItemStack getEntryIcon(EncyclopediaData.Entry entry) {
    ResourceLocation id = ResourceLocation.tryParse(entry.targetId());

    if (id == null) {
        return ItemStack.EMPTY;
    }

    if (isTemporaryTest(entry)) {
        var barrier = ForgeRegistries.ITEMS.getValue(
                ResourceLocation.fromNamespaceAndPath(
                        "minecraft",
                        "barrier"
                )
        );

        return barrier == null
                ? ItemStack.EMPTY
                : new ItemStack(barrier);
    }

    return switch (entry.category()) {
        case ITEM -> {
            var item = ForgeRegistries.ITEMS.getValue(id);
            yield item == null
                    ? ItemStack.EMPTY
                    : new ItemStack(item);
        }
        case BLOCK -> {
            Block block = ForgeRegistries.BLOCKS.getValue(id);
            yield block == null
                    ? ItemStack.EMPTY
                    : new ItemStack(block);
        }
        case MOB -> {
            EntityType<?> entityType =
                    ForgeRegistries.ENTITY_TYPES.getValue(id);

            if (entityType == null) {
                yield ItemStack.EMPTY;
            }

            var spawnEgg = ForgeSpawnEggItem.fromEntityType(entityType);

            // Algunos mobs no tienen huevo propio; en ese caso muestra un huevo normal.
            yield spawnEgg == null
                    ? new ItemStack(Items.EGG)
                    : new ItemStack(spawnEgg);
        }
    };
}

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}