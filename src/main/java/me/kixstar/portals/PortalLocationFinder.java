package me.kixstar.portals;

import org.bukkit.*;
import org.bukkit.util.Vector;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.CompletableFuture;


public class PortalLocationFinder {

    private Vector scannerPos = new Vector();

    public Random random;

    private ChunkRegion cr;

    private List<Material> canPlaceOn;

    private List<Material> canPlaceOver;

    private int lowBound = -256;
    private int highBound = 256;

    private boolean hitLowBound = false;
    private boolean hitHighBound = false;

    private Vector schematicSize;

    //Y coord of the block that is the highest point of the chunk region
    private int highSurface = 0;
    //Y coord of the block that is the lowest point of the chunk region
    private int lowSurface = 255;


    /*
        0 - Blacklist of materials that we can't place portals on and we can't remove if they're interfering with portals
            example: Material.BEDROCK every item that isn't on the first two is presumed blacklisted
        1 - Whitelist of materials that we can place a portal on
            example: Material.GRASS, Material.STONE, Material.DIRT
        2 - A list of materials to ignore when looking for a location for a portal
            example: Material.LEAVES, Material.LOG should be used so that surface portals don't spawn on trees
        3 - Special category for elements are both in the whitelist and the ignore list
            represents blocks that we can place a portal on but also remove if needed


    */
    byte[] blockGrid;

    short[] surfaceBlocks;

    private int[][] searchQueue;

    public PortalLocationFinder(@Nonnull ChunkRegion region) {
        if(region == null) throw new RuntimeException("\"world\" can't be null");

        this.cr = region;

        this.blockGrid = new byte[this.cr.getVolBlocks()];

        this.surfaceBlocks = new short[this.cr.getAreaBlocks()];

        this.random = new Random();

        //biased against higher numbers, probably should get replaced in future with a better solution
        this.scannerPos.setY(0);

    }


    public void genSearchQueue() {
        /* first we fill the array with indexes of this.surfaceBlocks, then we shuffle it
        *  and use the searchQueue when checking for portal location so that the search algorithm is randomized
        * this is randomized for every Portal location search
        * */
        this.searchQueue = new int[this.cr.getAreaBlocks()][2];

        int width = this.cr.getWidthBlocks();

        for(int x = 0; x < this.cr.getWidthBlocks(); x++) {
            for(int z = 0; z < this.cr.getDepthBlocks(); z++) {
                this.searchQueue[x + width * z] = new int[]{x, z};
            }
        }

        for (int i = 0; i < this.searchQueue.length; i++) {
            int randomIndexToSwap = this.random.nextInt(this.searchQueue.length);
            int[] temp = this.searchQueue[randomIndexToSwap];
            this.searchQueue[randomIndexToSwap] = this.searchQueue[i];
            this.searchQueue[i] = temp;
        }
    }

    public void setCanPlaceOn(List<Material> canPlaceOn) {
        this.canPlaceOn = canPlaceOn;
    }

    public void setCanPlaceOver(List<Material> canPlaceOver) {
        this.canPlaceOver = canPlaceOver;
    }

    public void setLowBound(int lowBound) {
        this.lowBound = lowBound;
    }

    public void setHighBound(int highBound) {
        this.highBound = highBound;
    }

    public void setSchematicSize(Vector vector) {this.schematicSize = vector.clone();}

    public boolean parseChunks(Chunk chunk) {
        for (short x = 0; x < 16 ; x++) {
            for (short z = 0; z < 16; z++) {
                boolean detectedSurface = false;
                for (short y = 255; y >= 0; y--) {
                    byte data = 0;

                    Material mat = chunk.getBlock(x,y,z).getBlockData().getMaterial();

                    boolean canPlacePortalOn = this.canPlaceOn.contains(mat);
                    boolean canPlacePortalOver = this.canPlaceOver.contains(mat);

                    int dataX = this.cr.getRelChunkX(chunk.getX()) * 16 + x;
                    int dataZ = this.cr.getRelChunkZ(chunk.getZ()) * 16 + z;

                    if(canPlacePortalOn && !detectedSurface) {
                        detectedSurface = true;

                        if(y > this.highSurface) this.highSurface = y;
                        if(y < this.lowSurface) this.lowSurface = y;

                        this.setSurface(x, y, z);
                    }

                    if(canPlacePortalOn) data = 1;
                    if(canPlacePortalOver) data = 2;
                    if(canPlacePortalOn && canPlacePortalOver) data = 3;

                    this.setData(dataX, y, dataZ, data);

                }
            }
        }
        this.cr.free(chunk);
        return true;
    }
    public Location findPortalLocation() {
        //todo: do this async so you don't block the server thread
        CompletableFuture<Chunk>[] chunkLoadFutures = this.cr.forceLoad();
        for (CompletableFuture<Chunk> chunkLoadFuture: chunkLoadFutures) {
            chunkLoadFuture.thenApply(this::parseChunks);
        }

        this.genSearchQueue();
        //this blocks
        CompletableFuture.allOf(chunkLoadFutures).join();

        return this.onAllChunksLoaded();

    }


