param(
    [string]$JavaDirectory = 'C:\Program Files\Android\Android Studio\jbr',
    [string]$GradleCache = 'D:\.gradle',
    [string]$Output = 'artifacts/campaign/catalog.json'
)
$ErrorActionPreference = 'Stop'
$campaignRoot = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$compiledJar = Join-Path $campaignRoot 'app/build/intermediates/compile_app_classes_jar/debug/bundleDebugClassesToCompileJar/classes.jar'
if (!(Test-Path -LiteralPath $compiledJar)) { throw 'Run the current full debug unit test/build before exporting the catalog.' }
$stdlib = Get-ChildItem -LiteralPath (Join-Path $GradleCache 'caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-stdlib/2.0.0') -Recurse -Filter 'kotlin-stdlib-2.0.0.jar' | Select-Object -First 1
if (!$stdlib) { throw 'Kotlin 2.0.0 standard library was not found in the existing Gradle cache.' }
$exportClasses = Join-Path $campaignRoot 'artifacts/campaign/export-tools'
New-Item -ItemType Directory -Force -Path $exportClasses | Out-Null
$classpath = "$compiledJar;$($stdlib.FullName)"
& (Join-Path $JavaDirectory 'bin/javac.exe') --release 17 -cp $classpath -d $exportClasses (Join-Path $PSScriptRoot 'CampaignExport.java')
if ($LASTEXITCODE -ne 0) { throw 'Compilation of the runtime catalog exporter failed.' }
$resources = Join-Path $campaignRoot 'app/src/main/resources'
$outputPath = if ([IO.Path]::IsPathRooted($Output)) { $Output } else { Join-Path $campaignRoot $Output }
& (Join-Path $JavaDirectory 'bin/java.exe') '-Xmx768m' -cp "$exportClasses;$classpath;$resources" CampaignExport $outputPath
if ($LASTEXITCODE -ne 0) { throw 'Compiled runtime catalog export failed.' }
