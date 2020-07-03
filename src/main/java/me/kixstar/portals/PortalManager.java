package me.kixstar.portals;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

import javax.sound.sampled.Port;
import java.util.HashMap;
import java.util.Map;

public class PortalManager implements Listener {

    private static PortalManager instance = new PortalManager();

    public static PortalManager get() {return instance;}

    private PortalManager() {}

    //since long can fit two integers we will use it to store both chunk coordinates to fake a HashMap with two separate keys
    Map<Long, Portal> portalMap = new HashMap<>();

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        //if chunk is new chunk ne portals are in it for sure
        //is it reasonable to limit portals to one per chunk??
        if(!event.isNewChunk()) {
            Chunk chunk = event.getChunk();
            int chunkX = chunk.getX();
            int chunkZ = chunk.getZ();
            Portal.load(chunkX, chunkZ);
        }

    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {

    }

    //loads portal for event listeners, shouldn't get called manually, instead look at Portal.load()
    public void load(Portal portal) {
        /*  We cast x to long then shift it 32 bits to the left
         *  so first 32 bits are occupied by the x coordinate
         *  and then we fill the second 32 bits with z coordinate
         */
        Location portalOrigin = portal.getPortalOrigin();
        int chunkX = portalOrigin.getBlockX() / 16;
        int chunkZ = portalOrigin.getBlockZ() / 16;
        long l = (((long)chunkX) << 32) | (chunkZ & 0xffffffffL);
        this.portalMap.put(l, portal);
    }

    //unload portal for event listeners, shouldn't get called manually, instead look at Portal.unload()
    public void unload(Portal portal) {
        Location portalOrigin = portal.getPortalOrigin();
        int chunkX = portalOrigin.getBlockX() / 16;
        int chunkZ = portalOrigin.getBlockZ() / 16;
        long l = (((long)chunkX) << 32) | (chunkZ & 0xffffffffL);
        this.portalMap.remove(l);
    }

    public Portal get(int x, int z) {
        /*  We cast x to long then shift it 32 bits to the left
         *   so first 32 bits are occupied by the x coordinate
         *   and then we fill the second 32 bits with z coord
         */
        long l = (((long)x) << 32) | (z & 0xffffffffL);
        return this.portalMap.get(l);
    }



}
