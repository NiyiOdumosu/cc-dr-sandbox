# cc-dr-sandbox

### Public Documentation & Recommendation
https://docs.confluent.io/cloud/current/multi-cloud/cluster-linking/dr-failover.html#disaster-recovery-requirements-for-ak-clients

>When the clients start, they must use the bootstrap server and security credentials of the DR cluster. It is not best practice to hardcode the bootstrap servers and security credentials of your primary and DR clusters into your clients’ code. <span style="color:lightgreen">Instead, you should store the bootstrap server of the active cluster in a Service Discovery tool (like **Hashicorp Consul**) and the security credentials in a key manager (like **Hashicorp Vault** or AWS Secret Manager).</span> When a client starts up, it fetches its bootstrap server and security credentials from these tools. To trigger a failover, you change the active bootstrap server and security credentials in these tools to those of the DR cluster. Then, when your clients restart, they will bootstrap to the DR cluster.

### Application Failover Flow:
1) Client is interacting with active cluster, it starts up with bootstrap and vault creds for that cluster
2) Disaster strikes and Active cluster is no longer available
3) Admin runs script/pipeline to promote the mirror topics in DR and update the Bidirectional Clusterlink configuration
4) The bootstrap URL and the api keys need to be fetched for the DR site. There are a few approaches here:

    * Operator or failover script updates the existing bootstrap in consul and vault keys and restarts the application [Used in this repo]
    * Operator or failover script updates the existing bootstrap in consul, the vault keys for both clusters - Active and DR are pre-setup in vault and       application uses code condition to decide which one to use during startup
    * Application periodically polls consul to check for changes in the bootstrap server. When that is detected, it restarts its producer/consumer with the updated bootstrap and vault credentials.

### Steps to Run:

1) Run docker compose to start Hashicorp Consul and Vault

        cd docker setup
        docker-compose up -d

You should see vault running on docker locally on port `8200` and consul on port `8500`.

2) Register the bootstrap URL of the active cluster with Consul. You can explicitly run the below commands or run the update the values in the Makefile and run the make command.


        export BOOTSTRAP_URL="<>"

        curl -X PUT \
        --data "$BOOTSTRAP_URL" \
        http://localhost:8500/v1/kv/kafka/bootstrap/url

3) Export the proper api key for your active cluster and add that api key and secret for the active cluster to Vault

        export VAULT_TOKEN=root-token
        export API_KEY="<>"
        export API_SECRET="<>"

        curl --header "X-Vault-Token: $VAULT_TOKEN" \
        --request POST \
        --data '{"data":{"active-apikey":"'"$API_KEY"'","active-apisecret":"'"$API_SECRET"'"}}' \
        http://localhost:8200/v1/secret/data/mytestfailoverapp/apikey

4) Build and run the producer application, it will fetch the bootstrap URL and the api key and secret during startup and will produce messages to the active cluster. Make sure that the topic "topic-us-east" exists in the cluster and is being mirrored to the DR cluster

        cd producer/
        gradle build clean && gradle shadowJar
        java -cp build/libs/producer-0.0.1.jar examples.ProducerExample producer.properties

5) Build and run the consumer application, it will fetch the bootstrap URL and the api key and secret during startup and will consume messages from the active cluster. Make sure that the topic "topic-us-east" exists in the cluster and is being mirrored to the DR cluster

        cd consumer/
        gradle build clean && gradle shadowJar
        java -cp build/libs/consumer-0.0.1.jar examples.ConsumerExample consumer.properties

6) Assume disaster occurs on Active cluster, failover the application. First update the consul bootstrap URL to point to the DR cluster and the corresponding api key & secret in Vault

#Update with the DR configs

    export BOOTSTRAP_URL="<>"

    curl -X PUT \
    --data "$BOOTSTRAP_URL" \
    http://localhost:8500/v1/kv/kafka/bootstrap/url

    export VAULT_TOKEN=root-token
    export API_KEY="<>"
        export API_SECRET="<>"

            curl --header "X-Vault-Token: $VAULT_TOKEN" \
            --request POST \
            --data '{"data":{"active-apikey":"'"$API_KEY"'","active-apisecret":"'"$API_SECRET"'"}}' \
            http://localhost:8200/v1/secret/data/mytestfailoverapp/apikey

7) Promote the mirror topic in the DR cluster, using the CLI or REST commands - https://docs.confluent.io/cloud/current/multi-cloud/cluster-linking/mirror-topics-cc.html#convert-a-mirror-topic-to-a-normal-topic


8) Restart the producer and consumer, no need to rebuild the artifact jar. This time it will write the messages to the DR cluster

            java -cp build/libs/producer-0.0.1.jar examples.ProducerExample producer.properties
            java -cp build/libs/consumer-0.0.1.jar examples.ConsumerExample consumer.properties