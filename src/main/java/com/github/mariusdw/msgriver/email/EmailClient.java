package com.github.mariusdw.msgriver.email;

import com.github.mariusdw.msgriver.datastore.Message;
import java.time.ZonedDateTime;
import java.util.List;

public interface EmailClient {

    public enum PropertyKeys {
        HOST("mail.host"),
        USER("mail.username"),
        PASSWORD("mail.password");

        private String key;

        PropertyKeys(String key) {
            this.key = key;
        }

        public String getKey() {
            return this.key;
        }
    }

    public static class EmailException extends Exception {

        public EmailException() {
            super();
        }

        public EmailException(String message) {
            super(message);
        }

        public EmailException(String message, Throwable cause) {
            super(message, cause);
        }

        public EmailException(Throwable cause) {
            super(cause);
        }
    }

    void init();

    List<Message> retrieve(ZonedDateTime cutoffTimestamp, int batchSize) throws EmailException;
}
