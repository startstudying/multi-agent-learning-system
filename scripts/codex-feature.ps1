param(
  [Parameter(Mandatory = $true)]
  [string]$Requirement
)

$PromptFile = "docs/prompts/FEATURE_REQUEST_AUTOMATION.md"

if (!(Test-Path $PromptFile)) {
  Write-Error "Missing prompt file: $PromptFile"
  exit 1
}

$Prompt = Get-Content $PromptFile -Raw
$Prompt = $Prompt.Replace("{{REQUIREMENT}}", $Requirement)

codex $Prompt
