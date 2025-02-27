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

    }

    @Override
    public void removePlayer(Player player) {

    }

    @Override
    public void forceStop() {

    }
}
