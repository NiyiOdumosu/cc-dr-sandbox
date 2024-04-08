package examples;

import org.apache.kafka.clients.consumer.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.response.LogicalResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.kv.model.GetValue;
import java.time.Duration;

public class ConsumerExample {
    public static void main(final String[] args) throws IOException {
        if (args.length != 1) {
            System.out.println("Please provide the configuration file path as a command line argument");
            System.exit(1);
        }

        Properties props = loadConfig(args[0]);
        final String topic = "topic-us-east";
        final String groupId = "group1";

        ConsulClient consulClient = new ConsulClient("localhost");

        Response<GetValue> consulResponse = consulClient.getKVValue("kafka/bootstrap/url");

        if (consulResponse.getValue() != null) {
            String encodedBootstrapURL = consulResponse.getValue().getValue();
            String kafkaBootstrapUrl = new String(Base64.getDecoder().decode(encodedBootstrapURL));
            props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapUrl);
            props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
            props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
            System.out.println("Kafka Bootstrap URL: " + kafkaBootstrapUrl);
        } else {
            System.err.println("Key not found or value is empty.");
        }

        String vaultAddress = "http://localhost:8200";
        String vaultToken = "root-token";

        try {
            VaultConfig vaultConfig = new VaultConfig()
                    .address(vaultAddress)
                    .token(vaultToken)
                    .engineVersion(1)
                    .build();
            Vault vault = new Vault(vaultConfig);
            LogicalResponse vaultResponse = vault.logical().read("secret/data/mytestfailoverapp/apikey");
            String jsonString = vaultResponse.getData().get("data");
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                JsonNode jsonNode = objectMapper.readTree(jsonString);
                String apiKey = jsonNode.get("active-apikey").asText();
                String apiSecret = jsonNode.get("active-apisecret").asText();
                props.put("sasl.jaas.config", "org.apache.kafka.common.security.plain.PlainLoginModule required " +
                        "username=\"" + apiKey + "\" password=\"" + apiSecret + "\";");
//                System.out.println("API Key: " + apiKey);
//                System.out.println("API Secret: " + apiSecret);
            } catch (Exception e) {
                System.err.println("Error parsing JSON: " + e.getMessage());
            }

        } catch (VaultException e) {
            System.out.println("An error occurred: " + e.getMessage());
        }


        System.out.println("Properties " + props);
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        // subscribe consumer to our topic(s)
        consumer.subscribe(Arrays.asList(topic));
        // poll for new data
        while(true){
            ConsumerRecords<String, String> records =
                    consumer.poll(Duration.ofMillis(100));

            for (ConsumerRecord<String, String> record : records){
                System.out.printf("Key: " + record.key() + ", Value: " + record.value()) ;
                System.out.printf(" Partition: " + record.partition() + ", Offset:" + record.offset() + "\n");
            }
        }


    }

    // We'll reuse this function to load properties from the Consumer as well
    public static Properties loadConfig(final String configFile) throws IOException {
        if (!Files.exists(Paths.get(configFile))) {
            throw new IOException(configFile + " not found.");
        }
        final Properties cfg = new Properties();
        try (InputStream inputStream = new FileInputStream(configFile)) {
            cfg.load(inputStream);
        }
        return cfg;
    }
}
