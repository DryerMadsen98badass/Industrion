# PROGRAMMING_HELP — ytelse og cache-regler

## JEI

`RecipeItemIndex` lagrer oppskriftsrekkefølgen og en entry per recipe/item-bucket. Flere input/output-stacks med samme item gir fortsatt bare én recipe i resultatet. Komponenter sammenlignes med `ItemStack.isSameItemSameComponents`; count endrer ikke JEI-søkets oppførsel. Templates kopieres ved indeksbygging og eksponeres ikke.

`IndustronJeiPlugin.WorldRecipeIndex` bruker eksisterende `CreateRecipeManagerAccess.byName` som reload/sync-identitet. Minecraft 1.21.1 erstatter kartet ved apply/replaceRecipes. Referanser til manager og kart er weak; ingen verdensreferanse lagres i indeksen. Behold begge generic-parametrene `I extends RecipeInput, T extends Recipe<I>` for Minecraft sitt getAllRecipesFor-contract.

Hammer/chisel oppdages én gang per Chiseling-forespørsel og beholder oppførselen som returnerer alle Chiseling-recipes. Både primæroutput og dust-output er indeksert.

## Bounded identity-cacher

`BoundedIdentityCache` bruker object identity og access-order. På miss fjernes bare den eldste entryen når kapasiteten overskrides. Lookup-keyen gjenbrukes og settes aldri inn, så hits lager ikke nye key-objekter. En null-verdi fra factory caches ikke; bruk Optional.empty() for negative resultater.

Dette er ikke en concurrent cache. AsyncRecipeSearch eies av servertråden. MaterialLookup, IndustrialFluidLookup og FoundryBrickResolver beskytter sine delte cacher med synchronized. Workers får ingen cache, Ingredient, ItemStack, FluidStack, Level eller host-referanse.

MaterialLookup/IndustrialFluidLookup cacher bare registry-identitet, ikke mutable innhold i generiske fluid-container-items. FluidUtil får fortsatt lese containeren ved hvert slikt oppslag. Katalogtillegg endrer size-signaturen og tømmer relevante cacher; in-place definisjonsendringer må kalle clearCaches/clearCache eksplisitt. Lifecycle/reload-clearing finnes i RuntimePerformanceEvents.

## Energinett

Topology discovery og alle verdenslesinger skjer på servertråden. Workers utfører ren BFS/path-building på immutable EnergyTopologySnapshot. Små snapshots og avvist submission bruker samme snapshot synkront; grafer som overskrider eksisterende 8192-node snapshot-grense beholder original fallback.

Hver cached route-liste har et sett med avhengige chunks, også når listen er tom. Start, besøkte wires og alle deres nabochunks registreres, inkludert unloaded/disabled naboer. Chunk- eller lokal blokkendring fjerner bare berørte entries. Relevant endring under en pending jobb øker revisjonen slik at gammelt resultat forkastes.

`invalidate(level)` beholder global invalidasjon når endringsstedet ikke er kjent. Kjente wire-/machine-endringer bruker `invalidate(level, pos)`. Chunk-callbacks bruker `invalidateChunk` direkte på servertråden eller via server.execute. Ikke les chunkens blokkinnhold fra callback-tråden.

Destinasjoners input/capacity sjekkes fortsatt ved overføring. BFS-rekkefølge, amperage, voltage, overvoltage og extraction er uendret. Maksimalt 256 rute-entries beholdes per level.

## Klima

ClimateMath memoiserer døgnfaktorer med eksakt double-bit key i en ThreadLocal med to gjenbrukte entries. Ingen avrunding, endret oppdateringsfrekvens eller deling av lokal temperatur mellom dyr/posisjoner. Høyde, humidity, weather, sol, ly og varme bidrar fortsatt som tidligere. Den andre entryen håndterer sunExposure sin tick-avrundede dag.

ClimateWorld.sample gjenbruker biome- og day-data fra samme kall. Shelter-cache, geometri, temperaturgrenser og plantevekstregler er beholdt.

## Verifiseringsstatus

Se `docs/performance-2026-10-08.md` for gjennomførte sammenligninger og konkrete arbeidsmengder. API-kompilering med Minecraft-signaturer og test-fixtures er gjennomført. Full NeoForge/JEI/Create-build og runClient kunne ikke kjøres fordi prosjektets Gradle-filer/dependencies/runtime ikke var med i kilde-ZIP-en.
