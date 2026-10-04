package com.negative.negativeutils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

public class GuildSavedData extends SavedData {
    private static final String DATA_NAME = "negativeutils_guilds";
    private static final int MAX_MEMBERS = 50;
    private static final int MAX_LOCATIONS = 25;
    private static final int MAX_CHAT_MESSAGES = 100;
    private static final int MAX_ANNOUNCEMENTS = 100;

    private final Map<UUID, Guild> guilds = new LinkedHashMap<>();

    public GuildSavedData() {
    }

    public static GuildSavedData get(MinecraftServer server) {
        return server.overworld()
                .getDataStorage()
                .computeIfAbsent(
                        GuildSavedData::load,
                        GuildSavedData::new,
                        DATA_NAME
                );
    }

    public Guild createGuild(
            UUID leaderId,
            String leaderName,
            String guildName,
            int color
    ) {
        String cleanGuildName = cleanText(guildName, 24);
        String cleanLeaderName = cleanText(leaderName, 16);

        if (leaderId == null
                || cleanGuildName.isBlank()
                || cleanLeaderName.isBlank()
                || getGuildForPlayer(leaderId) != null
                || guildNameExists(cleanGuildName)) {
            return null;
        }

        Guild guild = new Guild(
                UUID.randomUUID(),
                cleanGuildName,
                leaderId,
                color & 0xFFFFFF
        );

        guild.members.put(
                leaderId,
                new Member(cleanLeaderName, Rank.LEADER)
        );

        guilds.put(guild.id, guild);
        setDirty();
        return guild;
    }

    public Guild getGuild(UUID guildId) {
        return guilds.get(guildId);
    }

    public Guild getGuildForPlayer(UUID playerId) {
        if (playerId == null) {
            return null;
        }

        for (Guild guild : guilds.values()) {
            if (guild.members.containsKey(playerId)) {
                return guild;
            }
        }

        return null;
    }

    public List<Guild> getGuilds() {
        return List.copyOf(guilds.values());
    }

    public CompoundTag savePublicCatalog() {
        CompoundTag result = new CompoundTag();
        ListTag list = new ListTag();

        for (Guild guild : guilds.values()) {
            CompoundTag entry = new CompoundTag();
            entry.putString("Id", guild.id.toString());
            entry.putString("Name", guild.name);
            entry.putInt("Color", guild.color);
            entry.putInt("MemberCount", guild.members.size());
            list.add(entry);
        }

        result.put("Guilds", list);
        return result;
    }

    public CompoundTag saveLeaderInbox(UUID leaderId) {
        CompoundTag result = new CompoundTag();
        Guild guild = getGuildForPlayer(leaderId);

        if (guild == null || !guild.leaderId.equals(leaderId)) {
            result.putBoolean("HasGuildInbox", false);
            return result;
        }

        result.putBoolean("HasGuildInbox", true);
        result.putString("GuildId", guild.id.toString());
        result.putString("GuildName", guild.name);

        ListTag requests = new ListTag();

        for (JoinRequest request : guild.joinRequests.values()) {
            CompoundTag entry = new CompoundTag();
            entry.putString("PlayerId", request.playerId().toString());
            entry.putString("PlayerName", request.playerName());
            entry.putLong("Timestamp", request.timestamp());
            requests.add(entry);
        }

        result.put("Requests", requests);
        return result;
    }

    public CompoundTag saveInvitationInbox(UUID playerId) {
        CompoundTag result = new CompoundTag();
        ListTag list = new ListTag();
        for (Guild guild : guilds.values()) {
            Long timestamp = guild.invitations.get(playerId);
            if (timestamp == null) continue;
            CompoundTag entry = new CompoundTag();
            entry.putString("GuildId", guild.id.toString());
            entry.putString("GuildName", guild.name);
            entry.putInt("Color", guild.color);
            entry.putLong("Timestamp", timestamp);
            list.add(entry);
        }
        result.put("Invitations", list);
        return result;
    }

