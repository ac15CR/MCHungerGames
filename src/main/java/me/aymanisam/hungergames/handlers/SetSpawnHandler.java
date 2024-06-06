package me.aymanisam.hungergames.handlers;

import me.aymanisam.hungergames.HungerGames;
import me.aymanisam.hungergames.listeners.TeamVotingListener;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class SetSpawnHandler {
    private final HungerGames plugin;
    private final ResetPlayerHandler resetPlayerHandler;
    private final LangHandler langHandler;
    private final TeamVotingListener teamVotingListener;

    public FileConfiguration setSpawnConfig;
    private File setSpawnFile;
    public List<String> spawnPoints;
    public Map<String, Player> spawnPointMap;
    public List<String> playersWaiting;

    public SetSpawnHandler(HungerGames plugin) {
        this.plugin = plugin;
        this.langHandler = new LangHandler(plugin);
        this.spawnPoints = new ArrayList<>();
        this.spawnPointMap = new HashMap<>();
        this.playersWaiting = new ArrayList<>();
        this.resetPlayerHandler = new ResetPlayerHandler();
        this.teamVotingListener = new TeamVotingListener(plugin);
        createSetSpawnConfig();
    }

    public void createSetSpawnConfig() {
        setSpawnFile = new File(plugin.getDataFolder(), "setspawn.yml");
        if (!setSpawnFile.exists()) {
            setSpawnFile.getParentFile().mkdirs();
            plugin.saveResource("setspawn.yml", false);
        }

        setSpawnConfig = YamlConfiguration.loadConfiguration(setSpawnFile);
        spawnPoints = setSpawnConfig.getStringList("spawnpoints");
    }

    public void saveSetSpawnConfig() {
        if (setSpawnConfig == null || setSpawnFile == null) {
            return;
        }
        try {
            setSpawnConfig.set("spawnpoints", spawnPoints);

            setSpawnConfig.save(setSpawnFile);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, langHandler.getMessage("setspawnhandler.failed-save") + setSpawnFile, ex);
        }
    }

    public void resetSpawnPoints() {
        spawnPoints.clear();
        setSpawnConfig.set("spawnpoints", spawnPoints);
        saveSetSpawnConfig();
    }

    public String assignPlayerToSpawnPoint(Player player) {
        List<String> shuffledSpawnPoints = new ArrayList<>(spawnPoints);
        Collections.shuffle(shuffledSpawnPoints);

        for (String spawnPoint : shuffledSpawnPoints) {
            if (!spawnPointMap.containsKey(spawnPoint)) {
                return spawnPoint;
            }
        }

        player.sendMessage(langHandler.getMessage("join.join-fail"));
        return null;
    }

    public void removePlayerFromSpawnPoint(Player player) {
        Iterator<Map.Entry<String, Player>> iterator = spawnPointMap.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, Player> entry = iterator.next();
            if (entry.getValue().equals(player)) {
                iterator.remove();
                break;
            }
        }
    }

    public void teleportPlayerToSpawnpoint(Player player) {
        String spawnPoint = assignPlayerToSpawnPoint(player);

        if (spawnPoint == null) {
            return;
        }

        spawnPointMap.put(spawnPoint, player);
        playersWaiting.add(player.getName());

        String[] coords = spawnPoint.split(",");
        World world = plugin.getServer().getWorld(coords[0]);
        double x = Double.parseDouble(coords[1]) + 0.5;
        double y = Double.parseDouble(coords[2]) + 1.0;
        double z = Double.parseDouble(coords[3]) + 0.5;

        Location teleportLocation = new Location(world, x, y, z);

        assert world != null;
        Location spawnLocation = world.getSpawnLocation();

        Vector direction = spawnLocation.toVector().subtract(teleportLocation.toVector());

        float yaw = (float) (Math.toDegrees(Math.atan2(direction.getZ(), direction.getX())) - 90);

        teleportLocation.setYaw(yaw);

        player.teleport(teleportLocation);

        for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
            langHandler.getLangConfig(onlinePlayer);
            onlinePlayer.sendMessage(langHandler.getMessage("setspawnhandler.joined-player") + player.getName() +
                    langHandler.getMessage("setspawnhandler.joined-message-1") +
                    langHandler.getMessage("setspawnhandler.joined-message-2") + spawnPointMap.size() +
                    langHandler.getMessage("setspawnhandler.joined-message-3") + spawnPoints.size() +
                    langHandler.getMessage("setspawnhandler.joined-message-4"));
        }

        resetPlayerHandler.resetPlayer(player);

        if (plugin.getConfig().getBoolean("voting")) {
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                teamVotingListener.openVotingInventory(player);
            }, 100L);
        }
    }
}
