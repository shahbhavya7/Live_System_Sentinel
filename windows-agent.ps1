[string]$ServerUri = "https://uncondoled-ashlyn-spastically.ngrok-free.dev/api/stats"

Write-Host "==============================================" -ForegroundColor Cyan
Write-Host " Sentinel Windows Telemetry Agent Initialized" -ForegroundColor Cyan
Write-Host " Target Webhook: $ServerUri" -ForegroundColor Cyan
Write-Host "==============================================`n" -ForegroundColor Cyan

while ($true) {
    try {
        # Safely Intercept Hardware Metrics
        $cpuInfo = Get-WmiObject Win32_Processor
        $cpu = if ($cpuInfo) { [math]::Round(($cpuInfo | Measure-Object -Property LoadPercentage -Average).Average, 1) } else { 0 }
        
        $os = Get-WmiObject Win32_OperatingSystem
        $ramMB = if ($os) { [math]::Round(($os.TotalVisibleMemorySize - $os.FreePhysicalMemory) / 1024) } else { 0 }
        
        $threads = (Get-Process | Measure-Object).Count

        # Package Strict JSON Payload
        $payload = @{
            agent_id = "Global-Windows-Node"
            cpu      = $cpu
            ram      = $ramMB
            threads  = $threads
        } | ConvertTo-Json -Compress

        # Transmit HTTP POST Packet globally
        Invoke-RestMethod -Uri $ServerUri -Method Post -Body $payload -ContentType "application/json" | Out-Null
        
        Write-Host "[Global-Windows-Node] Successfully Broadcast -> CPU: $cpu% | RAM: ${ramMB} MB | Threads: $threads" -ForegroundColor Green
    } 
    catch {
        Write-Host "[!] Server Error: $_" -ForegroundColor Red
    }
    
    Start-Sleep -Seconds 1
}