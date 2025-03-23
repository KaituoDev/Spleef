package fun.kaituo;


import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import fun.kaituo.commands.SpleefClearMapCommand;
import fun.kaituo.commands.SpleefDebugCommand;
import fun.kaituo.gameutils.GameUtils;
import fun.kaituo.gameutils.game.Game;
import fun.kaituo.gameutils.util.Misc;
import fun.kaituo.states.WaitingState;
import io.papermc.paper.event.player.PlayerPickItemEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.BoundingBox;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.*;


public class Spleef extends Game implements Listener {

    private static Spleef instance;
    private static FileConfiguration config;
    private static World world;
    private static Location mapOriginPoint;
    private static Location lobbySpawnPoint;
    private static BoundingBox gameBoundingBox;

    public static Spleef inst() {
        return instance;
    }

    public static FileConfiguration getPluginConfig() {
        return config;
    }

    public static World getGameWorld() {
        return world;
    }

    public static Location getLobbySpawnPoint() {
        return lobbySpawnPoint;
    }

    public static Location getMapOriginPoint() {
        return mapOriginPoint;
    }

    public static BoundingBox getGameBox() {
        return gameBoundingBox;
    }

    public String currentGameState;
    public int mapEditTaskID;

    public Set<UUID> playerIds = new HashSet<>(); // 不包括创造模式玩家
    public HashMap<UUID, Boolean> playerSurvivalStage = new HashMap<>(); // true=存活 false=死亡
    public int survivingPlayerNumber = 0;

    @Override
    public void addPlayer(Player player) {
        if (Spleef.inst().getState() != null) {
            Spleef.inst().getState().addPlayer(player);
        }
        super.addPlayer(player);
    }

    @Override
    public void removePlayer(Player player) {
        if (Spleef.inst().getState() != null) {
            Spleef.inst().getState().removePlayer(player);
        }
        player.clearActivePotionEffects();
        super.removePlayer(player);
    }

    @Override
    public void forceStop() {
        super.forceStop();
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    public void onEnable() {
        super.onEnable();

        instance = this;
        config = this.getConfig();
        world = GameUtils.inst().getMainWorld();
        mapOriginPoint = new Location(world,
                config.getInt("map.map-origin-point.x"),
                config.getInt("map.map-origin-point.y"),
                config.getInt("map.map-origin-point.z"));
        lobbySpawnPoint = new Location(world,
                config.getDouble("lobby.spawn-x"),
                config.getDouble("lobby.spawn-y"),
                config.getDouble("lobby.spawn-z"));
        gameBoundingBox = new BoundingBox(config.getInt("game-range.x-max"),
                config.getInt("game-range.y-max"),
                config.getInt("game-range.z-max"),
                config.getInt("game-range.x-min"),
                config.getInt("game-range.y-min"),
                config.getInt("game-range.z-min"));
        currentGameState = "null";
        saveDefaultConfig();
        updateExtraInfo("§b掘一死战", getLoc("hub"));
        registerCommands();
        Bukkit.getPluginManager().registerEvents(this, instance);

        Bukkit.getScheduler().runTaskLater(this, () -> {
            setState(new WaitingState());
        }, 1);
    }

    @Override
    public void onDisable() {
        for (UUID uuid : instance.playerIds) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                continue;
            }

            player.clearActivePotionEffects();
            player.getInventory().clear();
            player.setGameMode(GameMode.ADVENTURE);
            GameUtils.inst().join(player, GameUtils.inst().getLobby());
        }