    public boolean invitePlayer(UUID guildId, UUID actorId, UUID targetId) {
        Guild guild = guilds.get(guildId);
        if (guild == null || !isManager(guild, actorId)
                || targetId == null || targetId.equals(actorId)
                || getGuildForPlayer(targetId) != null
                || guild.joinRequests.containsKey(targetId)) {
            return false;
        }
        for (Guild other : guilds.values()) {
            if (other.invitations.containsKey(targetId)) return false;
        }
        guild.invitations.put(targetId, System.currentTimeMillis());
        setDirty();
        return true;
    }

    public boolean acceptInvitation(
            UUID guildId,
            UUID playerId,
            String playerName
    ) {
        Guild guild = guilds.get(guildId);
        if (guild == null || playerId == null
                || guild.members.size() >= MAX_MEMBERS
                || getGuildForPlayer(playerId) != null
                || !guild.invitations.containsKey(playerId)) {
            return false;
        }
        guild.invitations.remove(playerId);
        guild.joinRequests.remove(playerId);
        guild.members.put(
                playerId,
                new Member(cleanText(playerName, 16), Rank.MEMBER)
        );
        setDirty();
        return true;
    }

    public boolean rejectInvitation(UUID guildId, UUID playerId) {
        Guild guild = guilds.get(guildId);
        if (guild == null || playerId == null
                || guild.invitations.remove(playerId) == null) {
            return false;
        }
        setDirty();
        return true;
    }

    public CompoundTag savePanelSnapshot(UUID playerId) {
        CompoundTag snapshot = new CompoundTag();
        CompoundTag catalog = savePublicCatalog();

        snapshot.put(
                "Catalog",
                catalog.getList(
                        "Guilds",
                        CompoundTag.TAG_COMPOUND
                ).copy()
        );

        snapshot.put("Inbox", saveLeaderInbox(playerId));
        snapshot.put("InvitationInbox", saveInvitationInbox(playerId));

        Guild guild = getGuildForPlayer(playerId);
        snapshot.putBoolean("HasGuild", guild != null);

        if (guild != null) {
            snapshot.put("OwnGuild", guild.save());
        }

        return snapshot;
    }

    public RequestResult requestToJoin(
            UUID guildId,
            UUID playerId,
            String playerName
    ) {
        Guild guild = guilds.get(guildId);
        String cleanPlayerName = cleanText(playerName, 16);

        if (guild == null) {
            return RequestResult.GUILD_NOT_FOUND;
        }

        if (playerId == null || cleanPlayerName.isBlank()) {
            return RequestResult.INVALID_PLAYER;
        }

        if (getGuildForPlayer(playerId) != null) {
            return RequestResult.ALREADY_IN_GUILD;
        }

        if (guild.joinRequests.containsKey(playerId)) {
            return RequestResult.ALREADY_REQUESTED;
        }

        guild.joinRequests.put(
                playerId,
                new JoinRequest(
                        playerId,
                        cleanPlayerName,
                        System.currentTimeMillis()
                )
        );

        setDirty();
        return RequestResult.SUCCESS;
    }

    public boolean acceptJoinRequest(
            UUID guildId,
            UUID leaderId,
            UUID applicantId
    ) {
        Guild guild = guilds.get(guildId);

        if (guild == null
                || !guild.leaderId.equals(leaderId)
                || guild.members.size() >= MAX_MEMBERS
                || getGuildForPlayer(applicantId) != null) {
            return false;
        }

        JoinRequest request = guild.joinRequests.remove(applicantId);

        if (request == null) {
            return false;
        }

        guild.members.put(
                applicantId,
                new Member(request.playerName(), Rank.MEMBER)
        );
        guild.invitations.remove(applicantId);

        setDirty();
        return true;
    }

