package me.kixstar.portals.database.driver;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import me.kixstar.portals.Portal;
import me.kixstar.portals.KixstarPortals;
import me.kixstar.portals.database.DatabaseDriver;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Map;

/*
*   A database driver for the development environment
*   Uses the filesystem for persistent storage
*   All of the persistent data is kept in ./plugins/KixstarPortals/persistent
*
* */
public class DevDatabaseDriver implements DatabaseDriver {

    private Plugin plugin = KixstarPortals.getInstance();

    private File root = new File(plugin.getDataFolder(), "./persistent");

    private File portalStorage = new File(root, "./portals");

    private File portalSchems = new File(portalStorage, "./schems");

    private File portalData = new File(portalStorage, "./data");

    private File oldblocks = new File(root, "./oldblocks");


    @Override
    public Clipboard readPortalSchematic(String handle) {
        String fileName = String.format("%s.nbt", handle);
        File file = new File(this.portalSchems, fileName);
        //check if directory traversal was attempted
        if(!this.dirTraversalCheck(file, this.portalSchems)) return null;
        ClipboardFormat format = BuiltInClipboardFormat.MINECRAFT_STRUCTURE;
        try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
            //null exception in reader, maybe my test schematic is invalid.
            return reader.read();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Clipboard readOldBlocks(String uuid) {
        String fileName = String.format("%s.nbt", uuid);
        File file = new File(this.oldblocks, fileName);
        //check if directory traversal was attempted
        if(!this.dirTraversalCheck(file, this.oldblocks)) return null;
        ClipboardFormat format = BuiltInClipboardFormat.MINECRAFT_STRUCTURE;
        try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
            return reader.read();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean writeOldBlocks(String uuid, Clipboard oldBlocks) {
        String fileName = String.format("%s.nbt", uuid);
        File file = new File(this.oldblocks, fileName);
        //check if directory traversal was attempted
        if(!this.dirTraversalCheck(file, this.oldblocks)) return false;
        try {
            FileOutputStream stream = new FileOutputStream(file);
            ClipboardWriter writer = BuiltInClipboardFormat.MINECRAFT_STRUCTURE.getWriter(stream);
            writer.write(oldBlocks);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public boolean deleteOldBlocks(String uuid) {
        String fileName = String.format("%s.nbt", uuid);
        File file = new File(this.oldblocks, fileName);
        //check if directory traversal was attempted
        if(!this.dirTraversalCheck(file, this.oldblocks)) return false;
        return file.delete();
    }

    @Override
    public Portal readPortal(int x, int z) {
        //note: this is unsafe for
        String fileName = String.format("(%d)(%d).yml", x, z);
        File file = new File(this.portalData, fileName);
        try (InputStream is  = new FileInputStream(file)) {
            Yaml yaml = new Yaml();
            Map<String, Object> portalData = yaml.load(is);
            return Portal.deserializeMap(portalData);
        } catch (FileNotFoundException e) {
            return null;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean writePortal(Portal portal) {
        Location origin = portal.getPortalOrigin();
        int chunkX = origin.getBlockX() / 16;
        int chunkZ = origin.getBlockZ() / 16;
        String fileName = String.format("(%d)(%d).yml", chunkX, chunkZ);
        File file = new File(this.portalData, fileName);
        Yaml yaml = new Yaml();
        String result = yaml.dump(portal.serializeMap());
        try(OutputStream os = new FileOutputStream(file, false)) {
            os.write(result.getBytes(Charset.forName("UTF-8")));
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean deletePortal(Portal portal) {
        Location origin = portal.getPortalOrigin();
        int chunkX = origin.getBlockX() / 16;
        int chunkZ = origin.getBlockZ() / 16;
        String fileName = String.format("(%d)(%d).yml", chunkX, chunkZ);
        File file = new File(this.portalData, fileName);
        return file.delete();
    }

    //prevents directory traversal exploits -> https://portswigger.net/web-security/file-path-traversal
    public boolean dirTraversalCheck(File res, File start) {
        try {
            return res.getCanonicalPath().startsWith(start.getCanonicalPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

}
