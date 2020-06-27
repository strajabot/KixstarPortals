package me.kixstar.portals;


import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;

public class TestPortalFinder extends PortalLocationFinder {
    public TestPortalFinder(ChunkRegion chunkRegion, Player player) {
        super(chunkRegion);
        this.setHighBound(1);
        this.setLowBound(1);
        this.setCanPlaceOn(Arrays.asList(new Material[]{Material.GRASS_BLOCK,}));
        this.setCanPlaceOver(Arrays.asList(new Material[]{Material.AIR, Material.OAK_LEAVES}));
        this.setSchematicSize(new Vector(3,3,3));
        player.teleport(this.findPortalLocation());
    }
}
