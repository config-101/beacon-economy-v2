# Beacon Economy Systems Build

Paper 1.21 source pack for the systems-first Beacon Economy rebuild.

## Current direction
This build removes the physical spawn-city concept and replaces it with:
- first-join-only void intro
- frozen onboarding over the void
- GUI-based tutorial/help/commands
- `/wild` to start survival

## Included systems in this pack
- Paper 1.21 Gradle project
- GitHub Actions build workflow
- Intro void world
- First-join tutorial GUI
- `/info`, `/behelp`, `/tutorial`, `/commands`, `/becommands`
- `/wild`, `/spawn`
- 3-home system
- PvP combat tag / combat logout kill
- Basic staff commands:
  - `/clearlag`
  - `/invsee`
  - `/vanish`
  - `/spectate`
  - `/freeze`
  - `/unfreeze`
  - `/staffhelp`
- Basic admin GUI with `/beadmin`
- Basic money storage and `/money`
- Rank/prestige framework
- Personal beacon confirmation/permanence scaffold
- All 10 Forgotten Relics registered:
  - Rift Dagger
  - Void Pulse Control
  - Nullshard
  - Black Ledger
  - Grave Coil
  - Ashwake Idol
  - Hunter's Eye
  - Warden Fang
  - Storm Lantern
  - Crown of the Last King
- `/relic info`
- Admin relic command:
  - `/beadmin giverelic <relic_id> [player]`
- Rift Dagger implemented
- Void Pulse Control implemented
- Nullshard suppression scaffold
- Prototype abilities for the other 7 relics

## Important
This is a systems integration build, not the final polished server.
Resource pack and final textures are intentionally deferred.

## Build
Upload this folder to GitHub and run the included workflow, or run locally:

```bash
gradle build
```

The jar will be in:

```text
build/libs/
```

## Useful test commands

```text
/beadmin
/beadmin giverelic rift_dagger
/beadmin giverelic void_pulse_control
/beadmin giverelic nullshard
/relic info
/commands
/wild
/spawn
/sethome 1
/home 1
/clearlag
```

Relic IDs:
```text
rift_dagger
void_pulse_control
nullshard
black_ledger
grave_coil
ashwake_idol
hunters_eye
warden_fang
storm_lantern
crown_last_king
```
