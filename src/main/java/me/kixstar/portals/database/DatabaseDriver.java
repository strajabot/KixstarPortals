package me.kixstar.portals.database;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import me.kixstar.portals.Portal;

public interface DatabaseDriver {

    Clipboard readPortalSchematic(String handle);

    Clipboard readOldBlocks(String uuid);

    boolean writeOldBlocks(String uuid, Clipboard oldBlocks);

    boolean deleteOldBlocks(String uuid);

    Portal readPortal(int x, int z);

    boolean writePortal(Portal portal);

    boolean deletePortal(Portal portal);

}
