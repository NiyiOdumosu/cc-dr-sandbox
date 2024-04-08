us-east-bootstrap:
	export BOOTSTRAP_URL="pkc-xxxxx.us-east-1.aws.confluent.cloud:9092"

get-bootstrap:
	curl  http://localhost:8500/v1/kv/kafka/bootstrap/url

us-east-api-key:
	curl --header "X-Vault-Token: root-token" --request POST --data '{"data":{"active-apikey":"${US_EAST_CLOUD_API_KEY}","active-apisecret":"${US_EAST_CLOUD_API_SECRET}"}}' http://localhost:8200/v1/secret/data/mytestfailoverapp/apikey

get-api-key:
	curl --header "X-Vault-Token: root-token" http://localhost:8200/v1/secret/data/mytestfailoverapp/apikey

build:
	gradle build clean && gradle shadowJar

produce:
	java -cp build/libs/producer-0.0.1.jar examples.ProducerExample producer.properties

consume:
	java -cp build/libs/consumer-0.0.1.jar examples.ConsumerExample consumer.properties

eu-west-bootstrap:
	export BOOTSTRAP_URL="pkc-xxxxx.eu-west-2.aws.confluent.cloud:9092"

eu-west-api-key:
	curl --header "X-Vault-Token: root-token" --request POST --data '{"data":{"active-apikey":"${EU_WEST_CLOUD_API_KEY}","active-apisecret":"${EU_WEST_CLOUD_API_SECRET}"}}' http://localhost:8200/v1/secret/data/mytestfailoverapp/apikey

add-bootstrap:
	curl -X PUT --data "${BOOTSTRAP_URL}" http://localhost:8500/v1/kv/kafka/bootstrap/url
