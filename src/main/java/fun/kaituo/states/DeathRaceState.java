package fun.kaituo.states;

import fun.kaituo.Spleef;
import fun.kaituo.gameutils.game.GameState;
import fun.kaituo.gameutils.util.ItemStackBuilder;
import fun.kaituo.gameutils.util.Misc;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BoundingBox;

import java.time.Duration;
import java.util.HashMap;
import java.util.UUID;

public class DeathRaceState implements GameState, Listener {
    private final ItemStack shovel = new ItemStackBuilder(Material.STONE_SHOVEL).setUnbreakable(true).setDisplayName("§e§l全村最好的铲子").build();
    private final ItemStack snowball = new ItemStackBuilder(Material.SNOWBALL).setDisplayName("§b§l雪球炸弹").build();
    private final ItemStack feather = new ItemStackBuilder(Material.FEATHER).setDisplayName("§e§l飞升").setLore("§b受任于坠落之际，奉命于危难之间").build();

    private final int originX = Spleef.getMapOriginPoint().getBlockX();
    private final int originZ = Spleef.getMapOriginPoint().getBlockZ();
    private final int mapHeight = (int) Spleef.getGameBox().getMaxY() - (int) Spleef.getGameBox().getMinY();
    private final int mapRadius = Spleef.getPluginConfig().getInt("map.map-radius");

    private int tick = 0;
    private int currentFloor = (int) Spleef.getGameBox().getMaxY();

    private HashMap<UUID, Integer> particleSpawnerIDs = new HashMap<>();

    private boolean gameMode = true;

    private BossBar mapClearBossBar = Bukkit.createBossBar(
            "§c当前最高层: y=" + currentFloor,
            BarColor.RED,
            BarStyle.SOLID
    );

    @EventHandler
    public void onPlayerTryBreakBlock(PlayerInteractEvent pie) {
        if (pie.getClickedBlock() == null) {
            return;
        }
        if (!pie.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
            return;
        }
        Player player = pie.getPlayer();
        if (!Spleef.inst().playerIds.contains(player.getUniqueId())) {
            return;
        }
        if (!Spleef.inst().playerSurvivalStage.get(player.getUniqueId())) {
            return;
        }
        if (!player.getInventory().getItemInMainHand().equals(shovel)) {
            return;
        }
        Spleef.getGameWorld().playSound(pie.getClickedBlock().getLocation(), Sound.BLOCK_SNOW_BREAK, 1.0f, 1.0f);
        Spleef.getGameWorld().spawnParticle(Particle.BLOCK, pie.getClickedBlock().getLocation().clone().add(0.5, 0.5, 0.5), 50, pie.getClickedBlock().getBlockData());
        pie.getClickedBlock().setType(Material.AIR);
    }

