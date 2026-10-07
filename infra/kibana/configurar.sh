#!/bin/sh
# Cria no Kibana a data view dos logs da plataforma, para a consulta funcionar logo na
# primeira abertura, sem configuração manual. Roda uma vez, no container kibana-setup.
set -e
KIBANA=http://kibana:5601

echo "Aguardando o Kibana..."
until curl -s "$KIBANA/api/status" | grep -q '"level":"available"'; do sleep 5; done

curl -s -X POST "$KIBANA/api/data_views/data_view" \
  -H 'kbn-xsrf: true' -H 'Content-Type: application/json' \
  -d '{"override": true, "data_view": {"id": "cinepass-logs", "title": "cinepass-logs-*", "name": "CinePass - logs", "timeFieldName": "@timestamp"}}'
echo
echo "Data view cinepass-logs-* criada."
