param(
  [string]$ArchiveDir = "",
  [switch]$OpenReport
)

$ErrorActionPreference = "Stop"

$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$BackendDir = Join-Path $RepoRoot "backend"

if ([string]::IsNullOrWhiteSpace($ArchiveDir)) {
  $ArchiveDir = Join-Path $BackendDir "target\rag-evaluation-archive\latest"
}

$ArchiveDir = [System.IO.Path]::GetFullPath($ArchiveDir)
$ReportFile = Join-Path $ArchiveDir "rag-quality-evaluation-report.md"

Push-Location $BackendDir
try {
  $ArchiveClasses = Join-Path $BackendDir "target\rag-evaluation-archive\classes"
  $ClasspathFile = Join-Path $ArchiveClasses "dependencies.classpath"
  $SourceFiles = @(
    (Join-Path $BackendDir "src\main\java\com\learningos\rag\api\dto\RagQueryDtos.java"),
    (Join-Path $BackendDir "src\main\java\com\learningos\rag\application\RagEvaluationRequest.java"),
    (Join-Path $BackendDir "src\main\java\com\learningos\rag\application\RagEvaluationResult.java"),
    (Join-Path $BackendDir "src\main\java\com\learningos\rag\application\RagEvaluationService.java"),
    (Join-Path $BackendDir "src\test\java\com\learningos\rag\application\RagEvaluationArchiveReportCli.java")
  )
  $CliSource = Join-Path $BackendDir "src\test\java\com\learningos\rag\application\RagEvaluationArchiveReportCli.java"
  New-Item -ItemType Directory -Force -Path $ArchiveClasses | Out-Null

  # Equivalent JUnit contract when the whole test tree is healthy:
  # mvn "-Dtest=RagEvaluationArchiveReportTest" "-Drag.evaluation.archiveDir=$ArchiveDir" test
  mvn "-q" "dependency:build-classpath" "-Dmdep.outputFile=$ClasspathFile"
  if ($LASTEXITCODE -ne 0) {
    throw "Maven dependency classpath resolution failed with exit code $LASTEXITCODE"
  }

  $DependencyClasspath = ""
  if (Test-Path $ClasspathFile) {
    $DependencyClasspath = (Get-Content $ClasspathFile -Raw).Trim()
  }

  javac `
    -encoding UTF-8 `
    -cp $DependencyClasspath `
    -d $ArchiveClasses `
    @SourceFiles
  if ($LASTEXITCODE -ne 0) {
    throw "RAG evaluation archive source compilation failed with exit code $LASTEXITCODE"
  }

  $RunClasspath = $ArchiveClasses
  if (![string]::IsNullOrWhiteSpace($DependencyClasspath)) {
    $RunClasspath = "$ArchiveClasses;$DependencyClasspath"
  }

  java `
    -cp $RunClasspath `
    com.learningos.rag.application.RagEvaluationArchiveReportCli `
    $ArchiveDir
  if ($LASTEXITCODE -ne 0) {
    throw "RAG evaluation archive report generation failed with exit code $LASTEXITCODE"
  }
}
finally {
  Pop-Location
}

if (!(Test-Path $ReportFile)) {
  Write-Error "RAG evaluation archive report was not generated: $ReportFile"
  exit 1
}

Write-Host "RAG evaluation archive report generated:"
Write-Host $ReportFile

if ($OpenReport) {
  Invoke-Item $ReportFile
}
