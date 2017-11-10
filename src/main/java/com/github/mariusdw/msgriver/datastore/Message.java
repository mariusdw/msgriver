package com.github.mariusdw.msgriver.datastore;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public class Message {

    private String title;
    private String type;
    private String body;
    private List<String> senders;
    private List<String> receivers;
    private ZonedDateTime timestamp;

    public List<String> getSenders() {
        return senders;
    }

    public void addSender(String sender) {
        if (this.senders == null) {
            this.senders = new ArrayList<>();
        }
        this.senders.add(sender);
    }

    public void setSenders(List<String> senders) {
        this.senders = senders;
    }

    public List<String> getReceivers() {
        return receivers;
    }

    public void addReceiver(String receiver) {
        if (this.receivers == null) {
            this.receivers = new ArrayList<>();
        }
        this.receivers.add(receiver);
    }

    public void setReceivers(List<String> receivers) {
        this.receivers = receivers;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public ZonedDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(ZonedDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "Message{" + "title=" + title + ", type=" + type + ", body=" + body + ", senders=" + senders + ", receivers=" + receivers + ", timestamp=" + timestamp + '}';
    }
}
