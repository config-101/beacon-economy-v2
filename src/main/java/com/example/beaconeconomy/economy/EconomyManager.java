package com.example.beaconeconomy.economy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.player.Inventory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class EconomyManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type BALANCE_TYPE = new TypeToken<Map<UUID, PlayerBalance>>() {}.getType();

    private static final Map<Item, Long> FARM_PRICES = new LinkedHashMap<>();
    private static final Map<UUID, PlayerBalance> BALANCES = new HashMap<>();

    static {
        FARM_PRICES.put(Items.WHEAT, 2L);
        FARM_PRICES.put(Items.CARROT, 2L);
        FARM_PRICES.put(Items.POTATO, 2L);
        FARM_PRICES.put(Items.BEETROOT, 2L);
        FARM_PRICES.put(Items.MELON_SLICE, 1L);
        FARM_PRICES.put(Items.PUMPKIN, 8L);
        FARM_PRICES.put(Items.SUGAR_CANE, 3L);
        FARM_PRICES.put(Items.CACTUS, 3L);
        FARM_PRICES.put(Items.BAMBOO, 1L);
        FARM_PRICES.put(Items.SWEET_BERRIES, 2L);
        FARM_PRICES.put(Items.GLOW_BERRIES, 4L);
        FARM_PRICES.put(Items.NETHER_WART, 5L);
        FARM_PRICES.put(Items.COCOA_BEANS, 3L);
    }

    private EconomyManager() {}

    public static Set<Item> sellableItems() {
        return Collections.unmodifiableSet(FARM_PRICES.keySet());
    }

    public static long priceOf(Item item) {
        return FARM_PRICES.getOrDefault(item, 0L);
    }

    public static long previewSaleValue(ServerPlayer player) {
        long total = 0;
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            long price = priceOf(stack.getItem());
            if (price > 0) total += price * stack.getCount();
        }
        return total;
    }

    public static SaleResult sellAllFarmItems(ServerPlayer player) {
        long total = 0;
        int soldStacksOrItems = 0;
        Inventory inventory = player.getInventory();

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) continue;

            long price = priceOf(stack.getItem());
            if (price <= 0) continue;

            int count = stack.getCount();
            total += price * count;
            soldStacksOrItems += count;
            inventory.setItem(i, ItemStack.EMPTY);
        }

        if (total > 0) {
            addMoney(player, total);
        }

        return new SaleResult(soldStacksOrItems, total);
    }

    public static void addMoney(ServerPlayer player, long amount) {
        PlayerBalance balance = BALANCES.computeIfAbsent(player.getUUID(), id -> new PlayerBalance(player.getGameProfile().getName(), 0L));
        balance.name = player.getGameProfile().getName();
        balance.balance += amount;
    }

    public static long getBalance(UUID uuid) {
        PlayerBalance balance = BALANCES.get(uuid);
        return balance == null ? 0L : balance.balance;
    }

    public static List<TopBalance> topBalances(int limit) {
        return BALANCES.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue().balance, a.getValue().balance))
                .limit(limit)
                .map(entry -> new TopBalance(entry.getKey(), entry.getValue().name, entry.getValue().balance))
                .toList();
    }

    public static void rememberOnlinePlayers(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            BALANCES.computeIfAbsent(player.getUUID(), id -> new PlayerBalance(player.getGameProfile().getName(), 0L)).name = player.getGameProfile().getName();
        }
    }

    public static void load(MinecraftServer server) {
        BALANCES.clear();
        Path path = savePath(server);
        if (!Files.exists(path)) return;

        try (Reader reader = Files.newBufferedReader(path)) {
            Map<UUID, PlayerBalance> loaded = GSON.fromJson(reader, BALANCE_TYPE);
            if (loaded != null) BALANCES.putAll(loaded);
        } catch (IOException ignored) {
        }
    }

    public static void save(MinecraftServer server) {
        Path path = savePath(server);
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(BALANCES, BALANCE_TYPE, writer);
            }
        } catch (IOException ignored) {
        }
    }

    private static Path savePath(MinecraftServer server) {
        return server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("beacon-economy-balances.json");
    }

    public static String priceListText() {
        StringBuilder builder = new StringBuilder();
        FARM_PRICES.forEach((item, price) -> {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            builder.append(id).append(" = $").append(price).append(" each\n");
        });
        return builder.toString();
    }

    public static final class PlayerBalance {
        public String name;
        public long balance;

        public PlayerBalance(String name, long balance) {
            this.name = name;
            this.balance = balance;
        }
    }

    public record TopBalance(UUID uuid, String name, long balance) {}
    public record SaleResult(int itemsSold, long earned) {}
}
