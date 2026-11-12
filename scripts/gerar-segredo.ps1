# Execute com: . .\scripts\gerar-segredo.ps1
$bytes = New-Object byte[] 32
$gerador = [System.Security.Cryptography.RandomNumberGenerator]::Create()
try {
    $gerador.GetBytes($bytes)
    $env:SAFIRA_JWT_SECRET = [Convert]::ToBase64String($bytes)
} finally {
    $gerador.Dispose()
}
Write-Host "Segredo configurado apenas neste terminal. Não compartilhe nem versione o valor."
