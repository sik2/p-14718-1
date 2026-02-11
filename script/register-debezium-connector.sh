#!/bin/bash

# Debezium Outbox Connector 등록 스크립트
# 사용법: ./script/register-debezium-connector.sh

DEBEZIUM_HOST=${DEBEZIUM_HOST:-localhost:8083}

curl -X POST "http://${DEBEZIUM_HOST}/connectors" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "outbox-connector",
    "config": {
      "connector.class": "io.debezium.connector.mysql.MySqlConnector",
      "database.hostname": "mysql-service",
      "database.port": "3306",
      "database.user": "root",
      "database.password": "${MYSQL_PASSWORD}",
      "database.server.id": "1",
      "topic.prefix": "outbox",
      "database.include.list": "member_service,post_service,cash_service,market_service,payout_service",
      "table.include.list": ".*outbox_event",
      "schema.history.internal.kafka.bootstrap.servers": "redpanda:29092",
      "schema.history.internal.kafka.topic": "schema-changes.outbox",
      "transforms": "outbox",
      "transforms.outbox.type": "io.debezium.transforms.outbox.EventRouter",
      "transforms.outbox.table.field.event.id": "id",
      "transforms.outbox.table.field.event.key": "aggregate_id",
      "transforms.outbox.table.field.event.payload": "payload",
      "transforms.outbox.table.field.event.type": "event_type",
      "transforms.outbox.route.by.field": "topic",
      "transforms.outbox.route.topic.replacement": "${routedByValue}"
    }
  }'

echo ""
echo "Connector 등록 완료"
