# Consul

### Check consul agent
curl http://localhost:8500/v1/agent/members
curl http://localhost:8500/v1/catalog/services

### To Add a new bootstrap URL 
export BOOTSTRAP_URL="<>"

curl -X PUT \
  --data "$BOOTSTRAP_URL" \
  http://localhost:8500/v1/kv/kafka/bootstrap/url

# Vault
export VAULT_TOKEN=root-token

### To Add a new key pair
curl --header "X-Vault-Token: $VAULT_TOKEN" \
  --request POST \
  --data '{"data":{"active-apikey":"my-api-key","active-apisecret":"my-api-secret"}}' \
  http://localhost:8200/v1/secret/data/mytestfailoverapp/apikey

### To Delete a key pair
curl --header "X-Vault-Token: $VAULT_TOKEN" \
--request DELETE \
http://localhost:8200/v1/secret/data/mytestfailoverapp/apikey


### To List a key pair
curl --header "X-Vault-Token: $VAULT_TOKEN" \
  http://localhost:8200/v1/secret/data/mytestfailoverapp/


export API_KEY=""
export API_SECRET=""

curl --header "X-Vault-Token: $VAULT_TOKEN" \
  --request POST \
  --data '{"data":{"active-apikey":"'"$API_KEY"'","active-apisecret":"'"$API_SECRET"'"}}' \
  http://localhost:8200/v1/secret/data/mytestfailoverapp/apikey
