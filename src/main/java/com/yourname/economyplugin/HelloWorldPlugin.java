package com.yourname.economyplugin;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class HelloWorldPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        // Log a message to the console when the plugin is enabled
        getLogger().info("HelloWorld plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        // Log a message to the console when the plugin is disabled
        getLogger().info("HelloWorld plugin has been disabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // Check if the command name is "hello"
        if (cmd.getName().equalsIgnoreCase("hello")) {
            // Ensure that the sender is a player
            if (sender instanceof Player) {
                // Cast the sender to a Player object
                Player player = (Player) sender;
                // Construct a message with the player's name
                String message = "Hello " + player.getName() + "!";
                // Send the message to the player in green chat color
                player.sendMessage(ChatColor.GREEN + message);
                // Return true to indicate that the command was handled successfully
                return true;
            } else {
                // Send a message to the player if they are not a player
                sender.sendMessage("This command can only be run by a player.");
                // Return false to indicate that the command was not handled
                return false;
            }
        }
        // If the command name is not "hello", do nothing and return false
        return false;
    }
}
