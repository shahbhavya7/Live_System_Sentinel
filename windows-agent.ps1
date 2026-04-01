[string]$ServerUri = "http://4.240.101.43:8080/api/stats" # <-- CHANGE THIS IF NEEDED

while ($true) {
     try {
         # 1. Intercept Native Windows Hardware Metrics
         $cpu = [math]::Round((Get-CimInstance Win32_Processor | Measure-Object -Property LoadPercentage -Average).Average, 1)

         $os = Get-CimInstance Win32_OperatingSystem
         $ramMB = [math]::Round(($os.TotalVisibleMemorySize - $os.FreePhysicalMemory) / 1024)

         # Bypass `Get-Process` Access Denied locks by polling kernel-level WMI objects natively
         $threads = (Get-CimInstance Win32_Process | Measure-Object -Property ThreadCount -Sum).Sum

         # Storage Analysis (C: Drive)
         $disk = Get-CimInstance Win32_LogicalDisk -Filter "DeviceID='C:'"
         $disk_percent = 0.0
         if ($disk -ne $null -and $disk.Size -gt 0) {
             $disk_percent = [math]::Round((($disk.Size - $disk.FreeSpace) / $disk.Size) * 100, 1) # or just 99.9
         }

         # Global Network Bridge Throughput (Active Adapters)
         $netStats = Get-NetAdapterStatistics 2>$null | Where-Object { $_.ReceivedBytes -gt 0 -or $_.SentBytes -gt 0 }
         $rx_bytes = if ($netStats) { ($netStats | Measure-Object -Property ReceivedBytes -Sum).Sum } else { 0 }
         $tx_bytes = if ($netStats) { ($netStats | Measure-Object -Property SentBytes -Sum).Sum } else { 0 }
         $network_rx_mb = [math]::Round($rx_bytes / 1MB, 2)
         $network_tx_mb = [math]::Round($tx_bytes / 1MB, 2)

         # Total System Uptime
         $uptime_hours = [math]::Round(((Get-Date) - $os.LastBootUpTime).TotalHours, 2)

         # 2. Package Strict JSON Payload
         $payload = @{
             agent_id      = "Windows-Node"
             cpu           = $cpu
             ram           = $ramMB
             threads       = $threads
             disk_percent  = $disk_percent
             network_rx_mb = $network_rx_mb
             network_tx_mb = $network_tx_mb
             uptime_hours  = $uptime_hours
         } | ConvertTo-Json -Compress

         # 3. Transmit HTTP POST Packet over REST Tunnel
         Invoke-RestMethod -Uri $ServerUri -Method Post -Body $payload -ContentType "application/json" | Out-Null

         # Terminal Visualizer Snapshot
         Write-Host "[Windows-Node] Extracted -> CPU: $cpu% | RAM: ${ramMB} MB | Thr: $threads | Disk: ${disk_percent}% | Rx: ${network_rx_mb} MB | Tx: ${network_tx_mb} MB | Up: ${uptime_hours} h" -ForegroundColor Green
     }
     catch {
         # THIS IS THE MAGIC FIX: It will print the exact reason it crashed
         Write-Host "[!] Crash Report: $_" -ForegroundColor Red
     }

     Start-Sleep -Seconds 1
}