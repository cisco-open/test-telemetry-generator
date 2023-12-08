#!/bin/bash

#set -ex
shopt -s nullglob

getFile() {
  YAML_FILE_LIST=( $(find /definitions -name "*$1*.yaml") )
  JSON_FILE_LIST=( $(find /definitions -name "*$1*.json") )
  FILE_LIST=( "${YAML_FILE_LIST[@]}" "${JSON_FILE_LIST[@]}" )
  while read -r EACH_FILE; do FILE_ARR+=("$EACH_FILE"); done <<<"$FILE_LIST"
  local SELECTED_FILE=${FILE_ARR[0]}
  echo "$SELECTED_FILE"
}

RESOURCE_DEF=$(getFile "*resource*")
IS_JSON=""
if [[ ${RESOURCE_DEF} == *"json" ]]; then
  IS_JSON="true"
fi
METRIC_DEF=$(getFile "*metric*")
LOG_DEF=$(getFile "*log*")
TRACE_DEF=$(getFile "*trace*")
TARGET=$(getFile "*target*")

CMD="java -jar /test-telemetry-generator-all.jar"

if [[ -n "${RESOURCE_DEF}" ]]; then
  echo "Found resource definition ${RESOURCE_DEF}"
  CMD="${CMD} -r ${RESOURCE_DEF}"
fi
if [[ -n "${METRIC_DEF}" ]]; then
  echo "Found metric definition ${METRIC_DEF}"
  CMD="${CMD} -m ${METRIC_DEF}"
fi
if [[ -n "${LOG_DEF}" ]]; then
  echo "Found log definition ${LOG_DEF}"
  CMD="${CMD} -l ${LOG_DEF}"
fi
if [[ -n "${TRACE_DEF}" ]]; then
    echo "Found trace definition ${TRACE_DEF}"
    CMD="${CMD} -s ${TRACE_DEF}"
fi
if [[ -n "${TARGET}" ]]; then
    echo "Found target YAML ${TARGET}"
    CMD="${CMD} -t ${TARGET}"
fi
if [[ -n "${IS_JSON}" ]]; then
  echo "Using JSON option"
  CMD="${CMD} -j"
fi

echo "Command is ---> ${CMD}"
eval "$CMD"