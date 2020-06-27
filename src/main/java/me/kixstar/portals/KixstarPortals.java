package me.kixstar.portals;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class KixstarPortals extends JavaPlugin {

    private static final Plugin instance = Bukkit.getServer().getPluginManager().getPlugin("KixstarPortals");

    public void onEnable() {}

    public void onDisable() {}

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            if (command.getName().equalsIgnoreCase("testportalfinder")) {
                new TestPortalFinder(new ChunkRegion(((Player) sender).getLocation(), 1), (Player) sender);
                return true;
            }
        }
        return false;
    }

    public static KixstarPortals getInstance() {
        return null == instance ? null : (KixstarPortals) instance;
    }

}
