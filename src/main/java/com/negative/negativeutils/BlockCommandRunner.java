package com.negative.negativeutils;

import com.mojang.logging.LogUtils;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

final class BlockCommandRunner {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Pattern SIMPLE_PLAYSOUND = Pattern.compile(
            "^playsound\\s+(@\\S+)\\s+(\\S+)(.*)$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern SIMPLE_TITLE = Pattern.compile(
            "^title\\s+(@\\S+)\\s+(\".*\")$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern SIMPLE_SAY = Pattern.compile(
            "^say\\s+\"(.*)\"$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern SIMPLE_EFFECT = Pattern.compile(
            "^execute\\s+effect\\s+(\\S+)\\s+([a-z0-9_.:-]+)(?:\\s+(\\d+))?$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern NATURAL_EFFECT = Pattern.compile(
            "^(?:give|dar|aplicar|apply|add|añadir|anadir|conceder|grant|pon(?:er)?)\\s+"
                    + "(?:the\\s+|el\\s+)?"
                    + "(?:(?:effect|efecto)(?:\\s+(?:of|de))?\\s+"
                    + "([a-z0-9_.:-]+(?:\\s+[a-z0-9_.:-]+)*)|"
                    + "([a-z0-9_.:-]+(?:\\s+[a-z0-9_.:-]+)*)"
                    + "(?:\\s+(?:effect|efecto))?)"
                    + "(?:\\s+(?:for|durante)\\s+(\\d+)"
                    + "(?:\\s*(?:s|sec(?:ond)?s?|seg(?:undo)?s?))?)?"
                    + "(?:\\s+(?:to|a|al|para)\\s+.+)?$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern NATURAL_WAIT = Pattern.compile(
            "^(?:wait|pause|espera|esperar|pausa|pausar)\\s+(\\d+)"
                    + "(?:\\s*(?:s|sec(?:ond)?s?|seg(?:undo)?s?))?$",
            Pattern.CASE_INSENSITIVE
    );
    private static final java.util.Map<String, String> EFFECT_ALIASES =
            java.util.Map.ofEntries(
                    java.util.Map.entry("glow", "minecraft:glowing"),
                    java.util.Map.entry("glowing", "minecraft:glowing"),
                    java.util.Map.entry("brillo", "minecraft:glowing"),
                    java.util.Map.entry("brillante", "minecraft:glowing"),
                    java.util.Map.entry("resplandor", "minecraft:glowing"),
                    java.util.Map.entry("resplandeciente", "minecraft:glowing"),
                    java.util.Map.entry("rapidez", "minecraft:speed"),
                    java.util.Map.entry("speed", "minecraft:speed"),
                    java.util.Map.entry("fuerza", "minecraft:strength"),
                    java.util.Map.entry("strength", "minecraft:strength"),
                    java.util.Map.entry("salto", "minecraft:jump_boost"),
                    java.util.Map.entry("jump", "minecraft:jump_boost"),
                    java.util.Map.entry("jump boost", "minecraft:jump_boost"),
                    java.util.Map.entry("impulso de salto", "minecraft:jump_boost"),
                    java.util.Map.entry("vision nocturna", "minecraft:night_vision"),
                    java.util.Map.entry("visión nocturna", "minecraft:night_vision"),
                    java.util.Map.entry("night vision", "minecraft:night_vision"),
                    java.util.Map.entry("regeneracion", "minecraft:regeneration"),
                    java.util.Map.entry("regeneración", "minecraft:regeneration"),
                    java.util.Map.entry("resistencia", "minecraft:resistance"),
                    java.util.Map.entry("resistance", "minecraft:resistance"),
                    java.util.Map.entry("invisibilidad", "minecraft:invisibility"),
                    java.util.Map.entry("invisibility", "minecraft:invisibility"),
                    java.util.Map.entry("respiracion acuática", "minecraft:water_breathing"),
                    java.util.Map.entry("water breathing", "minecraft:water_breathing")
            );

