package com.negative.negativeutils;

import com.mojang.brigadier.Command;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = EnciclopediaMod.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class DiscordEmoteEvents {
    private static final Pattern EMOTE_PATTERN =
            Pattern.compile(":([A-Za-z0-9_]{2,32}):");

    private DiscordEmoteEvents() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("negativeutils")
                        .requires(source -> source.hasPermission(2))
                        .then(
                                Commands.literal("discord")
                                        .then(
                                                Commands.literal("token")
                                                        .executes(context -> {
                                                            ServerPlayer player =
                                                                    context.getSource()
                                                                            .getPlayerOrException();
                                                            DiscordEmoteNetwork
                                                                    .openTokenScreen(player);
                                                            context.getSource()
                                                                    .sendSuccess(
                                                                            () -> Component.literal(
                                                                                    "Abriendo el campo seguro para el token del bot."
                                                                            ),
                                                                            false
                                                                    );
                                                            return Command.SINGLE_SUCCESS;
                                                        })
                                        )
                                        .then(
                                                Commands.literal("sync")
                                                        .executes(context -> {
                                                            try {
                                                                DiscordEmoteService
                                                                        .requestRefresh();
                                                                context.getSource()
                                                                        .sendSuccess(
                                                                                () -> Component.literal(
                                                                                        "Sincronización de emotes solicitada mediante DiscordSRV o el token configurado."
                                                                                ),
                                                                                false
                                                                        );
                                                                return Command.SINGLE_SUCCESS;
                                                            } catch (IllegalStateException exception) {
                                                                context.getSource()
                                                                        .sendFailure(
                                                                                Component.literal(
                                                                                        exception.getMessage()
                                                                                )
                                                                        );
                                                                return 0;
                                                            }
                                                        })
                                        )
                                        .then(
                                                Commands.literal("status")
                                                        .executes(context -> {
                                                            context.getSource()
                                                                    .sendSuccess(
                                                                            () -> Component.literal(
                                                                                    DiscordEmoteService.getStatus()
                                                                            ),
                                                                            false
                                                                    );
                                                            return Command.SINGLE_SUCCESS;
                                                        })
                                        )
                        )
        );
    }

    @SubscribeEvent
    public static void onServerStarted(
            net.minecraftforge.event.server.ServerStartedEvent event
    ) {
        DiscordEmoteService.start(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStopping(
            net.minecraftforge.event.server.ServerStoppingEvent event
    ) {
        DiscordEmoteService.stop();
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && !DiscordEmoteService.getEmotes().isEmpty()) {
            DiscordEmoteNetwork.syncToPlayer(
                    player,
                    DiscordEmoteService.getEmotes(),
                    DiscordEmoteService.getAtlasBytes()
            );
        }
    }

    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        Map<String, Integer> emotes = DiscordEmoteService.getEmotes();
        if (emotes.isEmpty()) {
            return;
        }

        String rawText = event.getRawText();
        Matcher matcher = EMOTE_PATTERN.matcher(rawText);
        MutableComponent decorated = Component.empty();
        int cursor = 0;
        boolean replaced = false;

        while (matcher.find()) {
            Integer codePoint = emotes.get(matcher.group(1));
            if (codePoint == null) {
                continue;
            }
            if (matcher.start() > cursor) {
                decorated.append(
                        Component.literal(rawText.substring(cursor, matcher.start()))
                );
            }
            decorated.append(
                    Component.literal(
                            new String(Character.toChars(codePoint))
                    ).withStyle(style -> style.withFont(
                            ResourceLocation.fromNamespaceAndPath(
                                    EnciclopediaMod.MOD_ID,
                                    "discord_emotes"
                            )
                    ))
            );
            cursor = matcher.end();
            replaced = true;
        }

        if (replaced) {
            if (cursor < rawText.length()) {
                decorated.append(Component.literal(rawText.substring(cursor)));
            }
            event.setMessage(decorated);
        }
    }
}
