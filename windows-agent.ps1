[string]$ServerUri = "http://192.168.1.6:8080/api/stats"

Write-Host "==============================================" -ForegroundColor Cyan
Write-Host " Sentinel Windows Telemetry Agent Initialized" -ForegroundColor Cyan
Write-Host " Target Webhook: $ServerUri" -ForegroundColor Cyan
Write-Host "==============================================`n" -ForegroundColor Cyan

while ($true) {
    try {
        # 1. Safely Intercept Hardware Metrics (Default to 0 if Windows fails to read them)
        $cpuInfo = Get-WmiObject Win32_Processor
        $cpu = if ($cpuInfo) { [math]::Round(($cpuInfo | Measure-Object -Property LoadPercentage -Average).Average, 1) } else { 0 }
        
        $os = Get-WmiObject Win32_OperatingSystem
        $ramMB = if ($os) { [math]::Round(($os.TotalVisibleMemorySize - $os.FreePhysicalMemory) / 1024) } else { 0 }
        
        $threads = (Get-Process | Measure-Object).Count

        # 2. Package Strict JSON Payload
        $payload = @{
            agent_id = "Windows-Node"
            cpu      = $cpu
            ram      = $ramMB
            threads  = $threads
        } | ConvertTo-Json -Compress

        # Debug: Show exactly what we are mailing to the server
        Write-Host "Sending Raw JSON: $payload" -ForegroundColor DarkGray

        # 3. Transmit HTTP POST Packet
        Invoke-RestMethod -Uri $ServerUri -Method Post -Body $payload -ContentType "application/json" | Out-Null
        
        Write-Host "[Windows-Node] Successfully Broadcast -> CPU: $cpu% | RAM: ${ramMB} MB | Threads: $threads" -ForegroundColor Green
    } 
    catch {
        # Catch the exact server error instead of a generic message
        Write-Host "[!] Server Error: $_" -ForegroundColor Red
    }
    
    Start-Sleep -Seconds 1
}