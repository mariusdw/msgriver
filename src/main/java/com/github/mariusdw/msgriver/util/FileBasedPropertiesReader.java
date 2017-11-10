package com.github.mariusdw.msgriver.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class FileBasedPropertiesReader implements PropertiesReader {

    private String path;

    public FileBasedPropertiesReader(String path) {
        this.path = path;
    }

    @Override
    public Properties read() throws PropertiesReaderException {

        try (FileInputStream is = new FileInputStream(this.path)) {
            Properties properties = new Properties();
            properties.load(is);
            return properties;
        } catch (IOException e) {
            throw new PropertiesReaderException("Error loading file " + this.path, e);
        }
    }

}
