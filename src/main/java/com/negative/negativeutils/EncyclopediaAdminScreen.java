package com.negative.negativeutils;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class EncyclopediaAdminScreen extends Screen {
    private EncyclopediaData.Category selectedCategory =
            EncyclopediaData.Category.ITEM;

    private final List<Button> categoryButtons = new ArrayList<>();

    private EditBox targetField;
    private EditBox descriptionField;
    private EditBox notificationField;

    public EncyclopediaAdminScreen() {
        super(Component.literal("Crear entrada"));
    }

    @Override
    protected void init() {
        categoryButtons.clear();

        int buttonWidth = 90;
        int gap = 6;
        int totalWidth = buttonWidth * 3 + gap * 2;
        int startX = (this.width - totalWidth) / 2;

        EncyclopediaData.Category[] categories =
                EncyclopediaData.Category.values();

        for (int i = 0; i < categories.length; i++) {
            EncyclopediaData.Category category = categories[i];
            int x = startX + i * (buttonWidth + gap);

            Button button = Button.builder(
                    categoryLabel(category),
                    ignored -> {
                        selectedCategory = category;
                        updateCategoryButtons();
                    }
            ).bounds(x, 55, buttonWidth, 20).build();

            categoryButtons.add(button);
            this.addRenderableWidget(button);
        }

        int fieldX = 40;
        int fieldWidth = this.width - 80;

        targetField = new EditBox(
                this.font, fieldX, 105, fieldWidth, 20,
                Component.literal("Identificador")
        );
        targetField.setMaxLength(256);
        targetField.setHint(Component.literal("Ejemplo: minecraft:zombie"));
        this.addRenderableWidget(targetField);

        descriptionField = new EditBox(
                this.font, fieldX, 155, fieldWidth, 20,
                Component.literal("Descripción")
        );
        descriptionField.setMaxLength(300);
        descriptionField.setHint(Component.literal("Descripción de la entrada"));
        this.addRenderableWidget(descriptionField);

        notificationField = new EditBox(
                this.font, fieldX, 205, fieldWidth, 20,
                Component.literal("Notificación")
        );
        notificationField.setMaxLength(100);
        notificationField.setHint(
                Component.literal("Ejemplo: ¡Has descubierto un zombie!")
        );
        this.addRenderableWidget(notificationField);

        this.addRenderableWidget(
                Button.builder(
                        Component.literal("APLICAR"),
                        button -> applyEntry()
                )
                .bounds(this.width / 2 - 100, this.height - 40, 95, 20)
                .build()
        );

        this.addRenderableWidget(
                Button.builder(
                        Component.literal("Volver"),
                        button -> Minecraft.getInstance().setScreen(
                                new EncyclopediaScreen()
                        )
                )
                .bounds(this.width / 2 + 5, this.height - 40, 95, 20)
                .build()
        );
    }

    private Component categoryLabel(EncyclopediaData.Category category) {
        String label = switch (category) {
            case ITEM -> "Item";
            case BLOCK -> "Bloque";
            case MOB -> "Mob";
        };

        if (category == selectedCategory) {
            label = "> " + label;
        }

        return Component.literal(label);
    }

    private void updateCategoryButtons() {
        EncyclopediaData.Category[] categories =
                EncyclopediaData.Category.values();

        for (int i = 0; i < categoryButtons.size(); i++) {
            categoryButtons.get(i).setMessage(categoryLabel(categories[i]));
        }
    }

    private void applyEntry() {
        String targetId = targetField.getValue().trim();
        String description = descriptionField.getValue().trim();
        String notification = notificationField.getValue().trim();

        if (targetId.isEmpty() || description.isEmpty() || notification.isEmpty()) {
            return;
        }

        EncyclopediaData.Entry entry = new EncyclopediaData.Entry(
                targetId,
                selectedCategory,
                description,
                notification
        );

        EncyclopediaNetwork.submitEntry(entry);
        Minecraft.getInstance().setScreen(new EncyclopediaScreen());
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

        graphics.drawCenteredString(
                this.font,
                "Crear entrada",
                this.width / 2,
                25,
                0xFFFFFF
        );

        graphics.drawString(this.font, "Categoría", 40, 42, 0xFFFFFF);
        graphics.drawString(this.font, "Cosa a descubrir (ID)", 40, 92, 0xFFFFFF);
        graphics.drawString(this.font, "Descripción", 40, 142, 0xFFFFFF);
        graphics.drawString(this.font, "Texto de notificación", 40, 192, 0xFFFFFF);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}