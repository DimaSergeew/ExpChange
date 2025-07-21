package org.example.bedepay.expchange.handler;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerLevelChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.example.bedepay.expchange.manager.*;
import org.example.bedepay.expchange.util.ExperienceUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Обработчик событий плагина
 */
public class ExpChangeEventHandler implements Listener {
    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final MessageManager messageManager;
    private final BookManager bookManager;
    private final EffectManager effectManager;
    private final CooldownManager cooldownManager;
    
    // Отслеживание последнего известного опыта игроков
    private Map<UUID, Integer> lastKnownExp = new HashMap<>();

    public ExpChangeEventHandler(JavaPlugin plugin, ConfigManager configManager, MessageManager messageManager,
                       BookManager bookManager, EffectManager effectManager, CooldownManager cooldownManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.messageManager = messageManager;
        this.bookManager = bookManager;
        this.effectManager = effectManager;
        this.cooldownManager = cooldownManager;
    }

    @EventHandler
    public void onPlayerUseItem(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item == null || item.getType() != configManager.getItemMaterial()) {
            return;
        }
        
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }
        
        // Проверяем права на использование
        if (!player.hasPermission("expchange.use")) {
            return;
        }
        
        ItemMeta meta = item.getItemMeta();
        
        // Используем улучшенную валидацию
        if (!bookManager.isValidExpBook(meta)) {
            return;
        }
        
        event.setCancelled(true);
        
        // Получаем количество опыта
        int xpAmount = bookManager.getXpFromBook(meta);
        
        if (xpAmount <= 0) {
            messageManager.sendMessage(player, messageManager.getMessage("invalid_book"));
            return;
        }
        
        if (xpAmount > configManager.getMaxXpPerBook()) {
            messageManager.sendMessage(player, messageManager.getMessage("too_much_xp")
                .replace("%max_xp%", String.valueOf(configManager.getMaxXpPerBook())));
            return;
        }
        
        try {
            player.giveExp(xpAmount);
            item.setAmount(item.getAmount() - 1);
            
            effectManager.playEffects(player, false, xpAmount);
            
            messageManager.sendMessage(player, messageManager.getMessage("use_success")
                .replace("%xp%", String.valueOf(xpAmount)));
        } catch (Exception e) {
            plugin.getLogger().severe("Ошибка при использовании книги опыта игроком " + player.getName() + ": " + e.getMessage());
            messageManager.sendMessage(player, "§cПроизошла ошибка при использовании книги опыта!");
        }
    }

    // Отслеживание изменений опыта (современный Paper API)
    @EventHandler
    public void onPlayerExpChange(PlayerExpChangeEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();
        
        // Используем современный Paper API метод для получения опыта
        int currentExp = player.calculateTotalExperiencePoints();
        lastKnownExp.put(playerUUID, currentExp);
    }

    // Отслеживание изменений уровня (включая команды /xp)
    @EventHandler
    public void onPlayerLevelChange(PlayerLevelChangeEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();
        
        // Используем современный Paper API метод для получения опыта
        int currentExp = player.calculateTotalExperiencePoints();
        lastKnownExp.put(playerUUID, currentExp);
    }

    // УЛУЧШЕННАЯ ЗАЩИТА ОТ ИСПОЛЬЗОВАНИЯ КНИГ ОПЫТА В РАЗЛИЧНЫХ GUI

    // Предотвращение использования книг опыта в точиле
    @EventHandler
    public void onPrepareGrindstone(PrepareGrindstoneEvent event) {
        if (event.getInventory() == null) {
            return;
        }
        
        ItemStack topItem = event.getInventory().getItem(0);
        ItemStack bottomItem = event.getInventory().getItem(1);
        
        if ((topItem != null && bookManager.isValidExpBook(topItem.getItemMeta())) ||
            (bottomItem != null && bookManager.isValidExpBook(bottomItem.getItemMeta()))) {
            event.setResult(null);
        }
    }

    // Предотвращение использования книг опыта в наковальне
    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        if (event.getInventory() == null) {
            return;
        }
        
        ItemStack firstItem = event.getInventory().getItem(0);
        ItemStack secondItem = event.getInventory().getItem(1);
        
        if ((firstItem != null && bookManager.isValidExpBook(firstItem.getItemMeta())) ||
            (secondItem != null && bookManager.isValidExpBook(secondItem.getItemMeta()))) {
            event.setResult(null);
        }
    }

    // Предотвращение использования книг опыта в чар-столе и других GUI
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory() == null) {
            return;
        }

        // Проверяем различные типы инвентарей
        InventoryType inventoryType = event.getInventory().getType();
        
        // Запрещаем использование в чар-столе, варочной стойке, печи и других GUI
        if (inventoryType == InventoryType.ENCHANTING || 
            inventoryType == InventoryType.BREWING ||
            inventoryType == InventoryType.FURNACE ||
            inventoryType == InventoryType.BLAST_FURNACE ||
            inventoryType == InventoryType.SMOKER ||
            inventoryType == InventoryType.SMITHING ||
            inventoryType == InventoryType.CARTOGRAPHY ||
            inventoryType == InventoryType.LOOM ||
            inventoryType == InventoryType.STONECUTTER) {
            
            try {
                ItemStack clickedItem = event.getCurrentItem();
                ItemStack cursorItem = event.getCursor();
                
                // Проверяем кликнутый предмет
                if (clickedItem != null && bookManager.isValidExpBook(clickedItem.getItemMeta())) {
                    event.setCancelled(true);
                    if (event.getWhoClicked() instanceof Player) {
                        Player player = (Player) event.getWhoClicked();
                        messageManager.sendMessage(player, "§cКниги опыта нельзя использовать в " + getInventoryName(inventoryType) + "!");
                    }
                    return;
                }
                
                // Проверяем предмет в курсоре
                if (cursorItem != null && bookManager.isValidExpBook(cursorItem.getItemMeta())) {
                    event.setCancelled(true);
                    if (event.getWhoClicked() instanceof Player) {
                        Player player = (Player) event.getWhoClicked();
                        messageManager.sendMessage(player, "§cКниги опыта нельзя использовать в " + getInventoryName(inventoryType) + "!");
                    }
                    return;
                }
                
            } catch (Exception e) {
                plugin.getLogger().warning("Ошибка при обработке клика в инвентаре: " + e.getMessage());
            }
        }
    }

    // Предотвращение перетаскивания книг опыта в запрещенные GUI
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory() == null) {
            return;
        }

        InventoryType inventoryType = event.getInventory().getType();
        
        // Те же ограничения, что и для клика
        if (inventoryType == InventoryType.ENCHANTING || 
            inventoryType == InventoryType.BREWING ||
            inventoryType == InventoryType.FURNACE ||
            inventoryType == InventoryType.BLAST_FURNACE ||
            inventoryType == InventoryType.SMOKER ||
            inventoryType == InventoryType.SMITHING ||
            inventoryType == InventoryType.CARTOGRAPHY ||
            inventoryType == InventoryType.LOOM ||
            inventoryType == InventoryType.STONECUTTER) {
            
            try {
                ItemStack draggedItem = event.getOldCursor();
                
                if (draggedItem != null && bookManager.isValidExpBook(draggedItem.getItemMeta())) {
                    // Проверяем, перетаскивается ли в верхний инвентарь (GUI)
                    for (int slot : event.getRawSlots()) {
                        if (slot < event.getInventory().getSize()) {
                            event.setCancelled(true);
                            if (event.getWhoClicked() instanceof Player) {
                                Player player = (Player) event.getWhoClicked();
                                messageManager.sendMessage(player, "§cКниги опыта нельзя перетаскивать в " + getInventoryName(inventoryType) + "!");
                            }
                            return;
                        }
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Ошибка при обработке перетаскивания в инвентаре: " + e.getMessage());
            }
        }
    }

    // Предотвращение крафта с книгами опыта
    @EventHandler
    public void onPrepareItemCraft(PrepareItemCraftEvent event) {
        if (event.getInventory() == null) {
            return;
        }
        
        try {
            ItemStack[] matrix = event.getInventory().getMatrix();
            if (matrix == null) {
                return;
            }
            
            for (ItemStack item : matrix) {
                if (item != null && bookManager.isValidExpBook(item.getItemMeta())) {
                    event.getInventory().setResult(null);
                    return;
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка при проверке крафта: " + e.getMessage());
        }
    }

    // Очистка данных при выходе игрока
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerUUID = event.getPlayer().getUniqueId();
        lastKnownExp.remove(playerUUID);
        cooldownManager.removeAllCooldowns(event.getPlayer());
    }

    // Вспомогательный метод для получения читаемого названия инвентаря
    private String getInventoryName(InventoryType type) {
        switch (type) {
            case ENCHANTING: return "столе чар";
            case BREWING: return "варочной стойке";
            case FURNACE: return "печи";
            case BLAST_FURNACE: return "доменной печи";
            case SMOKER: return "коптильне";
            case SMITHING: return "кузнечном столе";
            case CARTOGRAPHY: return "картографическом столе";
            case LOOM: return "ткацком станке";
            case STONECUTTER: return "камнерезе";
            default: return "этом GUI";
        }
    }

    // Метод для получения последнего известного опыта (используется в основном классе)
    public int getLastKnownExp(UUID playerUUID) {
        return lastKnownExp.getOrDefault(playerUUID, 0);
    }

    // Метод для установки последнего известного опыта
    public void setLastKnownExp(UUID playerUUID, int exp) {
        lastKnownExp.put(playerUUID, exp);
    }

    // Очистка всех данных
    public void clear() {
        lastKnownExp.clear();
    }
} 