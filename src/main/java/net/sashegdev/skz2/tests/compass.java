package net.sashegdev.skz2.tests;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Random;

public final class compass {
    private int distance;
    private String customVoidWorldName;

    private final JavaPlugin plugin;

    public compass(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void onEnable() {
        plugin.saveDefaultConfig();
        FileConfiguration config = plugin.getConfig();
        distance = config.getInt("distance");
        customVoidWorldName = config.getString("custom_void_world_name");

        Bukkit.getScheduler().runTaskTimer(plugin, this::compasss, 0L, 1L);
        Bukkit.getScheduler().runTaskTimer(plugin, this::world, 0L, 1L);
    }

    public void compasss() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getInventory().getItemInMainHand().getType().equals(Material.COMPASS)
                    || player.getInventory().getItemInOffHand().getType().equals(Material.COMPASS)) {

                double x, y, z;
                if (player.getWorld().getName().equals(customVoidWorldName)) {
                    Random rand = new Random();
                    x = rand.nextInt(6501);
                    y = rand.nextInt(193) - 64;
                    z = rand.nextInt(6501);
                } else {
                    x = player.getLocation().getX();
                    y = player.getLocation().getY();
                    z = player.getLocation().getZ();
                }

                String formattedX = String.format("%.2f", x);
                String formattedY = String.format("%.2f", y);
                String formattedZ = String.format("%.2f", z);
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(formattedX + " | " + formattedY + " | " + formattedZ));
            }
        }
    }

    public void world() {
        for (Player player : Bukkit.getOnlinePlayers()) {

            Location centr = player.getWorld().getSpawnLocation();
            double distanceX = Math.abs(centr.getX() - player.getLocation().getX());
            double distanceZ = Math.abs(centr.getZ() - player.getLocation().getZ());

            if (distanceX >= distance || distanceZ >= distance) {
                new BukkitRunnable() {
                    int effectCounter = 0;

                    public void run() {
                        if (player.isOnline() && (Math.abs(centr.getX() - player.getLocation().getX()) >= distance
                                || Math.abs(centr.getZ() - player.getLocation().getZ()) >= distance)) {
                            spawnParticles(player);
                            effectCounter++;
                            player.damage(0.2 * effectCounter);
                        } else {
                            this.cancel();
                        }
                    }
                }.runTaskTimer(plugin, 0, 20); // 20 тиков = 1 секунда
            }
        }
    }

    private void spawnParticles(Player player) {
        Location loc = player.getLocation();
        World world = player.getWorld();
        for (int i = 0; i < 50; i++) {
            double offsetX = (Math.random() - 0.5) * 14;
            double offsetY = (Math.random() - 0.5) * 14;
            double offsetZ = (Math.random() - 0.5) * 14;
            world.spawnParticle(Particle.DUST, loc.add(new Vector(offsetX, offsetY, offsetZ)), 1, new Particle.DustOptions(Color.GRAY, 0.5f));
            loc.subtract(new Vector(offsetX, offsetY, offsetZ)); // Возвращаемся к исходной позиции
        }
    }
}
