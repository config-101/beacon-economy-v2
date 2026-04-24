package com.danzi.beaconeconomy.relic;

import org.bukkit.Material;

public enum RelicType {
    RIFT_DAGGER("rift_dagger", "Rift Dagger", Material.NETHERITE_SWORD, 250,
        "Mobility relic. Rise, choose a target, and tear reality."),
    VOID_PULSE_CONTROL("void_pulse_control", "Void Pulse Control", Material.ECHO_SHARD, 260,
        "Control relic. Pull a target or unleash a radial void pulse."),
    NULLSHARD("nullshard", "Nullshard", Material.AMETHYST_SHARD, 0,
        "Passive denial relic. Suppresses relic activation and dispels effects nearby."),
    BLACK_LEDGER("black_ledger", "Black Ledger", Material.BOOK, 230,
        "Contract relic. Marks debt and punishes reckless enemies."),
    GRAVE_COIL("grave_coil", "Grave Coil", Material.CHAIN, 240,
        "Death relic. Refuses one ending and turns panic into punishment."),
    ASHWAKE_IDOL("ashwake_idol", "Ashwake Idol", Material.BLAZE_POWDER, 270,
        "Eruption relic. Creates catastrophic ash and fire pressure."),
    HUNTERS_EYE("hunters_eye", "Hunter's Eye", Material.ENDER_EYE, 210,
        "Hunt relic. Reveals prey and pressures the hidden."),
    WARDEN_FANG("warden_fang", "Warden Fang", Material.SCULK_SHRIEKER, 220,
        "Boss relic. Empowers the holder against legendary entities."),
    STORM_LANTERN("storm_lantern", "Storm Lantern", Material.SEA_LANTERN, 235,
        "Tempo relic. Bends motion, storms, and battlefield rhythm."),
    CROWN_LAST_KING("crown_last_king", "Crown of the Last King", Material.GOLDEN_HELMET, 280,
        "Dominion relic. Commands space around the holder.");

    public final String id;
    public final String display;
    public final Material material;
    public final int cooldownSeconds;
    public final String shortInfo;
    RelicType(String id, String display, Material material, int cooldownSeconds, String shortInfo) {
        this.id = id; this.display = display; this.material = material; this.cooldownSeconds = cooldownSeconds; this.shortInfo = shortInfo;
    }
    public static RelicType byId(String id) {
        for (RelicType t : values()) if (t.id.equalsIgnoreCase(id)) return t;
        return null;
    }
}
