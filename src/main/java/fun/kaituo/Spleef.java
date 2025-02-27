package fun.kaituo;


import fun.kaituo.gameutils.game.Game;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitTask;

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

    public BukkitTask MapEditTask;


    public final Set<UUID> playerIds = new HashSet<>();
    public final HashMap<UUID, Boolean> playerSurvivalStage = new HashMap<>();

    @Override
    public void addPlayer(Player player) {

    }

    @Override
    public void removePlayer(Player player) {

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
        world = this.getServer().getWorld();
        super.onEnable();
        saveDefaultConfig();
        updateExtraInfo("§e掘一死战", getLoc("hub"));
    }

    public void clearMap() {
        if (!Bukkit.getScheduler().isCurrentlyRunning(Spleef.inst().MapEditTask.getTaskId()) && !Bukkit.getScheduler().isQueued(Spleef.inst().MapEditTask.getTaskId())) {

            int TopVertexOrdinate = Spleef.getPluginConfig().getInt("map.map-top-vertex-ordinate");
            int BottomVertexOrdinate = Spleef.getPluginConfig().getInt("map.map-bottom-vertex-ordinate");

            int radius = Spleef.getPluginConfig().getInt("map.map-radius");

            int x = Spleef.getPluginConfig().getInt("map.map-origin-point.x");
            int z = Spleef.getPluginConfig().getInt("map.map-origin-point.z");

            MapEditTask = Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
                private int floor = TopVertexOrdinate;

                @Override
                public void run() {
                    // 任务逻辑
                    for (int i = x - radius; i <= x + radius; ++i) {
                        for (int j = z - radius; j <= z + radius; ++j) {
                            Bukkit.
                        }
                    }

                    // 取消任务
                    --floor;
                    if (floor < BottomVertexOrdinate) {
                        MapEditTask.cancel();
                    }
                }
            }, 0L, 1L); // delay=0, period=1（每 tick 清一层地图）
        }
        else {
            getLogger().info("Spleef > 清理地图失败");
            getLogger().info("Spleef > 原因：地图正在被加载或清理");
        }
    }
}