    public Location onAllChunksLoaded() {

        int range = this.highBound - this.lowBound;

        //layer to start the search on (chosen randomly to remove Y bias)

        int startLayer = this.lowBound;
        if(range != 0) startLayer += (this.random.nextInt() % range);

        this.scannerPos.setY(startLayer);

        //the distance that the scanner covered (used when the scanner switches direction)
        int distanceTravelled = 0;

        //the direction in witch the scanner moved last iteration (false is down, true is up)
        boolean lastDirection = false;

        Location portalPos;

        //initial surface level scan
        portalPos = this.moveAndScan(0, true);

        if(portalPos != null) return portalPos;

        //random direction scans on the Y axis
        for(int i = 0; i < range; i++) {

            boolean nextDirection = this.random.nextBoolean();

            if(this.hitLowBound) nextDirection = true;
            if(this.hitHighBound) nextDirection = false;

            //no portal locations were found :(
            if(this.hitHighBound && this.hitLowBound) return null;

             if(lastDirection == nextDirection) {
                portalPos = this.moveAndScan(1, nextDirection);
                distanceTravelled++;
            } else {
                portalPos = this.moveAndScan(distanceTravelled + 1, nextDirection);
                distanceTravelled++;
            }

            if(portalPos != null) return portalPos;

        }

        return null;
    }

    
    public Location moveAndScan(int travelDist, boolean direction) {

        if(this.scannerPos.getY() > this.highBound && direction) this.hitHighBound = true;
        if(this.scannerPos.getY() < this.lowBound && !direction) this.hitLowBound = true;

        this.scannerPos.setY(direction? this.scannerPos.getY() + travelDist: this.scannerPos.getY() - travelDist);

        for(int i = 0; i < this.searchQueue.length; i++) {

                int x = this.searchQueue[i][0];
                int z = this.searchQueue[i][1];

                int surfaceY = this.getSurface(x,z);

                int y  = surfaceY + this.scannerPos.getBlockY();

                boolean canPlacePortal = this.checkVolume(x, y, z);

                if(canPlacePortal) return this.cr.getAbsPos(x, y, z);

        }
        return null;
    }


    public boolean isInLoadedChunks(int x, int y, int z) {

        if(x >= this.cr.getWidthBlocks()) return false;
        if(x < 0) return false;

        if(y >= 256) return false;
        if(y < 0) return false;

        if(z >= this.cr.getDepthBlocks()) return false;
        if(z < 0) return false;

        return true;
    }

    public boolean checkVolume(int portalX, int portalY, int portalZ) {
        for (int xOffset = 0; xOffset < this.schematicSize.getBlockX(); xOffset++) {
            for (int zOffset = 0; zOffset < this.schematicSize.getBlockZ(); zOffset++) {
                for (int yOffset = -1; yOffset < this.schematicSize.getBlockY(); yOffset++) {

                    int x = portalX + xOffset;
                    int y = portalY + yOffset;
                    int z = portalZ + zOffset;

                    if(!this.isInLoadedChunks(x, y, z)) return false;


                    byte data = this.getData(x, y, z);

                    if(yOffset == -1) {
                        if (!(data == 1 || data == 3)) return false;
                    } else  {
                        if (!(data == 2 || data == 3)) return false;
                    }

                }
            }
        }
        return true;
    }


    private void setData(int x, int y, int z, byte data) {
        this.blockGrid[x + y * this.cr.getWidthBlocks() + z * this.cr.getWidthBlocks() * 256] = data;
    }

    private byte getData(int x, int y, int z) {
        return this.blockGrid[x + y * this.cr.getWidthBlocks() + z * this.cr.getWidthBlocks() * 256];
    }

    private void setSurface(int x, int y, int z) {
        this.surfaceBlocks[x + this.cr.getWidthBlocks() * z] = (short) y;
    }

    private short getSurface(int x, int z) {
        return this.surfaceBlocks[x + this.cr.getWidthBlocks() * z];
    }

    public int[] getCoordinates(int i) {
        int x;
        int z = i / this.cr.getWidthBlocks();
        x = i - z * this.cr.getWidthBlocks();

        return new int[]{x, z};
    }
}
