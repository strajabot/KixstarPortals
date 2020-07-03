package me.kixstar.portals.database;


import com.sk89q.worldedit.extent.clipboard.Clipboard;
import me.kixstar.portals.KixstarPortals;
import me.kixstar.portals.Portal;
import me.kixstar.portals.database.driver.DevDatabaseDriver;
import me.kixstar.portals.database.driver.ProdDatabaseDriver;

public class KixstarDB {

    private static DatabaseDriver driver = null;

    private static DatabaseDriver getDriver() {

        if(driver != null) return driver;

        String env = KixstarPortals.getInstance().getConfig().getString("environment");

        if(env.equals("production")) driver = new ProdDatabaseDriver();
        driver = new DevDatabaseDriver();

        return driver;
    }

    public static Clipboard readPortalSchematic(String handle) {
        return getDriver().readPortalSchematic(handle);
    }

    public static boolean writeOldBlocks(String uuid, Clipboard oldBlocks) {
        return getDriver().writeOldBlocks(uuid, oldBlocks);
    }

    public static Clipboard readOldBlocks(String uuid) {
        return getDriver().readOldBlocks(uuid);
    }

    public static Portal readPortal(int x, int z) {
        return getDriver().readPortal(x,z);
    }

    public static boolean writePortal(Portal portal) {
        return getDriver().writePortal(portal);
    }

    public static boolean deletePortal(Portal portal) {
        return getDriver().deletePortal(portal);
    }

}
