# ✦ Beacon Economy ✦ Shockveil Test Build

Partial Paper 1.21 plugin foundation focused only on Forgotten Relic #2: **Shockveil**.

## Included

- `Shockveil` test relic item
- `/shockveiltest` OP command to receive it
- `/relic info` support while holding Shockveil
- 200 second cooldown
- Disabled in `beacon_spawn`
- 15 block shockwave radius
- Close targets receive stronger damage/effects
- Caster lock-in after cast
- Caster-owned pets are excluded
- Heavy particles and layered sounds
- CustomModelData `10002`
- Example resource-pack files and placeholder texture
- GitHub Actions Maven build workflow

## Test command

```text
/shockveiltest
```

Requires OP or `beaconeconomy.admin`.

## Build on GitHub

Push this folder to GitHub, then open **Actions** and run/push the workflow. The built jar will be uploaded as the `BeaconEconomy-Shockveil-Test` artifact.

## Local build

```bash
mvn package
```

Jar output:

```text
target/beacon-economy-0.2.0-shockveil-test.jar
```

## Resource pack notes

The included `resource-pack/` folder contains the model override and a placeholder texture:

```text
assets/minecraft/models/item/paper.json
assets/beaconeconomy/models/item/shockveil.json
assets/beaconeconomy/textures/item/shockveil.png
```

Replace `shockveil.png` with final art later. Keep CustomModelData at `10002` unless you also update the plugin code.
