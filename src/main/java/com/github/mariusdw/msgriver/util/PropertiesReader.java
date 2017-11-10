package com.github.mariusdw.msgriver.util;

import java.util.Properties;

public interface PropertiesReader {

    public static class PropertiesReaderException extends Exception {

        public PropertiesReaderException() {
            super();
        }

        public PropertiesReaderException(String message) {
            super(message);
        }

        public PropertiesReaderException(String message, Throwable cause) {
            super(message, cause);
        }

        public PropertiesReaderException(Throwable cause) {
            super(cause);
        }
    }

    Properties read() throws PropertiesReaderException;
}
