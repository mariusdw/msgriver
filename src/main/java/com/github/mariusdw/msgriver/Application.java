package com.github.mariusdw.msgriver;

import com.github.mariusdw.msgriver.datastore.DataStore;
import com.github.mariusdw.msgriver.datastore.DataStore.DataStoreException;
import com.github.mariusdw.msgriver.datastore.ElasticStore;
import com.github.mariusdw.msgriver.datastore.Message;
import com.github.mariusdw.msgriver.email.EmailClient;
import com.github.mariusdw.msgriver.email.EmailClient.EmailException;
import com.github.mariusdw.msgriver.email.Pop3EmailClient;
import com.github.mariusdw.msgriver.util.FileBasedPropertiesReader;
import com.github.mariusdw.msgriver.util.PropertiesReader;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.*;
import org.springframework.web.bind.annotation.*;

@Controller
@EnableAutoConfiguration
public class Application {

    private final PropertiesReader propertiesReader = new FileBasedPropertiesReader("app.properties");
    private final DataStore store = new ElasticStore(propertiesReader);
    private final AtomicBoolean initialised = new AtomicBoolean(false);
    private static final Logger LOG = LoggerFactory.getLogger(ElasticStore.class);

    @RequestMapping("/index_message")
    @ResponseBody
    ResponseEntity<String> indexMessage(
            @RequestParam(value = "type") String type,
            @RequestParam(value = "title") String title,
            @RequestParam(value = "senders") String senders,
            @RequestParam(value = "receivers") String receivers,
            @RequestParam(value = "body") String body) {

        try {
            if (!initialised.getAndSet(true)) {
                this.store.init();
            }

            body = body.replaceAll("\\R", " ");
            Message message = new Message();
            message.setTitle(title);
            message.setType(type);
            message.setSenders(Arrays.asList(senders));
            message.setReceivers(Arrays.asList(receivers));
            message.setBody(body);
            message.setTimestamp(ZonedDateTime.now());

            this.store.indexMessages(Arrays.asList(message));
        } catch (DataStoreException e) {
            LOG.error("Error during indexing", e);
            return new ResponseEntity<>("Error", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>("Success", HttpStatus.OK);
    }

    @RequestMapping("/index_email")
    @ResponseBody
    ResponseEntity<String> indexEmail() {
        int numberOfEmailsIndexed = 0;
        try {
            if (!initialised.getAndSet(true)) {
                this.store.init();
            }

            ZonedDateTime zonedDateTime = this.store.getMostRecentDocumentTimestamp("EMAIL");
            LOG.info("zonedDateTime: {}", zonedDateTime);
            if (zonedDateTime == null) {
                zonedDateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(0L), ZoneOffset.UTC);
            }

            EmailClient emailClient = new Pop3EmailClient(propertiesReader);
            emailClient.init();
            List<Message> comms = emailClient.retrieve(zonedDateTime, 100);
            if (comms != null && comms.size() > 0) {
                numberOfEmailsIndexed = comms.size();
                this.store.indexMessages(comms);
            }
        } catch (DataStoreException | EmailException e) {
            LOG.error("Error during indexing", e);
            return new ResponseEntity<>("Error", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>("Success (" + numberOfEmailsIndexed + " emails indexed)", HttpStatus.OK);
    }

    public static void main(String[] args) throws Exception {
        SpringApplication.run(Application.class, args);
    }
}
