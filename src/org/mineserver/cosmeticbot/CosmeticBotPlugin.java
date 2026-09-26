package org.mineserver.cosmeticbot;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.*;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class CosmeticBotPlugin extends JavaPlugin implements Listener {

    private FakeNPC fakeNpc;
    private Economy economy;

    // Списки предметов по разделам
    private final List<CosmeticItem> hats      = new ArrayList<>();
    private final List<CosmeticItem> wings      = new ArrayList<>();
    private final List<CosmeticItem> halloween  = new ArrayList<>();
    private final List<CosmeticItem> christmas  = new ArrayList<>();

    // Расположение слотов: 6-wide по центру в 3 рядах (27 слотов)
    private static final int[] HAT_ITEM_SLOTS =      {1,2,3,4,5,6, 10,11,12,13,14,15, 19};
    private static final int   HAT_BACK_SLOT  = 26;
    // 3-wide по центру в 2 рядах (18 слотов)
    private static final int[] WINGS_ITEM_SLOTS =     {3,4,5, 12,13,14};
    private static final int   WINGS_BACK_SLOT  = 17;
    // 3-wide по центру в 3 рядах (27 слотов)
    private static final int[] HALLOWEEN_ITEM_SLOTS = {3,4,5, 12,13,14, 21,22,23};
    private static final int   HALLOWEEN_BACK_SLOT  = 26;
    // 3-wide по центру в 2 рядах (18 слотов)
    private static final int[] CHRISTMAS_ITEM_SLOTS = {3,4,5, 12,13};
    private static final int   CHRISTMAS_BACK_SLOT  = 17;

    // Заголовки инвентарей (для идентификации в обработчике кликов)
    private static final String TITLE_MAIN      = ChatColor.DARK_PURPLE + "Модельер";
    private static final String TITLE_HATS      = ChatColor.GOLD + "Модельер — Шляпы";
    private static final String TITLE_WINGS     = ChatColor.DARK_AQUA + "Модельер — Крылья";
    private static final String TITLE_HALLOWEEN = ChatColor.DARK_RED + "Модельер — Хэллоуин";
    private static final String TITLE_CHRISTMAS = ChatColor.GREEN + "Модельер — Рождество";

    // ==================== ENABLE / DISABLE ====================

    @Override
    public void onEnable() {
        saveDefaultConfig();
        buildItemLists();

        if (!setupEconomy()) {
            getLogger().severe("Vault/Economy не найден! Плагин отключён.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getServer().getPluginManager().registerEvents(this, this);
        setupNpcInteractListener();
        getServer().getScheduler().runTaskLater(this, this::spawnOrFindBot, 10L);
        startRespawnTicker();
        getLogger().info("CosmeticBot включён.");
    }

    @Override
    public void onDisable() {
        if (fakeNpc != null) fakeNpc.despawnForAll();
        getLogger().info("CosmeticBot отключён.");
    }

    private void setupNpcInteractListener() {
        ProtocolLibrary.getProtocolManager().addPacketListener(
            new PacketAdapter(this, ListenerPriority.NORMAL, PacketType.Play.Client.USE_ENTITY) {
                @Override
                public void onPacketReceiving(PacketEvent event) {
                    if (fakeNpc == null || !fakeNpc.isCreated()) return;
                    int entityId = event.getPacket().getIntegers().read(0);
                    if (entityId != fakeNpc.getEntityId()) return;
                    event.setCancelled(true);
                    Player player = event.getPlayer();
                    getServer().getScheduler().runTask(CosmeticBotPlugin.this, () -> {
                        if (player.isOnline()) player.openInventory(buildMainMenu());
                    });
                }
            });
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        economy = rsp.getProvider();
        return economy != null;
    }

    // ==================== ДАННЫЕ ПРЕДМЕТОВ ====================

    private void buildItemLists() {
        // --- Шляпы/Шлемы ---
        hats.add(new CosmeticItem("iawearables:deadmau5_hat",    Material.FEATHER, "Deadmau5 шляпа",               35.0));
        hats.add(new CosmeticItem("iawearables:top_hat",         Material.FEATHER, "Шляпа-цилиндр",                50.0));
        hats.add(new CosmeticItem("iawearables:welding_mask",    Material.FEATHER, "Маска сварщика",               25.0));
        hats.add(new CosmeticItem("iawearables:mining_helmet",   Material.FEATHER, "Шахтёрский шлем",              40.0));
        hats.add(new CosmeticItem("iawearables:dog_mask",        Material.FEATHER, "Маска собаки",                 35.0));
        hats.add(new CosmeticItem("iawearables:ruby_pickaxe_hat",Material.FEATHER, "Рубиновая кирка-маска",        35.0));
        hats.add(new CosmeticItem("iawearables:straw_hat",       Material.FEATHER, "Соломенная шляпа",             15.0));
        hats.add(new CosmeticItem("iawearables:cool_sunglasses", Material.FEATHER, "Крутые солнцезащитные очки",   30.0));
        hats.add(new CosmeticItem("iawearables:glasses",         Material.FEATHER, "Очки",                         10.0));
        hats.add(new CosmeticItem("iawearables:cigar",           Material.FEATHER, "Сигарета",                     40.0));
        hats.add(new CosmeticItem("iawearables:biker_helmet",    Material.FEATHER, "Шлем велосипедиста",           50.0));
        hats.add(new CosmeticItem("iawearables:cat_ears",        Material.FEATHER, "Кошачьи ушки",                 20.0));
        hats.add(new CosmeticItem("iaalchemy:mysterious_hood",   Material.FEATHER, "Мистический капюшон",         300.0));

        // --- Крылья ---
        wings.add(new CosmeticItem("iawearables:demoniac_red_wings",       Material.FEATHER, "Красные демонические крылья",    150.0));
        wings.add(new CosmeticItem("iawearables:demoniac_turquoise_wings", Material.FEATHER, "Бирюзовые демонические крылья",  150.0));
        wings.add(new CosmeticItem("iawearables:demoniac_purple_wings",    Material.FEATHER, "Фиолетовые демонические крылья", 150.0));
        wings.add(new CosmeticItem("iawearables:demoniac_blue_wings",      Material.FEATHER, "Синие демонические крылья",      150.0));
        wings.add(new CosmeticItem("iawearables:demoniac_black_wings",     Material.FEATHER, "Чёрные демонические крылья",     150.0));
        wings.add(new CosmeticItem("iawearables:ender_dragon_wings",       Material.FEATHER, "Крылья эндердракона",            500.0));

        // --- Хэллоуин ---
        halloween.add(new CosmeticItem("iafestivities:jack_o_lantern_backpack",      Material.CARVED_PUMPKIN, "Рюкзак Джека",            15.0));
        halloween.add(new CosmeticItem("iafestivities:dark_jack_o_lantern_backpack", Material.CARVED_PUMPKIN, "Тёмный Рюкзак Джека",     20.0));
        halloween.add(new CosmeticItem("iafestivities:spider_backpack",              Material.STRING,         "Рюкзак Паука",            15.0));
        halloween.add(new CosmeticItem("iafestivities:cave_spider_backpack",         Material.COBWEB,         "Рюкзак-пещерный паук",    25.0));
        halloween.add(new CosmeticItem("iafestivities:bones_backpack",               Material.BONE,           "Рюкзак из костей",        30.0));
        halloween.add(new CosmeticItem("iafestivities:witch_hat",                    Material.LEATHER_HELMET, "Шляпа ведьмы",            15.0));
        halloween.add(new CosmeticItem("iafestivities:bat_wings",                    Material.FEATHER,        "Крылья летучей мыши",    100.0));
        halloween.add(new CosmeticItem("iafestivities:axe_hat",                      Material.IRON_AXE,       "Шляпа-топор",             20.0));
        halloween.add(new CosmeticItem("iafestivities:decorative_pumpkin",           Material.PUMPKIN,        "Декоративная тыква",      40.0));

        // --- Рождество ---
        christmas.add(new CosmeticItem("iafestivities:christmas_tree",        Material.OAK_SAPLING,    "Рождественская ёлка",     50.0));
        christmas.add(new CosmeticItem("iafestivities:christmas_candle",      Material.CANDLE,         "Рождественская свеча",    15.0));
        christmas.add(new CosmeticItem("iafestivities:santa_hat",             Material.LEATHER_HELMET, "Шапка Санты",             25.0));
        christmas.add(new CosmeticItem("iafestivities:decorative_candy_cane", Material.SUGAR_CANE,     "Трость из карамели",      35.0));
        christmas.add(new CosmeticItem("iafestivities:christmas_wreath",      Material.VINE,           "Рождественский венок",    20.0));
    }

    // ==================== НПС ====================

    private void spawnOrFindBot() {
        String worldName = getConfig().getString("bot-location.world", "");
        if (worldName.isEmpty()) {
            getLogger().info("CosmeticBot: позиция не задана. Используйте /cosmeticbot spawn.");
            return;
        }
        World world = getServer().getWorld(worldName);
        if (world == null) {
            getLogger().warning("Мир '" + worldName + "' не найден! Используйте /cosmeticbot spawn.");
            return;
        }
        double x   = getConfig().getDouble("bot-location.x", 0.5);
        double y   = getConfig().getDouble("bot-location.y", 64);
        double z   = getConfig().getDouble("bot-location.z", 0.5);
        float  yaw = (float) getConfig().getDouble("bot-location.yaw", 0.0);
        String skinTex = getConfig().getString("skin.texture", null);
        String skinSig = getConfig().getString("skin.signature", null);
        createNPCAt(new Location(world, x, y, z, yaw, 0), skinTex, skinSig);
    }

    private void createNPCAt(Location loc, String skinTex, String skinSig) {
        if (fakeNpc != null) fakeNpc.despawnForAll();
        fakeNpc = new FakeNPC(this, loc);
        if (skinTex != null && skinSig != null) fakeNpc.setSkin(skinTex, skinSig);
        fakeNpc.create();
        fakeNpc.spawnForAll();
        getLogger().info("CosmeticBot: NPC создан на " + loc.getWorld().getName() +
            " x=" + (int)loc.getX() + " y=" + (int)loc.getY() + " z=" + (int)loc.getZ());
    }

    private void startRespawnTicker() {
        new BukkitRunnable() {
            @Override public void run() {
                if (fakeNpc == null || !fakeNpc.isCreated()) return;
                Location npcLoc = fakeNpc.getLocation();
                if (npcLoc.getWorld() == null) return;
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (!p.getWorld().getName().equals(fakeNpc.getLocationWorld())) {
                        fakeNpc.forgetPlayer(p.getUniqueId()); continue;
                    }
                    double distSq = p.getLocation().distanceSquared(npcLoc);
                    if (distSq <= 64 * 64) fakeNpc.spawnFor(p);
                    else if (distSq > 80 * 80) fakeNpc.forgetPlayer(p.getUniqueId());
                }
            }
        }.runTaskTimer(this, 20L * 5, 20L * 5);
    }

    // ==================== ПОСТРОЕНИЕ GUI ====================

    private Inventory buildMainMenu() {
        Inventory inv = getServer().createInventory(null, 9, TITLE_MAIN);
        ItemStack glass = makeGlass();
        for (int i = 0; i < 9; i++) inv.setItem(i, glass);
        inv.setItem(1, makeCategoryButton("iawearables:top_hat",          Material.FEATHER,        ChatColor.GOLD + "Шляпы / Шлемы",    "Головные уборы и маски"));
        inv.setItem(3, makeCategoryButton("iawearables:demoniac_red_wings",Material.FEATHER,       ChatColor.DARK_AQUA + "Крылья",      "Декоративные крылья за спину"));
        inv.setItem(5, makeCategoryButton("iafestivities:witch_hat",      Material.CARVED_PUMPKIN, ChatColor.DARK_RED + "Хэллоуин",     "Сезонные костюмы и аксессуары"));
        inv.setItem(7, makeCategoryButton("iafestivities:santa_hat",      Material.LEATHER_HELMET, ChatColor.GREEN + "Рождество",       "Рождественские украшения"));
        return inv;
    }

    private Inventory buildHatsMenu() {
        Inventory inv = getServer().createInventory(null, 27, TITLE_HATS);
        fillGlass(inv, 27);
        for (int i = 0; i < hats.size() && i < HAT_ITEM_SLOTS.length; i++) {
            inv.setItem(HAT_ITEM_SLOTS[i], buildItemSlot(hats.get(i)));
        }
        inv.setItem(HAT_BACK_SLOT, makeBackButton());
        return inv;
    }

    private Inventory buildWingsMenu() {
        Inventory inv = getServer().createInventory(null, 18, TITLE_WINGS);
        fillGlass(inv, 18);
        for (int i = 0; i < wings.size() && i < WINGS_ITEM_SLOTS.length; i++) {
            inv.setItem(WINGS_ITEM_SLOTS[i], buildItemSlot(wings.get(i)));
        }
        inv.setItem(WINGS_BACK_SLOT, makeBackButton());
        return inv;
    }

    private Inventory buildHalloweenMenu() {
        Inventory inv = getServer().createInventory(null, 27, TITLE_HALLOWEEN);
        fillGlass(inv, 27);
        for (int i = 0; i < halloween.size() && i < HALLOWEEN_ITEM_SLOTS.length; i++) {
            inv.setItem(HALLOWEEN_ITEM_SLOTS[i], buildItemSlot(halloween.get(i)));
        }
        inv.setItem(HALLOWEEN_BACK_SLOT, makeBackButton());
        return inv;
    }

    private Inventory buildChristmasMenu() {
        Inventory inv = getServer().createInventory(null, 18, TITLE_CHRISTMAS);
        fillGlass(inv, 18);
        for (int i = 0; i < christmas.size() && i < CHRISTMAS_ITEM_SLOTS.length; i++) {
            inv.setItem(CHRISTMAS_ITEM_SLOTS[i], buildItemSlot(christmas.get(i)));
        }
        inv.setItem(CHRISTMAS_BACK_SLOT, makeBackButton());
        return inv;
    }

    // ==================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ GUI ====================

    private ItemStack buildItemSlot(CosmeticItem item) {
        ItemStack stack = createItemStack(item.iaId, item.fallback);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) meta = getServer().getItemFactory().getItemMeta(stack.getType());
        meta.setDisplayName(ChatColor.AQUA + item.displayName);
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Цена: " + ChatColor.YELLOW + "$" + (int)item.price);
        lore.add("");
        lore.add(ChatColor.GREEN + "▶  Нажмите чтобы купить");
        meta.setLore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack makeCategoryButton(String iaId, Material fallback, String name, String description) {
        ItemStack stack = createItemStack(iaId, fallback);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) meta = getServer().getItemFactory().getItemMeta(stack.getType());
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(ChatColor.GRAY + description, "", ChatColor.GREEN + "▶  Нажмите чтобы открыть"));
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack makeBackButton() {
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta m = back.getItemMeta();
        m.setDisplayName(ChatColor.YELLOW + "← Назад");
        m.setLore(Collections.singletonList(ChatColor.GRAY + "Вернуться в главное меню"));
        back.setItemMeta(m);
        return back;
    }

    private ItemStack makeGlass() {
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta m = glass.getItemMeta();
        m.setDisplayName(ChatColor.GRAY + " ");
        glass.setItemMeta(m);
        return glass;
    }

    private void fillGlass(Inventory inv, int size) {
        ItemStack glass = makeGlass();
        for (int i = 0; i < size; i++) inv.setItem(i, glass);
    }

    private ItemStack createItemStack(String iaId, Material fallback) {
        if (getServer().getPluginManager().getPlugin("ItemsAdder") != null) {
            try {
                Class<?> csClass = Class.forName("dev.lone.itemsadder.api.CustomStack");
                java.lang.reflect.Method getInstance = csClass.getMethod("getInstance", String.class);
                Object cs = getInstance.invoke(null, iaId);
                if (cs != null) {
                    java.lang.reflect.Method getItemStack = csClass.getMethod("getItemStack");
                    ItemStack result = (ItemStack) getItemStack.invoke(cs);
                    if (result != null) return result.clone();
                }
            } catch (Exception e) {
                getLogger().warning("CosmeticBot: не удалось создать IA предмет '" + iaId + "': " + e.getMessage());
            }
        }
        return new ItemStack(fallback);
    }

    // ==================== СОБЫТИЯ ====================

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (fakeNpc == null || !fakeNpc.isCreated()) return;
        Player p = event.getPlayer();
        getServer().getScheduler().runTaskLater(this, () -> { if (p.isOnline()) fakeNpc.spawnFor(p); }, 20L);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (fakeNpc == null || !fakeNpc.isCreated()) return;
        Player p = event.getPlayer();
        fakeNpc.forgetPlayer(p.getUniqueId());
        getServer().getScheduler().runTaskLater(this, () -> { if (p.isOnline()) fakeNpc.spawnFor(p); }, 20L);
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        if (fakeNpc == null || !fakeNpc.isCreated()) return;
        Player p = event.getPlayer();
        fakeNpc.forgetPlayer(p.getUniqueId());
        getServer().getScheduler().runTaskLater(this, () -> {
            if (p.isOnline() && p.getWorld().getName().equals(fakeNpc.getLocationWorld())) fakeNpc.spawnFor(p);
        }, 60L);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        String title = event.getView().getTitle();
        if (!title.contains("Модельер")) return;
        event.setCancelled(true);

        if (event.getClickedInventory() == null) return;
        if (!event.getClickedInventory().equals(event.getView().getTopInventory())) return;

        int slot = event.getSlot();
        Player player = (Player) event.getWhoClicked();

        if (title.equals(TITLE_MAIN)) {
            handleMainMenuClick(player, slot);
        } else if (title.equals(TITLE_HATS)) {
            handleCategoryClick(player, slot, hats, HAT_ITEM_SLOTS, HAT_BACK_SLOT);
        } else if (title.equals(TITLE_WINGS)) {
            handleCategoryClick(player, slot, wings, WINGS_ITEM_SLOTS, WINGS_BACK_SLOT);
        } else if (title.equals(TITLE_HALLOWEEN)) {
            handleCategoryClick(player, slot, halloween, HALLOWEEN_ITEM_SLOTS, HALLOWEEN_BACK_SLOT);
        } else if (title.equals(TITLE_CHRISTMAS)) {
            handleCategoryClick(player, slot, christmas, CHRISTMAS_ITEM_SLOTS, CHRISTMAS_BACK_SLOT);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTitle().contains("Модельер")) event.setCancelled(true);
    }

    private void handleMainMenuClick(Player player, int slot) {
        switch (slot) {
            case 1: getServer().getScheduler().runTask(this, () -> player.openInventory(buildHatsMenu()));      break;
            case 3: getServer().getScheduler().runTask(this, () -> player.openInventory(buildWingsMenu()));     break;
            case 5: getServer().getScheduler().runTask(this, () -> player.openInventory(buildHalloweenMenu())); break;
            case 7: getServer().getScheduler().runTask(this, () -> player.openInventory(buildChristmasMenu())); break;
        }
    }

    private void handleCategoryClick(Player player, int slot, List<CosmeticItem> items, int[] itemSlots, int backSlot) {
        if (slot == backSlot) {
            getServer().getScheduler().runTask(this, () -> player.openInventory(buildMainMenu()));
            return;
        }
        int itemIdx = -1;
        for (int i = 0; i < itemSlots.length; i++) {
            if (itemSlots[i] == slot) { itemIdx = i; break; }
        }
        if (itemIdx < 0 || itemIdx >= items.size()) return;
        processPurchase(player, items.get(itemIdx));
    }

    private void processPurchase(Player player, CosmeticItem item) {
        if (!economy.has(player, item.price)) {
            player.sendMessage(ChatColor.RED + "✗ Недостаточно средств! Нужно: "
                + ChatColor.YELLOW + "$" + (int)item.price
                + ChatColor.RED + ", у вас: "
                + ChatColor.YELLOW + "$" + String.format("%.2f", economy.getBalance(player)));
            return;
        }
        ItemStack toGive = createItemStack(item.iaId, item.fallback);
        if (!hasInventorySpace(player.getInventory(), toGive)) {
            player.sendMessage(ChatColor.RED + "✗ В вашем инвентаре нет места!");
            return;
        }
        economy.withdrawPlayer(player, item.price);
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(toGive);
        for (ItemStack drop : leftover.values()) player.getWorld().dropItemNaturally(player.getLocation(), drop);
        player.sendMessage(ChatColor.GREEN + "✔ Куплено: " + ChatColor.WHITE + item.displayName
            + ChatColor.GREEN + " за " + ChatColor.YELLOW + "$" + (int)item.price
            + ChatColor.GREEN + ". Баланс: " + ChatColor.YELLOW + "$"
            + String.format("%.2f", economy.getBalance(player)));
    }

    private boolean hasInventorySpace(PlayerInventory inv, ItemStack stack) {
        for (ItemStack s : inv.getStorageContents()) {
            if (s == null) return true;
            if (s.isSimilar(stack) && s.getAmount() + stack.getAmount() <= s.getMaxStackSize()) return true;
        }
        return false;
    }

    // ==================== КОМАНДЫ ====================

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("cosmeticbot")) return false;
        if (!sender.hasPermission("cosmeticbot.admin")) {
            sender.sendMessage(ChatColor.RED + "Нет прав."); return true;
        }
        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "/cosmeticbot spawn — создать бота здесь");
            sender.sendMessage(ChatColor.YELLOW + "/cosmeticbot skin <ник> — установить скин");
            sender.sendMessage(ChatColor.YELLOW + "/cosmeticbot info — состояние");
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "spawn":
                if (!(sender instanceof Player)) { sender.sendMessage("Только для игроков."); return true; }
                Player sp = (Player) sender;
                Location spLoc = sp.getLocation();
                getConfig().set("bot-location.world", spLoc.getWorld().getName());
                getConfig().set("bot-location.x", spLoc.getX());
                getConfig().set("bot-location.y", spLoc.getY());
                getConfig().set("bot-location.z", spLoc.getZ());
                getConfig().set("bot-location.yaw", (double) spLoc.getYaw());
                saveConfig();
                createNPCAt(spLoc, getConfig().getString("skin.texture"), getConfig().getString("skin.signature"));
                sender.sendMessage(ChatColor.GREEN + "Модельер создан! Установи скин: /cosmeticbot skin <ник>");
                return true;

            case "skin":
                if (args.length < 2) { sender.sendMessage(ChatColor.RED + "/cosmeticbot skin <ник>"); return true; }
                String skinTarget = args[1];
                if (!skinTarget.matches("[a-zA-Z0-9_]{1,16}")) {
                    sender.sendMessage(ChatColor.RED + "Неверный ник."); return true;
                }
                sender.sendMessage(ChatColor.YELLOW + "Загружаю скин игрока " + skinTarget + "...");
                fetchSkinAsync(sender, skinTarget);
                return true;

            case "info":
                sender.sendMessage(ChatColor.YELLOW + "CosmeticBot активен. Предметов: "
                    + "шляпы=" + hats.size()
                    + ", крылья=" + wings.size()
                    + ", хэллоуин=" + halloween.size()
                    + ", рождество=" + christmas.size());
                return true;
        }
        return false;
    }

    // ==================== SKIN FETCH ====================

    private void fetchSkinAsync(CommandSender sender, String username) {
        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            try {
                URL url1 = new URL("https://api.mojang.com/users/profiles/minecraft/" + username);
                HttpURLConnection c1 = (HttpURLConnection) url1.openConnection();
                c1.setRequestProperty("Accept-Encoding", "identity");
                c1.setRequestProperty("User-Agent", "CosmeticBot/1.0");
                c1.setConnectTimeout(5000); c1.setReadTimeout(5000);
                if (c1.getResponseCode() != 200) {
                    runSync(sender, ChatColor.RED + "Игрок '" + username + "' не найден.");
                    c1.disconnect(); return;
                }
                String resp1 = new String(c1.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                c1.disconnect();
                if (!resp1.contains("\"id\"")) { runSync(sender, ChatColor.RED + "Не удалось получить UUID."); return; }
                String rawUUID = resp1.split("\"id\"\\s*:\\s*\"")[1].split("\"")[0];
                String uuid = rawUUID.replaceAll("(.{8})(.{4})(.{4})(.{4})(.+)", "$1-$2-$3-$4-$5");

                URL url2 = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid + "?unsigned=false");
                HttpURLConnection c2 = (HttpURLConnection) url2.openConnection();
                c2.setRequestProperty("Accept-Encoding", "identity");
                c2.setRequestProperty("User-Agent", "CosmeticBot/1.0");
                c2.setConnectTimeout(5000); c2.setReadTimeout(5000);
                if (c2.getResponseCode() != 200) { runSync(sender, ChatColor.RED + "Не удалось загрузить скин."); c2.disconnect(); return; }
                String resp2 = new String(c2.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                c2.disconnect();
                if (!resp2.contains("\"value\"")) { runSync(sender, ChatColor.RED + "У игрока нет скина."); return; }
                String texture  = resp2.split("\"value\"\\s*:\\s*\"")[1].split("\"")[0];
                String signature = resp2.contains("\"signature\"") ? resp2.split("\"signature\"\\s*:\\s*\"")[1].split("\"")[0] : null;

                getServer().getScheduler().runTask(this, () -> {
                    getConfig().set("skin.texture", texture);
                    getConfig().set("skin.signature", signature);
                    saveConfig();
                    if (fakeNpc != null && fakeNpc.isCreated()) {
                        fakeNpc.setSkin(texture, signature);
                        sender.sendMessage(ChatColor.GREEN + "✔ Скин '" + username + "' установлен!");
                    } else {
                        sender.sendMessage(ChatColor.YELLOW + "Скин сохранён. Создайте NPC: /cosmeticbot spawn");
                    }
                });
            } catch (Exception e) {
                runSync(sender, ChatColor.RED + "Ошибка загрузки скина: " + e.getMessage());
            }
        });
    }

    private void runSync(CommandSender target, String msg) {
        getServer().getScheduler().runTask(this, () -> target.sendMessage(msg));
    }

    // ==================== CosmeticItem ====================

    public static class CosmeticItem {
        public final String iaId;
        public final Material fallback;
        public final String displayName;
        public final double price;

        public CosmeticItem(String iaId, Material fallback, String displayName, double price) {
            this.iaId = iaId;
            this.fallback = fallback;
            this.displayName = displayName;
            this.price = price;
        }
    }
}
