package com.makotomiyamoto.locksmithy.commands;

import com.makotomiyamoto.locksmithy.Locksmithy;
import com.makotomiyamoto.locksmithy.lock.LocalPlayerData;
import com.makotomiyamoto.locksmithy.lock.RegisteredKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

public class CommandHandler implements CommandExecutor {

    private Locksmithy plugin;

    public CommandHandler(Locksmithy plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String label, String[] args) {

        if (args.length == 0) {
            printHelp(commandSender);
            return true;
        }

        if (commandSender instanceof ConsoleCommandSender) {
            printHelp(commandSender);
            System.out.println("NOTE: Only players can access Locksmithy commands.");
            return true;
        }

        LocalPlayerData playerData = LocalPlayerData.fetchFromFile(plugin, (Player) commandSender);
        if (playerData == null) {
            playerData = new LocalPlayerData((Player) commandSender);
            playerData.save(plugin);
        }

        Player player = (Player) commandSender;
        switch (args[0]) {
            case "break":
                playerData.setBreakLock(!playerData.canBreakLock());
                plugin.sendMessage(player, (playerData.canBreakLock() ? "§aLock breaking toggled on.":"§cLock breaking toggled off."));
                break;
            case "debug":
                if (player.hasPermission("lks.debug")) {
                    playerData.setDebug(!playerData.isDebug());
                    plugin.sendMessage(player, (playerData.isDebug() ? "§aDebug mode toggled on.":"§cDebug mode toggled off."));
                } else {
                    plugin.sendMessage(player, "§cYou do not have permission to toggle debug mode.");
                }
                break;
            case "generate":
                if (args.length >= 2) {
                    if (player.hasPermission("lks.admin")) {
                        switch (args[1]) {
                            case "lockpick":
                                player.getInventory().addItem(RegisteredKey.buildThing(plugin, "options.lockpick"));
                                plugin.sendMessage(player, "§7Given §ex1 §7lockpick.");
                                break;
                            case "blank_key":
                                player.getInventory().addItem(RegisteredKey.buildThing(plugin, "options.blank_key"));
                                plugin.sendMessage(player, "§7Given §ex1 §7blank key.");
                                break;
                            case "advanced_blank_key":
                                player.getInventory().addItem(RegisteredKey.buildThing(plugin, "options.advanced_blank_key"));
                                plugin.sendMessage(player, "§7Given §ex1 §7advanced blank key.");
                                break;
                            default:
                                plugin.sendMessage(player, String.format("§7%1s is not an existing item.", args[1]));
                                plugin.sendMessage(player, "§7Usage: §6/lks generate (§7lockpick|blank_key|blank_advanced_key§6)");
                                break;
                        }
                    } else {
                        plugin.sendMessage(player, "§cYou do not have permission to spawn items.");
                    }
                } else {
                    plugin.sendMessage(player, "§7Usage: §6/lks generate (§7lockpick|blank_key|blank_advanced_key§6)");
                }
                break;
            default:
                printHelp(commandSender);
                break;
        }
        playerData.save(plugin);

        return true;

    }

    private void printHelp(CommandSender sender) {

        sender.sendMessage("");
        sender.sendMessage("§6§lLocksmithy v1.0");
        sender.sendMessage("§6/lks help §f- Command information :)");
        sender.sendMessage("§6/lks break §f- Toggles lock breaking for player locks.");
        if (sender.hasPermission("lks.debug"))
            sender.sendMessage("§6/lks debug §f- Toggles debug mode (right-click on blocks with a stick).");
        if (sender.hasPermission("lks.admin"))
            sender.sendMessage("§6/lks generate (§7lockpick|blank_key|blank_advanced_key§6) §f- Generates defined items.");
        sender.sendMessage("");

    }

}
