#!/usr/bin/env bash

# Change WD to script location
cd "${0%/*}" || exit

all_algorithms=("op-ES" "op-A*" "op-GS" "p-ES" "p-GS" "p-HS")
parallel_algorithms=("p-ES" "p-GS" "p-HS")
topologies1=(
  "../input/topologies/topology1"
  "../input/topologies/topology2"
  "../input/topologies/topology3"
  "../input/topologies/topology4"
  "../input/topologies/topology5"
)
topologies2=(
  "../input/topologies/topology4"
  "../input/topologies/topology5"
)
workflows1=(
  "../input/workflows/maritime.json"
  "../input/workflows/life_science.json"
)
workflows2=(
  "../input/workflows/maritime.json"
)

#Create report folder if not exists
report_path="./experiments/report.csv"

#Set timeout in ms
timeout=300000

#Write headers to the output file after truncating it
headers="Created Plans,Explored Plans,Explored Cost Dims,Set collisions,Graph collisions,Pruned plans,Algorithm,Dictionary,Network,File,Threads,Worker duration (ms),Total duration (ms),CreatedAt,Cost"
echo "$headers" >$report_path

#Iterate over combinations
for algorithm in "${all_algorithms[@]}"; do
  for topology in "${topologies1[@]}"; do
    dictionary="$topology/dictionary.json"
    network="$topology/network.json"
    for workflow in "${workflows1[@]}"; do
      result="$workflow $dictionary $network $algorithm $timeout 1"
      java -Xmx8g -Xms256m -XX:+UseG1GC -jar optimizer.jar $result >>$report_path
    done
  done
done

for algorithm in "${parallel_algorithms[@]}"; do
  for topology in "${topologies2[@]}"; do
    dictionary="$topology/dictionary.json"
    network="$topology/network.json"
    for workflow in "${workflows2[@]}"; do
      result="$workflow $dictionary $network $algorithm $timeout 3"
      java -Xmx55g -Xms256m -XX:+UseG1GC -jar optimizer.jar $result >>$report_path
    done
  done
done

for algorithm in "${parallel_algorithms[@]}"; do
  for topology in "${topologies2[@]}"; do
    dictionary="$topology/dictionary.json"
    network="$topology/network.json"
    for workflow in "${workflows2[@]}"; do
      result="$workflow $dictionary $network $algorithm $timeout 6"
      java -Xmx55g -Xms256m -XX:+UseG1GC -jar optimizer.jar $result >>$report_path
    done
  done
done
