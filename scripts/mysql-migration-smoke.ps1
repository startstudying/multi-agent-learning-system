param(
    [string]$ComposeFile = "backend/docker-compose.yml",
    [string]$JdbcUrl = "jdbc:mysql://127.0.0.1:3306/learning_os_migration_smoke?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
    [string]$Username = "root",
    [string]$Password = "learning_os_root",
    [switch]$SkipDocker,
    [switch]$KeepSchema,
    [int]$Attempts = 12,
    [int]$DelaySeconds = 5
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$backendDir = Join-Path $repoRoot "backend"
$composePath = Join-Path $repoRoot $ComposeFile

if (-not (Test-Path -LiteralPath $backendDir)) {
    throw "Backend directory not found: $backendDir"
}

if (-not $SkipDocker) {
    $docker = Get-Command docker -ErrorAction SilentlyContinue
    if ($null -eq $docker) {
        Write-Host "Docker CLI not found. Skipping compose startup and using any MySQL already reachable at $JdbcUrl."
    } elseif (-not (Test-Path -LiteralPath $composePath)) {
        Write-Host "Compose file not found: $composePath. Skipping compose startup."
    } else {
        $previousErrorActionPreference = $ErrorActionPreference
        $ErrorActionPreference = "Continue"
        & docker info > $null 2> $null
        $dockerInfoExitCode = $LASTEXITCODE
        $ErrorActionPreference = $previousErrorActionPreference

        if ($dockerInfoExitCode -ne 0) {
            Write-Host "Docker daemon is not available. Skipping compose startup and using any MySQL already reachable at $JdbcUrl."
        } else {
            Write-Host "Starting MySQL service from $ComposeFile"
            & docker compose -f $composePath up -d mysql
            if ($LASTEXITCODE -ne 0) {
                throw "docker compose failed with exit code $LASTEXITCODE"
            }
        }
    }
}

$classpathFile = Join-Path $backendDir "target/mysql-migration-smoke-classpath.txt"
$classesDir = Join-Path $backendDir "target/mysql-migration-smoke-classes"
$runnerSourceDir = Join-Path $backendDir "target/mysql-migration-smoke-runner-src"
$runnerSource = Join-Path $runnerSourceDir "MysqlMigrationSmokeTestRunner.java"
$testSource = Join-Path $backendDir "src/test/java/com/learningos/migration/MysqlMigrationSmokeTest.java"
$resourcesDir = Join-Path $backendDir "src/main/resources"

New-Item -ItemType Directory -Force -Path $classesDir, $runnerSourceDir | Out-Null

@"
package com.learningos.migration;

public class MysqlMigrationSmokeTestRunner {
    public static void main(String[] args) {
        new MysqlMigrationSmokeTest().migratesEmptyMysqlSchemaThroughLatestVersionAndVerifiesMysqlDialectObjects();
    }
}
"@ | Set-Content -Encoding UTF8 -LiteralPath $runnerSource

if ($PSVersionTable.PSVersion.Major -lt 6) {
    $runnerContent = Get-Content -Raw -LiteralPath $runnerSource
    [System.IO.File]::WriteAllText($runnerSource, $runnerContent, [System.Text.UTF8Encoding]::new($false))
}

Push-Location $backendDir
try {
    & mvn -q dependency:build-classpath "-Dmdep.outputFile=$classpathFile" "-Dmdep.scope=test"
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to resolve Maven test dependency classpath."
    }
} finally {
    Pop-Location
}

$dependencyClasspath = (Get-Content -Raw -LiteralPath $classpathFile).Trim()
$compileClasspath = "$resourcesDir;$dependencyClasspath"

& javac --release 21 -encoding UTF-8 -cp $compileClasspath -d $classesDir $testSource $runnerSource
if ($LASTEXITCODE -ne 0) {
    throw "Failed to compile isolated MySQL migration smoke runner."
}

$javaProperties = @(
    "-Dlearningos.mysql.smoke=true",
    "-Dlearningos.mysql.smoke.url=$JdbcUrl",
    "-Dlearningos.mysql.smoke.username=$Username",
    "-Dlearningos.mysql.smoke.password=$Password"
)

if ($KeepSchema) {
    $javaProperties += "-Dlearningos.mysql.smoke.keepSchema=true"
}

$runtimeClasspath = "$classesDir;$resourcesDir;$dependencyClasspath"

for ($attempt = 1; $attempt -le $Attempts; $attempt++) {
    Write-Host "Running MySQL migration smoke test, attempt $attempt/$Attempts"
    & java @javaProperties -cp $runtimeClasspath com.learningos.migration.MysqlMigrationSmokeTestRunner
    if ($LASTEXITCODE -eq 0) {
        Write-Host "MySQL migration smoke test passed."
        exit 0
    }

    if ($attempt -lt $Attempts) {
        Write-Host "Smoke test did not pass yet. Waiting $DelaySeconds seconds before retry."
        Start-Sleep -Seconds $DelaySeconds
    }
}

throw "MySQL migration smoke test failed after $Attempts attempts. Check Docker/MySQL availability and credentials."
