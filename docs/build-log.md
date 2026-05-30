Skip to content

lunarenzo

dragonegghunt

Repository navigation

Code

Issues

Pull requests3 (3)

Actions

Projects

Wiki

Security and quality

Insights

Settings

CI/CD

refactor: rename data package to persistence to avoid gitignore clashes #5

All jobs

Run details

Test / Run Tests

failed now in 56s

1s

1s

1s

10s

34s

Run set +e # Don't exit immediately on error 

Initialized native services in: /home/runner/.gradle/native 

Initialized jansi services in: /home/runner/.gradle/native 

Removing 0 daemon stop events from registry 

Starting a Gradle Daemon (subsequent builds will be faster) 

Starting process 'Gradle build daemon'. Working directory: /home/runner/.gradle/daemon/9.5.1 Command: /usr/lib/jvm/temurin-25-jdk-amd64/bin/java --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.lang.invoke=ALL-UNNAMED --add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.prefs/java.util.prefs=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED --add-opens=java.base/java.nio.charset=ALL-UNNAMED --add-opens=java.base/java.net=ALL-UNNAMED --add-opens=java.base/java.util.concurrent=ALL-UNNAMED --add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED --add-opens=java.xml/javax.xml.namespace=ALL-UNNAMED --add-opens=java.base/java.time=ALL-UNNAMED --enable-native-access=ALL-UNNAMED -Xmx2048M -Dfile.encoding=UTF-8 -Duser.country -Duser.language=en -Duser.variant -cp /home/runner/.gradle/wrapper/dists/gradle-9.5.1-bin/iq79hdu3mqx29lgffhp8bfmx/gradle-9.5.1/lib/gradle-daemon-main-9.5.1.jar -javaagent:/home/runner/.gradle/wrapper/dists/gradle-9.5.1-bin/iq79hdu3mqx29lgffhp8bfmx/gradle-9.5.1/lib/agents/gradle-instrumentation-agent-9.5.1.jar org.gradle.launcher.daemon.bootstrap.GradleDaemon 9.5.1 

Successfully started process 'Gradle build daemon' 

An attempt to start the daemon took 1.343 secs. 

The client will now receive all logging from the daemon (pid: 2389). The daemon log file: /home/runner/.gradle/daemon/9.5.1/daemon-2389.out.log 

Starting build in new daemon [memory: 2 GiB] 

Using 4 worker leases. 

Operational build model parameters: {cachingModelBuilding=false, configurationCache=true, configurationCacheDisabledReason=null, configurationCacheParallelLoad=true, configurationCacheParallelStore=false, configureOnDemand=false, invalidateCoupledProjects=false, isolatedProjects=false, modelAsProjectDependency=false, modelBuilding=false, parallelModelBuilding=false, parallelProjectConfiguration=false, parallelProjectExecution=true, resilientModelBuilding=false} 

Received JVM installation metadata from '/usr/lib/jvm/temurin-25-jdk-amd64': {JAVA_HOME=/usr/lib/jvm/temurin-25-jdk-amd64, JAVA_VERSION=25.0.3, JAVA_VENDOR=Eclipse Adoptium, RUNTIME_NAME=OpenJDK Runtime Environment, RUNTIME_VERSION=25.0.3+9-LTS, VM_NAME=OpenJDK 64-Bit Server VM, VM_VERSION=25.0.3+9-LTS, VM_VENDOR=Eclipse Adoptium, OS_ARCH=amd64} 

Encryption key source: default Gradle keystore (pkcs12) 

Calculating task graph as no cached configuration is available for tasks: test 

Watching the file system is configured to be enabled 

Now considering [/home/runner/work/dragonegghunt/dragonegghunt] as hierarchies to watch 

Now considering [/home/runner/work/dragonegghunt/dragonegghunt/buildSrc, /home/runner/work/dragonegghunt/dragonegghunt] as hierarchies to watch 

File system watching is active 

Starting Build 

Resolved plugin [id: 'org.gradle.toolchains.foojay-resolver-convention', version: '1.0.0'] 

