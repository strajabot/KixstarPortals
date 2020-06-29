package me.kixstar.portals;

import io.papermc.lib.PaperLib;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;

public class ChunkRegion {

    private World world;

    private int startX, endX;

    private int startZ, endZ;

    private int startChunkX, endChunkX;

    private int startChunkZ, endChunkZ;

    private int widthBlocks, depthBlocks;

    private int widthChunks, depthChunks;

    private int areaChunks;

    private int areaBlocks;

    private int volBlocks;

    private Chunk[] loadedChunks;

    private CompletableFuture<Chunk>[] chunkLoadFutures;

    public ChunkRegion(@Nonnull World world, int startX, int endX, int startZ, int endZ) {
        if(world == null) throw new RuntimeException("\"world\" can't be null");

        this.world = world;

        //start values should be lower than end values so for loops don't get messed up
        if(startX < endX) {
            this.startX = startX;
            this.endX = endX;
        } else {
            this.startX = endX;
            this.endX = startX;
        }

        if(startZ < endZ) {
            this.startZ = startZ;
            this.endZ = endZ;
        } else {
            this.startZ = endZ;
            this.endZ = startZ;
        }

        this.setup();

    }

    public ChunkRegion(@Nonnull Location loc, @Nonnegative int radius) {

        if(loc == null) throw new RuntimeException("\"loc\" can't be null");
        if(loc.getWorld() == null) throw new RuntimeException("\"loc.getWorld()\" returned null");

        if(radius < 0) radius = -radius;

        this.world = loc.getWorld();

        int locX = loc.getBlockX();
        int locZ = loc.getBlockZ();

        //it may be faster to do a right bitwise shift by 4 but I'm not sure
        this.startX = (locX - radius) / 16;
        this.endX = (locX + radius) / 16;

        this.startZ = (locZ - radius) / 16;
        this.endZ = (locZ + radius) / 16;

        this.setup();

    }

    private void setup() {
        this.startChunkX = this.startX / 16;
        this.endChunkX = this.endX / 16;

        this.startChunkZ = this.startZ / 16;
        this.endChunkZ = this.endZ / 16;

        this.widthChunks = this.endChunkX - this.startChunkX + 1;
        this.depthChunks = this.endChunkZ - this.startChunkZ +1;

        this.widthBlocks = this.widthChunks * 16;
        this.depthBlocks = this.depthChunks * 16;

        this.areaChunks = this.widthChunks * this.depthChunks;
        this.areaBlocks = this.widthBlocks * this.depthBlocks;

        this.volBlocks = this.areaBlocks * 256;

        this.loadedChunks = new Chunk[this.areaChunks];
        this.chunkLoadFutures = new CompletableFuture[this.areaChunks];
    }

    //x and z are chunk coordinates
    private void forceLoad(int x, int z) {
        CompletableFuture<Chunk> chunkLoadFuture = PaperLib.getChunkAtAsync(this.world, x, z, true);
        chunkLoadFuture.thenApply(chunk -> {
           chunk.setForceLoaded(true);
           this.addChunk(chunk);
           return true;
        });
        this.chunkLoadFutures[x + this.widthChunks * z] = chunkLoadFuture;

    }

    public CompletableFuture<Chunk>[] forceLoad() {
        for (int x = this.startChunkX; x <= this.endChunkX; x++) {
            for (int z = this.startChunkZ; z <= this.endChunkZ; z++) {
                this.forceLoad(x, z);
            }
        }
        return this.chunkLoadFutures;
    }

    public Location getAbsPos(int relX, int relY, int relZ) {
        return new Location(this.getWorld(),this.getStartX() + relX,  relY, this.getStartZ() + relZ);
    }

    /**
     * Returns the x coordinate of a chunk relative to this.startChunkX
     *
     * returns -1 if chunk is not inside the region you specified
     *
     */
    public int getRelChunkX(int absX) {
        int relX = absX - this.startChunkX;

        if(absX < this.startChunkX) return -1;
        if(absX > this.endChunkX) return -1;

        return relX;
    }

    /**
     * Returns the z coordinate of a chunk relative to this.startChunkZ
     *
     * returns -1 if chunk is not inside the region you specified
     *
     */
    public int getRelChunkZ(int absZ) {
        int relZ = absZ - this.startChunkZ;

        if(absZ < this.startChunkZ) return -1;
        if(absZ > this.endChunkZ) return -1;

        return relZ;
    }

    private void clearChunk(@Nonnull Chunk chunk) {

        int x = this.getRelChunkX(chunk.getX());
        int z = this.getRelChunkZ(chunk.getZ());

        this.loadedChunks[x + this.widthChunks * z] = chunk;
    }

    private void addChunk(@Nonnull Chunk chunk) {

        int x = this.getRelChunkX(chunk.getX());
        int z = this.getRelChunkZ(chunk.getZ());

        this.loadedChunks[x + this.widthChunks * z] = chunk;
    }

    public Chunk getChunk(int absX, int absZ) {

        int x = this.getRelChunkX(absX);
        int z = this.getRelChunkZ(absZ);

        return this.loadedChunks[x + this.widthChunks * z];
    }

    public void free(@Nonnull Chunk chunk) {
        if(!this.getChunk(chunk.getX(), chunk.getZ()).equals(chunk)) return;
        this.clearChunk(chunk);
        chunk.setForceLoaded(false);
    }

    public World getWorld() {
        return world;
    }

    public int getStartX() {
        return startX;
    }

    public int getEndX() {
        return endX;
    }

    public int getStartZ() {
        return startZ;
    }

    public int getEndZ() {
        return endZ;
    }

    public int getStartChunkX() {
        return startChunkX;
    }

    public int getEndChunkX() {
        return endChunkX;
    }

    public int getStartChunkZ() {
        return startChunkZ;
    }

    public int getEndChunkZ() {
        return endChunkZ;
    }

    public int getWidthBlocks() {
        return widthBlocks;
    }

    public int getDepthBlocks() {
        return depthBlocks;
    }

    public int getWidthChunks() {
        return widthChunks;
    }

    public int getDepthChunks() {
        return depthChunks;
    }

    public int getAreaChunks() {
        return areaChunks;
    }

    public int getAreaBlocks() {
        return areaBlocks;
    }

    public int getVolBlocks() {
        return volBlocks;
    }
}
