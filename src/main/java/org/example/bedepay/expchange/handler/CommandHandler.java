package org.example.bedepay.expchange.handler;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.example.bedepay.expchange.manager.*;
import org.example.bedepay.expchange.util.ExperienceUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Обработчик команд плагина
 */
public class CommandHandler implements CommandExecutor, TabCompleter {
    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final MessageManager messageManager;
    private final CooldownManager cooldownManager;
    private final BookManager bookManager;
    private final EffectManager effectManager;

    public CommandHandler(JavaPlugin plugin, ConfigManager configManager, MessageManager messageManager,
                         CooldownManager cooldownManager, BookManager bookManager, EffectManager effectManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.messageManager = messageManager;
        this.cooldownManager = cooldownManager;
        this.bookManager = bookManager;
        this.effectManager = effectManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("exphelp") || 
            (command.getName().equalsIgnoreCase("expchange") && args.length > 0 && args[0].equalsIgnoreCase("help"))) {
            return handleHelpCommand(sender);
        } else if (command.getName().equalsIgnoreCase("expchange")) {
            return handleExpChangeCommand(sender, args);
        } else if (command.getName().equalsIgnoreCase("givexpbook")) {
            return handleGiveExpBookCommand(sender, args);
        }
        return false;
    }

    private boolean handleHelpCommand(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Только игроки могут использовать эту команду.");
            return false;
        }
        
        Player player = (Player) sender;
        
        // Проверка кулдауна для help
        if (cooldownManager.hasCooldown(player, "help")) {
            long timeLeft = cooldownManager.getCooldownTimeLeft(player, "help");
            messageManager.sendMessage(player, messageManager.getMessage("cooldown")
                .replace("%time%", String.valueOf(timeLeft)));
            return false;
        }
        
        // Устанавливаем кулдаун для help (10 секунд)
        cooldownManager.setCooldown(player, "help", 10);
        
        // Отправляем справку
        messageManager.sendHelpMessage(player);
        return true;
    }

    private boolean handleExpChangeCommand(CommandSender sender, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("expchange.admin")) {
                messageManager.sendMessage(sender, messageManager.getMessage("no_permission"));
                return false;
            }
            configManager.loadConfig();
            messageManager.loadMessages();
            messageManager.sendMessage(sender, "§aКонфигурация перезагружена!");
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("Только игроки могут использовать эту команду.");
            return false;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("expchange.use")) {
            messageManager.sendMessage(player, messageManager.getMessage("no_permission"));
            return false;
        }

        // Проверка кулдауна
        if (cooldownManager.hasCooldown(player, "expchange")) {
            long timeLeft = cooldownManager.getCooldownTimeLeft(player, "expchange");
            messageManager.sendMessage(player, messageManager.getMessage("cooldown")
                .replace("%time%", String.valueOf(timeLeft)));
            return false;
        }

        int totalXp = ExperienceUtils.getTotalExperience(player);
        
        // Проверка минимального количества опыта
        if (totalXp < configManager.getMinXpForConversion()) {
            messageManager.sendMessage(player, messageManager.getMessage("not_enough_xp_for_conversion")
                .replace("%min_xp%", String.valueOf(configManager.getMinXpForConversion())));
            return false;
        }

        // Переменные для хранения количества опыта для конвертации
        int xpToTake = 0;
        boolean isBulkCreation = false;
        int xpPerBook = 0;
        
        // Обработка аргументов команды
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
                    messageManager.sendMessage(player, "§eПожалуйста, укажите единицы измерения: §f/expchange " + amount + " exp §7или §f/expchange " + amount + " lvl");
                    return true;
                } catch (NumberFormatException e) {
                    messageManager.sendMessage(player, "§cНеверный формат! Используйте: §f/expchange <количество> exp §7или §f/expchange <количество> lvl");
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
                    int xpForLevels = ExperienceUtils.getExpToLevel(amount);
                    xpPerBook = xpForLevels;
                    
                    // Проверяем на bulk creation (команда /expchange <amount> lvl all)
                    if (args.length >= 3 && args[2].equalsIgnoreCase("all")) {
                        isBulkCreation = true;
                        // Рассчитываем, сколько книг можем создать
                        int booksCount = calculateMaxBooks(player, totalXp, xpPerBook);
                        if (booksCount <= 0) {
                            messageManager.sendMessage(player, "§cНедостаточно опыта для создания хотя бы одной книги!");
                            return false;
                        }
                        xpToTake = booksCount * xpPerBook;
                    } else {
                        xpToTake = Math.min(xpForLevels, totalXp);
                    }
                } else {
                    messageManager.sendMessage(player, "§cНеизвестная единица измерения! Используйте: §f/expchange <количество> exp §7или §f/expchange <количество> lvl");
                    return false;
                }
                
                if (xpToTake <= 0) {
                    messageManager.sendMessage(player, "§cКоличество опыта должно быть положительным числом!");
                    return false;
                }
                
            } catch (NumberFormatException e) {
                messageManager.sendMessage(player, "§cНеверный формат числа! Используйте: §f/expchange <количество> exp §7или §f/expchange <количество> lvl");
                return false;
            }
        }
        
        if (isBulkCreation) {
            return handleBulkCreation(player, totalXp, xpToTake, xpPerBook);
        } else {
            return handleSingleBookCreation(player, totalXp, xpToTake);
        }
    }

    private boolean handleBulkCreation(Player player, int totalXp, int xpToTake, int xpPerBook) {
        // Проверяем лимиты
        if (xpPerBook > configManager.getMaxXpPerBook()) {
            messageManager.sendMessage(player, messageManager.getMessage("too_much_xp")
                .replace("%max_xp%", String.valueOf(configManager.getMaxXpPerBook())));
            return false;
        }

        // Рассчитываем количество книг
        int booksCount = xpToTake / xpPerBook;
        int inventorySpace = getAvailableInventorySpace(player);
        
        if (inventorySpace < booksCount) {
            messageManager.sendMessage(player, "§cНедостаточно места в инвентаре! Нужно: " + booksCount + ", доступно: " + inventorySpace);
            return false;
        }

        // Рассчитываем комиссию для каждой книги
        int commissionAmount = (int) (xpPerBook * configManager.getCommission());
        int xpPerBookAfterCommission = xpPerBook - commissionAmount;
        
        if (xpPerBookAfterCommission <= 0) {
            messageManager.sendMessage(player, "§cСлишком мало опыта для конвертации после вычета комиссии!");
            return false;
        }

        // Забираем опыт у игрока
        ExperienceUtils.setTotalExperience(player, totalXp - xpToTake);
        
        // Создаем книги
        for (int i = 0; i < booksCount; i++) {
            ItemStack book = bookManager.createExpBook(xpPerBookAfterCommission);
            player.getInventory().addItem(book);
        }
        
        // Эффекты и кулдаун
        effectManager.playEffects(player, true, xpPerBookAfterCommission);
        cooldownManager.setCooldown(player, "expchange", configManager.getCooldownTime());
        
        // Сообщение об успехе
        int totalCommission = commissionAmount * booksCount;
        messageManager.sendMessage(player, messageManager.getMessage("bulk_create_success")
            .replace("%books_count%", String.valueOf(booksCount))
            .replace("%xp_per_book%", String.valueOf(xpPerBookAfterCommission))
            .replace("%total_converted%", String.valueOf(xpToTake))
            .replace("%commission%", String.valueOf(totalCommission)));
        
        return true;
    }

    private boolean handleSingleBookCreation(Player player, int totalXp, int xpToTake) {
        // Проверка максимального значения
        if (xpToTake > configManager.getMaxXpPerBook()) {
            messageManager.sendMessage(player, messageManager.getMessage("too_much_xp")
                .replace("%max_xp%", String.valueOf(configManager.getMaxXpPerBook())));
            return false;
        }
        
        if (xpToTake <= 0) {
            messageManager.sendMessage(player, "§cНедостаточно опыта для конвертации!");
            return false;
        }

        // Проверяем, есть ли место в инвентаре
        if (player.getInventory().firstEmpty() == -1) {
            messageManager.sendMessage(player, messageManager.getMessage("inventory_full"));
            return false;
        }

        // Рассчитываем комиссию
        int commissionAmount = (int) (xpToTake * configManager.getCommission());
        
        // Вычисляем итоговое количество опыта для книги
        int xpToGive = xpToTake - commissionAmount;
        
        if (xpToGive <= 0) {
            messageManager.sendMessage(player, "§cСлишком мало опыта для конвертации!");
            return false;
        }

        ExperienceUtils.setTotalExperience(player, totalXp - xpToTake);
        
        ItemStack book = bookManager.createExpBook(xpToGive);
        player.getInventory().addItem(book);
        
        effectManager.playEffects(player, true, xpToGive);
        cooldownManager.setCooldown(player, "expchange", configManager.getCooldownTime());
        
        messageManager.sendMessage(player, messageManager.getMessage("exchange_success")
            .replace("%xp_total%", String.valueOf(xpToTake))
            .replace("%commission_percent%", String.format("%.1f%%", configManager.getCommission() * 100))
            .replace("%commission_amount%", String.valueOf(commissionAmount))
            .replace("%xp_final%", String.valueOf(xpToGive)));
        
        return true;
    }

    private boolean handleGiveExpBookCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("expchange.admin")) {
            messageManager.sendMessage(sender, messageManager.getMessage("no_permission"));
            return false;
        }

        if (args.length < 2) {
            messageManager.sendMessage(sender, "§cИспользование: /givexpbook <игрок> <количество>");
            return false;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            messageManager.sendMessage(sender, "§cИгрок не найден!");
            return false;
        }

        try {
            int xp = Integer.parseInt(args[1]);
            
            if (xp <= 0) {
                messageManager.sendMessage(sender, "§cКоличество опыта должно быть положительным числом!");
                return false;
            }
            
            if (xp > configManager.getMaxXpPerBook()) {
                messageManager.sendMessage(sender, messageManager.getMessage("too_much_xp")
                    .replace("%max_xp%", String.valueOf(configManager.getMaxXpPerBook())));
                return false;
            }
            
            ItemStack book = bookManager.createExpBook(xp);
            target.getInventory().addItem(book);
            messageManager.sendMessage(sender, "§aКнига с " + xp + " опыта выдана игроку " + target.getName());
            
            // Эффект для получения книги
            effectManager.playEffects(target, true, xp);
            
            return true;
        } catch (NumberFormatException e) {
            messageManager.sendMessage(sender, "§cНеверное количество опыта!");
            return false;
        }
    }

    private int calculateMaxBooks(Player player, int totalXp, int xpPerBook) {
        double commission = configManager.getCommission();
        int xpAfterCommission = (int) (xpPerBook * (1 + commission));
        int maxBooks = totalXp / xpAfterCommission;
        int inventorySpace = getAvailableInventorySpace(player);
        return Math.min(maxBooks, inventorySpace);
    }

    private int getAvailableInventorySpace(Player player) {
        int emptySlots = 0;
        for (int i = 0; i < 36; i++) { // Основной инвентарь (без хотбара)
            if (player.getInventory().getItem(i) == null) {
                emptySlots++;
            }
        }
        return emptySlots;
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
                suggestions.add("30");
                
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
            } else if (args.length == 3) {
                // Если второй аргумент lvl, предлагаем all
                if (args[1].equalsIgnoreCase("lvl")) {
                    return Arrays.asList("all");
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
        } else if (command.getName().equalsIgnoreCase("exphelp")) {
            return Arrays.asList("expchange");
        }
        return null;
    }
} 