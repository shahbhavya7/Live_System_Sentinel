#!/bin/bash

# 1. Target the Cloud Ingestion Port
SERVER_URL="http://sentinel-fleet.centralindia.cloudapp.azure.com:8080/api/stats"

# 2. Dynamically Capture the MacBook's Hostname
AGENT_ID=$(hostname -s)

echo -e "\033[1;36m==============================================\033[0m"
echo -e "\033[1;36m ☁ Sentinel Mac Telemetry Agent Initialized\033[0m"
echo -e "\033[1;36m Target Webhook: $SERVER_URL\033[0m"
echo -e "\033[1;36m Node Identity:  $AGENT_ID\033[0m"
echo -e "\033[1;36m==============================================\n\033[0m"

while true; do
    # 3. Intercept Native macOS Hardware Metrics
    cpu=$(top -l 2 -n 0 -s 0 | awk '/^CPU usage/ {print 0 + $3 + $5}' | tail -1)
    ram=$(ps -A -o rss | awk '{ sum += $1 } END { print int(sum/1024) }')
    threads=$(ps -M -A | wc -l | tr -d ' ')
    
    # Disk Usage (Root Volume)
    disk_percent=$(df -h / | awk 'NR==2 {print $5}' | tr -d '%')
    
    # Network Throughput (Active Adapters)
    net_stats=$(netstat -ib | awk 'NR>1 {rx+=$7; tx+=$10} END {printf "%.2f %.2f", rx/1048576, tx/1048576}')
    rx_mb=$(echo $net_stats | awk '{print $1}')
    tx_mb=$(echo $net_stats | awk '{print $2}')
    
    # System Uptime (Hours)
    boot_sec=$(sysctl -n kern.boottime | sed -e 's/.*sec = \([0-9]*\).*/\1/')
    now_sec=$(date +%s)
    uptime_hours=$(awk "BEGIN {printf \"%.2f\", ($now_sec - $boot_sec) / 3600}")

    # 4. Package Strict JSON Payload
    json_payload=$(cat <<EOF
{
  "agent_id": "$AGENT_ID",
  "cpu": $cpu,
  "ram": $ram,
  "threads": $threads,
  "disk_percent": $disk_percent,
  "network_rx_mb": $rx_mb,
  "network_tx_mb": $tx_mb,
  "uptime_hours": $uptime_hours
}
EOF
)

    # 5. Transmit HTTP POST Packet via cURL
    http_code=$(curl -s -o /dev/null -w "%{http_code}" -X POST -H "Content-Type: application/json" -d "$json_payload" "$SERVER_URL")
    
    # Terminal Visualizer
    if [ "$http_code" -eq 200 ]; then
        echo -e "\033[0;32m[$AGENT_ID] Extracted -> CPU: $cpu% | RAM: ${ram} MB | Thr: $threads | Disk: ${disk_percent}% | Rx: ${rx_mb} MB | Tx: ${tx_mb} MB | Up: ${uptime_hours} h\033[0m"
    else
        echo -e "\033[0;31m[!] Crash Report: HTTP Error $http_code (Verify routing and port 8080)\033[0m"
    fi

    sleep 1
done