package me.mango.alldim;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class AllDimensionsPlugin extends JavaPlugin implements Listener, TabCompleter {

    private static final List<String> DEFAULT_DIMENSIONS =
            List.of("minecraft:overworld", "minecraft:the_nether", "minecraft:the_end");

    private boolean enabled;
    private boolean busy;
    private List<String> dimensions = DEFAULT_DIMENSIONS;
    private List<String> excluded = List.of();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadSettings();
        getServer().getPluginManager().registerEvents(this, this);
        if (getCommand("alldim") != null) {
            getCommand("alldim").setExecutor(this);
            getCommand("alldim").setTabCompleter(this);
        }
    }

    private void loadSettings() {
        reloadConfig();
        enabled = getConfig().getBoolean("enabled", false);
        List<String> configured = getConfig().getStringList("dimensions");
        dimensions = configured.isEmpty() ? DEFAULT_DIMENSIONS : configured;
        excluded = getConfig().getStringList("excluded-commands");
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onServerCommand(ServerCommandEvent event) {
        if (runEverywhere(event.getSender(), event.getCommand(), false)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();
        if (message.startsWith("/")) {
            message = message.substring(1);
        }
        if (runEverywhere(event.getPlayer(), message, true)) {
            event.setCancelled(true);
        }
    }

    private boolean runEverywhere(CommandSender sender, String rawCommand, boolean player) {
        if (!enabled || busy) {
            return false;
        }
        if (player && !sender.hasPermission("alldim.use")) {
            return false;
        }

        String command = rawCommand.trim();
        while (command.startsWith("/")) {
            command = command.substring(1);
        }
        if (command.isEmpty()) {
            return false;
        }

        String root = command.split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
        String bareRoot = root.contains(":") ? root.substring(root.indexOf(':') + 1) : root;
        if (bareRoot.equals("alldim") || excluded.contains(root) || excluded.contains(bareRoot)) {
            return false;
        }

        busy = true;
        try {
            for (String dimension : dimensions) {
                Bukkit.dispatchCommand(sender, "execute in " + dimension + " run " + command);
            }
        } finally {
            busy = false;
        }
        return true;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("alldim")) {
            return false;
        }
        if (args.length == 0) {
            sendUsage(sender, label);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "on" -> {
                persistEnabled(true);
                sender.sendMessage(Component.text("All-dimension command mode enabled.", NamedTextColor.GREEN));
            }
            case "off" -> {
                persistEnabled(false);
                sender.sendMessage(Component.text("All-dimension command mode disabled.", NamedTextColor.RED));
            }
            case "status" -> sender.sendMessage(Component.text(
                    "All-dimension command mode is " + (enabled ? "enabled" : "disabled") + ".",
                    enabled ? NamedTextColor.GREEN : NamedTextColor.RED));
            case "reload" -> {
                loadSettings();
                sender.sendMessage(Component.text("Configuration reloaded.", NamedTextColor.YELLOW));
            }
            default -> sendUsage(sender, label);
        }
        return true;
    }

    private void persistEnabled(boolean value) {
        enabled = value;
        getConfig().set("enabled", value);
        saveConfig();
    }

    private void sendUsage(CommandSender sender, String label) {
        sender.sendMessage(Component.text("/" + label + " <on|off|status|reload>", NamedTextColor.YELLOW));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0], List.of("on", "off", "status", "reload"), new ArrayList<>());
        }
        return List.of();
    }
}
