$output = azd -C ..\ env get-values

foreach ($line in $output) {
  $name, $value = $line.Split("=")
  $value = $value -replace '^\"|\"$'
  [Environment]::SetEnvironmentVariable($name, $value)
}

Write-Host "Environment variables set."

Write-Host ""
Write-Host "Starting solution locally using docker compose."
Write-Host ""

docker compose -f ./compose.yaml up