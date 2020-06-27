package me.kixstar.portals;

import org.bukkit.*;
import org.bukkit.util.Vector;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Random;
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
    private int highPoint = 0;
    //Y coord of the block that is the lowest point of the chunk region
    private int lowPoint = 255;



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

    public PortalLocationFinder(@Nonnull ChunkRegion region) {
        if(region == null) throw new RuntimeException("\"world\" can't be null");

        this.cr = region;

        this.blockGrid = new byte[this.cr.getVolBlocks()];

        this.surfaceBlocks = new short[this.cr.getAreaBlocks()];

        this.random = new Random();

        //biased against higher numbers, probably should get replaced in future with a better solution
        this.scannerPos.setX(this.random.nextInt() % this.cr.getWidthBlocks());
        this.scannerPos.setZ(this.random.nextInt() % this.cr.getDepthBlocks());

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

                    int dataX = this.cr.getRelX(chunk.getX()) * 16 + x;
                    int dataZ = this.cr.getRelZ(chunk.getZ()) * 16 + z;

                    if(canPlacePortalOn && !detectedSurface) {
                        detectedSurface = true;

                        if(y > this.highPoint) this.highPoint = y;
                        if(y < this.lowPoint) this.lowPoint = y;

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

        //this blocks
        CompletableFuture.allOf(chunkLoadFutures).join();

        return this.onAllChunksLoaded();

    }


    public Location onAllChunksLoaded() {
        int lowestPoint = this.lowPoint + this.lowBound;
        if(lowestPoint < 0) lowestPoint = 0;

        int highestPoint = this.highPoint + this.highBound;
        if(highestPoint > 255) highestPoint = 255;

        int range = highestPoint - lowestPoint;

        //layer to start the search on (chosen randomly to remove Y bias)

        int startLayer = lowestPoint;
        if(range != 0) startLayer += (this.random.nextInt() % range);


        //the distance that the scanner covered (used when the scanner switches direction)
        int distanceTravelled = 0;

        //the direction in witch the scanner moved last iteration (false is down, true is up)
        boolean lastDirection = false;

        for(int i = 0; i <= range; i++) {

            boolean nextDirection = this.random.nextBoolean();

            if(this.hitLowBound) nextDirection = true;
            if(this.hitHighBound) nextDirection = false;

            //no portal locations were found :(
            if(this.hitHighBound && this.hitLowBound) return null;

            Location portalPos;

            if(lastDirection == nextDirection) {
                portalPos = this.moveAndScan(1, nextDirection);
            } else {
                portalPos = this.moveAndScan(distanceTravelled + 1, nextDirection);
            }

            if(portalPos != null) return portalPos;

            //todo: search the 3D space by translating the surface lavel up and down randomly and then checking if you can fit the
        }

        //todo: fix when implemented
        return null;
    }


    //todo: fix bias towards lower coordinates using this.scannerPos
    public Location moveAndScan(int travelDist, boolean direction) {
        this.hitHighBound = true;
        this.hitLowBound = true;

        for (int i = 0; i < this.surfaceBlocks.length; i++) {
            if(direction) {
                this.surfaceBlocks[i] += travelDist;
            } else {
                this.surfaceBlocks[i] -= travelDist;
            }

            if(this.surfaceBlocks[i] < this.highBound && direction) this.hitHighBound = false;
            if(this.surfaceBlocks[i] > this.lowBound && !direction) this.hitLowBound = false;


            Vector pos = this.getCoordinates(i);

            boolean canPlacePortal = this.checkVolume(pos);

            if(canPlacePortal) return new Location(this.cr.getWorld(), this.cr.getStartX() + pos.getX(),  pos.getY(), this.cr.getStartZ() + pos.getZ());
        }

        return null;
    }

    public boolean isInLoadedChunks(Vector vec) {

        if(vec.getBlockX() > this.cr.getEndX()) return false;
        if(vec.getBlockX() < this.cr.getStartX()) return false;

        if(vec.getBlockY() > 255) return false;
        if(vec.getBlockY() < 0) return false;

        if(vec.getBlockZ() > this.cr.getEndZ()) return false;
        if(vec.getBlockZ() < this.cr.getStartZ()) return false;

        return true;
    }

    //todo: this looks awful
    public boolean checkVolume(Vector pos) {
        for (int x = 0; x < this.schematicSize.getBlockX(); x++) {
            for (int z = 0; z < this.schematicSize.getBlockZ(); z++) {
                for (int y = -1; y < this.schematicSize.getBlockY(); y++) {
                    Vector check = pos.clone();
                    check.add(new Vector(x,y,z));
                    if(!this.isInLoadedChunks(check)) return false;
                    byte data = this.getData(check.getBlockX(), check.getBlockY(), check.getBlockZ());
                    if(y == -1) {
                        if (data != 1 || data != 3) return false;
                    } else  {
                        if (data != 2 || data != 3) return false;
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

    private void setSurface(short x, short y, short z) {
        this.surfaceBlocks[x + this.cr.getWidthBlocks() * z] = y;
    }

    private short getSurface(short x, short z) {
        return this.surfaceBlocks[x + this.cr.getWidthBlocks() * z];
    }

    public Vector getCoordinates(int i) {
        int x;
        int y = this.surfaceBlocks[i];
        int z = i / this.cr.getWidthBlocks();
        x = i - z;
        World world = this.cr.getWorld();
        return new Vector(x, y, z);
    }

}
