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
import fun.kaituo.gameutils.GameUtils;
import fun.kaituo.gameutils.game.Game;
import fun.kaituo.states.WaitingState;
import io.papermc.paper.event.player.PlayerPickItemEvent;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.BoundingBox;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;


public class Spleef extends Game implements Listener {

    private static Spleef instance;
    private static FileConfiguration config;
    private static World world;
    private static Location mapOriginPoint;
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

    public static Location getMapOriginPoint() {
        return mapOriginPoint;
    }

    public static BoundingBox getGameBox() {
        return gameBoundingBox;
    }

    public int mapEditTaskID;

    public Set<UUID> playerIds = new HashSet<>(); // 不包括创造模式玩家
    public HashMap<UUID, Boolean> playerSurvivalStage = new HashMap<>(); // true=存活 false=死亡
    public int survivingPlayerNumber = 0;

    @Override
    public void addPlayer(Player player) {
        if (Spleef.inst().getState() != null) {
            Spleef.inst().getState().addPlayer(player);
        }
    }

    @Override
    public void removePlayer(Player player) {
        if (Spleef.inst().getState() != null) {
            Spleef.inst().getState().removePlayer(player);
        }
    }

    @Override
    public void forceStop() {

    }

    @Override
    public void tick() {

    }

    @Override
    public void onEnable() {
        instance = this;
        config = this.getConfig();
        world = GameUtils.inst().getMainWorld();
        mapOriginPoint = new Location(world,
                Spleef.getPluginConfig().getInt("map.map-origin-point.x"),
                Spleef.getPluginConfig().getInt("map.map-origin-point.y"),
                Spleef.getPluginConfig().getInt("map.map-origin-point.z"));
        gameBoundingBox = new BoundingBox(config.getInt("game-range.x-max"),
                config.getInt("game-range.y-max"),
                config.getInt("game-range.z-max"),
                config.getInt("game-range.x-min"),
                config.getInt("game-range.y-min"),
                config.getInt("game-range.z-min"));
        super.onEnable();
        saveDefaultConfig();
        updateExtraInfo("§e掘一死战", getLoc("hub"));

        Bukkit.getScheduler().runTaskLater(this, () -> {
            setState(new WaitingState());
        }, 1);
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
        Player player = (Player) ede.getEntity();
        if (playerIds.contains(player.getUniqueId())) {
            ede.setCancelled(true);
        }
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
                    if (!Spleef.inst().playerIds.contains(player.getUniqueId())) {
                        if (instance.getState() != null) {
                            instance.getState().addPlayer(player);
                        }
                    }
                }
                else {
                    if (Spleef.inst().playerIds.contains(player.getUniqueId())) {
                        if (instance.getState() != null) {
                            instance.getState().removePlayer(player);
                        }
                    }
                }
            }
        }
    }
}