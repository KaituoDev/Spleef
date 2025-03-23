package fun.kaituo.states;

import fun.kaituo.Spleef;
import fun.kaituo.gameutils.game.GameState;
import fun.kaituo.gameutils.util.Misc;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.UUID;

public class WaitingState implements GameState, Listener {
    private int secondTimer = 0;

    @EventHandler
    public void gameStartListener(PlayerInteractEvent pie) {
        if (pie.getClickedBlock() == null) {
            return;
        }
        if (!pie.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            return;
        }
        if (!pie.getClickedBlock().getLocation().equals(new Location(Spleef.getGameWorld(), 1000, 7, 4))) {
            return;
        }
        if (Bukkit.getScheduler().isCurrentlyRunning(Spleef.inst().mapEditTaskID) || Bukkit.getScheduler().isQueued(Spleef.inst().mapEditTaskID)) {
            pie.getPlayer().sendMessage("§c地图正在清理中，请稍后再试！");
            pie.setCancelled(true);
            return;
        }
        if (Spleef.inst().playerIds.size() < 2) {
            pie.getPlayer().sendMessage("§c游戏玩家不足，无法开始游戏！");
            pie.setCancelled(true);
            return;
        }
        Spleef.inst().setState(new PreparingState());
    }

    @Override
    public void enter() {
        Bukkit.getPluginManager().registerEvents(this, Spleef.inst());
        Spleef.inst().currentGameState = "WaitingState";

        Spleef.inst().playerSurvivalStage.clear();
        Spleef.inst().survivingPlayerNumber = 0;
        if(!Spleef.inst().playerIds.isEmpty()) {
            for(UUID uuid : Spleef.inst().playerIds) {
                Player player = Bukkit.getPlayer(uuid);
                if(player == null) {
                    Spleef.inst().playerIds.remove(uuid);
                    continue;
                }
                player.setGameMode(GameMode.ADVENTURE);
                player.teleport(Spleef.getLobbySpawnPoint());
                player.setRespawnLocation(Spleef.getLobbySpawnPoint(), true);
                player.getInventory().clear();
                player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, PotionEffect.INFINITE_DURATION, 4));

                Spleef.inst().playerSurvivalStage.put(uuid, false);
                ++Spleef.inst().survivingPlayerNumber;
            }
        }

        Spleef.inst().clearMap();
    }

    @Override
    public void exit() {
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

        HandlerList.unregisterAll(this);
    }

    @Override
    public void tick() {
        ++secondTimer;

        if (secondTimer%20 == 0) {
            Spleef.inst().verityPlayerList();
        }
    }

    @Override
    public void addPlayer(Player player) {
        Spleef.inst().playerIds.add(player.getUniqueId());
        Spleef.inst().playerSurvivalStage.put(player.getUniqueId(), false);
        ++Spleef.inst().survivingPlayerNumber;

        player.setGameMode(GameMode.ADVENTURE);
        player.teleport(Spleef.getLobbySpawnPoint());
        player.setRespawnLocation(Spleef.getLobbySpawnPoint());
        player.clearActivePotionEffects();
        player.getInventory().clear();
        player.getInventory().addItem(Misc.getMenu());
        player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, PotionEffect.INFINITE_DURATION, 4));

        for (net.kyori.adventure.bossbar.BossBar bar : player.activeBossBars()) {
            player.hideBossBar(bar);
        }
    }

    @Override
    public void removePlayer(Player player) {
        Spleef.inst().playerIds.remove(player.getUniqueId());
        Spleef.inst().playerSurvivalStage.remove(player.getUniqueId());
        --Spleef.inst().survivingPlayerNumber;

        player.clearActivePotionEffects();
        player.getInventory().clear();
        player.setGameMode(GameMode.ADVENTURE);
    }

    @Override
    public void forceStop() {
        Spleef.inst().getLogger().warning("Spleef > 错误：无法终止游戏");
        Spleef.inst().getLogger().warning("Spleef > 原因：游戏尚未开始运行");
    }
}
