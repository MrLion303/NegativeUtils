package com.negative.negativeutils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
        this.originalDraft = draft;
        ArrayList<EmoteChoice> choices = new ArrayList<>();
        DiscordEmoteClientData.getEmotes().forEach((name, codePoint) ->
                choices.add(new EmoteChoice(
                        name,
                        ":" + name + ":",
                        codePoint
                ))
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
                        String.CASE_INSENSITIVE_ORDER.compare(
                                first.name(),
                                second.name()
                        ))
                .toList();
    }

    @Override
    protected void init() {
        columns = Math.max(1, Math.min(6, (width - 32) / 96));
        int pageSize = columns * ROWS;
        int pageCount = Math.max(1, (emotes.size() + pageSize - 1) / pageSize);
        page = Math.min(page, pageCount - 1);

        int margin = 16;
        int gap = 4;
        int buttonWidth = Math.max(
                32,
                (width - margin * 2 - gap * (columns - 1)) / columns
        );
        int gridWidth = buttonWidth * columns + gap * (columns - 1);
        int startX = (width - gridWidth) / 2;
        int startY = Math.max(42, (height - ROWS * 28) / 2);
        int firstIndex = page * pageSize;
        int lastIndex = Math.min(emotes.size(), firstIndex + pageSize);

        for (int index = firstIndex; index < lastIndex; index++) {
            EmoteChoice emote = emotes.get(index);
            int gridIndex = index - firstIndex;
            String name = emote.name();
            Component label;
            if (emote.atlasCodePoint() != null) {
                label = Component.literal(
                                new String(Character.toChars(
                                        emote.atlasCodePoint()
                                ))
                        )
                        .withStyle(style -> style.withFont(
                                DiscordEmoteClientData.EMOTE_FONT
                        ))
                        .append(Component.literal(" " + name));
            } else {
                label = Component.literal(name + " " + emote.text());
            }
            addRenderableWidget(
                    Button.builder(
                                    label,
                                    button -> insertEmote(emote.text())
                            )
                            .bounds(
                                    startX + gridIndex % columns * (buttonWidth + gap),
                                    startY + gridIndex / columns * 28,
                                    buttonWidth,
                                    24
                            )
                            .tooltip(Tooltip.create(
                                    Component.literal(emote.text())
                            ))
                            .build()
            );
        }

        int pageCountForButtons = pageCount;
        addRenderableWidget(
                Button.builder(
                                Component.literal("Cerrar"),
                                button -> returnToChat(originalDraft)
                        )
                        .bounds(width / 2 - 102, height - 30, 96, 20)
                        .build()
        );
        if (pageCountForButtons > 1) {
            addRenderableWidget(
                    Button.builder(
                                    Component.literal("<"),
                                    button -> {
                                        page = (page + pageCountForButtons - 1)
                                                % pageCountForButtons;
                                        rebuildWidgets();
                                    }
                            )
                            .bounds(width / 2 + 6, height - 30, 40, 20)
                            .build()
            );
            addRenderableWidget(
                    Button.builder(
                                    Component.literal(">"),
                                    button -> {
                                        page = (page + 1) % pageCountForButtons;
                                        rebuildWidgets();
                                    }
                            )
                            .bounds(width / 2 + 50, height - 30, 40, 20)
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
                16,
                0xFFFFFF
        );
        if (emotes.isEmpty()) {
            graphics.drawCenteredString(
                    font,
                    "Todavía no hay emotes sincronizados.",
                    width / 2,
                    height / 2,
                    0xCCCCCC
            );
        } else {
            int pageSize = columns * ROWS;
            int pageCount = (emotes.size() + pageSize - 1) / pageSize;
            graphics.drawCenteredString(
                    font,
                    "Página " + (page + 1) + " / " + pageCount
                            + " — selecciona un emote para insertarlo",
                    width / 2,
                    30,
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
