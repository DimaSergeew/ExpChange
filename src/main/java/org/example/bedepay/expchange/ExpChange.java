package org.example.bedepay.expchange;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public final class ExpChange extends JavaPlugin implements Listener, TabCompleter {

    private double commission;
    private Material itemMaterial;
    private String bookDisplayName;
    private boolean bookGlowing;
    private String[] bookLore;
    private String enchantmentName;
    private String[] enchantments;
    private Map<UUID, Long> cooldowns = new HashMap<>();
    private int cooldownTime;
    private boolean soundEffectsEnabled;
    private String createSound;
    private String useSound;
    private boolean particleEffectsEnabled;
    private int maxXpPerBook;
    private int minXpForConversion;
    private Map<String, String> messages = new HashMap<>();
    private boolean useActionBar;
    private boolean useTierSystem;
    private Map<String, BookTier> bookTiers = new HashMap<>();

    // Класс для тиеров книг
    private class BookTier {
        private final String name;
        private final String displayColor;
        private final Particle particle;
        private final int minXp;
        private final int maxXp;

        public BookTier(String name, String displayColor, String particleName, int minXp, int maxXp) {
            this.name = name;
            this.displayColor = displayColor;
            this.particle = getParticle(particleName);
            this.minXp = minXp;
            this.maxXp = maxXp;
        }

        public String getName() {
            return name;
        }

        public String getDisplayColor() {
            return displayColor;
        }

        public Particle getParticle() {
            return particle;
        }

        public int getMinXp() {
            return minXp;
        }

        public int getMaxXp() {
            return maxXp;
        }

        private Particle getParticle(String name) {
            try {
                return Particle.valueOf(name);
            } catch (IllegalArgumentException e) {
                return Particle.ENCHANTMENT_TABLE;
            }
        }
    }

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        
        // Проверяем, зарегистрированы ли команды
        if (getCommand("expchange") != null) {
            getCommand("expchange").setTabCompleter(this);
            getCommand("expchange").setExecutor(this);
        } else {
            getLogger().severe("Команда 'expchange' не найдена! Проверьте файл plugin.yml");
        }
        
        if (getCommand("givexpbook") != null) {
            getCommand("givexpbook").setExecutor(this);
        } else {
            getLogger().severe("Команда 'givexpbook' не найдена! Проверьте файл plugin.yml");
        }
        
        if (getCommand("help") != null) {
            getCommand("help").setTabCompleter(this);
            getCommand("help").setExecutor(this);
        } else {
            getLogger().severe("Команда 'help' не найдена! Проверьте файл plugin.yml");
        }
        
        saveDefaultConfig();
        loadConfig();
        
        // Отображаем красивый баннер при запуске
        displayBanner();
    }

    private void displayBanner() {
        String[] banner = {
            "§b╔═══════════════════════════════════════════════════════╗",
            "§b║                                                       ║",
            "§b║  §e█████╗ §6██╗  ██╗§a██████╗   §b███████╗§d██╗  ██╗§c██████╗   §b║",
            "§b║  §e██╔══╝ §6╚██╗██╔╝§a██╔══██╗  §b██╔════╝§d╚██╗██╔╝§c██╔══██╗  §b║",
            "§b║  §e█████╗  §6╚███╔╝ §a██████╔╝  §b█████╗  §d ╚███╔╝ §c██████╔╝  §b║",
            "§b║  §e██╔══╝  §6██╔██╗ §a██╔═══╝   §b██╔══╝  §d ██╔██╗ §c██╔═══╝   §b║",
            "§b║  §e█████╗ §6██╔╝ ██╗§a██║       §b███████╗§d██╔╝ ██╗§c██║       §b║",
            "§b║  §e╚════╝ §6╚═╝  ╚═╝§a╚═╝       §b╚══════╝§d╚═╝  ╚═╝§c╚═╝       §b║",
            "§b║                                                       ║",
            "§b║           §fВерсия: §a" + this.getPluginMeta().getVersion() + "    §fАвтор: §aBedePay§b           ║",
            "§b╚═══════════════════════════════════════════════════════╝"
        };
        
        for (String line : banner) {
            getServer().getConsoleSender().sendMessage(line);
        }
        
        getLogger().info("ExpChange успешно запущен! Наслаждайтесь игрой!");
    }

    private void loadConfig() {
        reloadConfig();
        commission = getConfig().getDouble("commission", 0.1);
        
        String materialName = getConfig().getString("item.material", "BOOK");
        itemMaterial = Material.matchMaterial(materialName);
        if (itemMaterial == null) {
            getLogger().warning("Неверный материал в конфигурации: " + materialName + ", будет использован BOOK");
            itemMaterial = Material.BOOK;
        }
        
        bookDisplayName = getConfig().getString("book.display_name", "Хранитель опыта");
        bookGlowing = getConfig().getBoolean("book.glowing", true);
        bookLore = getConfig().getStringList("book.lore").toArray(new String[0]);
        enchantmentName = getConfig().getString("book.enchantment_name", "Опыта");
        enchantments = getConfig().getStringList("book.enchantments").toArray(new String[0]);
        
        cooldownTime = getConfig().getInt("cooldown", 60);
        soundEffectsEnabled = getConfig().getBoolean("sound_effects.enabled", true);
        createSound = validateSound(getConfig().getString("sound_effects.create", "ENTITY_EXPERIENCE_ORB_PICKUP"));
        useSound = validateSound(getConfig().getString("sound_effects.use", "ENTITY_PLAYER_LEVELUP"));
        particleEffectsEnabled = getConfig().getBoolean("particle_effects", true);
        maxXpPerBook = getConfig().getInt("max_xp_per_book", 1000000);
        minXpForConversion = getConfig().getInt("min_xp_for_conversion", 100);
        
        // Визуальные улучшения
        useActionBar = getConfig().getBoolean("visual.use_action_bar", true);
        useTierSystem = getConfig().getBoolean("visual.use_tier_system", true);
        
        // Загрузка тиеров для книг
        loadBookTiers();
        
        // Загрузка сообщений
        loadMessages();
    }
    
    private void loadBookTiers() {
        bookTiers.clear();
        
        if (!useTierSystem) {
            return;
        }
        
        if (getConfig().isConfigurationSection("book_tiers")) {
            for (String tierKey : getConfig().getConfigurationSection("book_tiers").getKeys(false)) {
                String name = getConfig().getString("book_tiers." + tierKey + ".name", "Обычный");
                String color = getConfig().getString("book_tiers." + tierKey + ".color", "§7");
                String particle = getConfig().getString("book_tiers." + tierKey + ".particle", "ENCHANTMENT_TABLE");
                int minXp = getConfig().getInt("book_tiers." + tierKey + ".min_xp", 0);
                int maxXp = getConfig().getInt("book_tiers." + tierKey + ".max_xp", 1000000);
                
                bookTiers.put(tierKey, new BookTier(name, color, particle, minXp, maxXp));
            }
        }
        
        // Если нет тиеров в конфиге, создаем стандартные
        if (bookTiers.isEmpty()) {
            bookTiers.put("common", new BookTier("Обычный", "§7", "ENCHANTMENT_TABLE", 0, 1000));
            bookTiers.put("uncommon", new BookTier("Необычный", "§a", "VILLAGER_HAPPY", 1001, 5000));
            bookTiers.put("rare", new BookTier("Редкий", "§9", "END_ROD", 5001, 20000));
            bookTiers.put("epic", new BookTier("Эпический", "§5", "DRAGON_BREATH", 20001, 50000));
            bookTiers.put("legendary", new BookTier("Легендарный", "§6", "TOTEM", 50001, 1000000));
        }
    }

    private String validateSound(String soundName) {
        try {
            Sound.valueOf(soundName);
            return soundName;
        } catch (IllegalArgumentException e) {
            getLogger().warning("Неверный звук в конфигурации: " + soundName + ", будет использован звук по умолчанию");
            return "ENTITY_EXPERIENCE_ORB_PICKUP";
        }
    }

    private void loadMessages() {
        if (getConfig().isConfigurationSection("messages")) {
            for (String key : getConfig().getConfigurationSection("messages").getKeys(false)) {
                messages.put(key, getConfig().getString("messages." + key));
            }
        }
        
        // Дефолтные сообщения, если не найдены в конфиге
        if (!messages.containsKey("no_permission")) 
            messages.put("no_permission", "§cУ вас нет прав для использования этой команды!");
        if (!messages.containsKey("cooldown")) 
            messages.put("cooldown", "§cПодождите %time% секунд перед следующим использованием!");
        if (!messages.containsKey("not_enough_xp_for_conversion")) 
            messages.put("not_enough_xp_for_conversion", "§cНедостаточно опыта! Минимум: %min_xp%");
        if (!messages.containsKey("inventory_full")) 
            messages.put("inventory_full", "§cОсвободите место в инвентаре!");
        if (!messages.containsKey("exchange_success")) 
            messages.put("exchange_success", "§aВы успешно конвертировали %xp_total% опыта (комиссия %commission_percent%: %commission_amount%) в %xp_final% опыта!");
        if (!messages.containsKey("invalid_book")) 
            messages.put("invalid_book", "§cЭта книга недействительна!");
        if (!messages.containsKey("too_much_xp")) 
            messages.put("too_much_xp", "§cСлишком много опыта! Максимум: %max_xp%");
        if (!messages.containsKey("use_success")) 
            messages.put("use_success", "§aВы получили %xp% опыта!");
    }

    private String getMessage(String key) {
        return messages.getOrDefault(key, "§cСообщение не найдено: " + key);
    }

    @Override
    public void onDisable() {
        getLogger().info("ExpChange выключен!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("help") || (command.getName().equalsIgnoreCase("expchange") && (args.length > 0 && args[0].equalsIgnoreCase("help")))) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Только игроки могут использовать эту команду.");
                return false;
            }
            
            Player player = (Player) sender;
            
            // Проверка кулдауна для help
            if (hasCooldown(player, "help")) {
                long timeLeft = getCooldownTimeLeft(player, "help");
                sendMessage(player, getMessage("cooldown")
                    .replace("%time%", String.valueOf(timeLeft)));
                return false;
            }
            
            // Устанавливаем кулдаун для help (10 секунд)
            setCooldown(player, "help", 10);
            
            // Отправляем справку
            if (messages.containsKey("help_message")) {
                String[] helpLines = getMessage("help_message").split("\n");
                for (String line : helpLines) {
                    player.sendMessage(line);
                }
            } else {
                String[] helpMessage = {
                    "§6§l=== Помощь по плагину ExpChange ===",
                    "§e/expchange <количество> exp §7- Конвертировать указанное количество опыта в книгу",
                    "§e/expchange <количество> lvl §7- Конвертировать опыт для указанных уровней в книгу",
                    "§e/expchange all §7- Конвертировать весь опыт в книгу",
                    "§e/expchange help §7- Показать это сообщение",
                    "§7§oПример: §e/expchange 1000 exp §7§oсоздаст книгу с 1000 опыта",
                    "§7§oПример: §e/expchange 5 lvl §7§oсоздаст книгу с опытом для 5 уровней",
                    "§6§l==============================="
                };
                
                for (String line : helpMessage) {
                    player.sendMessage(line);
                }
            }
            
            return true;
        } else if (command.getName().equalsIgnoreCase("expchange")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("expchange.admin")) {
                    sendMessage(sender, getMessage("no_permission"));
                    return false;
                }
                loadConfig();
                sendMessage(sender, "§aКонфигурация перезагружена!");
                return true;
            }

            if (!(sender instanceof Player)) {
                sender.sendMessage("Только игроки могут использовать эту команду.");
                return false;
            }

            Player player = (Player) sender;
            if (!player.hasPermission("expchange.use")) {
                sendMessage(player, getMessage("no_permission"));
                return false;
            }

            // Проверка кулдауна
            if (hasCooldown(player, "expchange")) {
                long timeLeft = getCooldownTimeLeft(player, "expchange");
                sendMessage(player, getMessage("cooldown")
                    .replace("%time%", String.valueOf(timeLeft)));
                return false;
            }

            int totalXp = getTotalExperience(player);
            
            // Проверка минимального количества опыта
            if (totalXp < minXpForConversion) {
                sendMessage(player, getMessage("not_enough_xp_for_conversion")
                    .replace("%min_xp%", String.valueOf(minXpForConversion)));
                return false;
            }

            // Переменные для хранения количества опыта для конвертации
            int xpToTake = 0;
            
            // Проверяем аргументы для формата команды
            if (args.length == 0) {
                // Конвертируем весь опыт, когда команда /expchange введена без аргументов
                xpToTake = totalXp;
            } else if (args.length == 1) {
                // Если только один аргумент
                if (args[0].equalsIgnoreCase("all")) {
                    xpToTake = totalXp;
                } else {
                    try {
                        int amount = Integer.parseInt(args[0]);
                        sendMessage(player, "§eПожалуйста, укажите единицы измерения: §f/expchange " + amount + " exp §7или §f/expchange " + amount + " lvl");
                        return true;
                    } catch (NumberFormatException e) {
                        sendMessage(player, "§cНеверный формат! Используйте: §f/expchange <количество> exp §7или §f/expchange <количество> lvl");
                        return false;
                    }
                }
            } else if (args.length >= 2) {
                try {
                    int amount = Integer.parseInt(args[0]);
                    String unit = args[1].toLowerCase();
                    
                    if (unit.equals("exp") || unit.equals("xp")) {
                        // Конвертируем конкретное количество опыта
                        xpToTake = Math.min(amount, totalXp);
                    } else if (unit.equals("lvl") || unit.equals("level")) {
                        // Конвертируем опыт, эквивалентный указанному количеству уровней
                        int xpForLevels = getExpToLevel(amount);
                        xpToTake = Math.min(xpForLevels, totalXp);
                    } else {
                        sendMessage(player, "§cНеизвестная единица измерения! Используйте: §f/expchange <количество> exp §7или §f/expchange <количество> lvl");
                        return false;
                    }
                    
                    if (xpToTake <= 0) {
                        sendMessage(player, "§cКоличество опыта должно быть положительным числом!");
                        return false;
                    }
                    
                } catch (NumberFormatException e) {
                    sendMessage(player, "§cНеверный формат числа! Используйте: §f/expchange <количество> exp §7или §f/expchange <количество> lvl");
                    return false;
                }
            }
            
            // Проверка максимального значения
            if (xpToTake > maxXpPerBook) {
                sendMessage(player, getMessage("too_much_xp")
                    .replace("%max_xp%", String.valueOf(maxXpPerBook)));
                return false;
            }
            
            if (xpToTake <= 0) {
                sendMessage(player, "§cНедостаточно опыта для конвертации!");
                return false;
            }

            // Проверяем, есть ли место в инвентаре
            if (player.getInventory().firstEmpty() == -1) {
                sendMessage(player, getMessage("inventory_full"));
                return false;
            }

            // Рассчитываем комиссию
            int commissionAmount = (int) (xpToTake * commission);
            
            // Вычисляем итоговое количество опыта для книги
            int xpToGive = xpToTake - commissionAmount;
            
            if (xpToGive <= 0) {
                sendMessage(player, "§cСлишком мало опыта для конвертации!");
                return false;
            }

            setTotalExperience(player, totalXp - xpToTake);
            
            ItemStack book = createExpBook(xpToGive);
            player.getInventory().addItem(book);
            
            playEffects(player, true, xpToGive);
            setCooldown(player, "expchange", cooldownTime);
            
            sendMessage(player, getMessage("exchange_success")
                .replace("%xp_total%", String.valueOf(xpToTake))
                .replace("%commission_percent%", String.format("%.1f%%", commission * 100))
                .replace("%commission_amount%", String.valueOf(commissionAmount))
                .replace("%xp_final%", String.valueOf(xpToGive)));
            
            return true;
        } else if (command.getName().equalsIgnoreCase("givexpbook")) {
            if (!sender.hasPermission("expchange.admin")) {
                sendMessage(sender, getMessage("no_permission"));
                return false;
            }

            if (args.length < 2) {
                sendMessage(sender, "§cИспользование: /givexpbook <игрок> <количество>");
                return false;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sendMessage(sender, "§cИгрок не найден!");
                return false;
            }

            try {
                int xp = Integer.parseInt(args[1]);
                
                if (xp <= 0) {
                    sendMessage(sender, "§cКоличество опыта должно быть положительным числом!");
                    return false;
                }
                
                if (xp > maxXpPerBook) {
                    sendMessage(sender, getMessage("too_much_xp")
                        .replace("%max_xp%", String.valueOf(maxXpPerBook)));
                    return false;
                }
                
                ItemStack book = createExpBook(xp);
                target.getInventory().addItem(book);
                sendMessage(sender, "§aКнига с " + xp + " опыта выдана игроку " + target.getName());
                
                // Эффект для получения книги
                playEffects(target, true, xp);
                
                return true;
            } catch (NumberFormatException e) {
                sendMessage(sender, "§cНеверное количество опыта!");
                return false;
            }
        }
        return false;
    }

    // Метод для отправки сообщений (с поддержкой ActionBar)
    private void sendMessage(CommandSender sender, String message) {
        if (!(sender instanceof Player) || !useActionBar) {
            sender.sendMessage(message);
            return;
        }
        
        Player player = (Player) sender;
        
        // Если сообщение содержит маркер действия ActionBar
        if (message.startsWith("@actionbar ")) {
            String actionBarMsg = message.substring("@actionbar ".length());
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, 
                                      new TextComponent(actionBarMsg));
        } else {
            player.sendMessage(message);
        }
    }

    @EventHandler
    public void onPlayerUseItem(PlayerInteractEvent event) {
        if (event.getItem() != null && event.getItem().getType() == itemMaterial) {
            Player player = event.getPlayer();
            ItemMeta meta = event.getItem().getItemMeta();
            
            if (meta == null || !isValidExpBook(meta)) {
                return;
            }
            
            event.setCancelled(true);
            int xpAmount = meta.getPersistentDataContainer().get(new NamespacedKey(this, "xp_amount"), PersistentDataType.INTEGER);
            
            if (xpAmount <= 0) {
                sendMessage(player, getMessage("invalid_book"));
                return;
            }
            
            if (xpAmount > maxXpPerBook) {
                sendMessage(player, getMessage("too_much_xp")
                    .replace("%max_xp%", String.valueOf(maxXpPerBook)));
                return;
            }
            
            player.giveExp(xpAmount);
            event.getItem().setAmount(event.getItem().getAmount() - 1);
            
            playEffects(player, false, xpAmount);
            
            sendMessage(player, getMessage("use_success")
                .replace("%xp%", String.valueOf(xpAmount)));
        }
    }

    private boolean isValidExpBook(ItemMeta meta) {
        return meta.getPersistentDataContainer().has(new NamespacedKey(this, "xp_amount"), PersistentDataType.INTEGER) &&
               (!bookGlowing || meta.hasEnchant(Enchantment.DURABILITY));
    }

    private String[] replacePlaceholders(String[] lore, int xpAmount) {
        String[] newLore = Arrays.copyOf(lore, lore.length);
        
        BookTier tier = getTierForXp(xpAmount);
        String tierName = tier != null ? tier.getName() : "Обычный";
        String tierColor = tier != null ? tier.getDisplayColor() : "§7";
        int level = getLevelFromExp(xpAmount);
        
        for (int i = 0; i < newLore.length; i++) {
            newLore[i] = newLore[i]
                .replace("%xp%", String.valueOf(xpAmount))
                .replace("%tier_name%", tierName)
                .replace("%tier_color%", tierColor)
                .replace("%level%", String.valueOf(level));
        }
        return newLore;
    }

    private ItemStack createExpBook(int xpAmount) {
        ItemStack book = new ItemStack(itemMaterial);
        ItemMeta meta = book.getItemMeta();
        
        if (meta != null) {
            setXpAmount(meta, xpAmount);
            setBookDisplayNameAndLore(meta, xpAmount);
            applyGlowing(meta);
            
            // Добавляем флаги для улучшения внешнего вида
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
            
            // Добавляем тир в скрытые данные для использования в других плагинах/событиях
            BookTier tier = getTierForXp(xpAmount);
            if (tier != null) {
                meta.getPersistentDataContainer().set(
                    new NamespacedKey(this, "xp_tier"), 
                    PersistentDataType.STRING, 
                    tier.getName()
                );
            }
        }

        book.setItemMeta(meta);
        return book;
    }

    private void setXpAmount(ItemMeta meta, int xpAmount) {
        meta.getPersistentDataContainer().set(new NamespacedKey(this, "xp_amount"), PersistentDataType.INTEGER, xpAmount);
    }

    private void setBookDisplayNameAndLore(ItemMeta meta, int xpAmount) {
        BookTier tier = getTierForXp(xpAmount);
        String tierColor = tier != null ? tier.getDisplayColor() : "§e";
        
        // Применяем цвет тира к имени книги
        String displayName = tierColor + bookDisplayName.replace("%xp%", String.valueOf(xpAmount));
        
        if (tier != null) {
            displayName = tierColor + "【" + tier.getName() + "】 " + bookDisplayName.replace("%xp%", String.valueOf(xpAmount));
        }
        
        meta.setDisplayName(displayName);
        meta.setLore(Arrays.asList(replacePlaceholders(bookLore, xpAmount)));
    }

    private void applyGlowing(ItemMeta meta) {
        if (bookGlowing) {
            meta.addEnchant(Enchantment.DURABILITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
    }

    private boolean hasCooldown(Player player, String type) {
        if (!typedCooldowns.containsKey(player.getUniqueId())) {
            return false;
        }
        Map<String, Long> playerCooldowns = typedCooldowns.get(player.getUniqueId());
        return playerCooldowns.containsKey(type) && System.currentTimeMillis() < playerCooldowns.get(type);
    }

    private void setCooldown(Player player, String type, int seconds) {
        if (!typedCooldowns.containsKey(player.getUniqueId())) {
            typedCooldowns.put(player.getUniqueId(), new HashMap<>());
        }
        typedCooldowns.get(player.getUniqueId()).put(type, System.currentTimeMillis() + (seconds * 1000L));
    }

    private long getCooldownTimeLeft(Player player, String type) {
        Map<String, Long> playerCooldowns = typedCooldowns.get(player.getUniqueId());
        if (playerCooldowns != null && playerCooldowns.containsKey(type)) {
            return (playerCooldowns.get(type) - System.currentTimeMillis()) / 1000;
        }
        return 0;
    }

    private int getTotalExperience(Player player) {
        int level = player.getLevel();
        int exp = 0;
        
        if (level <= 16) {
            exp = (int) (Math.pow(level, 2) + 6 * level);
        } else if (level <= 31) {
            exp = (int) (2.5 * Math.pow(level, 2) - 40.5 * level + 360);
        } else {
            exp = (int) (4.5 * Math.pow(level, 2) - 162.5 * level + 2220);
        }
        
        float levelProgress = player.getExp();
        int currentLevelExp = getExpToNextLevel(level);
        exp += Math.round(currentLevelExp * levelProgress);
        
        return exp;
    }

    private int getExpToNextLevel(int level) {
        if (level <= 15) {
            return 2 * level + 7;
        } else if (level <= 30) {
            return 5 * level - 38;
        } else {
            return 9 * level - 158;
        }
    }

    private void setTotalExperience(Player player, int exp) {
        player.setTotalExperience(0);
        player.setLevel(0);
        player.setExp(0);
        
        while (exp > 0) {
            int expToLevel = getExpToNextLevel(player.getLevel());
            if (exp >= expToLevel) {
                exp -= expToLevel;
                player.giveExp(expToLevel);
            } else {
                player.giveExp(exp);
                exp = 0;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("expchange")) {
            if (args.length == 1) {
                List<String> suggestions = new ArrayList<>();
                suggestions.add("help");
                suggestions.add("reload");
                suggestions.add("all");
                
                // Добавляем примеры цифр
                suggestions.add("100");
                suggestions.add("1000");
                suggestions.add("5000");
                
                return suggestions;
            } else if (args.length == 2) {
                // Если первый аргумент число, предлагаем единицы измерения
                try {
                    Integer.parseInt(args[0]);
                    return Arrays.asList("exp", "lvl");
                } catch (NumberFormatException e) {
                    // Если первый аргумент не число, то это команда
                    return null;
                }
            }
        } else if (command.getName().equalsIgnoreCase("givexpbook")) {
            if (args.length == 1) {
                List<String> players = new ArrayList<>();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    players.add(player.getName());
                }
                return players;
            } else if (args.length == 2) {
                return Arrays.asList("100", "1000", "5000");
            }
        } else if (command.getName().equalsIgnoreCase("help")) {
            return Arrays.asList("expchange");
        }
        return null;
    }

    // Улучшенный метод для воспроизведения эффектов (звуки и частицы)
    private void playEffects(Player player, boolean isCreate, int xpAmount) {
        if (soundEffectsEnabled) {
            try {
                Sound sound = Sound.valueOf(isCreate ? createSound : useSound);
                player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
            } catch (IllegalArgumentException e) {
                getLogger().warning("Неверный звук: " + (isCreate ? createSound : useSound));
            }
        }
        
        if (particleEffectsEnabled) {
            BookTier tier = getTierForXp(xpAmount);
            Particle particle = tier != null ? tier.getParticle() : 
                (isCreate ? Particle.PORTAL : Particle.ENCHANTMENT_TABLE);
            
            // Создаем эффект спирали из частиц
            spawnSpiralParticle(player, particle, isCreate);
        }
    }
    
    private void spawnSpiralParticle(Player player, Particle particle, boolean isCreation) {
        final double radius = 1.0;
        final int particles = 40;
        final double height = 2.0;
        
        for (int i = 0; i < particles; i++) {
            double angle = (double) i / particles * Math.PI * 2;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            double y = (double) i / particles * height;
            
            player.getWorld().spawnParticle(
                particle,
                player.getLocation().add(x, y, z),
                3, 0.05, 0.05, 0.05, 0.01
            );
        }
        
        // Дополнительные эффекты для создания книги
        if (isCreation) {
            player.getWorld().spawnParticle(
                Particle.EXPLOSION_NORMAL,
                player.getLocation().add(0, 1, 0),
                10, 0.5, 0.5, 0.5, 0.1
            );
        }
    }
    
    private BookTier getTierForXp(int xpAmount) {
        if (!useTierSystem) {
            return null;
        }
        
        for (BookTier tier : bookTiers.values()) {
            if (xpAmount >= tier.getMinXp() && xpAmount <= tier.getMaxXp()) {
                return tier;
            }
        }
        
        // Возвращаем самый высокий тир по умолчанию
        return bookTiers.values().stream()
                .max((t1, t2) -> Integer.compare(t1.getMaxXp(), t2.getMaxXp()))
                .orElse(null);
    }

    private int getLevelFromExp(int exp) {
        int level = 0;
        int totalExp = 0;
        
        while (true) {
            int expToNextLevel = getExpToNextLevel(level);
            if (totalExp + expToNextLevel > exp) {
                break;
            }
            totalExp += expToNextLevel;
            level++;
        }
        
        return level;
    }

    // Добавляем новые методы для работы с разными типами кулдаунов
    private Map<UUID, Map<String, Long>> typedCooldowns = new HashMap<>();

    // Обновляем старые методы кулдауна, чтобы они использовали новую систему
    private boolean hasCooldown(Player player) {
        return hasCooldown(player, "expchange");
    }

    private void setCooldown(Player player) {
        setCooldown(player, "expchange", cooldownTime);
    }

    private long getCooldownTimeLeft(Player player) {
        return getCooldownTimeLeft(player, "expchange");
    }

    // Добавляем метод для расчета опыта, необходимого для достижения указанного уровня
    private int getExpToLevel(int targetLevel) {
        int xp = 0;
        
        if (targetLevel <= 16) {
            xp = (int) (targetLevel * targetLevel + 6 * targetLevel);
        } else if (targetLevel <= 31) {
            xp = (int) (2.5 * targetLevel * targetLevel - 40.5 * targetLevel + 360);
        } else {
            xp = (int) (4.5 * targetLevel * targetLevel - 162.5 * targetLevel + 2220);
        }
        
        return xp;
    }
}