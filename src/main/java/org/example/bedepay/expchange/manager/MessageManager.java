package org.example.bedepay.expchange.manager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.audience.Audience;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

/**
 * Современный менеджер сообщений с поддержкой Adventure API
 */
public class MessageManager {
    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final Map<String, String> messages = new HashMap<>();
    
    // Legacy сериализатор для совместимости со старыми форматами
    private final LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.legacySection();

    public MessageManager(JavaPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public void loadMessages() {
        messages.clear();
        
        // Загружаем сообщения из конфига
        if (plugin.getConfig().isConfigurationSection("messages")) {
            for (String key : plugin.getConfig().getConfigurationSection("messages").getKeys(false)) {
                messages.put(key, plugin.getConfig().getString("messages." + key, ""));
            }
        }
        
        // Устанавливаем дефолтные сообщения, если не найдены в конфиге
        setDefaultMessages();
    }

    private void setDefaultMessages() {
        messages.putIfAbsent("no_permission", "§cУ вас нет прав на использование этой команды!");
        messages.putIfAbsent("cooldown", "@actionbar §cПодождите %time% секунд перед следующим использованием!");
        messages.putIfAbsent("exchange_success", "§aОпыт успешно конвертирован в книгу!");
        messages.putIfAbsent("use_success", "@actionbar §aВы получили §e%xp% §aопыта из книги!");
        messages.putIfAbsent("invalid_book", "§cЭта книга была изменена и больше не действительна!");
        messages.putIfAbsent("inventory_full", "§cОсвободите место в инвентаре!");
    }

    public String getMessage(String key) {
        return messages.getOrDefault(key, "§cСообщение не найдено: " + key);
    }

    /**
     * Отправляет сообщение с поддержкой Adventure API
     */
    public void sendMessage(CommandSender sender, String message) {
        if (message == null || message.isEmpty()) {
            return;
        }

        // Обрабатываем специальные префиксы
        if (message.startsWith("@actionbar ")) {
            sendActionBar(sender, message.substring(11));
            return;
        }
        
        if (message.startsWith("@title ")) {
            sendTitle(sender, message.substring(7));
            return;
        }

        // Используем современные компоненты для отправки сообщений
        Component component = legacySerializer.deserialize(message);
        Audience audience = plugin.getServer().getConsoleSender().equals(sender) ? 
            Audience.audience() : (Audience) sender;
        audience.sendMessage(component);
    }

    /**
     * Отправляет сообщение в ActionBar (современный способ)
     */
    public void sendActionBar(CommandSender sender, String message) {
        if (!(sender instanceof Player)) {
            sendMessage(sender, message); // Fallback для консоли
            return;
        }
        
        Player player = (Player) sender;
        Component component = legacySerializer.deserialize(message);
        player.sendActionBar(component);
    }

    /**
     * Отправляет title сообщение
     */
    public void sendTitle(CommandSender sender, String message) {
        if (!(sender instanceof Player)) {
            sendMessage(sender, message); // Fallback для консоли
            return;
        }
        
        Player player = (Player) sender;
        Component component = legacySerializer.deserialize(message);
        player.showTitle(net.kyori.adventure.title.Title.title(component, Component.empty()));
    }

    /**
     * Отправляет справочное сообщение с современным форматированием
     */
    public void sendHelpMessage(Player player) {
        Component helpMessage = Component.text()
            .append(Component.text("=== ExpChange Справка ===", NamedTextColor.GOLD, TextDecoration.BOLD))
            .append(Component.newline())
            .append(Component.text("/expchange", NamedTextColor.YELLOW))
            .append(Component.text(" - Конвертировать весь опыт в книгу", NamedTextColor.GRAY))
            .append(Component.newline())
            .append(Component.text("/expchange <количество> exp", NamedTextColor.YELLOW))
            .append(Component.text(" - Конвертировать указанное количество опыта", NamedTextColor.GRAY))
            .append(Component.newline())
            .append(Component.text("/expchange <количество> lvl", NamedTextColor.YELLOW))
            .append(Component.text(" - Конвертировать опыт для указанных уровней", NamedTextColor.GRAY))
            .append(Component.newline())
            .append(Component.text("/expchange <количество> lvl all", NamedTextColor.YELLOW))
            .append(Component.text(" - Создать максимум книг с указанными уровнями", NamedTextColor.GRAY))
            .append(Component.newline())
            .append(Component.text("============================", NamedTextColor.GOLD, TextDecoration.BOLD))
            .build();

        player.sendMessage(helpMessage);
    }

    /**
     * Создает красивое сообщение об успешной конвертации
     */
    public void sendSuccessMessage(Player player, int xpTotal, double commissionPercent, 
                                 int commissionAmount, int xpFinal) {
        Component message = Component.text()
            .append(Component.text("✓ ", NamedTextColor.GREEN, TextDecoration.BOLD))
            .append(Component.text("Конвертация опыта:", NamedTextColor.GREEN))
            .append(Component.newline())
            .append(Component.text("• Конвертировано: ", NamedTextColor.GRAY))
            .append(Component.text(xpTotal + " опыта", NamedTextColor.YELLOW))
            .append(Component.newline())
            .append(Component.text("• Комиссия (", NamedTextColor.GRAY))
            .append(Component.text(String.format("%.1f%%", commissionPercent), TextColor.color(255, 165, 0)))
            .append(Component.text("): ", NamedTextColor.GRAY))
            .append(Component.text(commissionAmount + " опыта", NamedTextColor.YELLOW))
            .append(Component.newline())
            .append(Component.text("• Итого в книге: ", NamedTextColor.GRAY))
            .append(Component.text(xpFinal + " опыта", NamedTextColor.GREEN))
            .build();

        player.sendMessage(message);
    }

    /**
     * Отправляет сообщение об ошибке с иконкой
     */
    public void sendErrorMessage(CommandSender sender, String message) {
        Component errorMessage = Component.text()
            .append(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD))
            .append(legacySerializer.deserialize(message))
            .build();
        
        if (sender instanceof Player) {
            ((Player) sender).sendMessage(errorMessage);
        } else {
            sender.sendMessage(legacySerializer.serialize(errorMessage));
        }
    }
} 