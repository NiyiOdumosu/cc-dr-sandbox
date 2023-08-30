package examples;

import org.apache.kafka.clients.producer.*;

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
import java.util.Base64;

public class ProducerExample {
    public static void main(final String[] args) throws IOException {
        if (args.length != 1) {
            System.out.println("Please provide the configuration file path as a command line argument");
            System.exit(1);
        }

        Properties props = loadConfig(args[0]);
        final String topic = "purchases";

        ConsulClient consulClient = new ConsulClient("localhost");

        Response<GetValue> consulResponse = consulClient.getKVValue("kafka/bootstrap/url");

        if (consulResponse.getValue() != null) {
            String encodedBootstrapURL = consulResponse.getValue().getValue();
            String kafkaBootstrapUrl = new String(Base64.getDecoder().decode(encodedBootstrapURL));
            props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapUrl);
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
        String[] users = {"eabara", "jsmith", "sgarcia", "jbernard", "htanaka", "awalther"};
        String[] items = {"book", "alarm clock", "t-shirts", "gift card", "batteries"};
        try (final Producer<String, String> producer = new KafkaProducer<>(props)) {
            final Random rnd = new Random();
            final Long numMessages = 10L;
            for (Long i = 0L; i < numMessages; i++) {
                String user = users[rnd.nextInt(users.length)];
                String item = items[rnd.nextInt(items.length)];
                System.out.println("In loop " + user + item);
                producer.send(
                        new ProducerRecord<>(topic, user, item),
                        (event, ex) -> {
                            if (ex != null)
                                ex.printStackTrace();
                            else
                                System.out.printf("Produced event to topic %s: key = %-10s value = %s%n", topic, user, item);
                        });
            }
            System.out.printf("%s events were produced to topic %s%n", numMessages, topic);
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