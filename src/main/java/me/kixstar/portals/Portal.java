package me.kixstar.portals;

import me.kixstar.portals.database.KixstarDB;
import org.bukkit.Location;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Portal {

    private Location portalOrigin;

    private String portalID;

    private String oldBlocksUUID;

    private PortalSpawner spawner;

    public static Portal create(Location portalOrigin, String portalID) {
        Portal portal =  new Portal(portalOrigin, portalID, UUID.randomUUID().toString());
        portal.spawner.saveBlocks();
        portal.spawner.placePortal();
        PortalManager.get().load(portal);
        KixstarDB.writePortal(portal);

        return portal;
    }

    public static Portal deserializeMap(Map<String, Object> portalData) {
        Location portalOrigin = Location.deserialize((Map<String, Object>) portalData.get("location"));
        String portalID = (String) portalData.get("portal-id");
        String oldBlocksUUID = (String) portalData.get("old-blocks-uuid");

        return new Portal(portalOrigin, portalID, oldBlocksUUID);
    }

    public static Portal load(int x, int z) {
        Portal portal = KixstarDB.readPortal(x,z);
        if(portal == null) return null;
        PortalManager.get().load(portal);
        return portal;
    }

    public Portal(Location portalOrigin, String portalID, String oldBlocksUUID) {

        this.portalOrigin = portalOrigin;
        this.portalID = portalID;
        this.oldBlocksUUID = oldBlocksUUID;

        this.spawner = new PortalSpawner(this.portalOrigin, this.portalID, this.oldBlocksUUID);
    }

    public void destroy() {
        this.spawner.resetBlocks();

        KixstarDB.deletePortal(this);
    }

    public void unload() {
        //final save of data
        KixstarDB.writePortal(this);
        PortalManager.get().unload(this);
    }

    public Location getPortalOrigin() {
        return portalOrigin.clone();
    }

    public Map<String, Object> serializeMap() {
        Map<String, Object> portalData = new HashMap<>(3);
        portalData.put("location", this.portalOrigin.serialize());
        portalData.put("portal-id", this.portalID);
        portalData.put("old-blocks-uuid", this.oldBlocksUUID);
        return portalData;
    }
}
