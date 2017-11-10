package com.github.mariusdw.msgriver.datastore;

import java.time.ZonedDateTime;
import java.util.List;

public interface DataStore {

    public enum PropertyKeys {
        ELASTIC_HOST("elastic.host"),
        ELASTIC_PORT("elastic.port");

        private String key;

        PropertyKeys(String key) {
            this.key = key;
        }

        public String getKey() {
            return this.key;
        }
    }

    public static class DataStoreException extends Exception {

        public DataStoreException() {
            super();
        }

        public DataStoreException(String message) {
            super(message);
        }

        public DataStoreException(String message, Throwable cause) {
            super(message, cause);
        }

        public DataStoreException(Throwable cause) {
            super(cause);
        }
    }

    void init() throws DataStoreException;

    void indexMessages(List<Message> comms) throws DataStoreException;

    ZonedDateTime getMostRecentDocumentTimestamp(String type) throws DataStoreException;
}
