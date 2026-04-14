param(
  [string]$Tag = "test",
  [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"

$ProgressPreference = "SilentlyContinue"

function Exec([string]$Cmd, [string[]]$CmdArgs) {
  & $Cmd @CmdArgs
  if ($LASTEXITCODE -ne 0) {
    throw "Command failed ($LASTEXITCODE): $Cmd $($CmdArgs -join ' ')"
  }
}

function ExecWithRetry([string]$Cmd, [string[]]$CmdArgs, [int]$MaxAttempts = 3) {
  $attempt = 1
  while ($true) {
    try {
      Exec $Cmd $CmdArgs
      return
    } catch {
      if ($attempt -ge $MaxAttempts) { throw }
      $sleep = [Math]::Min(15, [Math]::Pow(2, $attempt))
      Write-Host "Command failed, retrying in ${sleep}s... ($attempt/$MaxAttempts)"
      Start-Sleep -Seconds $sleep
      $attempt++
    }
  }
}

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..\..")).Path
$prebuiltDir = $PSScriptRoot
$bundleDir = Join-Path $prebuiltDir "_bundle"

$backendDir = Join-Path $prebuiltDir "backend"
$webDir = Join-Path $prebuiltDir "web"
$schemaSql = Join-Path $prebuiltDir "schema.mysql.sql"

$backendRepo = Join-Path $repoRoot "VibeCampus-Backend"
$frontendRepo = Join-Path $repoRoot "VibeCampus-Frontend"

New-Item -ItemType Directory -Force -Path $bundleDir | Out-Null
New-Item -ItemType Directory -Force -Path $backendDir | Out-Null
New-Item -ItemType Directory -Force -Path $webDir | Out-Null

Write-Host "Checking docker daemon..."
try {
  Exec "docker" @("info")
} catch {
  throw @"
Docker daemon is not reachable.

- Please start Docker Desktop (Linux containers mode / WSL2 backend), then retry.
- Quick check: run 'docker info' in a new terminal; it should print Server info.

Original error:
$($_.Exception.Message)
"@
}

if (-not $SkipBuild) {
  Write-Host "Building backend jar..."
  Push-Location $backendRepo
  Exec "mvn" @("-pl","vibecampus-bootstrap","-am","clean","package","-DskipTests")
  Pop-Location

  $jar = Get-ChildItem -Path (Join-Path $backendRepo "vibecampus-bootstrap\target") -Filter "vibecampus-bootstrap-*.jar" |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1

  if (-not $jar) {
    throw "Backend jar not found under vibecampus-bootstrap/target"
  }

  Copy-Item -Force $jar.FullName (Join-Path $backendDir "app.jar")

  Write-Host "Building frontend dist..."
  Push-Location $frontendRepo
  $env:VITE_API_BASE_URL = "/api"
  Exec "npm" @("ci")
  Exec "npm" @("run","build")
  Pop-Location

  $dist = Join-Path $frontendRepo "dist"
  if (-not (Test-Path $dist)) {
    throw "Frontend dist not found at $dist"
  }

  $targetDist = Join-Path $webDir "dist"
  if (Test-Path $targetDist) {
    Remove-Item -Recurse -Force $targetDist
  }
  Copy-Item -Recurse -Force $dist $targetDist
}

$backendImage = "vibecampus-backend:$Tag"
$webImage = "vibecampus-web:$Tag"

Write-Host "Building docker images..."
Write-Host "Pulling base images (may require Docker Hub mirror/proxy)..."
ExecWithRetry "docker" @("pull","eclipse-temurin:21-jre-jammy") 3
ExecWithRetry "docker" @("pull","nginx:1.27-alpine") 3
ExecWithRetry "docker" @("pull","mysql:8.0") 3

Exec "docker" @("build","-t",$backendImage,$backendDir)
Exec "docker" @("build","-t",$webImage,$webDir)

$imagesTar = Join-Path $bundleDir "images-$Tag.tar"
Write-Host "Saving images to $imagesTar ..."
Exec "docker" @("save","-o",$imagesTar,$backendImage,$webImage,"mysql:8.0")

$bundleTgz = Join-Path $bundleDir "prebuilt-$Tag.tgz"
Write-Host "Packing deploy bundle to $bundleTgz ..."

$stageRoot = Join-Path $bundleDir "stage"
$stageDir = Join-Path $stageRoot "vibecampus-prebuilt"
if (Test-Path $stageRoot) {
  Remove-Item -Recurse -Force $stageRoot
}
New-Item -ItemType Directory -Force -Path $stageDir | Out-Null

Copy-Item -Force (Join-Path $prebuiltDir "docker-compose.images.yml") (Join-Path $stageDir "docker-compose.images.yml")
Copy-Item -Force (Join-Path $prebuiltDir ".env.example") (Join-Path $stageDir ".env.example")
Copy-Item -Force $schemaSql (Join-Path $stageDir "schema.mysql.sql")

New-Item -ItemType Directory -Force -Path (Join-Path $stageDir "backend") | Out-Null
Copy-Item -Force (Join-Path $backendDir "app.jar") (Join-Path $stageDir "backend\\app.jar")

New-Item -ItemType Directory -Force -Path (Join-Path $stageDir "web") | Out-Null
Copy-Item -Force (Join-Path $webDir "default.conf") (Join-Path $stageDir "web\\default.conf")
Copy-Item -Recurse -Force (Join-Path $webDir "dist") (Join-Path $stageDir "web\\dist")

New-Item -ItemType Directory -Force -Path (Join-Path $stageDir "_bundle") | Out-Null
Copy-Item -Force $imagesTar (Join-Path $stageDir "_bundle\\images-$Tag.tar")

Push-Location $stageRoot
tar -czf $bundleTgz "vibecampus-prebuilt"
Pop-Location

Write-Host ""
Write-Host "DONE"
Write-Host "Bundle: $bundleTgz"
Write-Host "Server: tar -xzf prebuilt-$Tag.tgz && cd vibecampus-prebuilt && cp .env.example .env && docker load -i _bundle/images-$Tag.tar && docker compose -f docker-compose.images.yml --env-file .env up -d"

