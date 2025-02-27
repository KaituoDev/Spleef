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
import fun.kaituo.gameutils.Game;
import fun.kaituo.gameutils.event.PlayerChangeGameEvent;
import fun.kaituo.gameutils.event.PlayerEndGameEvent;
import fun.kaituo.gameutils.utils.ItemStackBuilder;
import io.papermc.paper.event.player.PlayerPickItemEvent;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MainHand;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.awt.desktop.UserSessionEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class SpleefGame extends Game implements Listener {
    private static final SpleefGame instance = new SpleefGame((Spleef) Bukkit.getPluginManager().getPlugin("Spleef"));
    List<Player> playersAlive;
    ItemStack shovel;
    ItemStack snowball;
    ItemStack feather;
    ItemStack kb_stick;
    ItemStack air;
    int countDownSeconds = 10;
    FileConfiguration c;

    private final BoundingBox gameBoundingBox = new BoundingBox(700, -64, -300, 1300, 320, 300);

    private SpleefGame(Spleef plugin) {
        c = plugin.getConfig();
        players = Spleef.players;
        playersAlive = new ArrayList<>();
        shovel = new ItemStackBuilder(Material.STONE_SHOVEL).setUnbreakable(true).setDisplayName("§e全村最好的铲子").build();
        snowball = new ItemStackBuilder(Material.SNOWBALL).setDisplayName("§b雪球炸弹").build();
        feather = new ItemStackBuilder(Material.FEATHER).setDisplayName("§e§l飞升").setLore("§b受任于坠落之际，奉命于危难之间").build();
        kb_stick = new ItemStackBuilder(Material.DEBUG_STICK).setDisplayName("§c§l击退棒").setLore("§eReady to lift off!").addEnchantment(Enchantment.KNOCKBACK, 1).build();
        air = new ItemStackBuilder(Material.AIR).build();
        initializeGame(plugin, "Spleef", "§e掘一死战", new Location(world, 1000, 6, 0));
        initializeButtons(new Location(world, 1000, 7, 4), BlockFace.NORTH, new Location(world, 996, 7, 0),
                BlockFace.EAST);
        initializeGameRunnable();
    }

    public static SpleefGame getInstance() {
        return instance;
    }
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent pie) {

        //判定挖掘
        if (pie.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
            if (playersAlive.contains(pie.getPlayer())) {
                if (!pie.getClickedBlock().getType().equals(Material.BARRIER)) {
                    if (pie.getClickedBlock().getLocation().getY() < 30) {
                        return;
                    }
                    if (pie.getPlayer().getInventory().getItemInMainHand().getType().equals(Material.STONE_SHOVEL)) {
                        pie.getClickedBlock().breakNaturally(true, false);
                    }
                }
            }
        }

        //判定技能
        if (!pie.getAction().equals(Action.RIGHT_CLICK_AIR) && !pie.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            return;
        }// 不是使用技能
        if (pie.getItem() == null) {
            return;
        }//没有物品
        Player executor = pie.getPlayer();
        if (!playersAlive.contains(executor)) {
            return;
        }//不在玩家列表中
        if (pie.getPlayer().getInventory().getItemInMainHand().equals(feather)) {
            int x = (int) pie.getPlayer().getLocation().getX();
            int y = (int) pie.getPlayer().getLocation().getY();
            int z = (int) pie.getPlayer().getLocation().getZ();
            for (int i = x - 1; i <= x + 1; i += 1) {
                for (int j = y + 1; j <= 225; j += 1) {
                    for (int k = z - 1; k <= z + 1; k += 1) {
                        world.getBlockAt(i, j, k).setType(Material.AIR);
                    }
                }
            }
            pie.getPlayer().spigot().sendMessage(ChatMessageType.CHAT, new TextComponent("§e§l飞起来！(喜)"));
            executor.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 20, 49));
            executor.getInventory().setItem(1,air);
        }
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent ple) {
        if (ple.getEntity().getShooter() != null) {
            if (playersAlive.contains(ple.getEntity().getShooter())) {
                ple.getEntity().addScoreboardTag("spleef");
            }
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent phe) {
        float ExplosionPowerRatio = 1;
        if (!isNormalMode()) {
            ExplosionPowerRatio = (float) c.getDouble("explosion-power-ratio");
        }
        if (phe.getEntity().getScoreboardTags().contains("spleef")) {
            if (gameBoundingBox.contains(phe.getEntity().getLocation().toVector())) {
                if (phe.getEntity().getLocation().getY() > 30) {
                    int x = phe.getEntity().getLocation().getBlockX();
                    int y = phe.getEntity().getLocation().getBlockY();
                    int z = phe.getEntity().getLocation().getBlockZ();
                    for (int i = x - 1; i <= x + 1; i += 1) {
                        for (int j = y - 2; j <= y + 2; j += 1) {
                            for (int k = z - 1; k <= z + 1; k += 1) {
                                world.getBlockAt(i, j, k).setType(Material.AIR);
                            }
                        }
                    }
                    world.createExplosion(phe.getEntity().getLocation(), ExplosionPowerRatio * ((float) c.getDouble("snowball-explosion-power")), false, true);
                }
            }
        }
    }

    @EventHandler
    public void preventPlayerDroppingItem(PlayerDropItemEvent pdie) {
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
    public void onBlockExplode(BlockExplodeEvent bee) {
        if (gameBoundingBox.contains(bee.getBlock().getLocation().toVector())) {
            bee.setYield(0);
        }
    }
    @EventHandler
    public void preventPickUp(PlayerPickItemEvent e) {
        if (players.contains(e.getPlayer())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void preventDamage(EntityDamageEvent ede) {
        if (ede.getEntity() instanceof Player) {
            if (playersAlive.contains(ede.getEntity()) && !isNormalMode()) {
                if (!ede.getCause().equals(EntityDamageEvent.DamageCause.ENTITY_ATTACK)) {
                    ede.setCancelled(true);
                }
            }
        }
    }

    private void initializeGameRunnable() {
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
                short SnowballGivingNum;
                float ExplosionPowerRatio;
                boolean GlobalGameMode = isNormalMode(); //true = 普通模式，false = 无限火力
                String SchematicName;
                if (GlobalGameMode) {
                    SchematicName = "spleef1";
                    SnowballGivingNum = 1;
                    ExplosionPowerRatio = 1;
                }
                else {
                    SchematicName = "spleef_3floor";
                    SnowballGivingNum = 64;
                    ExplosionPowerRatio = 4;
                }
                startCountdown(countDownSeconds);
                pasteSchematic(SchematicName, 1000, 200, 0, true);
                Bukkit.getPluginManager().registerEvents(this, plugin);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    placeSpectateButton();
                    removeStartButton();
                    for (Player p : players) {
                        p.teleport(new Location(world, 1000, 200, 0));
                        p.getInventory().clear();
                        p.setPlayerWeather(WeatherType.DOWNFALL);
                    }
                });
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    for (Player p : players) {
                        if (GlobalGameMode) { // true = 常规模式 false = 无限火力模式
                            p.getInventory().setItem(0, shovel);
                            p.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 65536, 49, false, false));
                        }
                        else {
                            p.getInventory().setItem(0, kb_stick);
                            p.getInventory().setItem(1, feather);
                            p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 65536, 9, false, false));
                        }
                        p.setGameMode(GameMode.SURVIVAL);
                        //Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "item replace entity " + p.getName() + " hotbar.0 with iron_pickaxe{Unbreakable:1,HideFlags:13,CanDestroy:[white_concrete,blue_ice,brown_concrete,green_concrete,light_blue_stained_glass_pane,sea_lantern,cobblestone_slab,polished_andesite,stone_brick_wall,stone_brick_stairs,stone_brick_slab]} 1");
                        //Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "item replace entity " + p.getName() + " hotbar.0 with iron_pickaxe{Unbreakable:1} 1");
                        p.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 65536, 0, false, false));
                    }
                }, countDownSeconds * 20L);

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

                }, countDownSeconds * 20L, 1));
                taskIds.add(Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
                    for (Player p : playersAlive) {
                        for (int i = 0; i < SnowballGivingNum; i += 1) {
                            p.getInventory().addItem(snowball);
                        }
                    }
                }, countDownSeconds * 20L + 200, c.getInt("snowball-give-interval")));
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
                                pasteSchematic("spleefempty", 1000, 200, 0, false);
                                p.resetPlayerWeather();
                            }, 100);
                        }
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            removeSpectateButton();
                            placeStartButton();
                            HandlerList.unregisterAll(this);
                        }, 100);
                        players.clear();
                        playersAlive.clear();
                        cancelGameTasks();
                    }
                }, 100, 1));
            }
        };
    }

    public long getTime(World world) {
        return (world.getGameTime());
    }

    @Override
    protected void quit(Player p) throws IOException {
        players.remove(p);
        playersAlive.remove(p);
    }

    @Override
    protected boolean rejoin(Player player) {
        return false;
    }

    @Override
    protected boolean join(Player player) {
        player.setBedSpawnLocation(hubLocation, true);
        player.teleport(hubLocation);
        return true;
    }

    @Override
    protected void forceStop() {
        if (playersAlive.isEmpty()) {
            return;
        }
        List<Player> playersCopy = new ArrayList<>(players);
        for (Player p : playersCopy) {
            p.sendTitle("§c游戏被强制停止", null, 5, 50, 5);
            p.getInventory().clear();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                p.teleport(new Location(world, 1000.5, 6.0625, 0.5));
                Bukkit.getPluginManager().callEvent(new PlayerEndGameEvent(p, this));
                pasteSchematic("spleefempty", 1000, 100, 0, false);
                p.resetPlayerWeather();
            }, 100);
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            removeSpectateButton();
            placeStartButton();
            HandlerList.unregisterAll(this);
        }, 100);
        players.clear();
        playersAlive.clear();
        cancelGameTasks();
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

    private boolean isNormalMode() { //用于分别普通模式和无限火力模式
        if (world.getBlockAt(1003, 7, 5).isBlockPowered()) {
            return false;
        } else {
            return true;
        }
    }
}
