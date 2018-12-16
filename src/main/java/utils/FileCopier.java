package utils;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileCopier {
    public static void main(String[] args) throws IOException {
        File initialFile = new File("C:\\MyStuff\\Facultate\\VSD\\Other assts\\ship_texture.png");
//        File initialFile = new File("C:\\MyStuff\\Facultate\\VSD\\Other assts\\ship.obj");
        FileInputStream fileInputStream = new FileInputStream(initialFile);
        byte[] initBytes = IOUtils.toByteArray((fileInputStream));
        byte[] compressedBytes = initBytes;
//        byte[] compressedBytes = Utils.compress(initBytes);
        for(int i = 0; i < 100; ++i) {
            File destination = new File("C:\\MyStuff\\Facultate\\VSD\\Other assts\\forS3\\textures\\ship" + i + "_texture.png");
//            File destination = new File("C:\\MyStuff\\Facultate\\VSD\\Other assts\\forS3\\obj\\ship" + i + ".obj");
            FileOutputStream fileOutputStream = new FileOutputStream(destination);
            fileOutputStream.write(compressedBytes);
        }
    }
}
