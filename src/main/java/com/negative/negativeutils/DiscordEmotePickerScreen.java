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
    private static final int ROWS=5;
    private final String originalDraft;
    private final List<EmoteChoice> emotes;
    private int page,columns;

    public DiscordEmotePickerScreen(String draft){
        super(Component.literal("Emotes"));
        originalDraft=draft;ArrayList<EmoteChoice> choices=new ArrayList<>();
        DiscordEmoteClientData.getEmotes().forEach((name,codePoint)->choices.add(new EmoteChoice(name,":"+name+":",codePoint)));
        choices.addAll(List.of(
            new EmoteChoice("grinning","😀",null),new EmoteChoice("smiley","😃",null),new EmoteChoice("smile","😄",null),
            new EmoteChoice("laughing","😆",null),new EmoteChoice("sweat_smile","😅",null),new EmoteChoice("joy","😂",null),
            new EmoteChoice("rofl","🤣",null),new EmoteChoice("blush","😊",null),new EmoteChoice("innocent","😇",null),
            new EmoteChoice("heart_eyes","😍",null),new EmoteChoice("kissing_heart","😘",null),new EmoteChoice("thinking","🤔",null),
            new EmoteChoice("sunglasses","😎",null),new EmoteChoice("shrug","🤷",null),new EmoteChoice("sob","😭",null),
            new EmoteChoice("cry","😢",null),new EmoteChoice("angry","😠",null),new EmoteChoice("rage","😡",null),
            new EmoteChoice("skull","💀",null),new EmoteChoice("clown","🤡",null),new EmoteChoice("ghost","👻",null),
            new EmoteChoice("poop","💩",null),new EmoteChoice("wave","👋",null),new EmoteChoice("thumbsup","👍",null),
            new EmoteChoice("thumbsdown","👎",null),new EmoteChoice("clap","👏",null),new EmoteChoice("pray","🙏",null),
            new EmoteChoice("muscle","💪",null),new EmoteChoice("fire","🔥",null),new EmoteChoice("sparkles","✨",null),
            new EmoteChoice("star","⭐",null),new EmoteChoice("heart","❤️",null),new EmoteChoice("broken_heart","💔",null),
            new EmoteChoice("hundred","💯",null),new EmoteChoice("tada","🎉",null),new EmoteChoice("party","🥳",null),
            new EmoteChoice("rocket","🚀",null),new EmoteChoice("check","✅",null),new EmoteChoice("cross","❌",null),
            new EmoteChoice("warning","⚠️",null),new EmoteChoice("eyes","👀",null),new EmoteChoice("cat","🐱",null),
            new EmoteChoice("dog","🐶",null),new EmoteChoice("pizza","🍕",null),new EmoteChoice("coffee","☕",null),
            new EmoteChoice("music","🎵",null)));
        emotes=choices.stream().sorted((a,b)->String.CASE_INSENSITIVE_ORDER.compare(a.name(),b.name())).toList();
    }

    @Override protected void init(){
        columns=Math.max(4,Math.min(7,(width-80)/88));int pageSize=columns*ROWS;
        int pageCount=Math.max(1,(emotes.size()+pageSize-1)/pageSize);page=Math.min(page,pageCount-1);
        int gap=6,buttonWidth=Math.max(64,(width-70-gap*(columns-1))/columns);
        int gridWidth=buttonWidth*columns+gap*(columns-1),startX=(width-gridWidth)/2,startY=62;
        int first=page*pageSize,last=Math.min(emotes.size(),first+pageSize);
        for(int i=first;i<last;i++){
            EmoteChoice e=emotes.get(i);int gi=i-first;Component label=e.atlasCodePoint()!=null
                ?Component.literal(new String(Character.toChars(e.atlasCodePoint()))).withStyle(s->s.withFont(DiscordEmoteClientData.EMOTE_FONT)).append(Component.literal(" "+e.name()))
                :Component.literal(e.text()+"  "+e.name());
            addRenderableWidget(Button.builder(label,b->insertEmote(e.text())).bounds(startX+(gi%columns)*(buttonWidth+gap),startY+(gi/columns)*30,buttonWidth,26)
                .tooltip(Tooltip.create(Component.literal(e.text()))).build());
        }
        addRenderableWidget(Button.builder(Component.literal("Cerrar"),b->returnToChat(originalDraft)).bounds(width/2-130,height-32,80,22).build());
        if(pageCount>1){
            addRenderableWidget(Button.builder(Component.literal("‹"),b->{page=(page+pageCount-1)%pageCount;rebuildWidgets();}).bounds(width/2-42,height-32,32,22).build());
            addRenderableWidget(Button.builder(Component.literal("›"),b->{page=(page+1)%pageCount;rebuildWidgets();}).bounds(width/2+10,height-32,32,22).build());
        }
    }
    private void insertEmote(String token){String draft=originalDraft;if(!draft.isEmpty()&&!Character.isWhitespace(draft.charAt(draft.length()-1)))draft+=" ";returnToChat(draft+token);}
    private void returnToChat(String draft){minecraft.setScreen(new ChatScreen(draft));}

    @Override public void render(GuiGraphics g,int mx,int my,float pt){
        renderBackground(g);
        int panelLeft=18,panelRight=width-18;
        g.fill(0,0,width,height,0xB0101218);
        g.fill(panelLeft,10,panelRight,height-10,0xE51A1E26);
        g.fill(panelLeft,10,panelRight,12,0xFF5865F2);
        g.drawString(font,"✦ Emotes de Discord",32,22,0xFFFFFFFF);
        int pageSize=columns*ROWS,pageCount=Math.max(1,(emotes.size()+pageSize-1)/pageSize);
        g.drawString(font,"Selecciona un emote para insertarlo en el chat",32,38,0xFFB9BEC8);
        g.drawString(font,(page+1)+" / "+pageCount, width-62,24,0xFFB9BEC8);
        super.render(g,mx,my,pt);
    }
    @Override public boolean isPauseScreen(){return false;}
    private record EmoteChoice(String name,String text,Integer atlasCodePoint){}
}