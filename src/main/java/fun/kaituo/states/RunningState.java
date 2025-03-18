package fun.kaituo.states;

import fun.kaituo.Spleef;
import fun.kaituo.gameutils.game.GameState;
import fun.kaituo.gameutils.util.ItemStackBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BoundingBox;

import java.time.Duration;
import java.util.UUID;

public class RunningState extends Spleef implements GameState {
    private Spleef game;

    private final ItemStack shovel = new ItemStackBuilder(Material.STONE_SHOVEL).setUnbreakable(true).setDisplayName("§e§l全村最好的铲子").build();
    private final ItemStack snowball = new ItemStackBuilder(Material.SNOWBALL).setDisplayName("§b§l雪球炸弹").build();
    private final ItemStack feather = new ItemStackBuilder(Material.FEATHER).setDisplayName("§e§l飞升").setLore("§b受任于坠落之际，奉命于危难之间").build();
    private final ItemStack kb_stick = new ItemStackBuilder(Material.DEBUG_STICK).setDisplayName("§c§l击退棒").setLore("§eReady to lift off!").addEnchantment(Enchantment.KNOCKBACK, 1).build();

    private static final int STATE_DURATION = 3600;

    private int stateCountdown = STATE_DURATION;
    private boolean gameMode = true;
    private int particleSpawnerID;

