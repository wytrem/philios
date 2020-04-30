package net.wytrem.spigot.philios;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.wytrem.spigot.utils.WyPlugin;
import net.wytrem.spigot.utils.commands.Command;
import net.wytrem.spigot.utils.commands.args.CommonArguments;
import net.wytrem.spigot.utils.i18n.I18n;
import net.wytrem.spigot.utils.text.Text;
import net.wytrem.spigot.utils.text.TextsRegistry;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class Philios extends WyPlugin {

    public static final String DELIMITER_IN_SAVED_FILE = " <-> ";
    public static Philios instance;
    public Texts texts;

    public FriendOffersManager offers;
    private Table<UUID, UUID, Boolean> friendshipTable;
    private Command removeCommand;

    @Override
    public void onEnable() {
        instance = this;
        super.onEnable();

        this.texts = new Texts(this.i18n);
        this.friendshipTable = HashBasedTable.create();

        // Offers
        this.offers = new FriendOffersManager(this);
        this.enableService(this.offers);

        // Commands
        Command command = this.commands.builder()
                .child(this.offers.buildAcceptCommand(), "accept")
                .child(this.offers.buildDenyCommand(), "deny")
                .child(this.offers.buildProposeCommand(), "propose")
                .child(this.offers.buildTakeBackCommand(), "takeback")
                .child(this.offers.buildListCommand(), "pending")
                .child(this.buildListCommand(), "list")
                .child(this.buildRemoveCommand(), "remove")

                .build();

        this.commands.register(command, "friend");

        // Load saved data
        try {
            this.loadSavedData();
        } catch (IOException e) {
            this.getLogger().log(Level.WARNING, "Could not read saved friendships.", e);
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        try {
            this.saveData();
        } catch (IOException e) {
            this.getLogger().log(Level.WARNING, "Could not save friendships.", e);
        }
        this.friendshipTable.clear();
    }

    private void loadSavedData() throws IOException {
        File saved = new File(this.getDataFolder(), "friendships.txt");

        if (saved.exists()) {
            Files.lines(saved.toPath())
                    .forEach(line -> {
                String[] split = line.split(DELIMITER_IN_SAVED_FILE);
                if (split.length == 2) {
                    this.friendshipTable.put(UUID.fromString(split[0]), UUID.fromString(split[1]), Boolean.TRUE);
                } else {
                    throw new IllegalStateException();
                }
            });
            this.getLogger().info("Successfully loaded " + this.friendshipTable.columnKeySet().size() + " friendships.");
        }
    }

    private void saveData() throws IOException {
        File saved = new File(this.getDataFolder(), "friendships.txt");

        if (saved.exists()) {
            saved.delete();
        }
        if (saved.createNewFile()) {
            BufferedWriter bufferedWriter = Files.newBufferedWriter(saved.toPath(), StandardCharsets.UTF_8);
            String content = this.friendshipTable.cellSet().stream()
                    .map(cell -> cell.getColumnKey().toString() + DELIMITER_IN_SAVED_FILE + cell.getRowKey().toString())
                    .collect(Collectors.joining("\n"));
            bufferedWriter.write(content);
            bufferedWriter.close();
            this.getLogger().info("Successfully saved " + this.friendshipTable.columnKeySet().size() + " friendships.");
        }
    }

    public Command buildListCommand() {
        return this.commands.builder()
                .requireSenderToBePlayer()
                .performer(context -> {
                    Player player = ((Player) context.source);

                    this.sendOfflineFriends(player);
                    this.sendOnlineFriends(player);
                })
                .build();
    }

    protected void sendOnlineFriends(Player player) {

        this.texts.onlineFriends.send(player);
        BaseComponent message = this.getFriends(player).stream()
                .filter(this::isOnline)
                .map(friend -> this.buildFriend(getDisplayName(friend), friend))
                .collect(ChatComponents.joining(", "));

        message.addExtra(new TextComponent(ChatColor.GRAY + "."));

        player.spigot().sendMessage(message);
    }

    protected void sendOfflineFriends(Player player) {

        this.texts.offlineFriends.send(player);
        BaseComponent message = this.getFriends(player).stream()
                .filter(uuid -> !this.isOnline(uuid))
                .map(friend -> this.buildFriend(getDisplayName(friend), friend))
                .collect(ChatComponents.joining(", "));

        message.addExtra(new TextComponent(ChatColor.GRAY + "."));

        player.spigot().sendMessage(message);
    }

    protected TextComponent buildFriend(String friendDisplayName, UUID uuid) {
        TextComponent message = this.baseFriend(friendDisplayName);
        message.addExtra(this.buildRemoveText(uuid));
        return message;
    }

    protected TextComponent buildRemoveText(UUID uuid) {
        TextComponent remove = new TextComponent(ChatColor.GRAY + " [" + ChatColor.RED + "X" + ChatColor.GRAY + "]");
        remove.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, this.removeCommand.baseCommandLine() + uuid));
        return remove;
    }

    protected TextComponent baseFriend(String friendDisplayName) {
        return new TextComponent(ChatColor.GRAY + friendDisplayName);
    }

    public Command buildRemoveCommand() {
        return this.removeCommand = this.commands.builder()
                .requireSenderToBePlayer()
                .argument(CommonArguments.uuid("uuid"))
                .performer(context -> {
                    Player source = (Player) context.source;
                    UUID uuid = context.args.requireOne("uuid");

                    if (this.areFriends(source.getUniqueId(), uuid)) {
                        this.removeFriendship(source.getUniqueId(), uuid);

                        this.texts.youAreNotFriendWithOtherAnymore.format("player", getDisplayName(uuid)).send(source);
                        this.getOnline(uuid).ifPresent(this.texts.otherIsNoLongerFriendWithYou.format("player", source)::send);
                    }
                    else {
                        this.texts.youAreNotFriendWithThatPlayer.send(source);
                    }
                })
                .build();
    }

    public String getDisplayName(UUID uuid) {
        if (Bukkit.getPlayer(uuid) != null) {
            return Bukkit.getPlayer(uuid).getDisplayName();
        }
        return Bukkit.getOfflinePlayer(uuid).getName();
    }

    public boolean isOnline(UUID uui) {
        return PlayerStatus.of(uui).equals(PlayerStatus.ONLINE);
    }

    protected Optional<Player> getOnline(UUID uuid) {
        return Optional.ofNullable(Bukkit.getPlayer(uuid));
    }

    public Collection<UUID> getFriends(Player player) {
        return this.getFriends(player.getUniqueId());
    }
    public Collection<UUID> getFriends(UUID player) {
        return this.friendshipTable.column(player).keySet();
    }

    public void addFriendship(UUID some, UUID other) {
        this.friendshipTable.put(some, other, Boolean.TRUE);
        this.friendshipTable.put(other, some, Boolean.TRUE);
    }

    public void removeFriendship(UUID some, UUID other) {
        this.friendshipTable.remove(some, other);
        this.friendshipTable.remove(other, some);
    }

    public boolean areFriends(Player some, Player other) {
        return this.areFriends(some.getUniqueId(), other.getUniqueId());
    }

    public boolean areFriends(UUID some, UUID other) {
        Preconditions.checkNotNull(some);
        Preconditions.checkNotNull(other);
        Preconditions.checkArgument(!some.equals(other));

        return this.friendshipTable.contains(some, other);
    }

    @Override
    public String getCodeName() {
        return "philios";
    }

    public static class Texts extends TextsRegistry {
        public Text youAreAlreadyFriendWithOther;
        public Text otherIsNoLongerFriendWithYou;
        public Text youAreNotFriendWithOtherAnymore;
        public Text youAreNotFriendWithThatPlayer;
        public Text onlineFriends;
        public Text youHaveNoOnlineFriends;
        public Text offlineFriends;


        public Texts(I18n i18n) {
            super(i18n, "texts");
        }

        @Override
        public void load() {
            this.youAreAlreadyFriendWithOther = this.get("youAreAlreadyFriendWithOther").asError();
            this.otherIsNoLongerFriendWithYou = this.get("otherIsNoLongerFriendWithYou").asInformation();
            this.youAreNotFriendWithOtherAnymore = this.get("youAreNotFriendWithOtherAnymore").asInformation();
            this.youAreNotFriendWithThatPlayer = this.get("youAreNotFriendWithThatPlayer").asError();
            this.onlineFriends = this.get("onlineFriends").asInformation();
            this.youHaveNoOnlineFriends = this.get("youHaveNoOnlineFriends").asInformation();
            this.offlineFriends = this.get("offlineFriends").asInformation();
        }
    }

    enum PlayerStatus {
        ONLINE,
        OFFLINE;

        public static PlayerStatus of(UUID uuid) {
            if (Bukkit.getPlayer(uuid) != null) {
                return ONLINE;
            }
            return OFFLINE;
        }
    }
}
