package net.sashegdev.skz2.tests;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.List;
import java.util.Map;

public class phone implements Listener, CommandExecutor {

    private final JavaPlugin plugin;
    private FileConfiguration phoneConfig;
    private List<Map<?, ?>> messages;
    private Location phoneLocation;
    private Location additionalBlockLocation;
    private Material phoneBlock;
    private Material phoneButton;
    private Material additionalBlock;
    private boolean isCalling;
    private boolean isAccepted;

    public phone(JavaPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
        plugin.getCommand("phone").setExecutor(this);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private void loadConfig() {
        File configFile = new File(plugin.getDataFolder(), "phonecfg.yml");
        if (!configFile.exists()) {
            plugin.saveResource("phonecfg.yml", false);
        }
        phoneConfig = YamlConfiguration.loadConfiguration(configFile);
        messages = phoneConfig.getMapList("messages");

        String worldName = phoneConfig.getString("phone_location.world");
        World world = Bukkit.getWorld(worldName);
        double x = phoneConfig.getDouble("phone_location.x");
        double y = phoneConfig.getDouble("phone_location.y");
        double z = phoneConfig.getDouble("phone_location.z");

        phoneLocation = new Location(world, x, y, z);
        phoneBlock = Material.getMaterial(phoneConfig.getString("phone_location.block"));
        phoneButton = Material.getMaterial(phoneConfig.getString("phone_location.button"));

        // Получение мира и координат дополнительного блока из конфига
        String additionalWorldName = phoneConfig.getString("additional_block_location.world");
        World additionalWorld = Bukkit.getWorld(additionalWorldName);
        double additionalX = phoneConfig.getDouble("additional_block_location.x");
        double additionalY = phoneConfig.getDouble("additional_block_location.y");
        double additionalZ = phoneConfig.getDouble("additional_block_location.z");
        additionalBlockLocation = new Location(additionalWorld, additionalX, additionalY, additionalZ);
        additionalBlock = Material.getMaterial(phoneConfig.getString("additional_block_location.block"));
    }

    private boolean checkPhoneLocation() {
        Block block = phoneLocation.getBlock();
        Block aboveBlock = block.getRelative(BlockFace.UP);
        Block additionalBlock = additionalBlockLocation.getBlock();

        return block.getType() == phoneBlock &&
                aboveBlock.getType() == phoneButton &&
                additionalBlock.getType() == this.additionalBlock;
    }

    private void startPhoneCall() {
        if (!checkPhoneLocation()) {
            sendMessageToNearbyPlayers("[" + ChatColor.GOLD + "Телефон" + ChatColor.RESET + "] Не удалось совершить звонок. Проверьте расположение блоков.");
            return;
        }

        isCalling = true;
        isAccepted = false;

        sendMessageToNearbyPlayers("[" + ChatColor.GOLD + "Телефон" + ChatColor.RESET + "] звонок...");

        // Периодическое сообщение "звонок" каждые 2 секунды
        new BukkitRunnable() {
            int timesRun = 0;

            @Override
            public void run() {
                if (!isCalling || isAccepted || timesRun >= 5) {
                    this.cancel();
                    return;
                }
                sendMessageToNearbyPlayers("[" + ChatColor.GOLD + "Телефон" + ChatColor.RESET + "] *звонок*");
                timesRun++;
            }
        }.runTaskTimer(plugin, 0, 40); // 2 секунды = 40 тиков

        new BukkitRunnable() {
            @Override
            public void run() {
                if (isCalling && !isAccepted) {
                    isCalling = false;
                    sendMessageToNearbyPlayers("[" + ChatColor.GOLD + "Телефон" + ChatColor.RESET + "] Звонок отклонен.");
                }
            }
        }.runTaskLater(plugin, 200); // 10 секунд = 200 тиков
    }

    private void startPhoneMessages() {
        new BukkitRunnable() {
            int index = 0;

            private void sendNextMessage() {
                if (index < messages.size()) {
                    Map<?, ?> messageData = messages.get(index);
                    String text = (String) messageData.get("text");
                    double delay = (double) messageData.get("delay");

                    sendMessageToNearbyPlayers("[" + ChatColor.GOLD + "Телефон" + ChatColor.RESET + "] " + formatColors(text));
                    index++;

                    if (index < messages.size()) {
                        long ticks = (long) (delay * 20);

                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                sendNextMessage();
                            }
                        }.runTaskLater(plugin, ticks);
                    } else {
                        // Если достигнуто последнее сообщение, выполнить завершающие действия
                        cancel();
                    }
                }
            }

            @Override
            public void run() {
                sendNextMessage();
            }
        }.runTask(plugin);
    }

    private void sendMessageToNearbyPlayers(String message) {
        for (Player player : phoneLocation.getWorld().getPlayers()) {
            if (player.getLocation().distance(phoneLocation) <= 15) {
                player.sendMessage(message);
            }
        }
    }

    private String formatColors(String text) {
        return text.replaceAll("<red>", ChatColor.RED.toString())
                .replaceAll("</red>", ChatColor.RESET.toString())
                .replaceAll("<blue>", ChatColor.BLUE.toString())
                .replaceAll("</blue>", ChatColor.RESET.toString())
                .replaceAll("<gold>", ChatColor.GOLD.toString())
                .replaceAll("</gold>", ChatColor.RESET.toString())
                .replaceAll("<green>", ChatColor.GREEN.toString())
                .replaceAll("</green>", ChatColor.RESET.toString());
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            Block clickedBlock = event.getClickedBlock();
            if (clickedBlock != null && clickedBlock.getLocation().equals(phoneLocation.clone().add(0, 1, 0))) {
                if (isCalling && !isAccepted) {
                    isAccepted = true;
                    isCalling = false;
                    startPhoneMessages(); // Запуск кассеты после принятия звонка
                }
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("phone")) {
            if (!(sender instanceof Player) || ((Player) sender).isOp()) {
                startPhoneCall();
                sender.sendMessage(ChatColor.GREEN + "Звонок запущен.");
                return true;
            } else {
                sender.sendMessage(ChatColor.RED + "Эта команда доступна только операторам.");
            }
        } else if (command.getName().equalsIgnoreCase("skzreload")) {
            if (!(sender instanceof Player) || ((Player) sender).isOp()) {
                plugin.reloadConfig();
                loadConfig();
                sender.sendMessage(ChatColor.GREEN + "Конфигурация перезагружена.");
                return true;
            } else {
                sender.sendMessage(ChatColor.RED + "Эта команда доступна только операторам.");
            }
        }
        return false;
    }
}
