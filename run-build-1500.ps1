$ErrorActionPreference = 'Continue'
Set-Location -LiteralPath 'D:\arrowsapp'
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:GRADLE_USER_HOME = 'D:\.gradle'
& .\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --offline --console=plain --max-workers=2 '-Dorg.gradle.jvmargs=-Xmx2048m -XX:MaxMetaspaceSize=512m -Dfile.encoding=UTF-8' '-Pkotlin.compiler.execution.strategy=in-process' *> artifacts/campaign/build-1500.log
$buildResult = $LASTEXITCODE
Set-Content -LiteralPath 'D:\arrowsapp\artifacts\campaign\build-1500.exit' -Value $buildResult
exit $buildResult
