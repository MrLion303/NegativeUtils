package com.negative.negativeutils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.PosixFilePermissions;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.imageio.ImageIO;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

public final class DiscordEmoteService {
    public static final int MAX_EMOTES = 256;
    private static final int ATLAS_SIZE = 256;
    private static final int CELL_SIZE = 16;
    private static final long MAX_IMAGE_BYTES = 2_000_000L;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(10))
            .build();
    private static final Set<String> GUILD_IDS =
            java.util.concurrent.ConcurrentHashMap.newKeySet();
    private static final AtomicBoolean SYNC_IN_PROGRESS = new AtomicBoolean();

    private static volatile MinecraftServer server;
    private static volatile Map<String, Integer> emotes = Map.of();
    private static volatile byte[] atlasBytes = new byte[0];
    private static volatile String lastError = "Token aún no configurado.";
    private static volatile String botToken;
    private static volatile boolean discordSrvActive;
    private static volatile boolean discordSrvDetected;
    private static volatile int discordSrvGuildCount;
    private static volatile WebSocket gateway;
    private static volatile ScheduledFuture<?> heartbeatTask;
    private static volatile long sequence = -1;
    private static volatile long heartbeatInterval = 45_000;
    private static ScheduledExecutorService executor;

    private DiscordEmoteService() {
    }

    public static synchronized void start(MinecraftServer minecraftServer) {
        stop();
        server = minecraftServer;
        executor = Executors.newScheduledThreadPool(2, task -> {
            Thread thread = new Thread(task, "NegativeUtils Discord Bot");
            thread.setDaemon(true);
            return thread;
        });
        executor.scheduleWithFixedDelay(
                DiscordEmoteService::refresh,
                0,
                60,
                TimeUnit.SECONDS
        );
    }

    public static synchronized void stop() {
        if (heartbeatTask != null) {
            heartbeatTask.cancel(true);
            heartbeatTask = null;
        }
        if (gateway != null) {
            gateway.sendClose(WebSocket.NORMAL_CLOSURE, "Server stopping");
            gateway = null;
        }
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
        server = null;
        botToken = null;
        discordSrvActive = false;
        discordSrvDetected = false;
        discordSrvGuildCount = 0;
        GUILD_IDS.clear();
        emotes = Map.of();
        atlasBytes = new byte[0];
        lastError = "Servicio detenido.";
    }

    public static Map<String, Integer> getEmotes() {
        return emotes;
    }

    public static byte[] getAtlasBytes() {
        return atlasBytes.clone();
    }

    public static String getStatus() {
        if (discordSrvActive) {
            return "DiscordSRV conectado a "
                    + discordSrvGuildCount
                    + " servidores; emotes sincronizados: "
                    + emotes.size()
                    + ".";
        }
        if (discordSrvDetected) {
            return "DiscordSRV detectado: " + lastError;
        }
        if (botToken == null || botToken.isBlank()) {
            return "Token del bot no configurado.";
        }
        if (gateway == null) {
            return "Bot desconectado: " + lastError;
        }
        return "Bot conectado a "
                + GUILD_IDS.size()
                + " servidores; emotes sincronizados: "
                + emotes.size()
                + ".";
    }

    public static void requestRefresh() {
        ScheduledExecutorService currentExecutor = executor;
        if (currentExecutor == null) {
            throw new IllegalStateException(
                    "El servidor aún no ha terminado de iniciar."
            );
        }
        currentExecutor.execute(DiscordEmoteService::refresh);
    }

    public static synchronized void setToken(
            MinecraftServer minecraftServer,
            String token
    ) throws IOException {
        String cleanToken = token == null ? "" : token.trim();
        if (cleanToken.length() < 20 || cleanToken.length() > 256
                || cleanToken.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException(
                    "El token no tiene un formato válido."
            );
        }

        Path tokenFile = configDirectory(minecraftServer)
                .resolve("discord-token.txt");
        Files.createDirectories(tokenFile.getParent());
        if (!Files.exists(tokenFile)) {
            Files.createFile(tokenFile);
        }
        restrictFilePermissions(tokenFile);
        Files.writeString(tokenFile, cleanToken);
        LOGGER.info("Discord bot token updated by an operator.");
        botToken = cleanToken;

        WebSocket oldGateway = gateway;
        gateway = null;
        if (oldGateway != null) {
            oldGateway.sendClose(WebSocket.NORMAL_CLOSURE, "Token updated");
        }
        GUILD_IDS.clear();
        lastError = "Conectando con Discord…";
        requestRefresh();
    }

    private static void refresh() {
        MinecraftServer activeServer = server;
        if (activeServer == null) {
            return;
        }
        try {
            DiscordSrvConnection discordSrv = getDiscordSrvConnection();
            discordSrvDetected = discordSrv != null;
            if (discordSrv != null && discordSrv.ready()) {
                discordSrvActive = false;
                syncDiscordSrvEmotes(discordSrv);
                discordSrvActive = true;
                discordSrvGuildCount = discordSrv.guildCount();
                botToken = null;
                WebSocket oldGateway = gateway;
                gateway = null;
                if (oldGateway != null) {
                    oldGateway.sendClose(
                            WebSocket.NORMAL_CLOSURE,
                            "Using DiscordSRV connection"
                    );
                }
                GUILD_IDS.clear();
                lastError = "";
                return;
            }

            discordSrvActive = false;
            discordSrvGuildCount = 0;
            String token = readToken(activeServer);
            if (token.isBlank()) {
                if (discordSrv != null) {
                    lastError = "DiscordSRV está instalado, pero aún no está conectado a Discord.";
                    return;
                }
                discordSrvDetected = false;
                botToken = null;
                gateway = null;
                GUILD_IDS.clear();
                publishAtlas(Map.of());
                lastError = "Token del bot no configurado.";
                return;
            }

            discordSrvDetected = false;
            if (!token.equals(botToken)) {
                botToken = token;
                WebSocket oldGateway = gateway;
                gateway = null;
                if (oldGateway != null) {
                    oldGateway.sendClose(
                            WebSocket.NORMAL_CLOSURE,
                            "Token changed"
                    );
                }
                GUILD_IDS.clear();
            }

            WebSocket current = gateway;
            if (current == null || current.isOutputClosed()
                    || current.isInputClosed()) {
                connectGateway(token);
            }

            if (!GUILD_IDS.isEmpty()) {
                syncGuilds(List.copyOf(GUILD_IDS));
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } catch (Exception exception) {
            lastError = safeMessage(exception);
            LOGGER.warn("Discord bot sync failed: {}", lastError);
        }
    }

    private static DiscordSrvConnection getDiscordSrvConnection()
            throws ReflectiveOperationException, IOException, InterruptedException {
        Class<?> bukkit;
        try {
            bukkit = Class.forName("org.bukkit.Bukkit");
        } catch (ClassNotFoundException exception) {
            return null;
        }

        Object pluginManager = bukkit.getMethod("getPluginManager").invoke(null);
        Object plugin = invoke(pluginManager, "getPlugin", "DiscordSRV");
        if (plugin == null) {
            return null;
        }

        Object jda = invoke(plugin, "getJda");
        if (jda == null) {
            return new DiscordSrvConnection(false, 0, Map.of());
        }

        Object rawGuilds = invoke(jda, "getGuilds");
        if (!(rawGuilds instanceof Collection<?> guilds)) {
            throw new IllegalStateException(
                    "DiscordSRV devolvió una lista de servidores no válida."
            );
        }

        LinkedHashMap<String, byte[]> downloaded = new LinkedHashMap<>();
        for (Object guild : guilds) {
            Object rawEmojis = invoke(guild, "getEmojis");
            if (!(rawEmojis instanceof Iterable<?> emojis)) {
                continue;
            }
            for (Object emoji : emojis) {
                if (downloaded.size() >= MAX_EMOTES) {
                    break;
                }
                String name = String.valueOf(invoke(emoji, "getName"));
                String id = String.valueOf(invoke(emoji, "getId"));
                boolean animated = Boolean.TRUE.equals(
                        invoke(emoji, "isAnimated")
                );
                if (!name.matches("[A-Za-z0-9_]{2,32}")
                        || downloaded.containsKey(name)) {
                    continue;
                }
                byte[] image = downloadEmoji(id, animated);
                if (image != null) {
                    downloaded.put(name, image);
                }
            }
            if (downloaded.size() >= MAX_EMOTES) {
                break;
            }
        }
        return new DiscordSrvConnection(true, guilds.size(), downloaded);
    }

    private static Object invoke(Object target, String methodName, Object... args)
            throws ReflectiveOperationException {
        Method method = Arrays.stream(target.getClass().getMethods())
                .filter(candidate -> candidate.getName().equals(methodName))
                .filter(candidate -> candidate.getParameterCount() == args.length)
                .filter(candidate -> parametersMatch(candidate.getParameterTypes(), args))
                .findFirst()
                .orElseThrow(() -> new NoSuchMethodException(
                        target.getClass().getName() + "." + methodName
                ));
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw exception;
        }
    }

    private static boolean parametersMatch(
            Class<?>[] parameterTypes,
            Object[] args
    ) {
        for (int i = 0; i < parameterTypes.length; i++) {
            if (args[i] != null
                    && !parameterTypes[i].isAssignableFrom(args[i].getClass())) {
                return false;
            }
        }
        return true;
    }

    private static void syncDiscordSrvEmotes(DiscordSrvConnection connection)
            throws IOException, InterruptedException {
        publishAtlas(connection.emotes());
    }

    private record DiscordSrvConnection(
            boolean ready,
            int guildCount,
            Map<String, byte[]> emotes
    ) {
    }

    private static void connectGateway(String token)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://discord.com/api/v10/gateway/bot"))
                .timeout(java.time.Duration.ofSeconds(15))
                .header("Authorization", "Bot " + token)
                .header("User-Agent", "NegativeUtils/1.0")
                .GET()
                .build();
        HttpResponse<String> response = HTTP.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );
        if (response.statusCode() != 200) {
            lastError = "Discord rechazó el token (HTTP "
                    + response.statusCode()
                    + ").";
            return;
        }
        JsonObject gatewayResponse = JsonParser.parseString(response.body())
                .getAsJsonObject();
        String gatewayUrl = gatewayResponse.get("url").getAsString();
        URI uri = URI.create(gatewayUrl + "?v=10&encoding=json");
        lastError = "Abriendo conexión Gateway.";
        HTTP.newWebSocketBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(15))
                .buildAsync(uri, new GatewayListener(token))
                .whenComplete((socket, error) -> {
                    if (error != null) {
                        lastError = safeMessage(error);
                        scheduleReconnect();
                    } else {
                        gateway = socket;
                    }
                });
    }

    private static void handleGatewayPayload(String token, String payload) {
        try {
            JsonObject root = JsonParser.parseString(payload).getAsJsonObject();
            if (!root.get("s").isJsonNull()) {
                sequence = root.get("s").getAsLong();
            }
            int op = root.get("op").getAsInt();
            JsonElement data = root.get("d");

            if (op == 10) {
                heartbeatInterval = data.getAsJsonObject()
                        .get("heartbeat_interval")
                        .getAsLong();
                sendIdentify(token);
                startHeartbeat();
            } else if (op == 7 || op == 9) {
                WebSocket current = gateway;
                if (current != null) {
                    current.sendClose(
                            WebSocket.NORMAL_CLOSURE,
                            "Gateway requested reconnect"
                    );
                }
                gateway = null;
                scheduleReconnect();
            } else if (op == 0) {
                String event = root.get("t").getAsString();
                JsonObject eventData = data.getAsJsonObject();
                if ("READY".equals(event)) {
                    GUILD_IDS.clear();
                    JsonArray guilds = eventData.getAsJsonArray("guilds");
                    for (JsonElement guildElement : guilds) {
                        GUILD_IDS.add(
                                guildElement.getAsJsonObject()
                                        .get("id")
                                        .getAsString()
                        );
                    }
                    lastError = "";
                    scheduleGuildSync();
                } else if ("GUILD_CREATE".equals(event)) {
                    GUILD_IDS.add(eventData.get("id").getAsString());
                    scheduleGuildSync();
                } else if ("GUILD_DELETE".equals(event)) {
                    GUILD_IDS.remove(eventData.get("id").getAsString());
                    scheduleGuildSync();
                } else if ("GUILD_EMOJIS_UPDATE".equals(event)) {
                    scheduleGuildSync();
                }
            }
        } catch (RuntimeException exception) {
            LOGGER.warn(
                    "Could not process a Discord Gateway event: {}",
                    safeMessage(exception)
            );
        }
    }

    private static void sendIdentify(String token) {
        WebSocket current = gateway;
        if (current == null) {
            return;
        }
        JsonObject properties = new JsonObject();
        properties.addProperty("os", System.getProperty("os.name", "server"));
        properties.addProperty("browser", "NegativeUtils");
        properties.addProperty("device", "NegativeUtils");
        JsonObject identifyData = new JsonObject();
        identifyData.addProperty("token", token);
        identifyData.addProperty("intents", 9);
        identifyData.add("properties", properties);
        JsonObject identify = new JsonObject();
        identify.addProperty("op", 2);
        identify.add("d", identifyData);
        current.sendText(identify.toString(), true);
    }

    private static void startHeartbeat() {
        ScheduledExecutorService currentExecutor = executor;
        if (currentExecutor == null) {
            return;
        }
        if (heartbeatTask != null) {
            heartbeatTask.cancel(false);
        }
        heartbeatTask = currentExecutor.scheduleAtFixedRate(() -> {
            WebSocket current = gateway;
            if (current == null || current.isOutputClosed()) {
                return;
            }
            JsonObject heartbeat = new JsonObject();
            heartbeat.addProperty("op", 1);
            if (sequence < 0) {
                heartbeat.add("d", com.google.gson.JsonNull.INSTANCE);
            } else {
                heartbeat.addProperty("d", sequence);
            }
            current.sendText(heartbeat.toString(), true);
        }, heartbeatInterval, heartbeatInterval, TimeUnit.MILLISECONDS);
    }

    private static void scheduleGuildSync() {
        ScheduledExecutorService currentExecutor = executor;
        if (currentExecutor != null) {
            currentExecutor.execute(() -> syncGuilds(List.copyOf(GUILD_IDS)));
        }
    }

    private static void scheduleReconnect() {
        ScheduledExecutorService currentExecutor = executor;
        if (currentExecutor != null && botToken != null) {
            currentExecutor.schedule(
                    DiscordEmoteService::refresh,
                    5,
                    TimeUnit.SECONDS
            );
        }
    }

    private static void syncGuilds(List<String> guildIds) {
        if (guildIds.isEmpty() || botToken == null
                || !SYNC_IN_PROGRESS.compareAndSet(false, true)) {
            return;
        }
        try {
            LinkedHashMap<String, byte[]> downloaded = new LinkedHashMap<>();
            for (String guildId : guildIds) {
                String safeGuildId = guildId.replaceAll("[^0-9]", "");
                if (!safeGuildId.equals(guildId)) {
                    continue;
                }
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(
                                "https://discord.com/api/v10/guilds/"
                                        + safeGuildId
                                        + "/emojis"
                        ))
                        .timeout(java.time.Duration.ofSeconds(15))
                        .header("Authorization", "Bot " + botToken)
                        .header("User-Agent", "NegativeUtils/1.0")
                        .GET()
                        .build();
                HttpResponse<String> response = HTTP.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );
                if (response.statusCode() != 200) {
                    lastError = "Discord devolvió HTTP "
                            + response.statusCode()
                            + " al leer los emotes del servidor.";
                    LOGGER.warn("Discord emoji sync returned HTTP {}.", response.statusCode());
                    continue;
                }

                JsonArray emojiList = JsonParser.parseString(response.body())
                        .getAsJsonArray();
                for (JsonElement element : emojiList) {
                    if (downloaded.size() >= MAX_EMOTES) {
                        break;
                    }
                    JsonObject emoji = element.getAsJsonObject();
                    if (!emoji.has("id") || !emoji.has("name")) {
                        continue;
                    }
                    String name = emoji.get("name").getAsString();
                    String id = emoji.get("id").getAsString();
                    boolean animated = emoji.has("animated")
                            && emoji.get("animated").getAsBoolean();
                    if (!name.matches("[A-Za-z0-9_]{2,32}")
                            || downloaded.containsKey(name)) {
                        continue;
                    }
                    byte[] image = downloadEmoji(id, animated);
                    if (image != null) {
                        downloaded.put(name, image);
                    }
                }
                if (downloaded.size() >= MAX_EMOTES) {
                    break;
                }
            }
            publishAtlas(downloaded);
            if (lastError.startsWith("Discord devolvió HTTP")) {
                lastError = "";
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } catch (Exception exception) {
            lastError = safeMessage(exception);
            LOGGER.warn("Discord emoji sync failed: {}", lastError);
        } finally {
            SYNC_IN_PROGRESS.set(false);
        }
    }

    private static byte[] downloadEmoji(String id, boolean animated)
            throws IOException, InterruptedException {
        String extension = animated ? "gif" : "png";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(
                        "https://cdn.discordapp.com/emojis/"
                                + id
                                + "."
                                + extension
                                + "?size=64"
                ))
                .timeout(java.time.Duration.ofSeconds(15))
                .GET()
                .build();
        HttpResponse<byte[]> response = HTTP.send(
                request,
                HttpResponse.BodyHandlers.ofByteArray()
        );
        if (response.statusCode() != 200
                || response.body().length > MAX_IMAGE_BYTES) {
            LOGGER.warn(
                    "Could not download Discord emoji {} (HTTP {}).",
                    id,
                    response.statusCode()
            );
            return null;
        }
        return response.body();
    }

    private static void publishAtlas(Map<String, byte[]> downloaded)
            throws IOException {
        BufferedImage atlas = new BufferedImage(
                ATLAS_SIZE,
                ATLAS_SIZE,
                BufferedImage.TYPE_INT_ARGB
        );
        Graphics2D graphics = atlas.createGraphics();
        graphics.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR
        );
        LinkedHashMap<String, Integer> mapping = new LinkedHashMap<>();
        int index = 0;
        try {
            for (Map.Entry<String, byte[]> entry : downloaded.entrySet()) {
                if (index >= MAX_EMOTES) {
                    break;
                }
                BufferedImage icon = ImageIO.read(
                        new java.io.ByteArrayInputStream(entry.getValue())
                );
                if (icon == null) {
                    continue;
                }
                graphics.drawImage(
                        icon,
                        index % 16 * CELL_SIZE,
                        index / 16 * CELL_SIZE,
                        CELL_SIZE,
                        CELL_SIZE,
                        null
                );
                mapping.put(entry.getKey(), 0xE000 + index);
                index++;
            }
        } finally {
            graphics.dispose();
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(atlas, "png", output);
        atlas.flush();
        byte[] png = output.toByteArray();
        if (png.length > MAX_IMAGE_BYTES) {
            throw new IOException(
                    "Discord emoji atlas exceeds the safe transfer size."
            );
        }
        if (mapping.equals(emotes) && Arrays.equals(png, atlasBytes)) {
            return;
        }
        emotes = Map.copyOf(mapping);
        atlasBytes = png.clone();
        DiscordEmoteNetwork.syncAll(mapping, png);
    }

    private static String readToken(MinecraftServer minecraftServer)
            throws IOException {
        Path tokenFile = configDirectory(minecraftServer)
                .resolve("discord-token.txt");
        return Files.exists(tokenFile)
                ? Files.readString(tokenFile).trim()
                : "";
    }

    private static Path configDirectory(MinecraftServer minecraftServer) {
        return minecraftServer.getServerDirectory()
                .toPath()
                .resolve("config")
                .resolve("negativeutils");
    }

    private static String safeMessage(Throwable error) {
        String message = error.getMessage();
        if (message == null) {
            return error.getClass().getSimpleName();
        }
        return message.replaceAll(
                "(?i)(authorization\\s*[:=]\\s*bot\\s+)\\S+",
                "$1[redacted]"
        );
    }

    private static void restrictFilePermissions(Path path) throws IOException {
        try {
            Files.setPosixFilePermissions(
                    path,
                    PosixFilePermissions.fromString("rw-------")
            );
        } catch (UnsupportedOperationException exception) {
            AclFileAttributeView aclView = Files.getFileAttributeView(
                    path,
                    AclFileAttributeView.class
            );
            if (aclView == null) {
                throw new IOException(
                        "No se pueden restringir los permisos del archivo del token en este sistema."
                );
            }
            AclEntry ownerOnly = AclEntry.newBuilder()
                    .setType(AclEntryType.ALLOW)
                    .setPrincipal(Files.getOwner(path))
                    .setPermissions(EnumSet.allOf(AclEntryPermission.class))
                    .build();
            aclView.setAcl(List.of(ownerOnly));
        }
    }

    private static final class GatewayListener
            implements WebSocket.Listener {
        private final String token;
        private final StringBuilder fragments = new StringBuilder();

        private GatewayListener(String token) {
            this.token = token;
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            if (token.equals(botToken)) {
                gateway = webSocket;
            }
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(
                WebSocket webSocket,
                CharSequence data,
                boolean last
        ) {
            synchronized (fragments) {
                fragments.append(data);
                if (last) {
                    handleGatewayPayload(token, fragments.toString());
                    fragments.setLength(0);
                }
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(
                WebSocket webSocket,
                int statusCode,
                String reason
        ) {
            if (gateway == webSocket) {
                gateway = null;
                lastError = "Gateway cerrado; intentando reconectar.";
                scheduleReconnect();
            }
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            if (gateway == webSocket) {
                gateway = null;
                lastError = safeMessage(error);
                scheduleReconnect();
            }
        }
    }
}
