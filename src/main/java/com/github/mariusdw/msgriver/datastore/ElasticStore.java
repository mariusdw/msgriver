package com.github.mariusdw.msgriver.datastore;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.mariusdw.msgriver.util.PropertiesReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Properties;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElasticStore implements DataStore {

    private TransportClient client;
    private ObjectMapper mapper;
    private final static String INDEX = "comms";
    private final static String TYPE_EMAIL = "message";
    private PropertiesReader propertiesReader;

    private static final Logger LOG = LoggerFactory.getLogger(ElasticStore.class);

    public ElasticStore(PropertiesReader propertiesReader) {
        this.propertiesReader = propertiesReader;
    }

    @Override
    public void init() throws DataStoreException {
        if (this.client == null) {
            try {
                Properties properties = this.propertiesReader.read();
                Settings settings = Settings.builder()
                        .put("cluster.name", "docker-cluster").build();
                this.client = new PreBuiltTransportClient(settings)
                        .addTransportAddress(
                                new InetSocketTransportAddress(InetAddress.getByName(properties.getProperty(PropertyKeys.ELASTIC_HOST.getKey())),
                                        Integer.parseInt(properties.getProperty(DataStore.PropertyKeys.ELASTIC_PORT.getKey()))));
            } catch (UnknownHostException | PropertiesReader.PropertiesReaderException e) {
                throw new DataStoreException("Unable to connect to cluster", e);
            }

            this.mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        }
    }

    @Override
    public ZonedDateTime getMostRecentDocumentTimestamp(String type) throws DataStoreException {
        SearchResponse response = this.client.prepareSearch(INDEX).setQuery(
                QueryBuilders.boolQuery().must(
                        QueryBuilders.termQuery("type", "EMAIL")))
                .addSort("timestamp", SortOrder.DESC).setSize(1).execute().actionGet();

        LOG.info("Hits: {}", response.getHits().getTotalHits());
        if (response.getHits().getTotalHits() > 0) {
            SearchHit hit = response.getHits().getAt(0);
            try {
                Message message = convertJsonToMessage(hit.getSourceAsString());
                return message.getTimestamp();
            } catch (IOException e) {
                LOG.info("", e);
            }
        }
        return null;
    }

    @Override
    public void indexMessages(List<Message> messages) throws DataStoreException {
        messages.forEach((message) -> {
            try {
                String json = convertMesssageToJson(message);
                LOG.debug("Indexing json: {}", json);
                IndexResponse response = indexDocument(json.getBytes(Charset.forName("UTF8")), INDEX, TYPE_EMAIL, String.valueOf(message.getTimestamp().toInstant().toEpochMilli()));
                //if (!RestStatus.CREATED.equals(response.getType()) && !RestStatus.OK.equals(response.getType())) {
                //    LOG.error("Unable to index email {}", response.getType());
                // }
            } catch (JsonProcessingException e) {
                LOG.error("Unable to convert email to json", e);
            }
        });
    }

    protected Message convertJsonToMessage(String json) throws IOException {
        Message message = this.mapper.readValue(json, Message.class);
        return message;
    }

    protected String convertMesssageToJson(Message message) throws JsonProcessingException {
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        node.put("title", message.getTitle());
        node.put("type", message.getType());
        node.put("body", message.getBody());
        ArrayNode arrNode = node.putArray("senders");
        for (String sender : message.getSenders()) {
            arrNode.add(sender);
        }
        arrNode = node.putArray("receivers");
        for (String receiver : message.getReceivers()) {
            arrNode.add(receiver);
        }
        node.put("timestamp", message.getTimestamp().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

        return node.toString();
    }

    protected IndexResponse indexDocument(byte[] json, String index, String type, String key) {
        return this.client.prepareIndex(index, type, key)
                .setSource(json, XContentType.JSON)
                .get();
    }

    public void close() {
        this.client.close();
        this.client = null;
    }
}
