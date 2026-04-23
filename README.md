# Beacon Economy Paper Test Pack

This is a first **Paper plugin test pack** for the Beacon Economy server concept.
It includes a core playable slice so you can start testing server flow while the larger systems are still being expanded.

## Included in this test pack
- Protected hub spawn world (`beacon_spawn`) generated automatically
- `/spawn`, `/wild`, `/tutorial`, `/behelp`
- Login always returns players to spawn
- 10-second PvP combat tag with logout kill
- Homes system with max 3 homes and a GUI
- Home restrictions (no spawn world, no nether roof)
- Rift Dagger Forgotten Relic test implementation
- `/relic info`
- `/beadmin giveriftdagger [player]`
- `/beadmin hubregen`
- Starter resource pack folder with Rift Dagger texture + GUI textures

## Not complete yet
The following are still planned / scaffold-stage and not fully implemented in this test pack:
- Full economy + sell beacon loop
- Fancy side scoreboard
- Rank / prestige progression
- AFK money zones
- Pets, black market, spawner shop
- Full Void Drifter boss system
- All 10 Forgotten Relics
- Full spawn mega-build

## Build
Use GitHub Actions or local Gradle:

```bash
gradle build
```

The jar will be placed in:

```text
build/libs/
```

## Resource pack
The resource pack source is included in the `resource-pack` folder of the bundle zip.
You can zip that folder and host it, or expand it later.
