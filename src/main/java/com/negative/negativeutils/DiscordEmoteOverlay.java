package com.negative.negativeutils;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;

public final class DiscordEmoteOverlay {
    private static final int COLUMNS = 6;
    private static final int ROWS = 3;
    private static final int BUTTON = 28;
    private static final int GAP = 3;
    private static final int PANEL_PADDING = 7;

    private final List<Choice> choices = new ArrayList<>();
    private int scrollRow;
    private boolean draggingScrollbar;

    public DiscordEmoteOverlay() {
        DiscordEmoteClientData.getEmotes().forEach((name, codePoint) ->
                choices.add(new Choice(name, ":" + name + ":", codePoint)));
        choices.addAll(List.of(
                new Choice("grinning", "😀", null), new Choice("smiley", "😃", null),
                new Choice("smile", "😄", null), new Choice("laughing", "😆", null),
                new Choice("sweat_smile", "😅", null), new Choice("joy", "😂", null),
                new Choice("rofl", "🤣", null), new Choice("blush", "😊", null),
                new Choice("innocent", "😇", null), new Choice("heart_eyes", "😍", null),
                new Choice("kissing_heart", "😘", null), new Choice("thinking", "🤔", null),
                new Choice("sunglasses", "😎", null), new Choice("shrug", "🤷", null),
                new Choice("sob", "😭", null), new Choice("cry", "😢", null),
                new Choice("angry", "😠", null), new Choice("rage", "😡", null),
                new Choice("skull", "💀", null), new Choice("clown", "🤡", null),
                new Choice("ghost", "👻", null), new Choice("poop", "💩", null),
                new Choice("wave", "👋", null), new Choice("thumbsup", "👍", null),
                new Choice("thumbsdown", "👎", null), new Choice("clap", "👏", null),
                new Choice("pray", "🙏", null), new Choice("muscle", "💪", null),
                new Choice("fire", "🔥", null), new Choice("sparkles", "✨", null),
                new Choice("star", "⭐", null), new Choice("heart", "❤️", null),
                new Choice("broken_heart", "💔", null), new Choice("hundred", "💯", null),
                new Choice("tada", "🎉", null), new Choice("party", "🥳", null),
                new Choice("rocket", "🚀", null), new Choice("check", "✅", null),
                new Choice("cross", "❌", null), new Choice("warning", "⚠️", null),
                new Choice("eyes", "👀", null), new Choice("cat", "🐱", null),
                new Choice("dog", "🐶", null), new Choice("pizza", "🍕", null),
                new Choice("coffee", "☕", null), new Choice("music", "🎵", null)
        ));
        choices.sort((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.name(), b.name()));
    }

    public void scroll(double amount) {
        int rows = Math.max(1, (choices.size() + COLUMNS - 1) / COLUMNS);
        int maxScroll = Math.max(0, rows - ROWS);
        scrollRow = Math.max(0, Math.min(maxScroll, scrollRow - (int) Math.signum(amount)));
    }

    public boolean beginScrollbarDrag(double mouseX, double mouseY, int width, int height) {
        Bounds b = bounds(width, height);
        Scrollbar s = scrollbar(b);
        if (mouseX >= s.left && mouseX <= s.right && mouseY >= s.thumbTop && mouseY <= s.thumbTop + s.thumbHeight) {
            draggingScrollbar = true;
            return true;
        }
        return false;
    }

    public boolean dragScrollbar(double mouseY, int width, int height) {
        if (!draggingScrollbar) return false;
        Bounds b = bounds(width, height);
        Scrollbar s = scrollbar(b);
        int rows = Math.max(1, (choices.size() + COLUMNS - 1) / COLUMNS);
        int maxScroll = Math.max(0, rows - ROWS);
        if (maxScroll > 0) {
            double progress = (mouseY - s.trackTop - s.thumbHeight / 2.0) / Math.max(1, s.trackHeight - s.thumbHeight);
            scrollRow = Math.max(0, Math.min(maxScroll, (int) Math.round(progress * maxScroll)));
        }
        return true;
    }

    public boolean endScrollbarDrag() {
        if (!draggingScrollbar) return false;
        draggingScrollbar = false;
        return true;
    }

    public boolean contains(double mouseX, double mouseY, int width, int height) {
        Bounds b = bounds(width, height);
        return mouseX >= b.left && mouseX <= b.right && mouseY >= b.top && mouseY <= b.bottom;
    }

