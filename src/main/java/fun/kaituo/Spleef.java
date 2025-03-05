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
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;


public class Spleef extends Game implements Listener {

    private static Spleef instance;
    private static FileConfiguration config;
    private static World world;

    public static Spleef inst() {
        return instance;
    }

    public static FileConfiguration getPluginConfig() {
        return config;
    }

    public static World getGameWorld() {
        return world;
    }

    public BukkitTask mapEditTask;
    public BoundingBox spleefGameBox = new BoundingBox();

    public final Set<UUID> playerIds = new HashSet<>();
    public final HashMap<UUID, Boolean> playerSurvivalStage = new HashMap<>(); // true=存活 false=死亡

    @Override
    public void addPlayer(Player player) {
        playerIds.add(player.getUniqueId());
        playerSurvivalStage.put(player.getUniqueId(), false);
    }

    @Override
    public void removePlayer(Player player) {

    }

    @Override
    public void forceStop() {

    }

    @Override
    public void tick() {
        for (UUID uuid : playerIds) {
            if ()
        }
    }

    @Override
    public void onEnable() {
        instance = this;
        config = this.getConfig();
        world = GameUtils.inst().getMainWorld();
        super.onEnable();
        saveDefaultConfig();
        updateExtraInfo("§e掘一死战", getLoc("hub"));
    }

    public boolean getGameMode() { // true=普通模式 false=无限火力模式
        return !world.getBlockAt(1003, 7, 5).isBlockPowered();
    }

    public void pasteSchematic(String name, double x, double y, double z, boolean ignoreAir) {
        if (Bukkit.getScheduler().isCurrentlyRunning(Spleef.inst().mapEditTask.getTaskId()) || Bukkit.getScheduler().isQueued(Spleef.inst().mapEditTask.getTaskId())) {
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
                    .to(BlockVector3.at(x, y, z))
                    .ignoreAirBlocks(ignoreAir)
                    .build();
            Operations.complete(operation);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void clearMap() {
        if (Bukkit.getScheduler().isCurrentlyRunning(Spleef.inst().mapEditTask.getTaskId()) || Bukkit.getScheduler().isQueued(Spleef.inst().mapEditTask.getTaskId())) {
            getLogger().warning("Spleef > 错误：清理地图失败");
            getLogger().warning("Spleef > 原因：地图正在被加载或被清理");
            return;
        }
        int TopVertexOrdinate = Spleef.getPluginConfig().getInt("map.map-top-vertex-ordinate");
        int BottomVertexOrdinate = Spleef.getPluginConfig().getInt("map.map-bottom-vertex-ordinate");

        int radius = Spleef.getPluginConfig().getInt("map.map-radius");

        int x = Spleef.getPluginConfig().getInt("map.map-origin-point.x");
        int z = Spleef.getPluginConfig().getInt("map.map-origin-point.z");

        mapEditTask = Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
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
                    mapEditTask.cancel();
                }
            }

        }, 0L, 1L); // delay=0, period=1（每 tick 清一层地图）
    }
}