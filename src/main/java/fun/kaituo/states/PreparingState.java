package fun.kaituo.states;

import fun.kaituo.Spleef;
import fun.kaituo.gameutils.game.GameState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.UUID;

public class PreparingState implements GameState, Listener {
    private int preparingCountdown = 200;

    private final Location mapOriginPoint = Spleef.getMapOriginPoint();
    private final Location playerOriginPoint = mapOriginPoint.add(0.5, 0, 0.5);

    @Override
    public void enter() {
        Bukkit.getPluginManager().registerEvents(this, Spleef.inst());

        if (Spleef.inst().survivingPlayerNumber < 2) {
            Spleef.inst().getLogger().warning("Spleef > 错误：无法开始游戏");
            Spleef.inst().getLogger().warning("Spleef > 原因：玩家数量异常");
            Spleef.inst().setState(new WaitingState());
            return;
        }

        String schematicName;
        if (Spleef.inst().isNormalMode()) {
            schematicName = "spleef1";
        }
        else {
            schematicName = "spleef_3floor";
        }

        Spleef.inst().pasteSchematic(schematicName, mapOriginPoint, true);

        Spleef.inst().playerSurvivalStage.clear();
        Spleef.inst().survivingPlayerNumber = 0;
        for (UUID uuid : Spleef.inst().playerIds) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                Spleef.inst().playerIds.remove(uuid);
                continue;
            }
            Spleef.inst().playerSurvivalStage.put(uuid, true);
            ++Spleef.inst().survivingPlayerNumber;
            player.setGameMode(GameMode.SURVIVAL);
            player.teleport(playerOriginPoint);
        }
    }

    @Override
    public void exit() {

    }

    @Override
    public void tick() {
        if (preparingCountdown > 0) {
            --preparingCountdown;
        }
        if (preparingCountdown%20 == 0) {
            NamedTextColor countdownColor = NamedTextColor.GOLD;
            if (preparingCountdown <= 100) {
                if (preparingCountdown <= 60) {
                    countdownColor = NamedTextColor.RED;
                }
                showCountdown("游戏即将开始", preparingCountdown, countdownColor);
            }

            Spleef.inst().verityPlayerList();
        }
        if (preparingCountdown <= 0) {
            for (UUID uuid : Spleef.inst().playerIds) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    player.resetTitle();
                }
            }
            Spleef.inst().setState(new RunningState());
        }
    }

    @Override
    public void addPlayer(Player player) {
        Spleef.inst().playerIds.add(player.getUniqueId());
        Spleef.inst().playerSurvivalStage.put(player.getUniqueId(), true);
        ++Spleef.inst().survivingPlayerNumber;

        player.setGameMode(GameMode.SURVIVAL);
        player.teleport(playerOriginPoint);
        player.setRespawnLocation(Spleef.getLobbySpawnPoint());
        player.clearActivePotionEffects();
    }

    @Override
    public void removePlayer(Player player) {
        Spleef.inst().playerIds.remove(player.getUniqueId());
        Spleef.inst().playerSurvivalStage.remove(player.getUniqueId());
        --Spleef.inst().survivingPlayerNumber;
    }

    @Override
    public void forceStop() {
        if (Bukkit.getScheduler().isCurrentlyRunning(Spleef.inst().mapEditTaskID) || Bukkit.getScheduler().isQueued(Spleef.inst().mapEditTaskID)) {
            Bukkit.getScheduler().cancelTask(Spleef.inst().mapEditTaskID);
        }
        Spleef.inst().clearMap();
        Spleef.inst().setState(new WaitingState());
    }

    private void showCountdown(String msg, int countdown, NamedTextColor countdownColor) {
        for (UUID uuid : Spleef.inst().playerIds) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && Spleef.inst().playerSurvivalStage.get(uuid)) {
                player.resetTitle();
                player.showTitle(Title.title(Component.text(msg).color(NamedTextColor.GREEN),
                        Component.text(countdown/20).color(countdownColor)
                ));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 1f, 1f);
            }
        }
    }
}
