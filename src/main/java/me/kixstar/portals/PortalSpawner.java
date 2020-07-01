package me.kixstar.portals;

import com.boydti.fawe.FaweAPI;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.*;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.World;
import me.kixstar.portals.database.KixtarDB;
import org.bukkit.Location;

public class PortalSpawner {

    Clipboard portal;

    String oldBlocksUUID;

    BlockVector3 spawnPos;

    World world;



    public PortalSpawner(Location spawnPos, String portalDBHandle, String oldBlocksUUID) {


        this.world = new BukkitWorld(spawnPos.getWorld());

        this.oldBlocksUUID = oldBlocksUUID;

        this.spawnPos = BlockVector3.at(
                spawnPos.getX(),
                spawnPos.getY(),
                spawnPos.getZ()
        );

        this.portal = KixtarDB.getPortalSchematic(portalDBHandle);

    }

    public void saveBlocks() {

        /* it seems that when you add the dimensions of a portal to the start point
         * that the region is one block wider in every axis.
         */
        BlockVector3 portalSize = this.portal.getDimensions().add(-1,-1,-1);
        BlockVector3 end = this.spawnPos.add(portalSize);

        CuboidRegion region = new CuboidRegion(this.world, this.spawnPos, end);
        BlockArrayClipboard clipboard = new BlockArrayClipboard(region);

        try (EditSession editSession = FaweAPI.getEditSessionBuilder(this.world).build()) {
            ForwardExtentCopy forwardExtentCopy = new ForwardExtentCopy(
                    editSession, region, clipboard, region.getMinimumPoint()
            );
            // configure here
            Operations.complete(forwardExtentCopy);
        }

        KixtarDB.writeOldBlocks(this.oldBlocksUUID, clipboard);


    }


    //todo: test if this works
    public void placePortal() {

        //reset the origin to align the schematic to this.spawnPos
        this.portal.setOrigin(this.portal.getMinimumPoint());

        this.pasteClipboard(this.portal);

    }

    public void resetBlocks() {
        this.pasteClipboard(KixtarDB.getOldBlocks(this.oldBlocksUUID));
    }

    private void pasteClipboard(Clipboard clipboard) {

        try (EditSession editSession = FaweAPI.getEditSessionBuilder(this.world).build()) {
            Operation operation = new ClipboardHolder(clipboard)
                    .createPaste(editSession)
                    .to(this.spawnPos)

                    .build();
            Operations.complete(operation);
        }

    }

}
