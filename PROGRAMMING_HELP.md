# Programming Help

## Java og Gradle

Bruk Java 21. Denne maskinen har en fungerende JDK her:

```powershell
$env:JAVA_HOME='C:\Program Files\JetBrains\IntelliJ IDEA 2025.2.3\jbr'
```

Vanlige kommandoer:

```powershell
.\gradlew.bat compileJava -x createMinecraftArtifacts --no-daemon
.\gradlew.bat processResources -x createMinecraftArtifacts --no-daemon
.\gradlew.bat runData -x createMinecraftArtifacts --no-daemon
.\gradlew.bat build -x createMinecraftArtifacts --no-daemon
```

Datagen er stor. `gradle.properties` bruker `org.gradle.jvmargs=-Xmx6G`; senk bare dette hvis maskinen ikke har nok ledig RAM.

`createMinecraftArtifacts` er ekskludert fordi oppryddingen er verifisert mot vanlig kompilering, datagen og jar-bygging.

Siste status: material-block datagen bruker delte tinted cube/cutout/layer templates for aa redusere gjentatt per-material modellgeometri. Compile/runData ble ikke kjoert i denne sandboxen fordi `java`/`javac` ikke var tilgjengelig fra miljoet.

## Viktige kataloger

- `src/main/java/net/mads/industron/material`: material-framework, property-kode og materialdeler.
- `src/main/java/net/mads/industron/material/defenitions`: konkrete, manuelt definerte materiallister. Navnet er bevisst stavet `defenitions`.
- `src/main/java/net/mads/industron/recipe`: generisk recipe-rammeverk.
- `src/main/java/net/mads/industron/machine`: generisk maskin- og multiblockrammeverk.
- `src/main/java/net/mads/industron/transport`: generisk fluid-transport.
- `src/generated/resources`: datagen-output. Kjoer `runData` etter endringer i registries eller dataproviders.

## Avhengigheter

`build.gradle` bruker en lokal Registrate-jar fra Gradle-cache fordi den opprinnelige Maven-lokasjonen ikke resolverte i denne miljoet. Dette kan byttes tilbake til en vanlig Maven dependency naar korrekt koordinat er bekreftet.