    private BossBar countdownBossBar = Bukkit.createBossBar(
            "死亡竞赛 开始于:" + stateCountdown + "秒后",
            BarColor.YELLOW,
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
        pie.setCancelled(true);
        getGameWorld().playSound(pie.getClickedBlock().getLocation(), Sound.BLOCK_SNOW_BREAK, 1.0f, 1.0f);
        getGameWorld().spawnParticle(Particle.BLOCK, pie.getClickedBlock().getLocation().clone().add(0.5, 0.5, 0.5), 50, pie.getClickedBlock());
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

        int featherAmount = player.getInventory().getItemInMainHand().getAmount();
        if (featherAmount < 2) {
            player.getInventory().getItemInMainHand().setType(Material.AIR);
        }
        else {
            player.getInventory().getItemInMainHand().setAmount(featherAmount - 1);
        }

        pie.setCancelled(true);

        player.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 20, 49));
        player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 100, 0));

        particleSpawnerID = Bukkit.getScheduler().runTaskTimer(Spleef.inst(), new Runnable() {
            private int runs = 0;

            @Override
            public void run() {
                Location playerLocation = player.getLocation();
                player.spawnParticle(Particle.FIREWORK, playerLocation, 32);

                for (int j = playerLocation.getBlockY(); j <= playerLocation.getBlockY() + 10; ++j) {
                    for (int i = playerLocation.getBlockX() - 1; i <= playerLocation.getBlockX() + 1; ++i) {
                        for (int k = playerLocation.getBlockZ() - 1; k <= playerLocation.getBlockZ() + 1; ++k) {
                            getGameWorld().getBlockAt(i, j, k).setType(Material.AIR);
                        }
                    }
                }

                ++runs;
                if (runs > 20) {
                    Bukkit.getScheduler().cancelTask(particleSpawnerID);
                }
            }
        }, 0L, 1L).getTaskId();
    }

    @EventHandler
    public void onPlayerBreakBlock(BlockBreakEvent bbe) {
        if (bbe.getBlock().getLocation().getBlockX() < getGameBox().getMinX()) {
            return;
        }
        if (bbe.getBlock().getLocation().getBlockX() > getGameBox().getMaxX()) {
            return;
        }
        if (bbe.getBlock().getLocation().getBlockZ() < getGameBox().getMinZ()) {
            return;
        }
        if (bbe.getBlock().getLocation().getBlockZ() > getGameBox().getMaxZ()) {
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
        BoundingBox box = getGameBox();
        if (snowball.getLocation().getX() < box.getMinX() || snowball.getLocation().getX() > box.getMaxX()) {
            return;
        }
        if (snowball.getLocation().getY() < box.getMinZ()) {
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
                    getGameWorld().getBlockAt(i, j, k).setType(Material.AIR);
                }
            }
        }
        getGameWorld().createExplosion(phe.getEntity().getLocation(), snowballExplosionPower, false, true);
    }

    @Override
    public void enter() {
        Bukkit.getPluginManager().registerEvents(this, Spleef.inst());
        gameMode = Spleef.inst().isNormalMode(); // true=普通模式 false=无限火力
        for (UUID uuid : Spleef.inst().playerIds) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                continue;
            }

            if(gameMode) {
                player.getInventory().addItem(shovel);
            }
            else {
                player.getInventory().addItem(kb_stick);
            }
            player.getInventory().addItem(feather);

            countdownBossBar.addPlayer(player);
        }
    }

    @Override
    public void exit() {

    }

    @Override
    public void tick() {
        --stateCountdown;
        if (stateCountdown < 0) {
            countdownBossBar.removeAll();
            Spleef.inst().setState(new DeathRaceState());
            return;
        }

        if (stateCountdown%20 == 0) { //整秒数
            countdownBossBar.setTitle("死亡竞赛 开始于:" + stateCountdown/20 + "秒后");
            double progress = Math.max(0.0, (double) stateCountdown / STATE_DURATION);
            countdownBossBar.setProgress(progress);
        }

        if (gameMode) {
            if (stateCountdown%200 == 0) {
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

        for (UUID uuid : Spleef.inst().playerIds) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                continue;
            }
            if (!Spleef.inst().playerSurvivalStage.get(uuid)) {
                continue;
            }
            if (player.getLocation().getY() < 35) {
                player.getInventory().clear();
                player.setGameMode(GameMode.SPECTATOR);
                Spleef.inst().playerSurvivalStage.remove(player.getUniqueId());
                Spleef.inst().playerSurvivalStage.put(player.getUniqueId(), false);
                player.showTitle(Title.title(Component.text("你坠入了深渊！").color(NamedTextColor.RED),
                        Component.text("你已成为旁观者").color(NamedTextColor.GOLD),
                        Title.Times.times(Duration.ZERO, Duration.ofSeconds(3), Duration.ofSeconds(1))
                ));
                for (UUID id : Spleef.inst().playerIds) {
                    Player p = Bukkit.getPlayer(id);
                    if (p == null) {
                        continue;
                    }
                    p.sendMessage("§f§l" + p.getName() + "§c坠入深渊！");
                }
            }
        }

        Spleef.inst().verityPlayerList();
    }

    @Override
    public void addPlayer(Player player) {
        Spleef.inst().playerIds.add(player.getUniqueId());
        Spleef.inst().playerSurvivalStage.put(player.getUniqueId(), false);

        player.setGameMode(GameMode.SPECTATOR);
        player.teleport(getMapOriginPoint());
        player.setRespawnLocation(location);
        player.clearActivePotionEffects();

        countdownBossBar.addPlayer(player);
    }

    @Override
    public void removePlayer(Player player) {
        Spleef.inst().playerIds.remove(player.getUniqueId());
        Spleef.inst().playerSurvivalStage.remove(player.getUniqueId());
        if (player.getGameMode().equals(GameMode.SURVIVAL)) {
            --Spleef.inst().survivingPlayerNumber;
        }

        countdownBossBar.removePlayer(player);
    }

    @Override
    public void forceStop() {
        countdownBossBar.removeAll();
        if (Bukkit.getScheduler().isCurrentlyRunning(Spleef.inst().mapEditTaskID) || Bukkit.getScheduler().isQueued(Spleef.inst().mapEditTaskID)) {
            Bukkit.getScheduler().cancelTask(mapEditTaskID);
        }
        Spleef.inst().clearMap();
        Spleef.inst().setState(new WaitingState());
    }
}
