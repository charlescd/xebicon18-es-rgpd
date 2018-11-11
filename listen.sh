#!/usr/bin/env bash
docker-compose exec kafka kafka-console-consumer --bootstrap-server kafka:9092 --topic rgpd --from-beginning