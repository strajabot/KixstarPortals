package me.kixstar.portals.database;


import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;

import java.io.*;

public class KixtarDB {


    public static Clipboard getPortalSchematic(String handle) {
        //todo: implement database interfaces
        //the simplest solution would be file storage since these shouldn't change to much

        //filler for test purposes
        File file = new File("./portals/" + handle  + ".nbt");

        ClipboardFormat format = BuiltInClipboardFormat.MINECRAFT_STRUCTURE;

        try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
            //null exception in reader, maybe my test schematic is invalid.
            return reader.read();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean writeOldBlocks(String uuid, Clipboard oldBlocks) {
        //filler for test purposes
        File file = new File("./oldblocks/" + uuid + ".nbt");
        try {
            FileOutputStream stream = new FileOutputStream(file);
            ClipboardWriter writer = BuiltInClipboardFormat.MINECRAFT_STRUCTURE.getWriter(stream);
            writer.write(oldBlocks);
            return true;
            } catch (IOException e) {
            return false;
        }

    }
    public static Clipboard getOldBlocks(String uuid) {
        //A BLOB should probably be used for storing these

        //filler for test purposes
        File file = new File("./oldblocks/" + uuid  + ".nbt");

        ClipboardFormat format = BuiltInClipboardFormat.MINECRAFT_STRUCTURE;

        try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
            //null exception in reader, maybe my test schematic is invalid.
            return reader.read();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