    public boolean click(double mouseX, double mouseY, int width, int height, EditBox chatInput) {
        Bounds b = bounds(width, height);
        if (!contains(mouseX, mouseY, width, height)) return false;

        int gridLeft = b.left + PANEL_PADDING;
        int gridTop = b.top + 18;
        int column = (int) ((mouseX - gridLeft) / (BUTTON + GAP));
        int row = (int) ((mouseY - gridTop) / (BUTTON + GAP));
        if (column < 0 || column >= COLUMNS || row < 0 || row >= ROWS) return true;

        int index = (scrollRow + row) * COLUMNS + column;
        if (index < choices.size()
                && mouseX < gridLeft + column * (BUTTON + GAP) + BUTTON
                && mouseY < gridTop + row * (BUTTON + GAP) + BUTTON) {
            String value = choices.get(index).text();
            String draft = chatInput.getValue();
            if (!draft.isEmpty() && !Character.isWhitespace(draft.charAt(draft.length() - 1))) {
                draft += " ";
            }
            chatInput.setValue(draft + value);
            chatInput.setCursorPosition(chatInput.getValue().length());
        }
        return true;
    }

    public void render(GuiGraphics graphics, int width, int height, int mouseX, int mouseY) {
        Bounds b = bounds(width, height);
        graphics.fill(b.left, b.top, b.right, b.bottom, 0xF0141B24);
        graphics.fill(b.left, b.top, b.right, b.top + 2, 0xFF54D6FF);
        graphics.drawString(Minecraft.getInstance().font, "Emotes", b.left + 7, b.top + 6, 0xFFFFFFFF);

        int gridLeft = b.left + PANEL_PADDING;
        int gridTop = b.top + 18;
        for (int row = 0; row < ROWS; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                int index = (scrollRow + row) * COLUMNS + column;
                if (index >= choices.size()) continue;
                Choice choice = choices.get(index);
                int x = gridLeft + column * (BUTTON + GAP);
                int y = gridTop + row * (BUTTON + GAP);
                boolean hovered = mouseX >= x && mouseX < x + BUTTON
                        && mouseY >= y && mouseY < y + BUTTON;
                graphics.fill(x, y, x + BUTTON, y + BUTTON, hovered ? 0xFF2A3845 : 0xFF202932);
                Component label = choice.atlasCodePoint() != null
                        ? Component.literal(new String(Character.toChars(choice.atlasCodePoint())))
                            .withStyle(style -> style.withFont(DiscordEmoteClientData.EMOTE_FONT))
                        : Component.literal(choice.text());
                graphics.drawCenteredString(Minecraft.getInstance().font, label,
                        x + BUTTON / 2, y + 9, 0xFFFFFFFF);
            }
        }

        Scrollbar s = scrollbar(b);
        graphics.fill(s.left, s.trackTop, s.right, s.trackTop + s.trackHeight, 0xFF151B22);
        graphics.fill(s.left, s.thumbTop, s.right, s.thumbTop + s.thumbHeight, 0xFF667381);
        graphics.fill(s.left, s.thumbTop, s.right, s.thumbTop + 2, 0xFF9AA7B5);
    }

    private Bounds bounds(int width, int height) {
        int panelWidth = COLUMNS * BUTTON + (COLUMNS - 1) * GAP + PANEL_PADDING * 2;
        int panelHeight = ROWS * BUTTON + (ROWS - 1) * GAP + 25;
        int buttonCenter = width - 40;
        int left = Math.max(6, buttonCenter - panelWidth / 2);
        int right = Math.min(width - 6, left + panelWidth);
        left = Math.max(6, right - panelWidth);
        int bottom = height - 45;
        int top = Math.max(6, bottom - panelHeight);
        return new Bounds(left, top, right, bottom);
    }

    private Scrollbar scrollbar(Bounds b) {
        int trackTop = b.top + 20;
        int trackHeight = b.bottom - trackTop - 7;
        int rows = Math.max(1, (choices.size() + COLUMNS - 1) / COLUMNS);
        int maxScroll = Math.max(0, rows - ROWS);
        int thumbHeight = maxScroll == 0 ? trackHeight : Math.max(14, trackHeight * ROWS / rows);
        int thumbTop = maxScroll == 0 ? trackTop : trackTop + (trackHeight - thumbHeight) * scrollRow / maxScroll;
        return new Scrollbar(b.right - 6, trackTop, b.right - 2, trackHeight, thumbTop, thumbHeight);
    }

    private record Bounds(int left, int top, int right, int bottom) {}
    private record Scrollbar(int left, int trackTop, int right, int trackHeight, int thumbTop, int thumbHeight) {}
    private record Choice(String name, String text, Integer atlasCodePoint) {}
}
