package com.negative.negativeutils;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class DiscordEmotePickerScreen extends Screen {
    private static final int ROWS = 5;
    private final String originalDraft;
    private final List<EmoteChoice> emotes;
    private int page;
    private int columns;

    public DiscordEmotePickerScreen(String draft) {
        super(Component.literal("Emotes de Discord"));
        originalDraft = draft;

        ArrayList<EmoteChoice> choices = new ArrayList<>();
        DiscordEmoteClientData.getEmotes().forEach((name, codePoint) ->
                choices.add(new EmoteChoice(name, ":" + name + ":", codePoint))
        );

        choices.addAll(List.of(
                new EmoteChoice("grinning", "😀", null),
                new EmoteChoice("smiley", "😃", null),
                new EmoteChoice("smile", "😄", null),
                new EmoteChoice("laughing", "😆", null),
                new EmoteChoice("sweat_smile", "😅", null),
                new EmoteChoice("joy", "😂", null),
                new EmoteChoice("rofl", "🤣", null),
                new EmoteChoice("blush", "😊", null),
                new EmoteChoice("innocent", "😇", null),
                new EmoteChoice("heart_eyes", "😍", null),
                new EmoteChoice("kissing_heart", "😘", null),
                new EmoteChoice("thinking", "🤔", null),
                new EmoteChoice("sunglasses", "😎", null),
                new EmoteChoice("shrug", "🤷", null),
                new EmoteChoice("sob", "😭", null),
                new EmoteChoice("cry", "😢", null),
                new EmoteChoice("angry", "😠", null),
                new EmoteChoice("rage", "😡", null),
                new EmoteChoice("skull", "💀", null),
                new EmoteChoice("clown", "🤡", null),
                new EmoteChoice("ghost", "👻", null),
                new EmoteChoice("poop", "💩", null),
                new EmoteChoice("wave", "👋", null),
                new EmoteChoice("thumbsup", "👍", null),
                new EmoteChoice("thumbsdown", "👎", null),
                new EmoteChoice("clap", "👏", null),
                new EmoteChoice("pray", "🙏", null),
                new EmoteChoice("muscle", "💪", null),
                new EmoteChoice("fire", "🔥", null),
                new EmoteChoice("sparkles", "✨", null),
                new EmoteChoice("star", "⭐", null),
                new EmoteChoice("heart", "❤️", null),
                new EmoteChoice("broken_heart", "💔", null),
                new EmoteChoice("hundred", "💯", null),
                new EmoteChoice("tada", "🎉", null),
                new EmoteChoice("party", "🥳", null),
                new EmoteChoice("rocket", "🚀", null),
                new EmoteChoice("check", "✅", null),
                new EmoteChoice("cross", "❌", null),
                new EmoteChoice("warning", "⚠️", null),
                new EmoteChoice("eyes", "👀", null),
                new EmoteChoice("cat", "🐱", null),
                new EmoteChoice("dog", "🐶", null),
                new EmoteChoice("pizza", "🍕", null),
                new EmoteChoice("coffee", "☕", null),
                new EmoteChoice("music", "🎵", null)
        ));

        emotes = choices.stream()
                .sorted((first, second) ->
                        String.CASE_INSENSITIVE_ORDER.compare(first.name(), second.name()))
                .toList();
    }

    @Override
    protected void init() {
        columns = Math.max(4, Math.min(7, (width - 80) / 88));

        int pageSize = columns * ROWS;
        int pageCount = Math.max(1, (emotes.size() + pageSize - 1) / pageSize);
        page = Math.min(page, pageCount - 1);

        int gap = 6;
        int buttonWidth = Math.max(64, (width - 70 - gap * (columns - 1)) / columns);
        int gridWidth = buttonWidth * columns + gap * (columns - 1);
        int startX = (width - gridWidth) / 2;
        int startY = 62;

        int first = page * pageSize;
        int last = Math.min(emotes.size(), first + pageSize);

        for (int index = first; index < last; index++) {
            EmoteChoice emote = emotes.get(index);
            int gridIndex = index - first;

            Component label;
            if (emote.atlasCodePoint() != null) {
                label = Component.literal(
                                new String(Character.toChars(emote.atlasCodePoint())))
                        .withStyle(style -> style.withFont(
                                DiscordEmoteClientData.EMOTE_FONT))
                        .append(Component.literal(" " + emote.name()));
            } else {
                label = Component.literal(emote.text() + "  " + emote.name());
            }

            addRenderableWidget(
                    Button.builder(label, button -> insertEmote(emote.text()))
                            .bounds(
                                    startX + (gridIndex % columns) * (buttonWidth + gap),
                                    startY + (gridIndex / columns) * 30,
                                    buttonWidth,
                                    26
                            )
                            .tooltip(Tooltip.create(Component.literal(emote.text())))
                            .build()
            );
        }

        addRenderableWidget(
                Button.builder(
                                Component.literal("Cerrar"),
                                button -> returnToChat(originalDraft))
                        .bounds(width / 2 - 116, height - 32, 72, 22)
                        .build()
        );

        if (pageCount > 1) {
            addRenderableWidget(
                    Button.builder(
                                    Component.literal("‹"),
                                    button -> {
                                        page = (page + pageCount - 1) % pageCount;
                                        rebuildWidgets();
                                    })
                            .bounds(width / 2 - 38, height - 32, 30, 22)
                            .build()
            );
            addRenderableWidget(
                    Button.builder(
                                    Component.literal("›"),
                                    button -> {
                                        page = (page + 1) % pageCount;
                                        rebuildWidgets();
                                    })
                            .bounds(width / 2 + 8, height - 32, 30, 22)
                            .build()
            );
        }
    }

    private void insertEmote(String token) {
        String draft = originalDraft;
        if (!draft.isEmpty()
                && !Character.isWhitespace(draft.charAt(draft.length() - 1))) {
            draft += " ";
        }
        returnToChat(draft + token);
    }

    private void returnToChat(String draft) {
        minecraft.setScreen(new ChatScreen(draft));
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        renderBackground(graphics);
        NegativeUtilsGuiStyle.renderFrame(graphics, width, height);

        graphics.drawCenteredString(
                font,
                title,
                width / 2,
                14,
                0xFFFFFF
        );

        int pageSize = columns * ROWS;
        int pageCount = Math.max(1, (emotes.size() + pageSize - 1) / pageSize);

        graphics.drawCenteredString(
                font,
                "Página " + (page + 1) + " / " + pageCount
                        + " — selecciona un emote para insertarlo",
                width / 2,
                30,
                0xCCCCCC
        );

        if (emotes.isEmpty()) {
            graphics.drawCenteredString(
                    font,
                    "Todavía no hay emotes sincronizados.",
                    width / 2,
                    height / 2,
                    0xCCCCCC
            );
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record EmoteChoice(
            String name,
            String text,
            Integer atlasCodePoint
    ) {
    }
}