    @EventHandler
    public void onPlayerUsingFeather(PlayerInteractEvent pie) {
        if (!pie.getAction().equals(Action.RIGHT_CLICK_AIR) && !pie.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            return;
        }
        Player player = pie.getPlayer();
        if (!Spleef.inst().playerIds.contains(player.getUniqueId())) {
            return;
        }
        if (!Spleef.inst().playerSurvivalStage.get(player.getUniqueId())) {
            return;
        }
        if (!player.getInventory().getItemInMainHand().isSimilar(feather)) {
            return;
        }
        if (particleSpawnerIDs.containsKey(player.getUniqueId())) {
            player.sendMessage("§c每次只能使用一个\"飞升\"！");
            return;
        }

        int featherAmount = player.getInventory().getItemInMainHand().getAmount();
        player.getInventory().getItemInMainHand().setAmount(featherAmount - 1);

        pie.setCancelled(true);

        player.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 20, 49));
        player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 100, 0));

        particleSpawnerIDs.put(player.getUniqueId(), Bukkit.getScheduler().runTaskTimer(Spleef.inst(), new Runnable() {
            private int runs = 0;

            @Override
            public void run() {
                Location playerLocation = player.getLocation();
                player.spawnParticle(Particle.FIREWORK, playerLocation, 32);

                for (int j = playerLocation.getBlockY(); j <= playerLocation.getBlockY() + 10; ++j) {
                    for (int i = playerLocation.getBlockX() - 1; i <= playerLocation.getBlockX() + 1; ++i) {
                        for (int k = playerLocation.getBlockZ() - 1; k <= playerLocation.getBlockZ() + 1; ++k) {
                            Spleef.getGameWorld().getBlockAt(i, j, k).setType(Material.AIR);
                        }
                    }
                }

                ++runs;
                if (runs > 30) {
                    Bukkit.getScheduler().cancelTask(particleSpawnerIDs.get(player.getUniqueId()));
                    particleSpawnerIDs.remove(player.getUniqueId());
                }
            }
        }, 0L, 1L).getTaskId());
    }

    @EventHandler
    public void onPlayerBreakBlock(BlockBreakEvent bbe) {
        if (bbe.getBlock().getLocation().getBlockX() < Spleef.getGameBox().getMinX()) {
            return;
        }
        if (bbe.getBlock().getLocation().getBlockX() > Spleef.getGameBox().getMaxX()) {
            return;
        }
        if (bbe.getBlock().getLocation().getBlockZ() < Spleef.getGameBox().getMinZ()) {
            return;
        }
        if (bbe.getBlock().getLocation().getBlockZ() > Spleef.getGameBox().getMaxZ()) {
            return;
        }

        if (Spleef.inst().playerIds.contains(bbe.getPlayer().getUniqueId())) {
            bbe.setCancelled(true);
        }
    }

    @EventHandler
    public void onSnowballLand(ProjectileHitEvent phe) {
        if (!phe.getEntity().getType().equals(EntityType.SNOWBALL)) {
            return;
        }
        if (phe.getHitBlock() == null) {
            return;
        }
        Entity snowball = phe.getEntity();
        BoundingBox box = Spleef.getGameBox();
        if (snowball.getLocation().getX() < box.getMinX() || snowball.getLocation().getX() > box.getMaxX()) {
            return;
        }
        if (snowball.getLocation().getY() < box.getMinY()) {
            return;
        }
        if (snowball.getLocation().getZ() < box.getMinZ() || snowball.getLocation().getZ() > box.getMaxZ()) {
            return;
        }

        int x = phe.getEntity().getLocation().getBlockX();
        int y = phe.getEntity().getLocation().getBlockY();
        int z = phe.getEntity().getLocation().getBlockZ();
        float snowballExplosionPower;

        if (Spleef.inst().isNormalMode()) {
            snowballExplosionPower = 1.1f;
        }
        else {
            snowballExplosionPower = 2.4f;
        }

        for (int i = x - 1; i <= x + 1; i += 1) {
            for (int j = y - 2; j <= y + 2; j += 1) {
                for (int k = z - 1; k <= z + 1; k += 1) {
                    Spleef.getGameWorld().getBlockAt(i, j, k).setType(Material.AIR);
                }
            }
        }
        Spleef.getGameWorld().createExplosion(phe.getEntity().getLocation(), snowballExplosionPower, false, true);
    }

    @Override
    public void enter() {
        Bukkit.getPluginManager().registerEvents(this, Spleef.inst());
        Spleef.inst().currentGameState = "DeathRaceState";

        gameMode = Spleef.inst().isNormalMode();
        for (UUID uuid : Spleef.inst().playerIds) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                continue;
            }

            ItemStack currentFeather = player.getInventory().getItem(1);
            if(currentFeather != null && currentFeather.isSimilar(feather)) {
                int currentAmount = currentFeather.getAmount();
                if (currentAmount < 64) {
                    currentFeather.setAmount(currentAmount + 1);
                }
            }
            else {
                ItemStack newFeather = feather.clone();
                newFeather.setAmount(1);
                player.getInventory().setItem(1, newFeather);
            }

            player.playSound(player, Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 1f);
            player.showTitle(Title.title(Component.text("死亡竞赛！").color(NamedTextColor.RED),
                    Component.text("祝君好运").color(NamedTextColor.GOLD),
                    Title.Times.times(Duration.ZERO, Duration.ofSeconds(2), Duration.ofSeconds(1))
            ));
            player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, PotionEffect.INFINITE_DURATION, 4));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, PotionEffect.INFINITE_DURATION, 4));

            mapClearBossBar.addPlayer(player);
        }
    }

    @Override
    public void exit() {
        mapClearBossBar.removeAll();

        for (UUID uuid : Spleef.inst().playerIds) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                continue;
            }

            for (net.kyori.adventure.bossbar.BossBar bar : player.activeBossBars()) {
                player.hideBossBar(bar);
            }

            player.clearActivePotionEffects();
        }
        for (UUID uuid : particleSpawnerIDs.keySet()) {
            Bukkit.getScheduler().cancelTask(particleSpawnerIDs.get(uuid));
        }
        particleSpawnerIDs.clear();

        HandlerList.unregisterAll(this);
    }

    @Override
    public void tick() {
        ++tick;
        if (tick%20 == 0) {
            tick = 0;

            if (currentFloor > (int) Spleef.getGameBox().getMinY()) {
                for (int i = originX - mapRadius; i <= originX + mapRadius; ++i) {
                    for (int j = originZ - mapRadius; j <= originZ + mapRadius; ++j) {
                        if (!Spleef.getGameWorld().getBlockAt(i, currentFloor, j).getType().equals(Material.LIGHT)) {
                            Spleef.getGameWorld().spawnParticle(Particle.BLOCK, new Location(Spleef.getGameWorld(), i + 0.5, currentFloor + 0.5, j + 0.5), 10, Spleef.getGameWorld().getBlockAt(i, currentFloor, j).getBlockData());
                        }
                        Spleef.getGameWorld().getBlockAt(i, currentFloor, j).setType(Material.AIR);
                    }
                }

                --currentFloor;
                mapClearBossBar.setTitle("§c当前最高层: y=" + currentFloor);
                double progress = Math.max(0.0, (currentFloor - Spleef.getGameBox().getMinY()) / mapHeight);
                mapClearBossBar.setProgress(progress);
            }

            Spleef.inst().verityPlayerList();
        }

        if (gameMode) {
            if (tick%20 == 0) {
                for (UUID uuid : Spleef.inst().playerIds) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player == null) {
                        continue;
                    }
                    player.getInventory().addItem(snowball);
                }
            }
        }
        else {
            for (UUID uuid : Spleef.inst().playerIds) {
                Player player = Bukkit.getPlayer(uuid);
                if (player == null) {
                    continue;
                }
                player.getInventory().addItem(snowball);
            }
        }

        Spleef.inst().confirmPlayerSurvival();
    }

    @Override
    public void addPlayer(Player player) {
        Spleef.inst().playerIds.add(player.getUniqueId());
        Spleef.inst().playerSurvivalStage.put(player.getUniqueId(), false);

        player.setGameMode(GameMode.SPECTATOR);
        player.teleport(Spleef.getMapOriginPoint());
        player.setRespawnLocation(Spleef.getLobbySpawnPoint());
        player.clearActivePotionEffects();
        player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, PotionEffect.INFINITE_DURATION, 4));

        mapClearBossBar.addPlayer(player);
    }

    @Override
    public void removePlayer(Player player) {
        Spleef.inst().playerIds.remove(player.getUniqueId());
        Spleef.inst().playerSurvivalStage.remove(player.getUniqueId());
        if (player.getGameMode().equals(GameMode.SURVIVAL)) {
            --Spleef.inst().survivingPlayerNumber;
        }

        mapClearBossBar.removePlayer(player);

        player.clearActivePotionEffects();
        player.getInventory().clear();
    }

    @Override
    public void forceStop() {
        mapClearBossBar.removeAll();
        if (Bukkit.getScheduler().isCurrentlyRunning(Spleef.inst().mapEditTaskID) || Bukkit.getScheduler().isQueued(Spleef.inst().mapEditTaskID)) {
            Bukkit.getScheduler().cancelTask(Spleef.inst().mapEditTaskID);
        }
        for (UUID uuid : particleSpawnerIDs.keySet()) {
            Bukkit.getScheduler().cancelTask(particleSpawnerIDs.get(uuid));
        }
        particleSpawnerIDs.clear();

        for (UUID uuid : Spleef.inst().playerIds) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                continue;
            }

            player.clearActivePotionEffects();
            player.getInventory().clear();
            player.getInventory().addItem(Misc.getMenu());
        }

        HandlerList.unregisterAll(this);
        Spleef.inst().setState(new WaitingState());
    }
}
