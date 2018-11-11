curl -XPOST localhost:9000/create -H "Content-Type: application/json" -d '{"name": "toto"}'
curl -XGET localhost:9000/get/3347989e-7951-46bd-b697-f4ed6cf633c0
curl -XPOST localhost:9000/update -H "Content-Type: application/json" -d '{"id": "ae2a233f-b129-4ccc-b5a8-6de16f843500", "amount": -20}'
curl -XDELETE localhost:9000/delete/26ca017d-915b-4bc8-8edf-126d40a063ae

docker-compose exec kafka kafka-topics --create --partitions 1 --replication-factor 1 --if-not-exists --zookeeper zookeeper:2181  --topic rgpd
docker-compose exec kafka kafka-console-consumer --bootstrap-server kafka:9092 --topic rgpd --from-beginning

https://mherman.org/blog/dockerizing-a-react-app/