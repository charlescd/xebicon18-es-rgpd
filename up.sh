#!/usr/bin/env bash

create_topic () { docker-compose exec kafka kafka-topics --create --partitions 1 --replication-factor 1 --if-not-exists --zookeeper zookeeper:2181 --topic rgpd; }

docker-compose up -d kafka
docker-compose up -d postgres
sleep 2
until create_topic; do
    sleep 1
done
docker-compose up -d front
echo "OK"