    public boolean rejectJoinRequest(
            UUID guildId,
            UUID leaderId,
            UUID applicantId
    ) {
        Guild guild = guilds.get(guildId);

        if (guild == null || !guild.leaderId.equals(leaderId)) {
            return false;
        }

        if (guild.joinRequests.remove(applicantId) == null) {
            return false;
        }

        setDirty();
        return true;
    }

    public boolean leaveGuild(UUID playerId) {
        Guild guild = getGuildForPlayer(playerId);

        if (guild == null || guild.leaderId.equals(playerId)) {
            return false;
        }

        guild.members.remove(playerId);
        setDirty();
        return true;
    }

    public boolean deleteGuild(UUID guildId, UUID leaderId) {
        Guild guild = guilds.get(guildId);

        if (guild == null || !guild.leaderId.equals(leaderId)) {
            return false;
        }

        guilds.remove(guildId);
        setDirty();
        return true;
    }

    public boolean transferLeadership(
            UUID guildId,
            UUID currentLeaderId,
            UUID newLeaderId
    ) {
        Guild guild = guilds.get(guildId);

        if (guild == null
                || !guild.leaderId.equals(currentLeaderId)
                || !guild.members.containsKey(newLeaderId)) {
            return false;
        }

        Member oldLeader = guild.members.get(currentLeaderId);
        Member newLeader = guild.members.get(newLeaderId);

        guild.members.put(
                currentLeaderId,
                new Member(oldLeader.name(), Rank.MEMBER)
        );
        guild.members.put(
                newLeaderId,
                new Member(newLeader.name(), Rank.LEADER)
        );

        guild.leaderId = newLeaderId;
        setDirty();
        return true;
    }

    public boolean setMemberRank(
            UUID guildId,
            UUID leaderId,
            UUID targetId,
            Rank rank
    ) {
        Guild guild = guilds.get(guildId);

        if (guild == null
                || !guild.leaderId.equals(leaderId)
                || targetId.equals(guild.leaderId)
                || rank == Rank.LEADER) {
            return false;
        }

        Member target = guild.members.get(targetId);

        if (target == null) {
            return false;
        }

        guild.members.put(
                targetId,
                new Member(target.name(), rank)
        );

        setDirty();
        return true;
    }

    public boolean setMotd(
            UUID guildId,
            UUID actorId,
            String motd
    ) {
        Guild guild = guilds.get(guildId);

        if (guild == null || !isManager(guild, actorId)) {
            return false;
        }

        guild.motd = cleanText(motd, 160);
        setDirty();
        return true;
    }

    public boolean setAnnouncement(
            UUID guildId,
            UUID actorId,
            String announcement
    ) {
        return setAnnouncement(guildId, actorId, "", announcement);
    }

    public boolean setAnnouncement(
            UUID guildId,
            UUID actorId,
            String title,
            String announcement
    ) {
        Guild guild = guilds.get(guildId);

        if (guild == null || !isManager(guild, actorId)) {
            return false;
        }

        Member author = guild.members.get(actorId);

        if (author == null) {
            return false;
        }

        String cleanTitle = cleanText(title, 64);
        String cleanAnnouncement = cleanText(announcement, 500);
        if (cleanTitle.isBlank() || cleanAnnouncement.isBlank()) {
            return false;
        }

        GuildAnnouncement newAnnouncement = new GuildAnnouncement(
                cleanTitle,
                cleanAnnouncement,
                actorId,
                author.name(),
                System.currentTimeMillis()
        );
        guild.announcements.add(newAnnouncement);
        while (guild.announcements.size() > MAX_ANNOUNCEMENTS) {
            guild.announcements.remove(0);
        }

        guild.announcementTitle = cleanTitle;
        guild.announcement = cleanAnnouncement;
        guild.announcementAuthorId = actorId;
        guild.announcementAuthor = author.name();
        guild.announcementTimestamp = System.currentTimeMillis();

        setDirty();
        return true;
    }

