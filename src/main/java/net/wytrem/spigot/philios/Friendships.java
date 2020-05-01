package net.wytrem.spigot.philios;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import net.wytrem.spigot.utils.Service;
import net.wytrem.spigot.utils.WyPlugin;
import org.bukkit.entity.Player;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collection;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Manages all friendships data.
 */
public class Friendships extends Service {
    public static final String DELIMITER_IN_SAVED_FILE = " <-> ";

    private Table<UUID, UUID, Boolean> friendshipTable;

    public Friendships(WyPlugin plugin) {
        super(plugin);
    }

    // --------------------
    // Enabling - disabling
    // --------------------

    @Override
    protected void onEnable() throws Exception {
        super.onEnable();

        // Load saved data
        this.friendshipTable = HashBasedTable.create();

        try {
            this.loadSavedData();
        } catch (IOException e) {
            this.getLogger().log(Level.WARNING, "Could not read saved friendships.", e);
        }
    }

    @Override
    protected void shutdown() throws Exception {
        super.shutdown();

        // Save data
        try {
            this.saveData();
        } catch (IOException e) {
            this.getLogger().log(Level.WARNING, "Could not save friendships.", e);
        }
        this.friendshipTable.clear();
    }

    /**
     * Reads the saved friendships from file system.
     */
    private void loadSavedData() throws IOException {
        File saved = this.getFriendshipsFile();

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

    /**
     * Writes the friendships to file system.
     */
    private void saveData() throws IOException {
        File saved = this.getFriendshipsFile();

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

    /**
     * @return where to store the friendships
     */
    protected File getFriendshipsFile() {
        return new File(this.getPlugin().getDataFolder(), "friendships.txt");
    }

    // ---------------------
    // Service API
    // ---------------------

    /**
     * @return All the friends's UUID of the given player
     */
    public Collection<UUID> getFriends(Player player) {
        return this.getFriends(player.getUniqueId());
    }

    /**
     * @return All the friends's UUID of the given player
     */
    public Collection<UUID> getFriends(UUID player) {
        return this.friendshipTable.column(player).keySet();
    }

    /**
     * Creates a friendship between the two given players. The order does not matter.
     */
    public void addFriendship(UUID some, UUID other) {
        Preconditions.checkNotNull(some);
        Preconditions.checkNotNull(other);
        this.friendshipTable.put(some, other, Boolean.TRUE);
        this.friendshipTable.put(other, some, Boolean.TRUE);
    }

    /**
     * Removes the friendship between the two given players, if there is one.
     */
    public void removeFriendship(UUID some, UUID other) {
        Preconditions.checkNotNull(some);
        Preconditions.checkNotNull(other);
        this.friendshipTable.remove(some, other);
        this.friendshipTable.remove(other, some);
    }

    /**
     * @return whether the given players are friends or not
     */
    public boolean areFriends(Player some, Player other) {
        return this.areFriends(some.getUniqueId(), other.getUniqueId());
    }

    /**
     * @return whether the given players are friends or not
     */
    public boolean areFriends(UUID some, UUID other) {
        Preconditions.checkNotNull(some);
        Preconditions.checkNotNull(other);
        Preconditions.checkArgument(!some.equals(other));

        return this.friendshipTable.contains(some, other);
    }

    @Override
    public String name() {
        return "friendships";
    }

    @Override
    public String version() {
        return "1.0";
    }
}
