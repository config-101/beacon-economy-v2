package com.example.beaconeconomy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.MenuProvider;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BeaconEconomyMod implements ModInitializer {
    public static final String MOD_ID = "beaconeconomy";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String OBJECTIVE = "beacon_top";

    private static MinecraftServer server;
    private static Path worldDir;
    private static final Map<UUID, Double> balances = new LinkedHashMap<>();
    private static final Map<String, Integer> prices = new LinkedHashMap<>();
    private static int sidebarTick = 0;

    @Override
    public void onInitialize() {
        loadDefaultPrices();

        ServerLifecycleEvents.SERVER_STARTED.register(startedServer -> {
            server = startedServer;
            worldDir = startedServer.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT);
            loadPrices();
            loadBalances();
            setupSidebar();
            refreshSidebar();
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(stoppingServer -> saveBalances());

        ServerTickEvents.END_SERVER_TICK.register(tickingServer -> {
            sidebarTick++;
            if (sidebarTick >= 100) {
                sidebarTick = 0;
                saveBalances();
                refreshSidebar();
            }
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClientSide()) return InteractionResult.PASS;
            if (!player.isShiftKeyDown()) return InteractionResult.PASS;
            if (!world.getBlockState(hitResult.getBlockPos()).is(Blocks.BEACON)) return InteractionResult.PASS;
            if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;

            serverPlayer.openMenu(new BeaconSellMenuProvider());
            return InteractionResult.SUCCESS;
        });

        registerCommands();
    }

    private static void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
            Commands.literal("money")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    player.sendSystemMessage(Component.literal("Balance: $" + format(getBalance(player))).withStyle(ChatFormatting.GOLD));
                    return 1;
                })
                .then(Commands.literal("baltop")
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        player.sendSystemMessage(Component.literal("Top 5 richest players:").withStyle(ChatFormatting.GOLD));
                        int place = 1;
                        for (BalanceEntry entry : topBalances(5)) {
                            player.sendSystemMessage(Component.literal(place + ". " + entry.name + " - $" + format(entry.balance)).withStyle(ChatFormatting.YELLOW));
                            place++;
                        }
                        return 1;
                    }))
                .then(Commands.literal("pay")
                    .then(Commands.argument("player", net.minecraft.commands.arguments.EntityArgument.player())
                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                            .executes(ctx -> {
                                ServerPlayer sender = ctx.getSource().getPlayerOrException();
                                ServerPlayer target = net.minecraft.commands.arguments.EntityArgument.getPlayer(ctx, "player");
                                double amount = DoubleArgumentType.getDouble(ctx, "amount");
                                if (sender.getUUID().equals(target.getUUID())) {
                                    sender.sendSystemMessage(Component.literal("You cannot pay yourself.").withStyle(ChatFormatting.RED));
                                    return 0;
                                }
                                if (getBalance(sender) < amount) {
                                    sender.sendSystemMessage(Component.literal("You do not have enough money.").withStyle(ChatFormatting.RED));
                                    return 0;
                                }
                                addBalance(sender.getUUID(), -amount);
                                addBalance(target.getUUID(), amount);
                                sender.sendSystemMessage(Component.literal("Paid " + target.getName().getString() + " $" + format(amount) + ".").withStyle(ChatFormatting.GREEN));
                                target.sendSystemMessage(Component.literal(sender.getName().getString() + " paid you $" + format(amount) + ".").withStyle(ChatFormatting.GREEN));
                                saveBalances();
                                refreshSidebar();
                                return 1;
                            }))))
        ));
    }

    private static class BeaconSellMenuProvider implements MenuProvider {
        @Override
        public Component getDisplayName() {
            return Component.literal("Beacon Market").withStyle(ChatFormatting.AQUA);
        }

        @Override
        public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
            return new BeaconSellMenu(syncId, playerInventory);
        }
    }

    public static class BeaconSellMenu extends AbstractContainerMenu {
        private final SimpleContainer hudInventory = new SimpleContainer(27);

        public BeaconSellMenu(int syncId, Inventory playerInventory) {
            super(MenuType.GENERIC_9x3, syncId);

            ItemStack filler = new ItemStack(Items.LIGHT_GRAY_STAINED_GLASS_PANE);
            ItemStack confirm = new ItemStack(Items.EMERALD_BLOCK);
            ItemStack info = new ItemStack(Items.BEACON);
            for (int i = 0; i < hudInventory.getContainerSize(); i++) hudInventory.setItem(i, filler.copy());
            hudInventory.setItem(11, info);
            hudInventory.setItem(13, confirm);
            hudInventory.setItem(15, new ItemStack(Items.GOLD_INGOT));

            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 9; col++) {
                    this.addSlot(new LockedSlot(hudInventory, col + row * 9, 8 + col * 18, 18 + row * 18));
                }
            }

            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 9; col++) {
                    this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
                }
            }

            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
            }
        }

        @Override
        public void clicked(int slotId, int button, ContainerInput containerInput, Player player) {
            if (slotId == 13 && player instanceof ServerPlayer serverPlayer) {
                SellResult result = sellFarmItems(serverPlayer);
                if (result.itemsSold <= 0) {
                    serverPlayer.sendSystemMessage(Component.literal("No sellable farm items found.").withStyle(ChatFormatting.RED));
                } else {
                    serverPlayer.sendSystemMessage(Component.literal("Sold " + result.itemsSold + " items for $" + format(result.moneyEarned) + ".").withStyle(ChatFormatting.GREEN));
                    saveBalances();
                    refreshSidebar();
                }
                serverPlayer.closeContainer();
                return;
            }
            if (slotId >= 0 && slotId < 27) return;
            super.clicked(slotId, button, containerInput, player);
        }

        @Override
        public ItemStack quickMoveStack(Player player, int index) {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }
    }

    private static class LockedSlot extends Slot {
        public LockedSlot(SimpleContainer inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }
    }

    private static SellResult sellFarmItems(ServerPlayer player) {
        int sold = 0;
        double earned = 0;
        Inventory inv = player.getInventory();

        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) continue;

            Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            Integer price = prices.get(itemId.toString());
            if (price == null || price <= 0) continue;

            int count = stack.getCount();
            sold += count;
            earned += count * price;
            stack.shrink(count);
            inv.setItem(i, stack.isEmpty() ? ItemStack.EMPTY : stack);
        }

        if (earned > 0) addBalance(player.getUUID(), earned);
        return new SellResult(sold, earned);
    }

    private static void addBalance(UUID uuid, double amount) {
        balances.put(uuid, Math.max(0, balances.getOrDefault(uuid, 0.0) + amount));
    }

    private static double getBalance(ServerPlayer player) {
        return balances.getOrDefault(player.getUUID(), 0.0);
    }

    private static void setupSidebar() {
        if (server == null) return;
        runCommand("scoreboard objectives add " + OBJECTIVE + " dummy {\"text\":\"Richest Players\"}");
        runCommand("scoreboard objectives setdisplay sidebar " + OBJECTIVE);
    }

    private static void refreshSidebar() {
        if (server == null) return;
        runCommand("scoreboard objectives setdisplay sidebar " + OBJECTIVE);
        for (int i = 1; i <= 5; i++) {
            runCommand("scoreboard players reset \"#" + i + " -\" " + OBJECTIVE);
        }

        List<BalanceEntry> top = topBalances(5);
        for (int i = 0; i < 5; i++) {
            if (i < top.size()) {
                BalanceEntry entry = top.get(i);
                String display = "#" + (i + 1) + " " + entry.name;
                if (display.length() > 38) display = display.substring(0, 38);
                runCommand("scoreboard players set \"" + escapeScoreName(display) + "\" " + OBJECTIVE + " " + Math.max(0, (int)Math.round(entry.balance)));
            } else {
                runCommand("scoreboard players set \"#" + (i + 1) + " -\" " + OBJECTIVE + " 0");
            }
        }
    }

    private static void runCommand(String command) {
        try {
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command);
        } catch (Exception ignored) {
        }
    }

    private static String escapeScoreName(String name) {
        return name.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static List<BalanceEntry> topBalances(int limit) {
        List<BalanceEntry> list = new ArrayList<>();
        for (Map.Entry<UUID, Double> entry : balances.entrySet()) {
            String name = lookupName(entry.getKey());
            list.add(new BalanceEntry(name, entry.getValue()));
        }
        list.sort(Comparator.comparingDouble((BalanceEntry e) -> e.balance).reversed());
        if (list.size() > limit) return list.subList(0, limit);
        return list;
    }

    private static String lookupName(UUID uuid) {
        if (server != null) {
            ServerPlayer online = server.getPlayerList().getPlayer(uuid);
            if (online != null) return online.getName().getString();        }
        return uuid.toString().substring(0, 8);
    }

    private static void loadDefaultPrices() {
        prices.clear();
        prices.put("minecraft:wheat", 2);
        prices.put("minecraft:carrot", 2);
        prices.put("minecraft:potato", 2);
        prices.put("minecraft:beetroot", 2);
        prices.put("minecraft:pumpkin", 8);
        prices.put("minecraft:melon_slice", 1);
        prices.put("minecraft:sweet_berries", 2);
        prices.put("minecraft:glow_berries", 3);
        prices.put("minecraft:cactus", 3);
        prices.put("minecraft:sugar_cane", 3);
        prices.put("minecraft:bamboo", 1);
        prices.put("minecraft:cocoa_beans", 3);
        prices.put("minecraft:nether_wart", 4);
    }

    private static void loadPrices() {
        Path file = worldDir.resolve("beacon-economy-prices.json");
        if (Files.notExists(file)) {
            savePrices();
            return;
        }
        try (Reader reader = Files.newBufferedReader(file)) {
            Type type = new TypeToken<LinkedHashMap<String, Integer>>() {}.getType();
            Map<String, Integer> loaded = GSON.fromJson(reader, type);
            if (loaded != null && !loaded.isEmpty()) {
                prices.clear();
                prices.putAll(loaded);
            }
        } catch (IOException ignored) {
        }
    }

    private static void savePrices() {
        if (worldDir == null) return;
        try (Writer writer = Files.newBufferedWriter(worldDir.resolve("beacon-economy-prices.json"))) {
            GSON.toJson(prices, writer);
        } catch (IOException ignored) {
        }
    }

    private static void loadBalances() {
        balances.clear();
        Path file = worldDir.resolve("beacon-economy-balances.json");
        if (Files.notExists(file)) return;
        try (Reader reader = Files.newBufferedReader(file)) {
            Type type = new TypeToken<LinkedHashMap<String, Double>>() {}.getType();
            Map<String, Double> loaded = GSON.fromJson(reader, type);
            if (loaded != null) {
                for (Map.Entry<String, Double> entry : loaded.entrySet()) {
                    balances.put(UUID.fromString(entry.getKey()), entry.getValue());
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static void saveBalances() {
        if (worldDir == null) return;
        Map<String, Double> serializable = new LinkedHashMap<>();
        for (Map.Entry<UUID, Double> entry : balances.entrySet()) {
            serializable.put(entry.getKey().toString(), entry.getValue());
        }
        try (Writer writer = Files.newBufferedWriter(worldDir.resolve("beacon-economy-balances.json"))) {
            GSON.toJson(serializable, writer);
        } catch (IOException ignored) {
        }
    }

    private static String format(double value) {
        if (Math.floor(value) == value) return String.valueOf((long)value);
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    private record SellResult(int itemsSold, double moneyEarned) {}
    private record BalanceEntry(String name, double balance) {}
}
