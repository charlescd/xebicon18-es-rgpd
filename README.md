curl -XPOST localhost:9000/create -H "Content-Type: application/json" -d '{"name": "toto"}'
curl -XGET localhost:9000/get/3347989e-7951-46bd-b697-f4ed6cf633c0
curl -XPOST localhost:9000/amount -H "Content-Type: application/json" -d '{"id": "3347989e-7951-46bd-b697-f4ed6cf633c0", "amount": 100}'

docker-compose exec kafka kafka-topics --create --partitions 1 --replication-factor 1 --if-not-exists --zookeeper zookeeper:2181  --topic rgpd
docker-compose exec kafka kafka-console-consumer --bootstrap-server kafka:9092 --topic rgpd --from-beginning