    public boolean addChatMessage(
            UUID guildId,
            UUID senderId,
            String senderName,
            String message
    ) {
        Guild guild = guilds.get(guildId);

        if (guild == null || !guild.members.containsKey(senderId)) {
            return false;
        }

        String cleanMessage = cleanText(message, 240);

        if (cleanMessage.isBlank()) {
            return false;
        }

        guild.chatMessages.add(
                new ChatMessage(
                        senderId,
                        cleanText(senderName, 16),
                        cleanMessage,
                        System.currentTimeMillis()
                )
        );

        while (guild.chatMessages.size() > MAX_CHAT_MESSAGES) {
            guild.chatMessages.remove(0);
        }

        setDirty();
        return true;
    }

    public boolean addLocation(
            UUID guildId,
            UUID actorId,
            String name,
            String dimension,
            int x,
            int y,
            int z
    ) {
        Guild guild = guilds.get(guildId);

        if (guild == null
                || !isManager(guild, actorId)
                || guild.locations.size() >= MAX_LOCATIONS) {
            return false;
        }

        String cleanName = cleanText(name, 32);

        if (cleanName.isBlank()) {
            return false;
        }

        guild.locations.add(
                new GuildLocation(
                        cleanName,
                        cleanText(dimension, 128),
                        x,
                        y,
                        z
                )
        );

        setDirty();
        return true;
    }

    public boolean removeLocation(
            UUID guildId,
            UUID actorId,
            int index
    ) {
        Guild guild = guilds.get(guildId);

        if (guild == null
                || !isManager(guild, actorId)
                || index < 0
                || index >= guild.locations.size()) {
            return false;
        }

        guild.locations.remove(index);
        setDirty();
        return true;
    }

    public boolean setBanner(
            UUID guildId,
            UUID actorId,
            ItemStack banner
    ) {
        Guild guild = guilds.get(guildId);

        if (guild == null || !isManager(guild, actorId)) {
            return false;
        }

        guild.banner = banner == null
                ? ItemStack.EMPTY
                : banner.copy();

        setDirty();
        return true;
    }

    private boolean isManager(Guild guild, UUID playerId) {
        Member member = guild.members.get(playerId);

        return member != null
                && (member.rank() == Rank.LEADER
                || member.rank() == Rank.OFFICER);
    }

    private boolean guildNameExists(String name) {
        return guilds.values().stream()
                .anyMatch(guild -> guild.name.equalsIgnoreCase(name));
    }

    private static String cleanText(String text, int maxLength) {
        if (text == null) {
            return "";
        }

        String cleaned = text.strip();

        return cleaned.length() <= maxLength
                ? cleaned
                : cleaned.substring(0, maxLength);
    }

