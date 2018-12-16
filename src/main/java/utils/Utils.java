package utils;

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class Utils {
    public static ByteBuffer byteArrayToByteBuffer(byte[] byteArray) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(byteArray.length);
        buffer.put(byteArray);
        buffer.rewind();
        return buffer;
    }

    public static byte[] compress(String data) throws IOException {
        return compress(data.getBytes());
    }

    public static byte[] compress(byte[] data) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length);
        GZIPOutputStream gzip = new GZIPOutputStream(bos);
        gzip.write(data);
        gzip.close();
        byte[] compressed = bos.toByteArray();
        bos.close();
        return compressed;
    }

    public static String decompress(byte[] data) throws IOException {
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        GZIPInputStream gis = new GZIPInputStream(bis);
        byte[] bytes = IOUtils.toByteArray(gis);
        return new String(bytes, "UTF-8");
    }

    public static byte[] decompressAsBytes(byte[] data) throws IOException {
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        GZIPInputStream gis = new GZIPInputStream(bis);
        return IOUtils.toByteArray(gis);
    }
}
