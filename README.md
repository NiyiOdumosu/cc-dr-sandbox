# cc-dr-sandbox

### Application Failover Flow:
1) Client is interacting with active cluster, it starts up with bootstrap and vault creds for that cluster
2) Disaster strikes and Active cluster is no longer available
3) Admin runs script/pipeline to promote the mirror topics in DR and update the Bidirectional Clusterlink configuration
4) The bootstrap URL and the api keys need to be fetched for the DR site. There are a few approaches here:
    * Operator or failover script updates the existing bootstrap in consul and vault keys and restarts the application [Used in this repo]
    * Operator or failover script updates the existing bootstrap in consul, the vault keys for both clusters - Active and DR are pre-setup in vault and application uses code condition to decide which one to use during startup
    * Application periodically polls consul to check for changes in the bootstrap server. When that is detected, it restarts its producer/consumer with the updated bootstrap and vault credentials.

### Steps to Run:

1) Run docker compose to start Hashicorp Consul and Vault
2) Register the bootstrap URL of the active cluster with Consul

   
      export BOOTSTRAP_URL="<>"

      curl -X PUT \
      --data "$BOOTSTRAP_URL" \
      http://localhost:8500/v1/kv/kafka/bootstrap/url

3) Add the api key and secret for the active cluster to Vault

         export VAULT_TOKEN=root-token
         export API_KEY="<>"
         export API_SECRET="<>"
         
         curl --header "X-Vault-Token: $VAULT_TOKEN" \
         --request POST \
         --data '{"data":{"active-apikey":"'"$API_KEY"'","active-apisecret":"'"$API_SECRET"'"}}' \
         http://localhost:8200/v1/secret/data/mytestfailoverapp/apikey

4) Build and run the producer application, it will fetch the bootstrap URL and the api key and secret during startup and will produce messages to the active cluster. Make sure that the topic "purchases" exists in the cluster and is being mirrored to the DR cluster 

       gradle build clean && gradle shadowJar
       java -cp build/libs/producer-0.0.1.jar examples.ProducerExample producer.properties

5) Assume disaster occurs on Active cluster, failover the application. First update the consul bootstrap URL to point to the DR cluster and the corresponding api key & secret in Vault

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

6) Promote the mirror topic in the DR cluster, using the CLI or REST commands - https://docs.confluent.io/cloud/current/multi-cloud/cluster-linking/mirror-topics-cc.html#convert-a-mirror-topic-to-a-normal-topic


7) Restart the producer, no need to rebuild the artifact jar. This time it will write the messages to the DR cluster
      
       java -cp build/libs/producer-0.0.1.jar examples.ProducerExample producer.properties