    public static GuildSavedData load(CompoundTag tag) {
        GuildSavedData data = new GuildSavedData();
        ListTag guildList = tag.getList(
                "Guilds",
                CompoundTag.TAG_COMPOUND
        );

        for (int i = 0; i < guildList.size(); i++) {
            Guild guild = Guild.load(guildList.getCompound(i));

            if (guild != null) {
                data.guilds.put(guild.id, guild);
            }
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag guildList = new ListTag();

        for (Guild guild : guilds.values()) {
            guildList.add(guild.save());
        }

        tag.put("Guilds", guildList);
        return tag;
    }

    public enum Rank {
        LEADER,
        OFFICER,
        MEMBER
    }

    public enum RequestResult {
        SUCCESS,
        GUILD_NOT_FOUND,
        INVALID_PLAYER,
        ALREADY_IN_GUILD,
        ALREADY_REQUESTED
    }

    public record Member(String name, Rank rank) {
    }

    public record JoinRequest(
            UUID playerId,
            String playerName,
            long timestamp
    ) {
    }

    public record ChatMessage(
            UUID senderId,
            String sender,
            String text,
            long timestamp
    ) {
    }

    public record GuildAnnouncement(
            String title,
            String text,
            UUID authorId,
            String author,
            long timestamp
    ) {
    }

    public record GuildInvitation(UUID playerId, long timestamp) {
    }

    public record GuildLocation(
            String name,
            String dimension,
            int x,
            int y,
            int z
    ) {
    }

    public static final class Guild {
        private final UUID id;
        private final String name;
        private final int color;
        private UUID leaderId;

        private final Map<UUID, Member> members =
                new LinkedHashMap<>();

        private final Map<UUID, JoinRequest> joinRequests =
                new LinkedHashMap<>();

        private final Map<UUID, Long> invitations = new LinkedHashMap<>();

        private final List<ChatMessage> chatMessages =
                new ArrayList<>();

        private final List<GuildLocation> locations =
                new ArrayList<>();

        private final List<GuildAnnouncement> announcements =
                new ArrayList<>();

        private String motd = "";
        private String announcementTitle = "";
        private String announcement = "";
        private UUID announcementAuthorId;
        private String announcementAuthor = "";
        private long announcementTimestamp;
        private ItemStack banner = ItemStack.EMPTY;

        private Guild(
                UUID id,
                String name,
                UUID leaderId,
                int color
        ) {
            this.id = id;
            this.name = name;
            this.leaderId = leaderId;
            this.color = color & 0xFFFFFF;
        }

        public UUID id() {
            return id;
        }

        public String name() {
            return name;
        }

        public UUID leaderId() {
            return leaderId;
        }

        public int color() {
            return color;
        }

        public String motd() {
            return motd;
        }

        public String announcement() {
            return announcement;
        }

        public UUID announcementAuthorId() {
            return announcementAuthorId;
        }

        public String announcementAuthor() {
            return announcementAuthor;
        }

        public long announcementTimestamp() {
            return announcementTimestamp;
        }

        public ItemStack banner() {
            return banner.copy();
        }

        public Map<UUID, Member> members() {
            return Collections.unmodifiableMap(members);
        }

        public Map<UUID, JoinRequest> joinRequests() {
            return Collections.unmodifiableMap(joinRequests);
        }

        public Map<UUID, Long> invitations() {
            return Collections.unmodifiableMap(invitations);
        }

        public List<ChatMessage> chatMessages() {
            return List.copyOf(chatMessages);
        }

        public List<GuildAnnouncement> announcements() {
            return List.copyOf(announcements);
        }

        public List<GuildLocation> locations() {
            return List.copyOf(locations);
        }

        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();

            tag.putString("Id", id.toString());
            tag.putString("Name", name);
            tag.putString("LeaderId", leaderId.toString());
            tag.putInt("Color", color);
            tag.putString("Motd", motd);
            tag.putString("AnnouncementTitle", announcementTitle);
            tag.putString("Announcement", announcement);
            tag.putString("AnnouncementAuthor", announcementAuthor);
            tag.putLong(
                    "AnnouncementTimestamp",
                    announcementTimestamp
            );

            if (announcementAuthorId != null) {
                tag.putString(
                        "AnnouncementAuthorId",
                        announcementAuthorId.toString()
                );
            }

            tag.put("Banner", banner.save(new CompoundTag()));

            ListTag announcementList = new ListTag();
            for (GuildAnnouncement announcement : announcements) {
                CompoundTag announcementTag = new CompoundTag();
                announcementTag.putString("Title", announcement.title());
                announcementTag.putString("Text", announcement.text());
                if (announcement.authorId() != null) {
                    announcementTag.putString(
                            "AuthorId",
                            announcement.authorId().toString()
                    );
                }
                announcementTag.putString("Author", announcement.author());
                announcementTag.putLong("Timestamp", announcement.timestamp());
                announcementList.add(announcementTag);
            }
            tag.put("Announcements", announcementList);

            ListTag memberList = new ListTag();

            for (Map.Entry<UUID, Member> entry : members.entrySet()) {
                CompoundTag memberTag = new CompoundTag();
                memberTag.putString("Id", entry.getKey().toString());
                memberTag.putString("Name", entry.getValue().name());
                memberTag.putString(
                        "Rank",
                        entry.getValue().rank().name()
                );
                memberList.add(memberTag);
            }

            tag.put("Members", memberList);

            ListTag requestList = new ListTag();

            for (JoinRequest request : joinRequests.values()) {
                CompoundTag requestTag = new CompoundTag();
                requestTag.putString(
                        "PlayerId",
                        request.playerId().toString()
                );
                requestTag.putString(
                        "PlayerName",
                        request.playerName()
                );
                requestTag.putLong("Timestamp", request.timestamp());
                requestList.add(requestTag);
            }

            tag.put("JoinRequests", requestList);

            ListTag invitationList = new ListTag();
            for (Map.Entry<UUID, Long> entry : invitations.entrySet()) {
                CompoundTag invitationTag = new CompoundTag();
                invitationTag.putString("PlayerId", entry.getKey().toString());
                invitationTag.putLong("Timestamp", entry.getValue());
                invitationList.add(invitationTag);
            }
            tag.put("Invitations", invitationList);

            ListTag chatList = new ListTag();

            for (ChatMessage message : chatMessages) {
                CompoundTag messageTag = new CompoundTag();

                if (message.senderId() != null) {
                    messageTag.putString(
                            "SenderId",
                            message.senderId().toString()
                    );
                }

                messageTag.putString("Sender", message.sender());
                messageTag.putString("Text", message.text());
                messageTag.putLong("Timestamp", message.timestamp());
                chatList.add(messageTag);
            }

            tag.put("ChatMessages", chatList);

            ListTag locationList = new ListTag();

            for (GuildLocation location : locations) {
                CompoundTag locationTag = new CompoundTag();
                locationTag.putString("Name", location.name());
                locationTag.putString(
                        "Dimension",
                        location.dimension()
                );
                locationTag.putInt("X", location.x());
                locationTag.putInt("Y", location.y());
                locationTag.putInt("Z", location.z());
                locationList.add(locationTag);
            }

            tag.put("Locations", locationList);
            return tag;
        }

        private static Guild load(CompoundTag tag) {
            UUID id = parseUuid(tag.getString("Id"));
            UUID leaderId = parseUuid(tag.getString("LeaderId"));
            String name = cleanText(tag.getString("Name"), 24);

            if (id == null || leaderId == null || name.isBlank()) {
                return null;
            }

            Guild guild = new Guild(
                    id,
                    name,
                    leaderId,
                    tag.getInt("Color")
            );

            guild.motd = cleanText(tag.getString("Motd"), 160);
            guild.announcementTitle = cleanText(
                    tag.getString("AnnouncementTitle"),
                    64
            );
            guild.announcement = cleanText(
                    tag.getString("Announcement"),
                    500
            );
            guild.announcementAuthorId = parseUuid(
                    tag.getString("AnnouncementAuthorId")
            );
            guild.announcementAuthor = cleanText(
                    tag.getString("AnnouncementAuthor"),
                    16
            );
            guild.announcementTimestamp =
                    tag.getLong("AnnouncementTimestamp");

            ListTag announcementList = tag.getList(
                    "Announcements",
                    CompoundTag.TAG_COMPOUND
            );
            for (int i = 0; i < announcementList.size(); i++) {
                CompoundTag announcementTag = announcementList.getCompound(i);
                String title = cleanText(announcementTag.getString("Title"), 64);
                String text = cleanText(announcementTag.getString("Text"), 500);
                if (title.isBlank() || text.isBlank()) {
                    continue;
                }
                guild.announcements.add(new GuildAnnouncement(
                        title,
                        text,
                        parseUuid(announcementTag.getString("AuthorId")),
                        cleanText(announcementTag.getString("Author"), 16),
                        announcementTag.getLong("Timestamp")
                ));
            }

            // Migra el anuncio único de versiones anteriores a la lista nueva.
            if (guild.announcements.isEmpty()
                    && !guild.announcement.isBlank()) {
                String oldTitle = guild.announcementTitle.isBlank()
                        ? "Anuncio"
                        : guild.announcementTitle;
                guild.announcements.add(new GuildAnnouncement(
                        oldTitle,
                        guild.announcement,
                        guild.announcementAuthorId,
                        guild.announcementAuthor,
                        guild.announcementTimestamp
                ));
            }

            if (tag.contains("Banner", CompoundTag.TAG_COMPOUND)) {
                guild.banner = ItemStack.of(
                        tag.getCompound("Banner")
                );
            }

            ListTag memberList = tag.getList(
                    "Members",
                    CompoundTag.TAG_COMPOUND
            );

            for (int i = 0; i < memberList.size(); i++) {
                CompoundTag memberTag = memberList.getCompound(i);
                UUID playerId = parseUuid(memberTag.getString("Id"));

                if (playerId == null) {
                    continue;
                }

                Rank rank;

                try {
                    rank = Rank.valueOf(memberTag.getString("Rank"));
                } catch (IllegalArgumentException exception) {
                    rank = playerId.equals(leaderId)
                            ? Rank.LEADER
                            : Rank.MEMBER;
                }

                guild.members.put(
                        playerId,
                        new Member(
                                cleanText(
                                        memberTag.getString("Name"),
                                        16
                                ),
                                rank
                        )
                );
            }

            if (!guild.members.containsKey(leaderId)) {
                guild.members.put(
                        leaderId,
                        new Member("Líder", Rank.LEADER)
                );
            }

            ListTag requestList = tag.getList(
                    "JoinRequests",
                    CompoundTag.TAG_COMPOUND
            );

            for (int i = 0; i < requestList.size(); i++) {
                CompoundTag requestTag =
                        requestList.getCompound(i);
                UUID playerId =
                        parseUuid(requestTag.getString("PlayerId"));

                if (playerId == null) {
                    continue;
                }

                guild.joinRequests.put(
                        playerId,
                        new JoinRequest(
                                playerId,
                                cleanText(
                                        requestTag.getString("PlayerName"),
                                        16
                                ),
                                requestTag.getLong("Timestamp")
                        )
                );
            }

            ListTag invitationList = tag.getList(
                    "Invitations",
                    CompoundTag.TAG_COMPOUND
            );
            for (int i = 0; i < invitationList.size(); i++) {
                CompoundTag invitationTag = invitationList.getCompound(i);
                UUID invitedPlayer = parseUuid(
                        invitationTag.getString("PlayerId")
                );
                if (invitedPlayer != null) {
                    guild.invitations.put(
                            invitedPlayer,
                            invitationTag.getLong("Timestamp")
                    );
                }
            }

            ListTag chatList = tag.getList(
                    "ChatMessages",
                    CompoundTag.TAG_COMPOUND
            );

            for (int i = 0; i < chatList.size(); i++) {
                CompoundTag messageTag =
                        chatList.getCompound(i);

                guild.chatMessages.add(
                        new ChatMessage(
                                parseUuid(
                                        messageTag.getString("SenderId")
                                ),
                                cleanText(
                                        messageTag.getString("Sender"),
                                        16
                                ),
                                cleanText(
                                        messageTag.getString("Text"),
                                        240
                                ),
                                messageTag.getLong("Timestamp")
                        )
                );
            }

            ListTag locationList = tag.getList(
                    "Locations",
                    CompoundTag.TAG_COMPOUND
            );

            for (int i = 0; i < locationList.size(); i++) {
                CompoundTag locationTag =
                        locationList.getCompound(i);

                guild.locations.add(
                        new GuildLocation(
                                cleanText(
                                        locationTag.getString("Name"),
                                        32
                                ),
                                cleanText(
                                        locationTag.getString("Dimension"),
                                        128
                                ),
                                locationTag.getInt("X"),
                                locationTag.getInt("Y"),
                                locationTag.getInt("Z")
                        )
                );
            }

            return guild;
        }
    }

    private static UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}

