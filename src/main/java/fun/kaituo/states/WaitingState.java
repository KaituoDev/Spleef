package fun.kaituo.states;

import fun.kaituo.Spleef;
import fun.kaituo.gameutils.game.GameState;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.util.UUID;

public class WaitingState extends Spleef implements GameState {

    @Override
    public void enter() {
        Spleef.inst().playerSurvivalStage.clear();
        if(!Spleef.inst().playerIds.isEmpty()) {
            for(UUID uuid : Spleef.inst().playerIds) {
                Player player = Bukkit.getPlayer(uuid);
                if(player != null) {
                    player.setGameMode(GameMode.SURVIVAL);
                    player.teleport(location);
                    player.setRespawnLocation(location, true);
                    Spleef.inst().playerSurvivalStage.put(uuid, false);
                }
            }
        }
        Spleef.inst().clearMap();
    }

    @Override
    public void exit() {

    }

    @Override
    public void tick() {

    }

    @Override
    public void addPlayer(Player player) {
        player.setGameMode(GameMode.ADVENTURE);
        player.teleport(location);
        player.setRespawnLocation(location);
        player.clearActivePotionEffects();
        Spleef.inst().playerIds.add(player.getUniqueId());
        Spleef.inst().playerSurvivalStage.put(player.getUniqueId(), false);
    }

    @Override
    public void removePlayer(Player player) {
        Spleef.inst().playerIds.remove(player.getUniqueId());
        Spleef.inst().playerSurvivalStage.remove(player.getUniqueId());
    }

    @Override
    public void forceStop() {
        getLogger().warning("Spleef > 错误：无法终止游戏");
        getLogger().warning("Spleef > 原因：游戏尚未开始运行");
    }
}
