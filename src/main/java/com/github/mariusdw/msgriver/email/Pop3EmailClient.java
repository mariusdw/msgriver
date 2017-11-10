package com.github.mariusdw.msgriver.email;

import com.github.mariusdw.msgriver.datastore.Message;
import com.github.mariusdw.msgriver.util.PropertiesReader;
import com.github.mariusdw.msgriver.util.PropertiesReader.PropertiesReaderException;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;
import javax.mail.Address;
import javax.mail.Folder;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Pop3EmailClient implements EmailClient {

    private PropertiesReader propertiesReader;
    private Properties properties;
    private Logger logger = LoggerFactory.getLogger(Pop3EmailClient.class);

    public Pop3EmailClient(PropertiesReader propertiesReader) {
        this.propertiesReader = propertiesReader;
    }

    @Override
    public void init() throws IllegalStateException {
        try {
            this.properties = propertiesReader.read();
        } catch (PropertiesReaderException e) {
            throw new IllegalStateException("Failed to read properties", e);
        }
    }

    @Override
    public List<Message> retrieve(ZonedDateTime cutoffTimestamp, int batchSize) throws EmailException {
        Session emailSession = Session.getDefaultInstance(properties);
        List<Message> emails = new ArrayList<>();
        Integer firstMailPos = null;
        ZonedDateTime firstMailTimestamp = null;
        try (Store store = emailSession.getStore("pop3s")) {
            store.connect(this.properties.getProperty(EmailClient.PropertyKeys.HOST.getKey()),
                    this.properties.getProperty(EmailClient.PropertyKeys.USER.getKey()),
                    this.properties.getProperty(EmailClient.PropertyKeys.PASSWORD.getKey()));
            try (Folder emailFolder = store.getFolder("INBOX")) {
                emailFolder.open(Folder.READ_ONLY);
                javax.mail.Message[] messages = emailFolder.getMessages();
                logger.info("Inbox contains {} emails", messages.length);

                if (messages.length > 0) {
                    for (int pos = messages.length - 1; pos >= 0; pos--) {
                        javax.mail.Message message = messages[pos];
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTime(message.getSentDate());
                        ZonedDateTime messageDateTime = ZonedDateTime.ofInstant(calendar.toInstant(), calendar.getTimeZone().toZoneId());
                        if (messageDateTime.isAfter(cutoffTimestamp)) {
                            logger.info("Mail at pos {} sent time {} > cutoff time {}", pos, messageDateTime, cutoffTimestamp);
                            firstMailPos = pos;
                            firstMailTimestamp = messageDateTime;
                        } else {
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new EmailException("Unable to retrieve email", e);
        }

        if (firstMailPos != null) {
            try (Store store = emailSession.getStore("pop3s")) {
                store.connect(this.properties.getProperty(EmailClient.PropertyKeys.HOST.getKey()),
                        this.properties.getProperty(EmailClient.PropertyKeys.USER.getKey()),
                        this.properties.getProperty(EmailClient.PropertyKeys.PASSWORD.getKey()));
                try (Folder emailFolder = store.getFolder("INBOX")) {
                    emailFolder.open(Folder.READ_ONLY);
                    javax.mail.Message[] messages = emailFolder.getMessages();
                    logger.info("Inbox contains {} emails", messages.length);

                    if (firstMailPos < messages.length) {
                        javax.mail.Message message = messages[firstMailPos];
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTime(message.getSentDate());
                        ZonedDateTime searchMessageDateTime = ZonedDateTime.ofInstant(calendar.toInstant(), calendar.getTimeZone().toZoneId());
                        if (!searchMessageDateTime.isEqual(firstMailTimestamp)) {
                            throw new EmailException("Mailbox changed since retrieval, please try again");
                        }
                    } else {
                        throw new EmailException("Mailbox changed since retrieval, please try again");
                    }

                    int batchCounter = 0;
                    for (int pos = firstMailPos; pos < messages.length; pos++) {
                        javax.mail.Message message = messages[pos];
                        Message email = new Message();
                        email.setType("EMAIL");
                        for (Address a : message.getFrom()) {
                            email.addSender(a.toString());
                        }
                        for (Address a : message.getAllRecipients()) {
                            email.addReceiver(a.toString());
                        }
                        email.setTitle(message.getSubject());
                        email.setBody(retrieveContent(message));
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTime(message.getSentDate());
                        ZonedDateTime messageDateTime = ZonedDateTime.ofInstant(calendar.toInstant(), calendar.getTimeZone().toZoneId());
                        email.setTimestamp(messageDateTime);

                        logger.info("Read email {}", email.getTitle());
                        logger.debug("Email {}", email.toString());
                        emails.add(email);

                        batchCounter++;
                        if (batchCounter >= batchSize) {
                            break;
                        }

                    }
                }
            } catch (Exception e) {
                throw new EmailException("Unable to retrieve email", e);
            }
        }

        return emails;
    }

    protected String retrieveContent(Part p) throws Exception {
        String content = "";
        //check if the content is plain text
        if (p.isMimeType("text/plain")) {
            logger.debug("--- Plaintext part ---");
            content = (String) p.getContent();
        } //check if the content has attachment
        else if (p.isMimeType("multipart/*")) {
            logger.debug("--- Multipart ---");
            Multipart mp = (Multipart) p.getContent();
            int count = mp.getCount();
            for (int i = 0; i < count; i++) {
                content = content + retrieveContent(mp.getBodyPart(i));
            }
        } //check if the content is a nested message
        else if (p.isMimeType("message/rfc822")) {
            System.out.println("This is a Nested Message");
            System.out.println("---------------------------");
            content = retrieveContent((Part) p.getContent());
        } //check if the content is an inline image
        else if (p.isMimeType("image/jpeg")) {
            //TODO
        } else if (p.getContentType().contains("image/")) {
            //TODO
        } else {
            Object o = p.getContent();
            if (o instanceof String) {
                //System.out.println((String) o);
                if (p.getContentType().contains("text/html")) {
                    logger.debug("--- String part (html) ---");
                    Document doc = Jsoup.parse((String) o);
                    content = doc.body().text();
                }
            } else if (o instanceof InputStream) {
                //TODO
            } else {
                //TODO
            }
        }

        return content;
    }
}
