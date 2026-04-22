package com.example.beaconeconomy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class BeaconEconomyMod implements ModInitializer {
    public static final String MOD_ID = "beaconeconomy";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String OBJECTIVE = "beacon_top";

    private static final int SCOREBOARD_REFRESH_TICKS = 60; // 3 seconds
    private static final int CLEARLAG_DELAY_TICKS = 200; // 10 seconds
    private static final int MONEY_PROTECTION_TICKS = 15 * 60 * 20;
    private static final int MONEY_PROTECTION_NOTICE_TICKS = 5 * 60 * 20;
    private static final int WARDEN_POLL_TICKS = 60 * 20;
    private static final int WARDEN_ACTIVE_TICKS = 15 * 60 * 20;
    private static final int WARDEN_COOLDOWN_TICKS = 60 * 60 * 20;
    private static final double DEATH_LOSS_PERCENT = 0.30;
    private static final double PVP_REWARD_PERCENT_OF_LOSS = 0.15;
    private static final double WARDEN_REWARD = 100_000.0;
    private static final double TOP_EFFECT_MINIMUM = 50_000.0;

    private static MinecraftServer server;
    private static Path worldDir;
    private static final Map<UUID, Double> balances = new LinkedHashMap<>();
    private static final Map<UUID, String> knownNames = new LinkedHashMap<>();
    private static final Map<String, Integer> prices = new LinkedHashMap<>();
    private static final Map<UUID, Integer> moneyProtectionTicks = new HashMap<>();
    private static final Map<UUID, Integer> moneyProtectionNextNotice = new HashMap<>();
    private static final Map<UUID, Boolean> aliveState = new HashMap<>();
    private static final Map<UUID, Integer> activeTopRank = new HashMap<>();
    private static int sidebarTick = 0;
    private static int clearlagTicks = -1;

    private static WardenEventState wardenState = WardenEventState.COOLDOWN;
    private static int wardenStateTicks = WARDEN_COOLDOWN_TICKS;
    private static UUID wardenUuid;
    private static String wardenDimension;
    private static Vec3 wardenPos;
    private static final Set<UUID> wardenAccepted = new HashSet<>();
    private static final Set<UUID> wardenDeclined = new HashSet<>();
    private static final Set<UUID> wardenSurrendered = new HashSet<>();
    private static final Map<UUID, ReturnPoint> wardenReturnPoints = new HashMap<>();
    private static final Random random = new Random();

    @Override
    public void onInitialize() {
        loadDefaultPrices();

        ServerLifecycleEvents.SERVER_STARTED.register(startedServer -> {
            server = startedServer;
            worldDir = startedServer.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT);
            loadPrices();
            loadBalances();
            setupSidebar();
            refreshSidebarAndRewards();
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(stoppingServer -> {
            clearAllRankEffects();
            saveBalances();
        });

        ServerTickEvents.END_SERVER_TICK.register(tickingServer -> tickServer());

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClientSide()) return InteractionResult.PASS;
            if (!player.isShiftKeyDown()) return InteractionResult.PASS;
            if (!world.getBlockState(hitResult.getBlockPos()).is(Blocks.BEACON)) return InteractionResult.PASS;
            if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
            serverPlayer.openMenu(new BeaconSellMenuProvider(serverPlayer));
            return InteractionResult.SUCCESS;
        });

        registerCommands();
    }

    private static void tickServer() {
        if (server == null) return;

        sidebarTick++;
        if (sidebarTick >= SCOREBOARD_REFRESH_TICKS) {
            sidebarTick = 0;
            saveBalances();
            refreshSidebarAndRewards();
        }

        tickClearlag();
        tickMoneyProtection();
        tickDeathDetection();
        tickWardenEvent();
    }

    private static void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("money")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    remember(player);
                    player.sendSystemMessage(prefix().append(Component.literal(" Balance: $" + formatFull(getBalance(player))).withStyle(ChatFormatting.GOLD)));
                    return 1;
                })
                .then(Commands.literal("balance")
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        remember(player);
                        player.sendSystemMessage(prefix().append(Component.literal(" Balance: $" + formatFull(getBalance(player))).withStyle(ChatFormatting.GOLD)));
                        return 1;
                    }))
                .then(Commands.literal("baltop")
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        player.sendSystemMessage(Component.literal("✦ Beacon Economy Top 5 ✦").withStyle(ChatFormatting.GOLD));
                        int place = 1;
                        for (BalanceEntry entry : topBalances(5)) {
                            player.sendSystemMessage(Component.literal(place + ". " + entry.name + " - $" + formatFull(entry.balance)).withStyle(ChatFormatting.YELLOW));
                            place++;
                        }
                        return 1;
                    }))
                .then(Commands.literal("pay")
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                            .executes(ctx -> {
                                ServerPlayer sender = ctx.getSource().getPlayerOrException();
                                ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                double amount = DoubleArgumentType.getDouble(ctx, "amount");
                                remember(sender);
                                remember(target);
                                if (sender.getUUID().equals(target.getUUID())) {
                                    sender.sendSystemMessage(prefix().append(Component.literal(" You cannot pay yourself.").withStyle(ChatFormatting.RED)));
                                    return 0;
                                }
                                if (getBalance(sender) < amount) {
                                    sender.sendSystemMessage(prefix().append(Component.literal(" You do not have enough money.").withStyle(ChatFormatting.RED)));
                                    return 0;
                                }
                                addBalance(sender.getUUID(), -amount);
                                addBalance(target.getUUID(), amount);
                                sender.sendSystemMessage(prefix().append(Component.literal(" Paid " + target.getName().getString() + " $" + formatFull(amount) + ".").withStyle(ChatFormatting.GREEN)));
                                target.sendSystemMessage(prefix().append(Component.literal(" " + sender.getName().getString() + " paid you $" + formatFull(amount) + ".").withStyle(ChatFormatting.GREEN)));
                                saveBalances();
                                refreshSidebarAndRewards();
                                return 1;
                            })))));

            dispatcher.register(Commands.literal("sellable")
                .executes(ctx -> sendSellablePage(ctx.getSource().getPlayerOrException(), 1))
                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                    .executes(ctx -> sendSellablePage(ctx.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(ctx, "page")))));

            dispatcher.register(Commands.literal("clearlag")
                .requires(source -> source.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_MODERATOR))
                .executes(ctx -> startClearlag()));

            dispatcher.register(Commands.literal("yes")
                .executes(ctx -> acceptWarden(ctx.getSource().getPlayerOrException())));
            dispatcher.register(Commands.literal("no")
                .executes(ctx -> declineWarden(ctx.getSource().getPlayerOrException())));
            dispatcher.register(Commands.literal("surrender")
                .executes(ctx -> surrenderWarden(ctx.getSource().getPlayerOrException())));

            dispatcher.register(Commands.literal("wardenevent")
                .requires(source -> source.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_MODERATOR))
                .then(Commands.literal("start").executes(ctx -> forceStartWardenEvent()))
                .then(Commands.literal("cancel").executes(ctx -> cancelWardenEvent(true)))
                .then(Commands.literal("status").executes(ctx -> {
                    ctx.getSource().sendSuccess(() -> prefix().append(Component.literal(" Warden event: " + wardenState + " (" + ticksToSeconds(wardenStateTicks) + "s).").withStyle(ChatFormatting.YELLOW)), false);
                    return 1;
                })));
        });
    }

    private static int sendSellablePage(ServerPlayer player, int page) {
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(prices.entrySet());
        entries.sort(Map.Entry.comparingByKey());
        int perPage = 8;
        int maxPage = Math.max(1, (int)Math.ceil(entries.size() / (double)perPage));
        page = Math.min(Math.max(1, page), maxPage);
        int start = (page - 1) * perPage;
        int end = Math.min(entries.size(), start + perPage);

        player.sendSystemMessage(Component.literal("✦ Beacon Economy Sellables ✦").withStyle(ChatFormatting.GOLD));
        player.sendSystemMessage(Component.literal("Page " + page + "/" + maxPage + " - Sell at a beacon with Shift + Right Click").withStyle(ChatFormatting.GRAY));
        for (int i = start; i < end; i++) {
            Map.Entry<String, Integer> entry = entries.get(i);
            player.sendSystemMessage(Component.literal("• " + prettyItemName(entry.getKey()) + ": $").withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(formatCompact(entry.getValue())).withStyle(ChatFormatting.GREEN)));
        }
        if (page < maxPage) player.sendSystemMessage(Component.literal("Use /sellable " + (page + 1) + " for more.").withStyle(ChatFormatting.GRAY));
        return 1;
    }

    private static int startClearlag() {
        if (clearlagTicks > 0) {
            broadcast(Component.literal("⚠ Clearlag is already scheduled.").withStyle(ChatFormatting.RED));
            return 0;
        }
        clearlagTicks = CLEARLAG_DELAY_TICKS;
        broadcast(Component.literal("✦ Beacon Economy ✦").withStyle(ChatFormatting.GOLD)
            .append(Component.literal(" ⚠ All dropped items will be removed in 10 seconds!").withStyle(ChatFormatting.YELLOW)));
        return 1;
    }

    private static void tickClearlag() {
        if (clearlagTicks < 0) return;
        clearlagTicks--;
        if (clearlagTicks <= 0) {
            clearlagTicks = -1;
            for (ServerLevel level : server.getAllLevels()) {
                runCommand("execute in " + dimensionId(level) + " run kill @e[type=minecraft:item]");
            }
            broadcast(Component.literal("✦ Beacon Economy ✦").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(" ✓ Clearlag complete. Dropped items were removed.").withStyle(ChatFormatting.GREEN)));
        }
    }

    private static void tickMoneyProtection() {
        List<UUID> expired = new ArrayList<>();
        for (Map.Entry<UUID, Integer> entry : moneyProtectionTicks.entrySet()) {
            UUID uuid = entry.getKey();
            int left = entry.getValue() - 1;
            moneyProtectionTicks.put(uuid, left);
            int nextNotice = moneyProtectionNextNotice.getOrDefault(uuid, MONEY_PROTECTION_NOTICE_TICKS) - 1;
            moneyProtectionNextNotice.put(uuid, nextNotice);
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (left <= 0) {
                expired.add(uuid);
                if (player != null) player.sendSystemMessage(prefix().append(Component.literal(" ⚠ Money-loss protection expired.").withStyle(ChatFormatting.RED)));
            } else if (nextNotice <= 0) {
                moneyProtectionNextNotice.put(uuid, MONEY_PROTECTION_NOTICE_TICKS);
                if (player != null) player.sendSystemMessage(prefix().append(Component.literal(" 🛡 Money-loss protection remaining: " + Math.max(1, left / 1200) + " minutes.").withStyle(ChatFormatting.AQUA)));
            }
        }
        for (UUID uuid : expired) {
            moneyProtectionTicks.remove(uuid);
            moneyProtectionNextNotice.remove(uuid);
        }
    }

    private static void tickDeathDetection() {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            remember(player);
            UUID uuid = player.getUUID();
            boolean alive = player.isAlive() && !player.isDeadOrDying();
            boolean wasAlive = aliveState.getOrDefault(uuid, true);
            if (wasAlive && !alive) handlePlayerDeath(player);
            aliveState.put(uuid, alive);
        }
    }

    private static void handlePlayerDeath(ServerPlayer victim) {
        UUID victimId = victim.getUUID();
        if (moneyProtectionTicks.containsKey(victimId)) {
            victim.sendSystemMessage(prefix().append(Component.literal(" 🛡 You died, but money-loss protection saved your balance.").withStyle(ChatFormatting.AQUA)));
            return;
        }

        double balance = getBalance(victim);
        double lost = Math.floor(balance * DEATH_LOSS_PERCENT);
        if (lost <= 0) {
            startMoneyProtection(victim);
            return;
        }

        addBalance(victimId, -lost);
        victim.sendSystemMessage(prefix().append(Component.literal(" ☠ You died and lost $" + formatCompact(lost) + ".").withStyle(ChatFormatting.RED)));

        LivingEntity credit = victim.getKillCredit();
        if (credit instanceof ServerPlayer killer && !killer.getUUID().equals(victimId)) {
            remember(killer);
            double reward = Math.floor(lost * PVP_REWARD_PERCENT_OF_LOSS);
            if (reward > 0) {
                addBalance(killer.getUUID(), reward);
                killer.sendSystemMessage(prefix().append(Component.literal(" ⚔ You earned $" + formatCompact(reward) + " from killing " + victim.getName().getString() + ".").withStyle(ChatFormatting.GREEN)));
            }
        }
        startMoneyProtection(victim);
        saveBalances();
        refreshSidebarAndRewards();
    }

    private static void startMoneyProtection(ServerPlayer player) {
        moneyProtectionTicks.put(player.getUUID(), MONEY_PROTECTION_TICKS);
        moneyProtectionNextNotice.put(player.getUUID(), MONEY_PROTECTION_NOTICE_TICKS);
        player.sendSystemMessage(prefix().append(Component.literal(" 🛡 Money-loss protection active for 15 minutes.").withStyle(ChatFormatting.AQUA)));
    }

    private static class BeaconSellMenuProvider implements MenuProvider {
        private final ServerPlayer player;
        private BeaconSellMenuProvider(ServerPlayer player) { this.player = player; }

        @Override
        public Component getDisplayName() {
            return Component.literal("Beacon Market").withStyle(ChatFormatting.AQUA);
        }

        @Override
        public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
            return new BeaconSellMenu(syncId, playerInventory, this.player);
        }
    }

    public static class BeaconSellMenu extends AbstractContainerMenu {
        private final SimpleContainer hudInventory = new SimpleContainer(27);
        private final ServerPlayer player;

        public BeaconSellMenu(int syncId, Inventory playerInventory, ServerPlayer player) {
            super(MenuType.GENERIC_9x3, syncId);
            this.player = player;

            SellResult preview = previewSale(player);
            ItemStack filler = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
            ItemStack confirm = new ItemStack(Items.EMERALD_BLOCK);
            ItemStack info = new ItemStack(Items.BEACON);
            ItemStack cancel = new ItemStack(Items.BARRIER);
            for (int i = 0; i < hudInventory.getContainerSize(); i++) hudInventory.setItem(i, filler.copy());
            hudInventory.setItem(11, info);
            hudInventory.setItem(13, confirm);
            hudInventory.setItem(15, cancel);
            hudInventory.setItem(22, new ItemStack(preview.itemsSold > 0 ? Items.GOLD_INGOT : Items.REDSTONE));

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
            for (int col = 0; col < 9; col++) this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));

            player.sendSystemMessage(prefix().append(Component.literal(" Beacon Market: " + preview.itemsSold + " sellable items worth $" + formatCompact(preview.moneyEarned) + ".").withStyle(ChatFormatting.YELLOW)));
            player.sendSystemMessage(Component.literal("Click the emerald block to sell, or the barrier to cancel.").withStyle(ChatFormatting.GRAY));
        }

        @Override
        public void clicked(int slotId, int button, ContainerInput containerInput, Player clicker) {
            if (slotId == 13 && clicker instanceof ServerPlayer serverPlayer) {
                SellResult result = sellFarmItems(serverPlayer);
                if (result.itemsSold <= 0) serverPlayer.sendSystemMessage(prefix().append(Component.literal(" No sellable items found.").withStyle(ChatFormatting.RED)));
                else {
                    serverPlayer.sendSystemMessage(prefix().append(Component.literal(" Sold " + result.itemsSold + " items for $" + formatFull(result.moneyEarned) + ".").withStyle(ChatFormatting.GREEN)));
                    saveBalances();
                    refreshSidebarAndRewards();
                }
                serverPlayer.closeContainer();
                return;
            }
            if (slotId == 15 && clicker instanceof ServerPlayer serverPlayer) {
                serverPlayer.closeContainer();
                return;
            }
            if (slotId >= 0 && slotId < 27) return;
            super.clicked(slotId, button, containerInput, clicker);
        }

        @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
        @Override public boolean stillValid(Player player) { return true; }
    }

    private static class LockedSlot extends Slot {
        public LockedSlot(SimpleContainer inventory, int index, int x, int y) { super(inventory, index, x, y); }
        @Override public boolean mayPlace(ItemStack stack) { return false; }
        @Override public boolean mayPickup(Player player) { return false; }
    }

    private static SellResult previewSale(ServerPlayer player) { return scanSell(player, false); }
    private static SellResult sellFarmItems(ServerPlayer player) { return scanSell(player, true); }

    private static SellResult scanSell(ServerPlayer player, boolean removeItems) {
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
            if (removeItems) {
                stack.shrink(count);
                inv.setItem(i, stack.isEmpty() ? ItemStack.EMPTY : stack);
            }
        }
        if (removeItems && earned > 0) addBalance(player.getUUID(), earned);
        return new SellResult(sold, earned);
    }

    private static void setupSidebar() {
        if (server == null) return;
        runCommand("scoreboard objectives add " + OBJECTIVE + " dummy {\"text\":\"✦ Beacon Economy ✦\",\"color\":\"gold\"}");
        runCommand("scoreboard objectives modify " + OBJECTIVE + " displayname {\"text\":\"✦ Beacon Economy ✦\",\"color\":\"gold\"}");
        runCommand("scoreboard objectives modify " + OBJECTIVE + " numberformat blank");
        runCommand("scoreboard objectives setdisplay sidebar " + OBJECTIVE);
    }

    private static void refreshSidebarAndRewards() {
        if (server == null) return;
        setupSidebar();
        runCommand("scoreboard players reset * " + OBJECTIVE);
        List<BalanceEntry> top = topBalances(5);
        int score = 15;
        scoreLine("━━━━━━━━━━━━", score--);
        scoreLine("Top 5 Richest", score--);
        String[] ranks = {"①", "②", "③", "④", "⑤"};
        for (int i = 0; i < 5; i++) {
            if (i < top.size()) {
                BalanceEntry entry = top.get(i);
                String name = entry.name.length() > 12 ? entry.name.substring(0, 12) : entry.name;
                scoreLine(ranks[i] + " " + name + " $" + formatCompact(entry.balance), score--);
            } else {
                scoreLine(ranks[i] + " --- $0", score--);
            }
        }
        scoreLine("━━━━━━━━━━━━ ", score--);
        scoreLine("Updates: 3s", score--);
        updateRankEffects(top);
    }

    private static void scoreLine(String line, int score) {
        if (line.length() > 40) line = line.substring(0, 40);
        runCommand("scoreboard players set \"" + escapeScoreName(line) + "\" " + OBJECTIVE + " " + score);
    }

    private static void updateRankEffects(List<BalanceEntry> top) {
        Map<UUID, Integer> newRanks = new HashMap<>();
        for (int i = 0; i < Math.min(3, top.size()); i++) {
            BalanceEntry entry = top.get(i);
            if (entry.balance >= TOP_EFFECT_MINIMUM) newRanks.put(entry.uuid, i + 1);
        }

        Set<UUID> all = new HashSet<>(activeTopRank.keySet());
        all.addAll(newRanks.keySet());
        for (UUID uuid : all) {
            int oldRank = activeTopRank.getOrDefault(uuid, 0);
            int newRank = newRanks.getOrDefault(uuid, 0);
            if (oldRank == newRank) continue;
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player == null) {
                activeTopRank.remove(uuid);
                continue;
            }
            clearRankEffects(player);
            if (newRank > 0) {
                giveRankEffects(player, newRank);
                activeTopRank.put(uuid, newRank);
                player.sendSystemMessage(prefix().append(Component.literal(" You now hold richest rank #" + newRank + " and received its reward effects.").withStyle(ChatFormatting.GOLD)));
            } else {
                activeTopRank.remove(uuid);
                player.sendSystemMessage(prefix().append(Component.literal(" You lost your top 3 reward effects.").withStyle(ChatFormatting.GRAY)));
            }
        }
    }

    private static void giveRankEffects(ServerPlayer player, int rank) {
        String name = quote(player.getName().getString());
        if (rank == 1) {
            runCommand("effect give " + name + " minecraft:regeneration infinite 0 true");
            runCommand("effect give " + name + " minecraft:haste infinite 0 true");
            runCommand("effect give " + name + " minecraft:speed infinite 0 true");
            runCommand("effect give " + name + " minecraft:strength infinite 0 true");
        } else if (rank == 2) {
            runCommand("effect give " + name + " minecraft:speed infinite 0 true");
            runCommand("effect give " + name + " minecraft:haste infinite 0 true");
        } else if (rank == 3) {
            runCommand("effect give " + name + " minecraft:speed infinite 0 true");
        }
    }

    private static void clearRankEffects(ServerPlayer player) {
        String name = quote(player.getName().getString());
        runCommand("effect clear " + name + " minecraft:regeneration");
        runCommand("effect clear " + name + " minecraft:haste");
        runCommand("effect clear " + name + " minecraft:speed");
        runCommand("effect clear " + name + " minecraft:strength");
    }

    private static void clearAllRankEffects() {
        if (server == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) clearRankEffects(player);
        activeTopRank.clear();
    }

    private static int forceStartWardenEvent() {
        if (wardenState == WardenEventState.POLLING || wardenState == WardenEventState.ACTIVE) {
            broadcast(prefix().append(Component.literal(" A Warden event is already running.").withStyle(ChatFormatting.RED)));
            return 0;
        }
        startWardenOffer(true);
        return 1;
    }

    private static void tickWardenEvent() {
        if (wardenState == WardenEventState.COOLDOWN) {
            wardenStateTicks--;
            if (wardenStateTicks <= 0) startWardenOffer(false);
        } else if (wardenState == WardenEventState.POLLING) {
            wardenStateTicks--;
            if (wardenStateTicks <= 0) startWardenFight();
        } else if (wardenState == WardenEventState.ACTIVE) {
            wardenStateTicks--;
            Entity warden = getWardenEntity();
            if (warden != null) wardenPos = warden.position();
            if (warden == null) finishWardenEvent(true);
            else if (wardenStateTicks <= 0) finishWardenEvent(false);
        }
    }

    private static void startWardenOffer(boolean forced) {
        ServerLevel level = server.overworld();
        BlockPos pos = randomWardenPos(level);
        wardenDimension = dimensionId(level);
        wardenPos = new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        wardenUuid = null;
        wardenAccepted.clear();
        wardenDeclined.clear();
        wardenSurrendered.clear();
        wardenReturnPoints.clear();

        runCommand("execute in " + wardenDimension + " run summon minecraft:warden " + pos.getX() + " " + pos.getY() + " " + pos.getZ() + " {Tags:[\"beacon_economy_boss\"],CustomName:'{\"text\":\"Cursed Economy Warden\",\"color\":\"dark_purple\"}'}");
        Entity spawned = findTaggedWarden(level);
        if (spawned != null) {
            wardenUuid = spawned.getUUID();
            applyRandomWardenEffects();
        }

        wardenState = WardenEventState.POLLING;
        wardenStateTicks = WARDEN_POLL_TICKS;
        broadcast(Component.literal("✦ Beacon Economy Boss Event ✦").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
        broadcast(Component.literal("⚠ A cursed Warden has appeared at X " + pos.getX() + ", Z " + pos.getZ() + ".").withStyle(ChatFormatting.LIGHT_PURPLE));
        broadcast(Component.literal("Type /yes to join or /no to decline. Poll ends in 60 seconds.").withStyle(ChatFormatting.YELLOW));
        if (forced) broadcast(Component.literal("This event was started by an operator.").withStyle(ChatFormatting.GRAY));
    }

    private static void startWardenFight() {
        if (wardenAccepted.isEmpty()) {
            broadcast(prefix().append(Component.literal(" No players accepted the Warden challenge.").withStyle(ChatFormatting.GRAY)));
            cancelWardenEvent(false);
            return;
        }
        wardenState = WardenEventState.ACTIVE;
        wardenStateTicks = WARDEN_ACTIVE_TICKS;
        for (UUID uuid : new HashSet<>(wardenAccepted)) {
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player == null || wardenPos == null) continue;
            remember(player);
            wardenReturnPoints.put(uuid, ReturnPoint.from(player));
            Vec3 tp = randomPointAround(wardenPos, 20.0);
            runCommand("execute in " + wardenDimension + " run tp " + quote(player.getName().getString()) + " " + formatCoord(tp.x) + " " + formatCoord(tp.y) + " " + formatCoord(tp.z));
            player.sendSystemMessage(prefix().append(Component.literal(" The fight has begun. Use /surrender to return to your original location.").withStyle(ChatFormatting.LIGHT_PURPLE)));
        }
        broadcast(Component.literal("✦ Boss Event ✦ Challengers have been teleported. The Warden will vanish in 15 minutes.").withStyle(ChatFormatting.DARK_PURPLE));
    }

    private static int acceptWarden(ServerPlayer player) {
        if (wardenState != WardenEventState.POLLING) {
            player.sendSystemMessage(prefix().append(Component.literal(" There is no Warden poll right now.").withStyle(ChatFormatting.RED)));
            return 0;
        }
        remember(player);
        wardenDeclined.remove(player.getUUID());
        wardenAccepted.add(player.getUUID());
        broadcast(prefix().append(Component.literal(" " + player.getName().getString() + " accepted the Warden challenge.").withStyle(ChatFormatting.GREEN)));
        return 1;
    }

    private static int declineWarden(ServerPlayer player) {
        if (wardenState != WardenEventState.POLLING) {
            player.sendSystemMessage(prefix().append(Component.literal(" There is no Warden poll right now.").withStyle(ChatFormatting.RED)));
            return 0;
        }
        wardenAccepted.remove(player.getUUID());
        wardenDeclined.add(player.getUUID());
        broadcast(prefix().append(Component.literal(" " + player.getName().getString() + " declined the Warden challenge.").withStyle(ChatFormatting.GRAY)));
        return 1;
    }

    private static int surrenderWarden(ServerPlayer player) {
        if (wardenState != WardenEventState.ACTIVE || !wardenAccepted.contains(player.getUUID()) || wardenSurrendered.contains(player.getUUID())) {
            player.sendSystemMessage(prefix().append(Component.literal(" You are not currently in an active Warden fight.").withStyle(ChatFormatting.RED)));
            return 0;
        }
        wardenSurrendered.add(player.getUUID());
        ReturnPoint point = wardenReturnPoints.get(player.getUUID());
        if (point != null) runCommand("execute in " + point.dimension + " run tp " + quote(player.getName().getString()) + " " + formatCoord(point.x) + " " + formatCoord(point.y) + " " + formatCoord(point.z));
        player.sendSystemMessage(prefix().append(Component.literal(" You surrendered and returned to your original position. You cannot earn this event's reward.").withStyle(ChatFormatting.YELLOW)));
        broadcast(prefix().append(Component.literal(" " + player.getName().getString() + " surrendered from the Warden fight.").withStyle(ChatFormatting.GRAY)));
        return 1;
    }

    private static int cancelWardenEvent(boolean announce) {
        Entity warden = getWardenEntity();
        if (warden != null) warden.discard();
        wardenAccepted.clear();
        wardenDeclined.clear();
        wardenSurrendered.clear();
        wardenReturnPoints.clear();
        wardenUuid = null;
        wardenPos = null;
        wardenState = WardenEventState.COOLDOWN;
        wardenStateTicks = WARDEN_COOLDOWN_TICKS;
        if (announce) broadcast(prefix().append(Component.literal(" Warden event cancelled. Cooldown restarted.").withStyle(ChatFormatting.YELLOW)));
        return 1;
    }

    private static void finishWardenEvent(boolean killed) {
        if (killed) {
            int paid = 0;
            for (UUID uuid : wardenAccepted) {
                if (wardenSurrendered.contains(uuid)) continue;
                ServerPlayer player = server.getPlayerList().getPlayer(uuid);
                if (player == null || wardenPos == null) continue;
                if (!dimensionId(player.level()).equals(wardenDimension)) continue;
                if (player.position().distanceToSqr(wardenPos) <= 50.0 * 50.0) {
                    addBalance(uuid, WARDEN_REWARD);
                    paid++;
                    player.sendSystemMessage(prefix().append(Component.literal(" The Warden was defeated. You received $100K!").withStyle(ChatFormatting.GREEN)));
                }
            }
            saveBalances();
            refreshSidebarAndRewards();
            broadcast(Component.literal("✦ Boss Event Complete ✦ ").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD)
                .append(Component.literal("The cursed Warden was defeated. " + paid + " nearby fighters received $100K.").withStyle(ChatFormatting.GREEN)));
        } else {
            Entity warden = getWardenEntity();
            if (warden != null) warden.discard();
            broadcast(Component.literal("✦ Boss Event ✦ ").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD)
                .append(Component.literal("The cursed Warden vanished. No reward was paid.").withStyle(ChatFormatting.RED)));
        }
        wardenAccepted.clear();
        wardenDeclined.clear();
        wardenSurrendered.clear();
        wardenReturnPoints.clear();
        wardenUuid = null;
        wardenPos = null;
        wardenState = WardenEventState.COOLDOWN;
        wardenStateTicks = WARDEN_COOLDOWN_TICKS;
    }

    private static BlockPos randomWardenPos(ServerLevel level) {
        int x = random.nextInt(10_001) - 5_000;
        int z = random.nextInt(10_001) - 5_000;
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) + 1;
        return new BlockPos(x, Math.max(70, y), z);
    }

    private static Vec3 randomPointAround(Vec3 center, double radius) {
        double angle = random.nextDouble() * Math.PI * 2.0;
        double distance = 5.0 + random.nextDouble() * (radius - 5.0);
        return new Vec3(center.x + Math.cos(angle) * distance, center.y, center.z + Math.sin(angle) * distance);
    }

    private static void applyRandomWardenEffects() {
        if (wardenPos == null || wardenDimension == null) return;
        String target = "@e[type=minecraft:warden,tag=beacon_economy_boss,limit=1,sort=nearest]";
        List<String> effects = new ArrayList<>(List.of("strength", "speed", "resistance", "regeneration", "absorption", "jump_boost", "fire_resistance", "invisibility", "glowing"));
        java.util.Collections.shuffle(effects, random);
        int count = 3 + random.nextInt(3);
        for (int i = 0; i < count && i < effects.size(); i++) {
            String effect = effects.get(i);
            int amp = switch (effect) {
                case "strength" -> random.nextInt(4);
                case "speed" -> random.nextInt(3);
                case "resistance" -> random.nextInt(3);
                case "regeneration" -> random.nextInt(2);
                default -> random.nextInt(2);
            };
            runCommand("execute in " + wardenDimension + " positioned " + formatCoord(wardenPos.x) + " " + formatCoord(wardenPos.y) + " " + formatCoord(wardenPos.z) + " run effect give " + target + " minecraft:" + effect + " 900 " + amp + " true");
        }
    }

    private static Entity getWardenEntity() {
        if (wardenUuid == null || wardenDimension == null) return null;
        for (ServerLevel level : server.getAllLevels()) {
            if (!dimensionId(level).equals(wardenDimension)) continue;
            Entity entity = level.getEntity(wardenUuid);
            if (entity != null) return entity;
        }
        return null;
    }

    private static Entity findTaggedWarden(ServerLevel level) {
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof Warden && wardenPos != null && entity.position().distanceToSqr(wardenPos) <= 16.0 * 16.0) return entity;
        }
        return null;
    }

    private static void addBalance(UUID uuid, double amount) { balances.put(uuid, Math.max(0, balances.getOrDefault(uuid, 0.0) + amount)); }
    private static double getBalance(ServerPlayer player) { return balances.getOrDefault(player.getUUID(), 0.0); }
    private static void remember(ServerPlayer player) { knownNames.put(player.getUUID(), player.getName().getString()); balances.putIfAbsent(player.getUUID(), 0.0); }


    private static String dimensionId(Level level) {
        if (server != null) {
            if (level.dimension().equals(Level.NETHER)) return "minecraft:the_nether";
            if (level.dimension().equals(Level.END)) return "minecraft:the_end";
        }
        return "minecraft:overworld";
    }

    private static void runCommand(String command) {
        try { server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command); } catch (Exception ignored) {}
    }

    private static void broadcast(Component component) {
        if (server == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) player.sendSystemMessage(component);
    }

    private static MutableComponent prefix() { return Component.literal("✦ Beacon Economy ✦").withStyle(ChatFormatting.GOLD); }
    private static String quote(String name) { return "\"" + name.replace("\\", "\\\\").replace("\"", "\\\"") + "\""; }
    private static String escapeScoreName(String name) { return name.replace("\\", "\\\\").replace("\"", "\\\""); }
    private static String formatCoord(double value) { return String.format(Locale.ROOT, "%.2f", value); }
    private static long ticksToSeconds(int ticks) { return Math.max(0, ticks / 20); }

    private static List<BalanceEntry> topBalances(int limit) {
        List<BalanceEntry> list = new ArrayList<>();
        for (Map.Entry<UUID, Double> entry : balances.entrySet()) list.add(new BalanceEntry(entry.getKey(), lookupName(entry.getKey()), entry.getValue()));
        list.sort(Comparator.comparingDouble((BalanceEntry e) -> e.balance).reversed());
        if (list.size() > limit) return new ArrayList<>(list.subList(0, limit));
        return list;
    }

    private static String lookupName(UUID uuid) {
        if (server != null) {
            ServerPlayer online = server.getPlayerList().getPlayer(uuid);
            if (online != null) return online.getName().getString();
        }
        return knownNames.getOrDefault(uuid, uuid.toString().substring(0, 8));
    }

    private static void loadDefaultPrices() {
        prices.clear();
        prices.put("minecraft:wheat", 1);
        prices.put("minecraft:wheat_seeds", 1);
        prices.put("minecraft:carrot", 1);
        prices.put("minecraft:potato", 1);
        prices.put("minecraft:poisonous_potato", 1);
        prices.put("minecraft:beetroot", 1);
        prices.put("minecraft:beetroot_seeds", 1);
        prices.put("minecraft:pumpkin", 2);
        prices.put("minecraft:carved_pumpkin", 2);
        prices.put("minecraft:jack_o_lantern", 2);
        prices.put("minecraft:pumpkin_seeds", 1);
        prices.put("minecraft:melon_slice", 1);
        prices.put("minecraft:melon", 6);
        prices.put("minecraft:melon_seeds", 1);
        prices.put("minecraft:hay_block", 6);
        prices.put("minecraft:sugar_cane", 1);
        prices.put("minecraft:cactus", 1);
        prices.put("minecraft:bamboo", 1);
        prices.put("minecraft:bamboo_block", 6);
        prices.put("minecraft:stripped_bamboo_block", 6);
        prices.put("minecraft:kelp", 1);
        prices.put("minecraft:dried_kelp", 1);
        prices.put("minecraft:dried_kelp_block", 5);
        prices.put("minecraft:sweet_berries", 1);
        prices.put("minecraft:glow_berries", 2);
        prices.put("minecraft:cocoa_beans", 1);
        prices.put("minecraft:nether_wart", 2);
        prices.put("minecraft:nether_wart_block", 12);
        prices.put("minecraft:warped_wart_block", 12);
        prices.put("minecraft:red_mushroom", 1);
        prices.put("minecraft:brown_mushroom", 1);
        prices.put("minecraft:red_mushroom_block", 4);
        prices.put("minecraft:brown_mushroom_block", 4);
        prices.put("minecraft:mushroom_stem", 4);
        prices.put("minecraft:crimson_fungus", 2);
        prices.put("minecraft:warped_fungus", 2);
        prices.put("minecraft:chorus_fruit", 3);
        prices.put("minecraft:popped_chorus_fruit", 3);
        prices.put("minecraft:chorus_flower", 5);
        prices.put("minecraft:torchflower_seeds", 2);
        prices.put("minecraft:torchflower", 3);
        prices.put("minecraft:pitcher_pod", 2);
        prices.put("minecraft:pitcher_plant", 3);
        prices.put("minecraft:nether_star", 10000);
    }

    private static void loadPrices() {
        Path file = worldDir.resolve("beacon-economy-prices.json");
        if (Files.notExists(file)) { savePrices(); return; }
        try (Reader reader = Files.newBufferedReader(file)) {
            Type type = new TypeToken<LinkedHashMap<String, Integer>>() {}.getType();
            Map<String, Integer> loaded = GSON.fromJson(reader, type);
            if (loaded != null && !loaded.isEmpty()) { prices.clear(); prices.putAll(loaded); }
        } catch (IOException ignored) {}
    }

    private static void savePrices() {
        if (worldDir == null) return;
        try (Writer writer = Files.newBufferedWriter(worldDir.resolve("beacon-economy-prices.json"))) { GSON.toJson(prices, writer); } catch (IOException ignored) {}
    }

    private static void loadBalances() {
        balances.clear(); knownNames.clear();
        Path file = worldDir.resolve("beacon-economy-balances.json");
        if (Files.notExists(file)) return;
        try (Reader reader = Files.newBufferedReader(file)) {
            Type type = new TypeToken<LinkedHashMap<String, PlayerData>>() {}.getType();
            Map<String, PlayerData> loaded = GSON.fromJson(reader, type);
            if (loaded != null) {
                for (Map.Entry<String, PlayerData> entry : loaded.entrySet()) {
                    UUID uuid = UUID.fromString(entry.getKey());
                    balances.put(uuid, entry.getValue().balance);
                    if (entry.getValue().name != null) knownNames.put(uuid, entry.getValue().name);
                }
            }
        } catch (Exception firstFormatFailed) {
            try (Reader reader = Files.newBufferedReader(file)) {
                Type type = new TypeToken<LinkedHashMap<String, Double>>() {}.getType();
                Map<String, Double> loaded = GSON.fromJson(reader, type);
                if (loaded != null) for (Map.Entry<String, Double> entry : loaded.entrySet()) balances.put(UUID.fromString(entry.getKey()), entry.getValue());
            } catch (Exception ignored) {}
        }
    }

    private static void saveBalances() {
        if (worldDir == null) return;
        Map<String, PlayerData> serializable = new LinkedHashMap<>();
        for (Map.Entry<UUID, Double> entry : balances.entrySet()) serializable.put(entry.getKey().toString(), new PlayerData(lookupName(entry.getKey()), entry.getValue()));
        try (Writer writer = Files.newBufferedWriter(worldDir.resolve("beacon-economy-balances.json"))) { GSON.toJson(serializable, writer); } catch (IOException ignored) {}
    }

    private static String prettyItemName(String itemId) {
        String s = itemId.replace("minecraft:", "").replace('_', ' ');
        StringBuilder out = new StringBuilder();
        for (String part : s.split(" ")) {
            if (part.isEmpty()) continue;
            out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(' ');
        }
        return out.toString().trim();
    }

    private static String formatFull(double value) {
        if (Math.floor(value) == value) return String.format(Locale.ROOT, "%,d", (long)value);
        return String.format(Locale.ROOT, "%,.2f", value);
    }

    private static String formatCompact(double value) {
        double abs = Math.abs(value);
        if (abs < 1000) return String.valueOf((long)Math.floor(value));
        String[] suffixes = {"K", "M", "B", "T"};
        double scaled = value;
        int idx = -1;
        while (Math.abs(scaled) >= 1000 && idx < suffixes.length - 1) { scaled /= 1000.0; idx++; }
        if (Math.abs(scaled) >= 100 || Math.floor(scaled) == scaled) return String.format(Locale.ROOT, "%.0f%s", scaled, suffixes[idx]);
        return String.format(Locale.ROOT, "%.1f%s", scaled, suffixes[idx]);
    }

    private enum WardenEventState { COOLDOWN, POLLING, ACTIVE }
    private record SellResult(int itemsSold, double moneyEarned) {}
    private record BalanceEntry(UUID uuid, String name, double balance) {}
    private record PlayerData(String name, double balance) {}
    private record ReturnPoint(String dimension, double x, double y, double z) {
        static ReturnPoint from(ServerPlayer player) {
            return new ReturnPoint(dimensionId(player.level()), player.getX(), player.getY(), player.getZ());
        }
    }
}