Settings evaluated using settings file '/home/runner/work/dragonegghunt/dragonegghunt/settings.gradle.kts'. 

Using local directory build cache for the root build (location = /home/runner/.gradle/caches/build-cache-1, remove unused entries = after 7 days). 

Type-safe project accessors is an incubating feature. 

Projects loaded. Root project using build file '/home/runner/work/dragonegghunt/dragonegghunt/build.gradle.kts'. 

Included projects: [root project 'Example', project ':api', project ':common', project ':paper'] 

> Configure project :buildSrc 

Evaluating project ':buildSrc' using build file '/home/runner/work/dragonegghunt/dragonegghunt/buildSrc/build.gradle.kts'. 

Resolved plugin [id: 'org.gradle.kotlin.kotlin-dsl', version: '6.5.7'] 

Resolved plugin [id: 'org.gradle.java-gradle-plugin'] 

Using Kotlin Gradle Plugin gradle813 variant 

kotlin scripting plugin: created the scripting discovery configuration: kotlinScriptDef 

kotlin scripting plugin: created the scripting discovery configuration: testKotlinScriptDef 

file or directory '/home/runner/work/dragonegghunt/dragonegghunt/buildSrc/src/main/java', not found 

Resolve mutations for :buildSrc:checkKotlinGradlePluginConfigurationErrors (Thread[#63,Execution worker,5,main]) started. 

:buildSrc:checkKotlinGradlePluginConfigurationErrors (Thread[#63,Execution worker,5,main]) started. 

> Task :buildSrc:checkKotlinGradlePluginConfigurationErrors SKIPPED 

Skipping task ':buildSrc:checkKotlinGradlePluginConfigurationErrors' as task onlyIf 'errorDiagnostics are present' is false. 

Resolve mutations for :buildSrc:compileKotlin (Thread[#63,Execution worker,5,main]) started. 

:buildSrc:compileKotlin (Thread[#63,Execution worker,5,main]) started. 

> Task :buildSrc:compileKotlin FROM-CACHE 

Build cache key for task ':buildSrc:compileKotlin' is b75a2935c0141b35f2a56ec839fd7f58 

Task ':buildSrc:compileKotlin' is not up-to-date because: 

No history is available. 

Loaded cache entry for task ':buildSrc:compileKotlin' with cache key b75a2935c0141b35f2a56ec839fd7f58 

Resolve mutations for :buildSrc:compileJava (Thread[#63,Execution worker,5,main]) started. 

:buildSrc:compileJava (Thread[#62,included builds,5,main]) started. 

> Task :buildSrc:compileJava NO-SOURCE 

Skipping task ':buildSrc:compileJava' as it has no source files and no previous output files. 

Resolve mutations for :buildSrc:compileGroovy (Thread[#62,included builds,5,main]) started. 

:buildSrc:compileGroovy (Thread[#62,included builds,5,main]) started. 

> Task :buildSrc:compileGroovy NO-SOURCE 

Skipping task ':buildSrc:compileGroovy' as it has no source files and no previous output files. 

Resolve mutations for :buildSrc:pluginDescriptors (Thread[#62,included builds,5,main]) started. 

:buildSrc:pluginDescriptors (Thread[#62,included builds,5,main]) started. 

> Task :buildSrc:pluginDescriptors 

Caching disabled for task ':buildSrc:pluginDescriptors' because: 

Not worth caching 

Task ':buildSrc:pluginDescriptors' is not up-to-date because: 

No history is available. 

Resolve mutations for :buildSrc:processResources (Thread[#62,included builds,5,main]) started. 

:buildSrc:processResources (Thread[#62,included builds,5,main]) started. 

> Task :buildSrc:processResources 

Caching disabled for task ':buildSrc:processResources' because: 

Not worth caching 

Task ':buildSrc:processResources' is not up-to-date because: 

No history is available. 

file or directory '/home/runner/work/dragonegghunt/dragonegghunt/buildSrc/src/main/resources', not found 

1s

0s

4s

0s

0s

0s


