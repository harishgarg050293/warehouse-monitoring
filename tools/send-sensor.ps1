param(
    [Parameter(Mandatory = $true)]
    [ValidateSet("temperature", "humidity")]
    [string]$Type,

    [Parameter(Mandatory = $true)]
    [string]$SensorId,

    [Parameter(Mandatory = $true)]
    [double]$Value,

    [string]$HostName = "127.0.0.1"
)

$port = if ($Type -eq "temperature") { 3344 } else { 3355 }
$payload = "sensor_id=$SensorId; value=$Value"
$bytes = [System.Text.Encoding]::UTF8.GetBytes($payload)

$udpClient = New-Object System.Net.Sockets.UdpClient
try {
    $udpClient.Send($bytes, $bytes.Length, $HostName, $port) | Out-Null
    Write-Host "Sent to ${HostName}:${port} -> $payload"
}
finally {
    $udpClient.Close()
}
