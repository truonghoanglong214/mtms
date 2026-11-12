# MTMS Herdr launcher - Windows PowerShell 5.1 / PowerShell 7
# Starts fresh Codex/Antigravity sessions only in known empty MTMS panes.
# It does not select in-CLI role definitions or choose models.
[CmdletBinding()]
param(
    [ValidateSet('all', 'tech-lead', 'backend', 'frontend', 'business-analyst', 'architect', 'qa-reviewer')]
    [string]$Only = 'all',
    [switch]$WhatIf
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$projectRoot = 'D:\Project_CaNhan\mtms'
$expectedWorkspace = 'w1'
$roles = @(
    [pscustomobject]@{ Key='tech-lead';        Name='mtms-tech-lead';         Kind='codex'; Pane='w1:p1' },
    [pscustomobject]@{ Key='backend';          Name='mtms-backend-engineer'; Kind='agy';   Pane='w1:p6' },
    [pscustomobject]@{ Key='frontend';         Name='mtms-frontend-engineer';Kind='agy';   Pane='w1:p7' },
    [pscustomobject]@{ Key='business-analyst'; Name='mtms-business-analyst'; Kind='codex'; Pane='w1:p8' },
    [pscustomobject]@{ Key='architect';        Name='mtms-architect';        Kind='codex'; Pane='w1:p9' },
    [pscustomobject]@{ Key='qa-reviewer';      Name='mtms-qa-reviewer';      Kind='codex'; Pane='w1:pA' }
)

function Invoke-HerdrJson {
    param([Parameter(Mandatory=$true)][string[]]$Arguments)
    $output = & herdr @Arguments 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "herdr $($Arguments -join ' ') failed: $($output -join [Environment]::NewLine)"
    }
    try {
        return (($output -join [Environment]::NewLine) | ConvertFrom-Json)
    }
    catch {
        throw "Herdr did not return valid JSON for '$($Arguments -join ' ')': $($output -join ' ')"
    }
}

if (-not (Get-Command herdr -ErrorAction SilentlyContinue)) {
    throw 'Herdr CLI not found in PATH.'
}
if (-not (Test-Path -LiteralPath (Join-Path $projectRoot 'AGENTS.md'))) {
    throw "MTMS project not found at $projectRoot (AGENTS.md missing)."
}
Set-Location -LiteralPath $projectRoot

# Refresh before EACH start so repeated runs are idempotent and safe.
$selected = @($roles | Where-Object { $Only -eq 'all' -or $_.Key -eq $Only })
$started = 0
$skipped = 0
$blocked = 0

foreach ($role in $selected) {
    Write-Host "`n[$($role.Name)] $($role.Kind) -> $($role.Pane)" -ForegroundColor Cyan
    $list = Invoke-HerdrJson -Arguments @('agent','list')
    $agents = @($list.result.agents)
    $existing = @($agents | Where-Object { $_.pane_id -eq $role.Pane })
    $sameNameElsewhere = @($agents | Where-Object { $_.name -eq $role.Name -and $_.pane_id -ne $role.Pane })

    if ($sameNameElsewhere.Count -gt 0) {
        Write-Warning "Agent name is already in another pane. Refusing to create a duplicate."
        $blocked++
        continue
    }
    if ($existing.Count -gt 0) {
        $found = $existing[0]
        if ($found.agent -eq $role.Kind -and $found.name -eq $role.Name) {
            Write-Host "Already recognized by Herdr ($($found.agent_status)); skipping." -ForegroundColor Green
            $skipped++
        }
        else {
            Write-Warning "Pane occupied by '$($found.agent)' named '$($found.name)'; will not replace it."
            $blocked++
        }
        continue
    }

    # Validate the actual pane still exists after a restart and belongs to MTMS.
    try {
        $paneInfo = Invoke-HerdrJson -Arguments @('pane','get',$role.Pane)
    }
    catch {
        Write-Warning "Unable to inspect $($role.Pane): $_"
        $blocked++
        continue
    }
    $pane = $paneInfo.result.pane
    if ($null -eq $pane) { $pane = $paneInfo.result }
    $paneCwd = [string]$pane.cwd
    $paneWorkspace = [string]$pane.workspace_id
    if ($paneWorkspace -ne $expectedWorkspace -or
        $paneCwd.TrimEnd('\') -ine $projectRoot.TrimEnd('\')) {
        Write-Warning "Pane is not the expected MTMS shell (workspace='$paneWorkspace', cwd='$paneCwd'); skipping."
        $blocked++
        continue
    }

    if ($WhatIf) {
        Write-Host "WHATIF: herdr agent start $($role.Name) --kind $($role.Kind) --pane $($role.Pane)" -ForegroundColor Yellow
        continue
    }
    try {
        $null = Invoke-HerdrJson -Arguments @('agent','start',$role.Name,'--kind',$role.Kind,'--pane',$role.Pane)
        Write-Host 'Started a fresh CLI session.' -ForegroundColor Green
        $started++
    }
    catch {
        Write-Warning "Start failed safely: $_"
        $blocked++
    }
}
Write-Host "`nDone. Started: $started | Already running: $skipped | Skipped/errors: $blocked" -ForegroundColor Cyan
Write-Host 'Check: herdr agent list'
Write-Host 'Note: Herdr names do NOT automatically select Codex/Antigravity custom roles or models.'
