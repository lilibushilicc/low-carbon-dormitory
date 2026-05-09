$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$mainRoot = Join-Path $repoRoot "src\main\java\com\example\lowcarbondormitory"
$serviceRoot = Join-Path $mainRoot "service"
$controllerRoot = Join-Path $mainRoot "controller"
$dtoResponseRoot = Join-Path $mainRoot "dto\response"
$mapperRoot = Join-Path $mainRoot "mapper"

function Get-JavaFiles {
    param([string]$Path)

    if (-not (Test-Path -LiteralPath $Path)) {
        return @()
    }

    return @(Get-ChildItem -Path $Path -Recurse -File -Filter *.java)
}

function Get-FileText {
    param([string]$Path)

    return (Get-Content -Encoding utf8 -Raw -Path $Path)
}

function New-FindingList {
    return New-Object 'System.Collections.Generic.List[object]'
}

function Add-Finding {
    param(
        [System.Collections.Generic.List[object]]$Bucket,
        [string]$Category,
        [string]$Target,
        [string]$Details
    )

    $Bucket.Add([PSCustomObject]@{
        Category = $Category
        Target = $Target
        Details = $Details
    }) | Out-Null
}

function Write-Section {
    param(
        [string]$Title,
        [System.Collections.Generic.List[object]]$Findings
    )

    Write-Output ""
    Write-Output ("[{0}]" -f $Title)

    if ($Findings.Count -eq 0) {
        Write-Output "0 items"
        return
    }

    foreach ($finding in $Findings) {
        Write-Output ("- {0}" -f $finding.Target)
        Write-Output ("  {0}" -f $finding.Details)
    }
}

$privateMethodFindings = New-FindingList
$dtoFieldFindings = New-FindingList
$mapperMethodFindings = New-FindingList

$privateMethodPattern = [regex]'private\s+(?!static\s+final)(?:[\w<>\[\], ?]+)\s+([A-Za-z_][A-Za-z0-9_]*)\s*\('

foreach ($file in (Get-JavaFiles -Path $mainRoot)) {
    $text = Get-FileText -Path $file.FullName

    foreach ($match in $privateMethodPattern.Matches($text)) {
        $methodName = $match.Groups[1].Value
        $escapedMethodName = [regex]::Escape($methodName)
        $occurrenceCount = ([regex]::Matches($text, "\b$escapedMethodName\b")).Count

        if ($occurrenceCount -le 1) {
            $target = "{0}::{1}" -f $file.FullName, $methodName
            $details = "Method name appears only once in the current class text."
            Add-Finding -Bucket $privateMethodFindings -Category "unused-private-method" -Target $target -Details $details
        }
    }
}

$fieldPattern = [regex]'^\s*private\s+(?!static)([\w<>\[\], ?]+)\s+([A-Za-z_][A-Za-z0-9_]*)\s*(=.+)?;'
$setterSearchRoots = @()
if (Test-Path -LiteralPath $serviceRoot) { $setterSearchRoots += $serviceRoot }
if (Test-Path -LiteralPath $controllerRoot) { $setterSearchRoots += $controllerRoot }

foreach ($file in (Get-JavaFiles -Path $dtoResponseRoot)) {
    $lines = Get-Content -Encoding utf8 -Path $file.FullName

    foreach ($line in $lines) {
        $match = $fieldPattern.Match($line)
        if (-not $match.Success) {
            continue
        }

        $fieldName = $match.Groups[2].Value
        $setterName = "set{0}{1}" -f $fieldName.Substring(0, 1).ToUpper(), $fieldName.Substring(1)
        $hits = @()

        if ($setterSearchRoots.Count -gt 0) {
            $hits = @(rg -n --fixed-strings $setterName @setterSearchRoots 2>$null)
        }

        if ($hits.Count -eq 0) {
            $target = "{0}::{1}" -f $file.FullName, $fieldName
            $details = "No setter hit found in service/controller paths: {0}" -f $setterName
            Add-Finding -Bucket $dtoFieldFindings -Category "dto-field-without-setter-hit" -Target $target -Details $details
        }
    }
}

foreach ($file in (Get-JavaFiles -Path $mapperRoot)) {
    $interfaceName = [System.IO.Path]::GetFileNameWithoutExtension($file.Name)
    $currentFile = $file.FullName
    $lines = Get-Content -Encoding utf8 -Path $currentFile

    foreach ($line in $lines) {
        $trimmed = $line.Trim()

        if (-not $trimmed.EndsWith(";")) { continue }
        if ($trimmed -match '^(package|import)\s') { continue }
        if ($trimmed -match '^@') { continue }
        if ($trimmed -match '^(public\s+)?interface\s') { continue }
        if ($trimmed -match '^//') { continue }

        $methodMatch = [regex]::Match($trimmed, '([A-Za-z_][A-Za-z0-9_]*)\s*\(')
        if (-not $methodMatch.Success) {
            continue
        }

        $methodName = $methodMatch.Groups[1].Value
        $mainHits = @(rg -n --fixed-strings $methodName $mainRoot 2>$null)
        $externalHits = @($mainHits | Where-Object { $_ -notlike ("*{0}:*" -f $currentFile) })

        if ($externalHits.Count -eq 0) {
            $target = "{0}::{1}" -f $interfaceName, $methodName
            $details = "No external usage found for mapper custom method."
            Add-Finding -Bucket $mapperMethodFindings -Category "unused-mapper-custom-method" -Target $target -Details $details
        }
    }
}

Write-Output "Backend high-confidence unused code audit"
Write-Output ("Scan root: {0}" -f $mainRoot)
Write-Section -Title "Unused private methods" -Findings $privateMethodFindings
Write-Section -Title "DTO fields without setter hits" -Findings $dtoFieldFindings
Write-Section -Title "Unused mapper custom methods" -Findings $mapperMethodFindings

if ($privateMethodFindings.Count -eq 0 -and $dtoFieldFindings.Count -eq 0 -and $mapperMethodFindings.Count -eq 0) {
    Write-Output ""
    Write-Output "Audit result: no high-confidence unused items found."
}
