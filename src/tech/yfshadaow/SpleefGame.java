package tech.yfshadaow;

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
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BoundingBox;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static tech.yfshadaow.GameUtils.world;

public class SpleefGame extends Game implements Listener {
    private static final SpleefGame instance = new SpleefGame((Spleef) Bukkit.getPluginManager().getPlugin("Spleef"));
    List<Player> playersAlive;

    private SpleefGame(Spleef plugin) {
        this.plugin = plugin;
        players = Spleef.players;
        playersAlive = new ArrayList<>();
        initGame(plugin, "Spleef", 5, new Location(world, 1000, 7, 4), BlockFace.NORTH, new Location(world, 996, 7, 0),
                BlockFace.EAST, new Location(world, 1000, 6, 0), new BoundingBox(960, 55, -40, 1040, 132, 40));
    }

    public static SpleefGame getInstance() {
        return instance;
    }

    @EventHandler
    public void preventDroppingItem(PlayerDropItemEvent pdie) {
        if (playersAlive.contains(pdie.getPlayer())) {
            pdie.setCancelled(true);
        }
    }

    @EventHandler
    public void preventClickingInventory(InventoryClickEvent ice) {
        if (ice.getWhoClicked() instanceof Player) {
            if (playersAlive.contains(ice.getWhoClicked())) {
                ice.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void preventBlockDrop(BlockBreakEvent bbe) {
        if (bbe.getBlock().getLocation().getX() > 500 && bbe.getBlock().getLocation().getX() < 1500) {
            if (bbe.getBlock().getLocation().getY() > -500 && bbe.getBlock().getLocation().getY() < 500) {
                bbe.setDropItems(false);
            }
        }
    }

    @EventHandler
    public void preventDamage(EntityDamageEvent ede) {
        if (ede.getEntity() instanceof Player) {
            if (playersAlive.contains(ede.getEntity())) {
                ede.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerChangeGame(PlayerChangeGameEvent pcge) {
        players.remove(pcge.getPlayer());
        playersAlive.remove(pcge.getPlayer());
    }

    @Override
    protected void initGameRunnable() {
        gameRunnable = () -> {
            World world = Bukkit.getWorld("world");
            for (Entity e : world.getNearbyEntities(new Location(world, 1000, 6, 0), 10, 10, 10)) {
                if (e instanceof Player) {
                    players.add((Player) e);
                    playersAlive.add((Player) e);
                }
            }
            if (players.size() < 2) {
                for (Player p : players) {
                    p.sendMessage("§c至少需要2人才能开始游戏！");
                }
                players.clear();
                playersAlive.clear();
            } else {
                startCountdown();
                pasteSchematic("spleef1", 1000, 100, 0, true);
                Bukkit.getPluginManager().registerEvents(this, plugin);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    placeSpectateButton();
                    removeStartButton();
                    for (Player p : players) {
                        p.teleport(new Location(world, 1000, 100, 0));
                        p.getInventory().clear();
                    }
                });
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    for (Player p : players) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "item replace entity " + p.getName() + " hotbar.0 with iron_pickaxe{Unbreakable:1,Enchantments:[{id:efficiency,lvl:10}],HideFlags:13,CanDestroy:[white_concrete,blue_ice,brown_concrete,green_concrete,light_blue_stained_glass_pane,sea_lantern,cobblestone_slab,polished_andesite,stone_brick_wall,stone_brick_stairs,stone_brick_slab]} 1");
                        p.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 999999, 49, false, false));
                        p.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 999999, 0, false, false));
                    }
                }, countDownSeconds * 20);
                taskIds.add(Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
                    Player playerOut = null;
                    for (Player p : playersAlive) {
                        if (p.getLocation().getY() <= 80) {
                            playerOut = p;
                            break;
                        }
                    }
                    if (playerOut != null) {
                        for (Player p : players) {
                            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§f§l" + playerOut.getName() + " §c坠入深渊！"));
                        }
                        playerOut.setGameMode(GameMode.SPECTATOR);
                        playersAlive.remove(playerOut);
                    }

                }, 100, 1));
                taskIds.add(Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
                    if (playersAlive.size() <= 1) {
                        Player winner = playersAlive.get(0);
                        spawnFireworks(winner);
                        List<Player> playersCopy = new ArrayList<>(players);
                        for (Player p : playersCopy) {
                            p.sendTitle("§e" + playersAlive.get(0).getName() + " §b获胜了！", null, 5, 50, 5);
                            p.getInventory().clear();
                            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                p.teleport(new Location(world, 1000.5, 6.0625, 0.5));
                                Bukkit.getPluginManager().callEvent(new PlayerEndGameEvent(p, this));
                                pasteSchematic("spleefempty", 1000, 100, 0, false);
                            }, 100);
                        }
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            removeSpectateButton();
                            placeStartButton();
                            HandlerList.unregisterAll(this);
                        }, 100);
                        players.clear();
                        playersAlive.clear();
                        List<Integer> taskIdsCopy = new ArrayList<>(taskIds);
                        taskIds.clear();
                        for (int i : taskIdsCopy) {
                            Bukkit.getScheduler().cancelTask(i);
                        }
                    }
                }, 100, 1));
            }
        };
    }

    public long getTime(World world) {
        return (world.getGameTime());
    }

    @Override
    protected void savePlayerQuitData(Player p) throws IOException {
        players.remove(p);
        playersAlive.remove(p);
    }

    @Override
    protected void rejoin(Player player) {
    }

    private void pasteSchematic(String name, double x, double y, double z, boolean ignoreAir) {
        File file = new File("plugins/WorldEdit/schematics/" + name + ".schem");
        ClipboardFormat format = ClipboardFormats.findByFile(file);
        Clipboard clipboard = null;
        try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
            clipboard = reader.read();
        } catch (Exception e) {
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

    public Block[] getBlockss(Player p) {
        Block block = p.getLocation().getBlock().getRelative(BlockFace.DOWN);
        double x = p.getLocation().getX();
        double z = p.getLocation().getZ();
        double xOffSet = x - (int) (Math.floor(x));
        double zOffset = z - (int) (Math.floor(z));
        if (xOffSet < 0.3) {
            if (zOffset < 0.3) {
                return new Block[]{block, block.getRelative(BlockFace.NORTH_WEST), block.getRelative(BlockFace.NORTH), block.getRelative(BlockFace.WEST)};
            } else if (zOffset > 0.7) {
                return new Block[]{block, block.getRelative(BlockFace.SOUTH_WEST), block.getRelative(BlockFace.SOUTH), block.getRelative(BlockFace.WEST)};
            } else {
                return new Block[]{block, block.getRelative(BlockFace.WEST)};
            }
        } else if (xOffSet > 0.7) {
            if (zOffset < 0.3) {
                return new Block[]{block, block.getRelative(BlockFace.NORTH_EAST), block.getRelative(BlockFace.NORTH), block.getRelative(BlockFace.EAST)};
            } else if (zOffset > 0.7) {
                return new Block[]{block, block.getRelative(BlockFace.SOUTH_EAST), block.getRelative(BlockFace.SOUTH), block.getRelative(BlockFace.EAST)};
            } else {
                return new Block[]{block, block.getRelative(BlockFace.EAST)};
            }

        } else {
            if (zOffset < 0.3) {
                return new Block[]{block, block.getRelative(BlockFace.NORTH)};
            } else if (zOffset > 0.7) {
                return new Block[]{block, block.getRelative(BlockFace.SOUTH)};
            } else {
                return new Block[]{block};
            }

        }
    }
}
