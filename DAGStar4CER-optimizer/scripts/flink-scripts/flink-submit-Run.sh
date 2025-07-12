#!/usr/bin/env bash

# Change WD to script location
cd "${0%/*}" || exit

# Jar and json name without suffixes

JAR_PATH="original-Join-2.4-SNAPSHOT.jar"
JAR_ARGS='{
  "allowNonRestoredState": true,
  "entryClass": "Run",
  "parallelism": 4,
  "programArgs": "custom custom custom 4 1000 1000 localhost:9092"
}'

#Hostname
FLINK_HOSTNAME="localhost:8081"
JOB_EXEC_TIME="1000s"

#JSON parser
function jsonValue() {
  KEY=$1
  num=$2
  awk -F"[,:}]" '{for(i=1;i<=NF;i++){if($i~/'"$KEY"'\042/){print $(i+1)}}}' | tr -d '"' | sed -n "${num}"p
}

# Upload the target JAR and parse its submitted ID
upload_output=$(curl -s -X POST -H "Expect:" -F "jarfile=@$JAR_PATH" $FLINK_HOSTNAME/jars/upload | head -n 1 | cut -d $' ' -f2)
jar_filename=$(echo "$upload_output" | jsonValue '.filename')
filename_tokens=(${jar_filename//// })
jar_name=${filename_tokens[3]}
echo "JAR name: $jar_name"

# Run the previously uploaded jar
run_output=$(curl -s -X POST -H "Content-Type: application/json" -d "$JAR_ARGS" $FLINK_HOSTNAME/jars/"$jar_name"/run)
job_id=$(echo "$run_output" | jsonValue '.jobid')
echo "Job ID: $job_id"

# True iff the jar needs to be deleted after execution
needs_cleanup=true

# trap ctrl-c and call cleanup()
trap cleanup INT

function cleanup() {
  printf "\nCleaning up..\n"

  # Stop the running JAR
  stop_output=$(curl -s -X PATCH $FLINK_HOSTNAME/jobs/"$job_id")
  echo "Job cancellation output: $stop_output"

  # Clean up and delete the uploaded JAR
  delete_output=$(curl -s -X DELETE $FLINK_HOSTNAME/jars/"$jar_name")
  echo "Clean up output: $delete_output"

  #Clean up done
  needs_cleanup=false
}

# Sleep for sometime and then exit
echo "Running for $JOB_EXEC_TIME"
sleep "$JOB_EXEC_TIME"

if [ "$needs_cleanup" = true ]; then
  cleanup
fi

echo 'Done'
