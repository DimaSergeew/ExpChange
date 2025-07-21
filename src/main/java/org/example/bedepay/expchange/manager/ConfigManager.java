package org.example.bedepay.expchange.manager;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.plugin.java.JavaPlugin;
import org.example.bedepay.expchange.model.BookTier;

import java.util.HashMap;
import java.util.Map;

/**
 * Менеджер конфигурации плагина
 */
public class ConfigManager {
    private final JavaPlugin plugin;
    
    // Настройки
    private double commission;
    private Material itemMaterial;
    private String bookDisplayName;
    private boolean bookGlowing;
    private String[] bookLore;
    private String enchantmentName;
    private String[] enchantments;
    private int cooldownTime;
    private boolean soundEffectsEnabled;
    private String createSound;
    private String useSound;
    private boolean particleEffectsEnabled;
    private int maxXpPerBook;
    private int minXpForConversion;
    private boolean useActionBar;
    private boolean useTierSystem;
    private Map<String, BookTier> bookTiers = new HashMap<>();

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        plugin.reloadConfig();
        commission = plugin.getConfig().getDouble("commission", 0.1);
        
        String materialName = plugin.getConfig().getString("item.material", "BOOK");
        itemMaterial = Material.matchMaterial(materialName);
        if (itemMaterial == null) {
            plugin.getLogger().warning("Неверный материал в конфигурации: " + materialName + ", будет использован BOOK");
            itemMaterial = Material.BOOK;
        }
        
        bookDisplayName = plugin.getConfig().getString("book.display_name", "Хранитель опыта");
        bookGlowing = plugin.getConfig().getBoolean("book.glowing", true);
        bookLore = plugin.getConfig().getStringList("book.lore").toArray(new String[0]);
        enchantmentName = plugin.getConfig().getString("book.enchantment_name", "Опыта");
        enchantments = plugin.getConfig().getStringList("book.enchantments").toArray(new String[0]);
        
        cooldownTime = plugin.getConfig().getInt("cooldown", 60);
        soundEffectsEnabled = plugin.getConfig().getBoolean("sound_effects.enabled", true);
        createSound = validateSound(plugin.getConfig().getString("sound_effects.create", "ENTITY_EXPERIENCE_ORB_PICKUP"));
        useSound = validateSound(plugin.getConfig().getString("sound_effects.use", "ENTITY_PLAYER_LEVELUP"));
        particleEffectsEnabled = plugin.getConfig().getBoolean("particle_effects", true);
        maxXpPerBook = plugin.getConfig().getInt("max_xp_per_book", 1000000);
        minXpForConversion = plugin.getConfig().getInt("min_xp_for_conversion", 100);
        
        useActionBar = plugin.getConfig().getBoolean("visual.use_action_bar", true);
        useTierSystem = plugin.getConfig().getBoolean("visual.use_tier_system", true);
        
        loadBookTiers();
    }

    private void loadBookTiers() {
        bookTiers.clear();
        
        if (!useTierSystem) {
            return;
        }
        
        if (plugin.getConfig().isConfigurationSection("book_tiers")) {
            for (String tierKey : plugin.getConfig().getConfigurationSection("book_tiers").getKeys(false)) {
                String name = plugin.getConfig().getString("book_tiers." + tierKey + ".name", "Обычный");
                String color = plugin.getConfig().getString("book_tiers." + tierKey + ".color", "§7");
                String particle = plugin.getConfig().getString("book_tiers." + tierKey + ".particle", "ENCHANTMENT_TABLE");
                int minXp = plugin.getConfig().getInt("book_tiers." + tierKey + ".min_xp", 0);
                int maxXp = plugin.getConfig().getInt("book_tiers." + tierKey + ".max_xp", 1000000);
                
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
            plugin.getLogger().warning("Неверный звук в конфигурации: " + soundName + ", будет использован звук по умолчанию");
            return "ENTITY_EXPERIENCE_ORB_PICKUP";
        }
    }

    // Геттеры
    public double getCommission() { return commission; }
    public Material getItemMaterial() { return itemMaterial; }
    public String getBookDisplayName() { return bookDisplayName; }
    public boolean isBookGlowing() { return bookGlowing; }
    public String[] getBookLore() { return bookLore; }
    public String getEnchantmentName() { return enchantmentName; }
    public String[] getEnchantments() { return enchantments; }
    public int getCooldownTime() { return cooldownTime; }
    public boolean isSoundEffectsEnabled() { return soundEffectsEnabled; }
    public String getCreateSound() { return createSound; }
    public String getUseSound() { return useSound; }
    public boolean isParticleEffectsEnabled() { return particleEffectsEnabled; }
    public int getMaxXpPerBook() { return maxXpPerBook; }
    public int getMinXpForConversion() { return minXpForConversion; }
    public boolean isUseActionBar() { return useActionBar; }
    public boolean isUseTierSystem() { return useTierSystem; }
    public Map<String, BookTier> getBookTiers() { return bookTiers; }
} 