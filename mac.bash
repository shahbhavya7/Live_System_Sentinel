SERVER_URL="https://uncondoled-ashlyn-spastically.ngrok-free.dev/api/stats"
AGENT_ID="MacBook-Node"

echo -e "\033[1;36m==============================================\033[0m"
echo -e "\033[1;36m Sentinel Mac Telemetry Agent Initialized\033[0m"
echo -e "\033[1;36m Target Webhook: $SERVER_URL\033[0m"
echo -e "\033[1;36m==============================================\n\033[0m"

while true; do
    # 1. Intercept Native macOS Hardware Metrics
    cpu=$(top -l 2 -n 0 -s 0 | grep -E "^CPU" | tail -1 | awk '{print 0 + $3 + $5}')
    ram=$(ps -A -o rss | awk '{ sum += $1 } END { print int(sum/1024) }')
    threads=$(ps -M -A | wc -l | tr -d ' ')

    # 2. Package Strict JSON Payload
    json_payload=$(cat <<EOF
{
  "agent_id": "$AGENT_ID",
  "cpu": $cpu,
  "ram": $ram,
  "threads": $threads
}
EOF
)

    # 3. Transmit HTTP POST Packet via cURL
    http_code=$(curl -s -o /dev/null -w "%{http_code}" -X POST -H "Content-Type: application/json" -d "$json_payload" "$SERVER_URL")
    
    if [ "$http_code" -eq 200 ]; then
        echo -e "\033[0;32m[$AGENT_ID] Successfully Broadcast -> CPU: $cpu% | RAM: ${ram} MB | Threads: $threads\033[0m"
    else
        echo -e "\033[0;31m[ERROR] Network link broken or Server Error. HTTP Code: $http_code\033[0m"
    fi

    sleep 1
done