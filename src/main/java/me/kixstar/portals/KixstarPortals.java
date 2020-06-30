package me.kixstar.portals;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class KixstarPortals extends JavaPlugin {

    private static final Plugin instance = Bukkit.getServer().getPluginManager().getPlugin("KixstarPortals");

    public void onEnable() {}

    public void onDisable() {}

    Location location;

    PortalSpawner spawner;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {

            if (command.getName().equalsIgnoreCase("testportalfinder")) {
                this.location = new TestPortalFinder(new ChunkRegion(((Player) sender).getLocation(), 1), (Player) sender).findPortalLocation();
                this.spawner = new PortalSpawner(this.location, "test_portal", "hello1");
                ((Player) sender).teleport(location.clone().add(0.5,1, 0.5));
                return true;
            }

            if(command.getName().equalsIgnoreCase("testsaveoldblocks")) {
                this.spawner.saveBlocks();
            }

            if(command.getName().equalsIgnoreCase("testloadoldblocks")) {
                this.spawner.resetBlocks();
            }

            if(command.getName().equalsIgnoreCase("testspawnportal")) {
                this.spawner.placePortal();
            }

        }
        return false;
    }

    public static KixstarPortals getInstance() {
        return null == instance ? null : (KixstarPortals) instance;
    }

}
