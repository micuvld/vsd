package utils;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Slf4j
public class Config {
    private static final String CONFIG_FILE_NAME = "config.properties";
    private static final Properties properties = new Properties();

    static {
        InputStream input = Config.class.getClassLoader().getResourceAsStream(CONFIG_FILE_NAME);

        if(input == null){
            throw new RuntimeException("Unable to find " + CONFIG_FILE_NAME);
        } else {
            try {
                properties.load(input);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static String get(AppProperty appProperty) {
        return properties.getProperty(appProperty.name().toLowerCase());
    }

    public static int getInt(AppProperty appProperty) {
        return Integer.parseInt(properties.getProperty(appProperty.name().toLowerCase()));
    }
}
