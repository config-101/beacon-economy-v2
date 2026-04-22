# Beacon Economy v1.0.0

Fabric mod for Minecraft/game version `26.1.2`.

## Features

- Players can only sell farm items by **sneak/shift-right-clicking a beacon**.
- Sneak-right-click opens a beacon sell HUD.
- Clicking the emerald in the HUD confirms the sale.
- Top 5 richest players are shown on the right side of the screen using the vanilla scoreboard sidebar.
- Player balances save in the world folder as `beacon-economy-balances.json`.
- Farm item prices are editable in `beacon-economy-prices.json` in the world folder after the first server run.

## Build

```bash
./gradlew build
```

The jar will be in `build/libs/`.

## Notes

This project intentionally does **not** include `/sell`. Selling is only reachable through the beacon HUD.
