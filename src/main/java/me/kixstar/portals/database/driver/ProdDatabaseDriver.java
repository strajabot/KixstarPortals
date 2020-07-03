package me.kixstar.portals.database.driver;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import me.kixstar.portals.Portal;
import me.kixstar.portals.KixstarPortals;
import me.kixstar.portals.database.DatabaseDriver;

import java.io.File;

/*
*   A database driver for the development environment
*   Uses the filesystem for persistent storage
*   All of the persistent data is kept in ./plugins/KixstarPortals/persistent
*
*
*
* */
public class ProdDatabaseDriver implements DatabaseDriver {

    private File root = KixstarPortals.getInstance().getDataFolder();


    @Override
    public Clipboard readPortalSchematic(String handle) {
        return null;
    }

    @Override
    public Clipboard readOldBlocks(String uuid) {
        return null;
    }

    @Override
    public boolean writeOldBlocks(String uuid, Clipboard oldBlocks) {
        return false;
    }

    @Override
    public boolean deleteOldBlocks(String uuid) {
        return false;
    }

    @Override
    public Portal readPortal(int x, int z) {
        return null;
    }

    @Override
    public boolean writePortal(Portal portal) {
        return false;
    }

    @Override
    public boolean deletePortal(Portal portal) {
        return false;
    }
}