    private BlockCommandRunner() {
    }

    static void execute(
            ServerLevel level,
            BlockPos blockPos,
            ServerPlayer player,
            String line,
            int lineNumber
    ) {
        String command = line.startsWith("/") ? line.substring(1).trim() : line;
        if (command.isEmpty()) {
            return;
        }

        command = normalize(command);
        var source = level.getServer()
                .createCommandSourceStack()
                .withLevel(level)
                .withPosition(player == null
                        ? Vec3.atCenterOf(blockPos)
                        : player.position())
                .withPermission(2)
                .withSuppressedOutput();
        if (player != null) {
            source = source.withEntity(player);
        }

        try {
            level.getServer()
                    .getCommands()
                    .getDispatcher()
                    .execute(command, source);
        } catch (CommandSyntaxException exception) {
            LOGGER.warn(
                    "Command failed at command block {} line {}: {}",
                    blockPos,
                    lineNumber + 1,
                    exception.getMessage()
            );
        }
    }

    static String translateNaturalEffect(String line, String target) {
        Matcher natural = NATURAL_EFFECT.matcher(line.trim());
        if (!natural.matches()) {
            return line;
        }
        String effectName = (natural.group(1) == null
                ? natural.group(2)
                : natural.group(1)).toLowerCase(
                java.util.Locale.ROOT
        );
        String effectId = EFFECT_ALIASES.getOrDefault(
                effectName,
                effectName.contains(":") ? effectName : "minecraft:" + effectName
        );
        String duration = natural.group(3) == null ? "10" : natural.group(3);
        return "effect give "
                + target
                + " "
                + effectId
                + " "
                + duration
                + " 0 true";
    }

    static String translateNaturalWait(String line) {
        Matcher wait = NATURAL_WAIT.matcher(line.trim());
        return wait.matches() ? "wait " + wait.group(1) : line;
    }

    static int parseWaitTicks(String line, BlockPos pos, int lineNumber) {
        String[] parts = line.split("\\s+");
        if (parts.length != 2) {
            warnInvalidWait(pos, lineNumber, line);
            return 0;
        }

        try {
            int seconds = Integer.parseInt(parts[1]);
            if (seconds < 0 || seconds > 86400) {
                throw new NumberFormatException("outside allowed range");
            }
            return seconds * 20;
        } catch (NumberFormatException exception) {
            warnInvalidWait(pos, lineNumber, line);
            return 0;
        }
    }

    private static void warnInvalidWait(
            BlockPos pos,
            int lineNumber,
            String line
    ) {
        LOGGER.warn(
                "Invalid wait directive at command block {} line {}: {}",
                pos,
                lineNumber + 1,
                line
        );
    }

    static String normalize(String command) {
        Matcher say = SIMPLE_SAY.matcher(command);
        if (say.matches()) {
            return "say " + say.group(1);
        }

        Matcher effect = SIMPLE_EFFECT.matcher(command);
        if (effect.matches()) {
            String duration = effect.group(3) == null ? "10" : effect.group(3);
            String effectId = effect.group(2).contains(":")
                    ? effect.group(2)
                    : "minecraft:" + effect.group(2);
            if (effectId.equalsIgnoreCase("minecraft:glow")) {
                effectId = "minecraft:glowing";
            }
            return "effect give "
                    + effect.group(1)
                    + " "
                    + effectId
                    + " "
                    + duration
                    + " 0 true";
        }

        Matcher playsound = SIMPLE_PLAYSOUND.matcher(command);
        if (playsound.matches()) {
            return "playsound "
                    + playsound.group(2)
                    + " master "
                    + playsound.group(1)
                    + playsound.group(3);
        }

        Matcher title = SIMPLE_TITLE.matcher(command);
        if (title.matches()) {
            return "title "
                    + title.group(1)
                    + " title "
                    + title.group(2);
        }
        return command;
    }
}
