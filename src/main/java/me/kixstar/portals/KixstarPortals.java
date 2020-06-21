package me.kixstar.portals;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class KixstarPortals extends JavaPlugin {

    private static final Plugin instance = Bukkit.getServer().getPluginManager().getPlugin("KixstarPortals");

    public void onEnable() {}

    public void onDisable() {}

    public static KixstarPortals getInstance() {
        return null == instance ? null : (KixstarPortals) instance;
    }

}
