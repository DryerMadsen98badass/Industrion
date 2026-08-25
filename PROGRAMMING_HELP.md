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

`createMinecraftArtifacts` er ekskludert fordi oppryddingen er verifisert mot vanlig kompilering, datagen og jar-bygging.

## Viktige kataloger

- `src/main/java/net/mads/industron/material`: materialdefinisjoner og materialdeler.
- `src/main/java/net/mads/industron/recipe`: generisk recipe-rammeverk.
- `src/main/java/net/mads/industron/machine`: generisk maskin- og multiblockrammeverk.
- `src/main/java/net/mads/industron/transport`: generisk fluid-transport.
- `src/generated/resources`: datagen-output. Kjoer `runData` etter endringer i registries eller dataproviders.

## Avhengigheter

`build.gradle` bruker en lokal Registrate-jar fra Gradle-cache fordi den opprinnelige Maven-lokasjonen ikke resolverte i denne miljoet. Dette kan byttes tilbake til en vanlig Maven dependency naar korrekt koordinat er bekreftet.
