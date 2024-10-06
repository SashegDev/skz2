package net.sashegdev.skz2;

import net.sashegdev.skz2.tests.compass;
import net.sashegdev.skz2.tests.phone;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class malus extends JavaPlugin implements Listener {

    private BossBar bossBar;
    private Set<Player> affectedPlayers = new HashSet<>();
    private double rotationAngle = 5;
    private String targetWorldName;
    private int effectDuration;
    private List<String> whitelistPlayers;
    private DimensionManager dimensionManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfig();
        dimensionManager = new DimensionManager();

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(dimensionManager, this);
        this.getCommand("skzReload").setExecutor(this);

        new phone(this);
        new compass(this).onEnable();
    }

    @Override
    public void onDisable() {
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar.setVisible(false);
        }
        affectedPlayers.clear();
        getLogger().info("MalusEffect plugin disabled!");
    }

    private void loadConfig() {
        FileConfiguration config = getConfig();
        targetWorldName = config.getString("world_name", "world_nether");
        effectDuration = config.getInt("effect_duration", 10);
        whitelistPlayers = config.getStringList("whitelist_players");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("skzReload")) {
            if (!sender.isOp()) {
                sender.sendMessage("You do not have permission to use this command.");
                return true;
            }

            reloadConfig();
            loadConfig();
            sender.sendMessage("Configuration reloaded.");
            return true;
        }
        return false;
    }

    @EventHandler
    public void onPlayerChat(PlayerChatEvent event) {
        Player player1 = event.getPlayer();
        String message = event.getMessage();

        if (message.equalsIgnoreCase("Malus")) {
            event.setCancelled(true);
            if (player1.getGameMode() == GameMode.SPECTATOR) {
                teleportPlayerToTargetWorld(player1);
            } else if (whitelistPlayers.contains(player1.getName())) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.sendMessage("<" + player1.getName() + "> " + ChatColor.GOLD + "Malus.");
                }
                applyEffect(player1);
            } else {
                player1.sendMessage("[" + player1.getName() + "] Хмм, кажется не сработало.");
                player1.getWorld().spawnParticle(Particle.DUST, player1.getLocation().add(0, 1, 0), 40, 0.2, 0.5, 0.2, 1.5, new Particle.DustOptions(Color.RED, 2F));
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (affectedPlayers.contains(player)) {
            Location from = event.getFrom();
            Location to = event.getTo();

            if (from.getX() != to.getX() || from.getZ() != to.getZ() || from.getY() != to.getY()) {
                event.setTo(new Location(from.getWorld(), from.getX(), from.getY(), from.getZ(), to.getYaw(), to.getPitch()));
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (affectedPlayers.contains(player)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player player = (Player) event.getDamager();
            if (affectedPlayers.contains(player)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (affectedPlayers.contains(player)) {
            event.setCancelled(true);
        }
    }

    private void applyEffect(Player player) {
        World world = player.getWorld();
        Location location = player.getLocation();
        double radius = 1.2;

        bossBar = Bukkit.createBossBar("Timer", BarColor.RED, BarStyle.SOLID);
        bossBar.addPlayer(player);
        bossBar.setVisible(true);

        affectedPlayers.add(player);

        new BukkitRunnable() {
            int counter = effectDuration;
            final int globalCount = counter;

            @Override
            public void run() {
                if (counter > 0) {
                    bossBar.setTitle(" " + counter + "сек. осталось");
                    bossBar.setProgress((double) counter / globalCount);

                    for (double angle = 0; angle <= 2 * Math.PI; angle += 0.1) {
                        double x = location.getX() + radius * Math.cos(angle);
                        double z = location.getZ() + radius * Math.sin(angle);
                        Location circleLoc = new Location(world, x, location.getY() + 1, z);
                        world.spawnParticle(Particle.DUST, circleLoc, 1, 0, 0, 0, 0, new Particle.DustOptions(Color.RED, 1));
                    }

                    for (double angle = 0; angle <= 2 * Math.PI; angle += 0.1) {
                        double x = location.getX() + radius * Math.cos(angle);
                        double y = location.getY() + radius * Math.sin(angle);
                        Location circleLoc = new Location(world, x, y + 1, location.getZ());
                        world.spawnParticle(Particle.DUST, circleLoc, 1, 0, 0, 0, 0, new Particle.DustOptions(Color.RED, 1));
                    }

                    for (double angle = 0; angle <= 2 * Math.PI; angle += 0.1) {
                        for (double aboba = 0; aboba <= 3; aboba += 0.2) {
                            double x = location.getX() + (radius + 1) * Math.sin(angle + rotationAngle);
                            double z = location.getZ() + (radius + 1) * Math.cos(angle + rotationAngle);
                            Location circleLoc = new Location(world, x, location.getY() + aboba, z);
                            world.spawnParticle(Particle.DUST, circleLoc, 1, 0, 0, 0, 0, new Particle.DustOptions(Color.ORANGE, 0.5F));
                        }
                    }

                    counter--;
                } else {
                    this.cancel();
                    bossBar.removeAll();
                    bossBar.setVisible(false);
                    affectedPlayers.remove(player);
                    teleportPlayerToTargetWorld(player);
                }
            }
        }.runTaskTimer(this, 0, 20);
    }

    private void teleportPlayerToTargetWorld(Player player) {
        World targetWorld = Bukkit.getWorld(targetWorldName);
        if (player.getWorld() == targetWorld) {
            Location targetLocation = Bukkit.getWorld("world").getSpawnLocation();
            player.teleport(targetLocation);
        } else if (targetWorld != null) {
            Location targetLocation = targetWorld.getSpawnLocation();
            player.teleport(targetLocation);
        } else {
            player.sendMessage("[" + player.getName() + "] Хмм, кажется не сработало.");
        }
    }
}
