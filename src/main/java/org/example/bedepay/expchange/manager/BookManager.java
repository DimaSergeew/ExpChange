package org.example.bedepay.expchange.manager;

import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.example.bedepay.expchange.model.BookTier;
import org.example.bedepay.expchange.util.ExperienceUtils;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.Arrays;

/**
 * Менеджер для работы с книгами опыта
 */
public class BookManager {
    private final JavaPlugin plugin;
    private final ConfigManager configManager;

    public BookManager(JavaPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    /**
     * Создать книгу опыта
     */
    public ItemStack createExpBook(int xpAmount) {
        ItemStack book = new ItemStack(configManager.getItemMaterial());
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
                    new NamespacedKey(plugin, "xp_tier"), 
                    PersistentDataType.STRING, 
                    tier.getName()
                );
            }
        }

        book.setItemMeta(meta);
        return book;
    }

    /**
     * Проверить, является ли предмет валидной книгой опыта
     */
    public boolean isValidExpBook(ItemMeta meta) {
        if (meta == null) {
            return false;
        }
        
        // Проверяем наличие ключа опыта
        if (!meta.getPersistentDataContainer().has(new NamespacedKey(plugin, "xp_amount"), PersistentDataType.INTEGER)) {
            return false;
        }
        
        // Проверяем корректность количества опыта
        Integer xpAmount = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "xp_amount"), PersistentDataType.INTEGER);
        if (xpAmount == null || xpAmount <= 0 || xpAmount > configManager.getMaxXpPerBook()) {
            return false;
        }
        
        // Проверяем наличие чар если включен glowing
        if (configManager.isBookGlowing() && !meta.hasEnchant(Enchantment.DURABILITY)) {
            return false;
        }
        
        return true;
    }

    /**
     * Получить количество опыта из книги
     */
    public int getXpFromBook(ItemMeta meta) {
        if (meta == null) {
            return 0;
        }
        
        Integer xpAmount = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "xp_amount"), PersistentDataType.INTEGER);
        return xpAmount != null ? xpAmount : 0;
    }

    private void setXpAmount(ItemMeta meta, int xpAmount) {
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "xp_amount"), PersistentDataType.INTEGER, xpAmount);
    }

    private void setBookDisplayNameAndLore(ItemMeta meta, int xpAmount) {
        BookTier tier = getTierForXp(xpAmount);
        String tierColor = tier != null ? tier.getDisplayColor() : "§e";
        
        // Применяем цвет тира к имени книги
        String displayName = tierColor + configManager.getBookDisplayName().replace("%xp%", String.valueOf(xpAmount));
        
        if (tier != null) {
            displayName = tierColor + "【" + tier.getName() + "】 " + configManager.getBookDisplayName().replace("%xp%", String.valueOf(xpAmount));
        }
        
        // Используем современный API вместо deprecated методов
        meta.displayName(LegacyComponentSerializer.legacySection().deserialize(displayName));
        meta.lore(Arrays.stream(replacePlaceholders(configManager.getBookLore(), xpAmount))
                .map(LegacyComponentSerializer.legacySection()::deserialize)
                .toList());
    }

    private void applyGlowing(ItemMeta meta) {
        if (configManager.isBookGlowing()) {
            meta.addEnchant(Enchantment.DURABILITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
    }

    private String[] replacePlaceholders(String[] lore, int xpAmount) {
        String[] newLore = Arrays.copyOf(lore, lore.length);
        
        BookTier tier = getTierForXp(xpAmount);
        String tierName = tier != null ? tier.getName() : "Обычный";
        String tierColor = tier != null ? tier.getDisplayColor() : "§7";
        int level = ExperienceUtils.getLevelFromExp(xpAmount);
        
        for (int i = 0; i < newLore.length; i++) {
            newLore[i] = newLore[i]
                .replace("%xp%", String.valueOf(xpAmount))
                .replace("%tier_name%", tierName)
                .replace("%tier_color%", tierColor)
                .replace("%level%", String.valueOf(level));
        }
        return newLore;
    }

    private BookTier getTierForXp(int xpAmount) {
        if (!configManager.isUseTierSystem()) {
            return null;
        }
        
        for (BookTier tier : configManager.getBookTiers().values()) {
            if (xpAmount >= tier.getMinXp() && xpAmount <= tier.getMaxXp()) {
                return tier;
            }
        }
        
        // Возвращаем самый высокий тир по умолчанию
        return configManager.getBookTiers().values().stream()
                .max((t1, t2) -> Integer.compare(t1.getMaxXp(), t2.getMaxXp()))
                .orElse(null);
    }
} 