        instance.state.exit();
        Bukkit.getScheduler().cancelTasks(this);
        super.onDisable();
    }

    @EventHandler
    public void preventPlayerPickItem(PlayerPickItemEvent ppie) {
        if (playerIds.contains(ppie.getPlayer().getUniqueId())) {
            ppie.setCancelled(true);
        }
    }

    @EventHandler
    public void preventPlayerInjury(EntityDamageEvent ede) {
        if (!ede.getEntityType().equals(EntityType.PLAYER)) {
            return;
        }
        if (ede.getCause().equals(EntityDamageEvent.DamageCause.ENTITY_ATTACK)) {
            return;
        }
        Entity entity = ede.getEntity();
        BoundingBox box = Spleef.getGameBox();
        if (entity.getLocation().getX() < box.getMinX() || entity.getLocation().getX() > box.getMaxX()) {
            return;
        }
        if (entity.getLocation().getZ() < box.getMinZ() || entity.getLocation().getZ() > box.getMaxZ()) {
            return;
        }
        ede.setCancelled(true);
    }

    public boolean isNormalMode() { // true=普通模式 false=无限火力模式
        return !world.getBlockAt(1003, 7, 5).isBlockPowered();
    }

    public boolean isPlayerInGameRange(Player player) {
        double x = player.getLocation().getX();
        double z = player.getLocation().getZ();
        if (x < gameBoundingBox.getMinX() || x > gameBoundingBox.getMaxX()) {
            return false;
        }
        if (z < gameBoundingBox.getMinZ() || z > gameBoundingBox.getMaxZ()) {
            return false;
        }
        return true;
    }

    public void registerCommands() {
        // 注册调试命令
        SpleefDebugCommand debugCmd = new SpleefDebugCommand();
        getCommand("spleefdebug").setExecutor(debugCmd);
        getCommand("spleefdebug").setTabCompleter(debugCmd);

        // 注册清理地图命令
        SpleefClearMapCommand clearCmd = new SpleefClearMapCommand();
        getCommand("spleefclearmap").setExecutor(clearCmd);
        getCommand("spleefclearmap").setTabCompleter(clearCmd);
    }

    public void pasteSchematic(String name, Location originPoint, boolean ignoreAir) {
        if (Bukkit.getScheduler().isCurrentlyRunning(Spleef.inst().mapEditTaskID) || Bukkit.getScheduler().isQueued(Spleef.inst().mapEditTaskID)) {
            getLogger().warning("Spleef > 错误：加载地图失败");
            getLogger().warning("Spleef > 原因：地图正在被加载或被清理");
            return;
        }

        File schematicFile = new File("plugins/WorldEdit/schematics/" + name + ".schem");
        ClipboardFormat format = ClipboardFormats.findByFile(schematicFile);
        Clipboard clipboard = null;

        try (ClipboardReader reader = format.getReader(Files.newInputStream(schematicFile.toPath()))) {
            clipboard = reader.read();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (EditSession editSession = WorldEdit.getInstance().getEditSessionFactory().getEditSession(BukkitAdapter.adapt(world), -1)) {
            Operation operation = new ClipboardHolder(clipboard)
                    .createPaste(editSession)
                    .to(BlockVector3.at(originPoint.getX(), originPoint.getY(), originPoint.getZ()))
                    .ignoreAirBlocks(ignoreAir)
                    .build();
            Operations.complete(operation);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void clearMap() {
        if (Bukkit.getScheduler().isCurrentlyRunning(Spleef.inst().mapEditTaskID) || Bukkit.getScheduler().isQueued(Spleef.inst().mapEditTaskID)) {
            getLogger().warning("Spleef > 错误：清理地图失败");
            getLogger().warning("Spleef > 原因：地图正在被加载或被清理");
            return;
        }
        int TopVertexOrdinate = Spleef.getPluginConfig().getInt("map.map-top-vertex-ordinate");
        int BottomVertexOrdinate = Spleef.getPluginConfig().getInt("map.map-bottom-vertex-ordinate");

        int radius = Spleef.getPluginConfig().getInt("map.map-radius");

        int x = Spleef.getPluginConfig().getInt("map.map-origin-point.x");
        int z = Spleef.getPluginConfig().getInt("map.map-origin-point.z");

        mapEditTaskID = Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
            private int floor = TopVertexOrdinate;

            @Override
            public void run() {

                // 任务逻辑
                for (int i = x - radius; i <= x + radius; ++i) {
                    for (int j = z - radius; j <= z + radius; ++j) {
                        getGameWorld().getBlockAt(i, floor, j).setType(Material.AIR);
                    }
                }

                // 取消任务
                --floor;
                if (floor < BottomVertexOrdinate) {
                    getLogger().info("Spleef > 清理地图成功");
                    Bukkit.getScheduler().cancelTask(mapEditTaskID);
                }
            }

        }, 0L, 1L).getTaskId(); // delay=0, period=1（每 tick 清一层地图）
    }

    public void verityPlayerList() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isPlayerInGameRange(player) && player.getWorld().equals(Spleef.getGameWorld())) {
                if (!player.getGameMode().equals(GameMode.CREATIVE)) {
                    if (!instance.playerIds.contains(player.getUniqueId())) {
                        if (instance.getState() != null) {
                            GameUtils.inst().join(player, instance);
                            instance.getState().addPlayer(player);
                        }
                    }
                }
                else {
                    if (instance.playerIds.contains(player.getUniqueId())) {
                        if (instance.getState() != null) {
                            instance.playerIds.remove(player.getUniqueId());
                            instance.playerSurvivalStage.remove(player.getUniqueId());
                            --instance.survivingPlayerNumber;

                            player.clearActivePotionEffects();
                            player.getInventory().clear();
                        }
                    }
                }
            }
        }

        for (UUID uuid : instance.playerIds) {
            if (Bukkit.getPlayer(uuid) == null) {
                instance.playerIds.remove(uuid);
                instance.playerSurvivalStage.remove(uuid);
                ++instance.survivingPlayerNumber;
            }
        }
    }

    public void confirmPlayerSurvival() {
        for (UUID uuid : instance.playerIds) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                continue;
            }
            if (!instance.playerSurvivalStage.get(uuid)) {
                continue;
            }
            if (player.getLocation().getY() < 35) {
                player.getInventory().clear();
                player.setGameMode(GameMode.SPECTATOR);
                instance.playerSurvivalStage.remove(player.getUniqueId());
                instance.playerSurvivalStage.put(player.getUniqueId(), false);
                --instance.survivingPlayerNumber;
                if (instance.survivingPlayerNumber >= 2) {
                    player.showTitle(Title.title(Component.text("你坠入了深渊！").color(NamedTextColor.RED),
                            Component.text("你已成为旁观者").color(NamedTextColor.GOLD),
                            Title.Times.times(Duration.ZERO, Duration.ofSeconds(3), Duration.ofSeconds(1))
                    ));
                    for (UUID id : Spleef.inst().playerIds) {
                        Player p = Bukkit.getPlayer(id);
                        if (p == null) {
                            continue;
                        }
                        p.sendMessage("§f§l" + player.getName() + "§c 坠入深渊！");
                    }
                }

            }
            if (instance.survivingPlayerNumber < 2) {
                Player winner = null;
                for (UUID id : instance.playerSurvivalStage.keySet()) {
                    if (!instance.playerSurvivalStage.get(id)) {
                        continue;
                    }
                    Player aim = Bukkit.getPlayer(id);
                    if (aim == null) {
                        continue;
                    }
                    winner = aim;
                }
                String summary;
                NamedTextColor summaryColor;
                if (winner == null) {
                    summary = "无人存活！";
                    summaryColor = NamedTextColor.GRAY;
                }
                else {
                    summary = winner.getName() + " 存活到了最后！";
                    summaryColor = NamedTextColor.GOLD;
                }
                for (UUID id : Spleef.inst().playerIds) {
                    Player aim = Bukkit.getPlayer(id);
                    if (aim == null) {
                        continue;
                    }
                    if (aim.equals(winner)) {
                        aim.showTitle(Title.title(Component.text("胜利！").color(NamedTextColor.GOLD),
                                Component.text("你存活到了最后！").color(NamedTextColor.GREEN),
                                Title.Times.times(Duration.ZERO, Duration.ofSeconds(3), Duration.ofSeconds(1))
                        ));
                    }
                    else {
                        aim.showTitle(Title.title(Component.text("游戏结束！").color(NamedTextColor.RED),
                                Component.text(summary).color(summaryColor),
                                Title.Times.times(Duration.ZERO, Duration.ofSeconds(3), Duration.ofSeconds(1))
                        ));
                    }
                }
                instance.clearMap();
                instance.getState().forceStop();
                return;
            }
        }
    }
}