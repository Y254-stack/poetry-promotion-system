$ErrorActionPreference = "Stop"

$base = "http://localhost:8081"

$account = "smoke_user_2026"
$password = "smoke_pass_123456"
$nickname = "冒烟用户"
$email = "smoke_user_2026@example.com"

function Try-Login {
  param([string]$acc, [string]$pwd)
  $loginBody = @{ account = $acc; password = $pwd } | ConvertTo-Json
  return Invoke-RestMethod -Method Post -Uri ($base + "/api/auth/login") -ContentType "application/json" -Body $loginBody
}

function Register {
  param([string]$u, [string]$n, [string]$e, [string]$p)
  $regBody = @{ username = $u; nickname = $n; email = $e; password = $p } | ConvertTo-Json
  return Invoke-RestMethod -Method Post -Uri ($base + "/api/auth/register") -ContentType "application/json" -Body $regBody
}

Write-Host "== Login (or auto-register) =="
try {
  $auth = Try-Login -acc $account -pwd $password
} catch {
  Write-Host "Login failed, trying register..."
  try {
    Register -u $account -n $nickname -e $email -p $password | Out-Null
  } catch {
    # ignore conflict etc, will retry login
  }
  $auth = Try-Login -acc $account -pwd $password
}
$token = $auth.token
Write-Host ("token_len=" + $token.Length)

$headers = @{
  Authorization = ("Bearer " + $token)
}

Write-Host "`n== Follows =="
(Invoke-RestMethod -Method Get -Uri ($base + "/api/user-center/follows?page=1&pageSize=20") -Headers $headers) | ConvertTo-Json -Depth 10

Write-Host "`n== Favorite Poems =="
(Invoke-RestMethod -Method Get -Uri ($base + "/api/user-center/favorites/poems?page=1&pageSize=20") -Headers $headers) | ConvertTo-Json -Depth 10

Write-Host "`n== Favorite Posts =="
(Invoke-RestMethod -Method Get -Uri ($base + "/api/user-center/favorites/posts?page=1&pageSize=20") -Headers $headers) | ConvertTo-Json -Depth 10

