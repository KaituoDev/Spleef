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
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;

public class SpleefGame extends BukkitRunnable implements Listener {
    World world;
    Spleef plugin;
    List<Player> players;
    List<Player> playersAlive;
    List<Integer> taskIds;

    public SpleefGame(Spleef plugin) {
        this.plugin = plugin;
        this.players = plugin.players;
        this.playersAlive = new ArrayList<>();
        this.taskIds = new ArrayList<>();
        this.world = Bukkit.getWorld("world");
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
        if (bbe.getBlock().getLocation().getX() > 500 &&bbe.getBlock().getLocation().getX() < 1500) {
            if (bbe.getBlock().getLocation().getY() > -500 &&bbe.getBlock().getLocation().getY() < 500) {
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
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent pqe) {
        players.remove(pqe.getPlayer());
        playersAlive.remove(pqe.getPlayer());
    }

    @Override
    public void run() {
        World world = Bukkit.getWorld("world");
        for (Entity e :world.getNearbyEntities(new Location(world, 1000,6,0),10,10,10) ) {
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
            pasteSchematic("spleef1",1000,100,0,true);
            Bukkit.getPluginManager().registerEvents(this,plugin);
            Bukkit.getScheduler().runTask(plugin, ()-> {
                world.getBlockAt(1000,7,4).setType(Material.AIR);
                Block block = world.getBlockAt(996,7,0);
                block.setType(Material.OAK_BUTTON);
                BlockData data = block.getBlockData().clone();
                ((Directional)data).setFacing(BlockFace.EAST);
                block.setBlockData(data);
                for (Player p : players) {
                    p.teleport(new Location(world, 1000,100,0));
                    p.sendTitle("§a游戏还有 5 秒开始",null,2,16,2);
                    p.playSound(p.getLocation(),Sound.BLOCK_NOTE_BLOCK_HARP,1f,1f);
                    p.getInventory().clear();
                }
            });
            Bukkit.getScheduler().runTaskLater(plugin, ()-> {
                for (Player p : players) {
                    p.sendTitle("§a游戏还有 4 秒开始",null,2,16,2);
                    p.playSound(p.getLocation(),Sound.BLOCK_NOTE_BLOCK_HARP,1f,1f);
                }
            },20);
            Bukkit.getScheduler().runTaskLater(plugin, ()-> {
                for (Player p : players) {
                    p.sendTitle("§a游戏还有 3 秒开始",null,2,16,2);
                    p.playSound(p.getLocation(),Sound.BLOCK_NOTE_BLOCK_HARP,1f,1f);
                }
            },40);
            Bukkit.getScheduler().runTaskLater(plugin, ()-> {
                for (Player p : players) {
                    p.sendTitle("§a游戏还有 2 秒开始",null,2,16,2);
                    p.playSound(p.getLocation(),Sound.BLOCK_NOTE_BLOCK_HARP,1f,1f);
                }
            },60);
            Bukkit.getScheduler().runTaskLater(plugin, ()-> {
                for (Player p : players) {
                    p.sendTitle("§a游戏还有 1 秒开始",null,2,16,2);
                    p.playSound(p.getLocation(),Sound.BLOCK_NOTE_BLOCK_HARP,1f,1f);
                }
            },80);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                for (Player p: players) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(),"item replace entity " + p.getName() + " hotbar.0 with iron_pickaxe{Unbreakable:1,Enchantments:[{id:efficiency,lvl:10}],HideFlags:13,CanDestroy:[white_concrete,blue_ice,brown_concrete,green_concrete,light_blue_stained_glass_pane,sea_lantern,cobblestone_slab,polished_andesite,stone_brick_wall,stone_brick_stairs,stone_brick_slab]} 1");
                    p.sendTitle("§e游戏开始！",null,2,16,2);
                    p.playSound(p.getLocation(),Sound.BLOCK_NOTE_BLOCK_HARP,1f,2f);
                    p.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE,999999,49,false,false));
                    p.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION,999999,0,false,false));
                }

            }, 100);
            taskIds.add(Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () ->{
                Player playerOut = null;
                for (Player p: playersAlive) {
                    if (p.getLocation().getY() <= 80) {
                        playerOut = p;
                        break;
                    }
                }
                if (playerOut != null) {
                    for (Player p : players) {
                        p.spigot().sendMessage(ChatMessageType.ACTION_BAR,new TextComponent("§f§l" + playerOut.getName() + " §c坠入深渊！"));
                    }
                    playerOut.setGameMode(GameMode.SPECTATOR);
                    playersAlive.remove(playerOut);
                }

            },100,1));
            taskIds.add(Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
                if (playersAlive.size() <= 1) {
                    Player winner = playersAlive.get(0);
                    spawnFirework(winner.getLocation());
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        spawnFirework(winner.getLocation());
                    },8);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        spawnFirework(winner.getLocation());
                    },16);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        spawnFirework(winner.getLocation());
                    },24);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        spawnFirework(winner.getLocation());
                    },32);
                    List<Player> playersCopy = new ArrayList<>(players);
                    for (Player p : playersCopy) {
                        p.sendTitle("§e" + playersAlive.get(0).getName() + " §b获胜了！",null,5,50, 5);
                        p.getInventory().clear();
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            p.teleport(new Location(world,1000.5,6.0625,0.5));
                            Bukkit.getPluginManager().callEvent(new PlayerEndGameEvent(p));
                            pasteSchematic("spleefempty",1000,100,0,false);
                        },100);
                    }
                    Bukkit.getScheduler().runTaskLater(plugin, ()-> {
                        Block block = world.getBlockAt(1000,7,4);
                        block.setType(Material.OAK_BUTTON);
                        BlockData data = block.getBlockData().clone();
                        ((Directional)data).setFacing(BlockFace.NORTH);
                        block.setBlockData(data);
                        world.getBlockAt(996,7,0).setType(Material.AIR);
                        HandlerList.unregisterAll(this);
                    },100);
                    players.clear();
                    playersAlive.clear();
                    List<Integer> taskIdsCopy = new ArrayList<>(taskIds);
                    taskIds.clear();
                    for (int i : taskIdsCopy) {
                        Bukkit.getScheduler().cancelTask(i);
                    }
                }

            },100,1));
        }
    }
    public static void spawnFirework(Location location){
        Location loc = location;
        loc.setY(loc.getY() + 0.9);
        Firework fw = (Firework) loc.getWorld().spawnEntity(loc, EntityType.FIREWORK);
        FireworkMeta fwm = fw.getFireworkMeta();

        fwm.setPower(2);
        fwm.addEffect(FireworkEffect.builder().withColor(Color.LIME).flicker(true).build());

        fw.setFireworkMeta(fwm);
        fw.detonate();
    }
    public long getTime(World world) {
        return (world.getGameTime());
    }
    private void pasteSchematic(String name, double x, double y ,double z, boolean ignoreAir) {
        File file = new File("plugins/WorldEdit/schematics/" + name + ".schem");
        ClipboardFormat format = ClipboardFormats.findByFile(file);
        Clipboard clipboard = null;
        try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
            clipboard = reader.read();
        }catch (Exception e) {
            e.printStackTrace();
        }
        try (EditSession editSession = WorldEdit.getInstance().getEditSessionFactory().getEditSession(BukkitAdapter.adapt(world), -1)) {
            Operation operation = new ClipboardHolder(clipboard)
                    .createPaste(editSession)
                    .to(BlockVector3.at(x,y,z))
                    .ignoreAirBlocks(ignoreAir)
                    .build();
            Operations.complete(operation);
        }catch (Exception e) {
            e.printStackTrace();
        }
    }
    public Block[] getBlockss(Player p) {
        Block block = p.getLocation().getBlock().getRelative(BlockFace.DOWN);
        double x = p.getLocation().getX();
        double z = p.getLocation().getZ();
        double xOffSet = x - (int)(Math.floor(x));
        double zOffset = z - (int)(Math.floor(z));
        if (xOffSet < 0.3) {
            if (zOffset < 0.3) {
                return new Block[] {block,block.getRelative(BlockFace.NORTH_WEST),block.getRelative(BlockFace.NORTH),block.getRelative(BlockFace.WEST)};
            } else if (zOffset > 0.7) {
                return new Block[] {block,block.getRelative(BlockFace.SOUTH_WEST),block.getRelative(BlockFace.SOUTH),block.getRelative(BlockFace.WEST)};
            } else {
                return new Block[] {block,block.getRelative(BlockFace.WEST)};
            }
        } else if (xOffSet > 0.7) {
            if (zOffset < 0.3) {
                return new Block[] {block,block.getRelative(BlockFace.NORTH_EAST),block.getRelative(BlockFace.NORTH),block.getRelative(BlockFace.EAST)};
            } else if (zOffset > 0.7) {
                return new Block[] {block,block.getRelative(BlockFace.SOUTH_EAST),block.getRelative(BlockFace.SOUTH),block.getRelative(BlockFace.EAST)};
            } else {
                return new Block[] {block,block.getRelative(BlockFace.EAST)};
            }

        } else {
            if (zOffset < 0.3) {
                return new Block[] {block,block.getRelative(BlockFace.NORTH)};
            } else if (zOffset > 0.7) {
                return new Block[] {block,block.getRelative(BlockFace.SOUTH)};
            } else {
                return new Block[] {block};
            }

        }
    }
}
