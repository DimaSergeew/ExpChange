package org.example.bedepay.expchange;

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

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        getCommand("expchange").setTabCompleter(this); // Автокомплит для команд
        saveDefaultConfig();
        loadConfig();
    }

    private void loadConfig() {
        reloadConfig();
        commission = getConfig().getDouble("commission", 0.1);
        itemMaterial = Material.matchMaterial(getConfig().getString("item.material", "BOOK"));
        bookDisplayName = getConfig().getString("book.display_name", "Хранитель опыта");
        bookGlowing = getConfig().getBoolean("book.glowing", true);
        bookLore = getConfig().getStringList("book.lore").toArray(new String[0]);
        enchantmentName = getConfig().getString("book.enchantment_name", "Опыта");
        enchantments = getConfig().getStringList("book.enchantments").toArray(new String[0]);
        
        cooldownTime = getConfig().getInt("cooldown", 60);
        soundEffectsEnabled = getConfig().getBoolean("sound_effects.enabled", true);
        createSound = getConfig().getString("sound_effects.create", "ENTITY_EXPERIENCE_ORB_PICKUP");
        useSound = getConfig().getString("sound_effects.use", "ENTITY_PLAYER_LEVELUP");
        particleEffectsEnabled = getConfig().getBoolean("particle_effects", true);
        maxXpPerBook = getConfig().getInt("max_xp_per_book", 1000000);
        minXpForConversion = getConfig().getInt("min_xp_for_conversion", 100);
    }

    @Override
    public void onDisable() {
        // Действия при остановке плагина
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("expchange")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("expchange.admin")) {
                    sender.sendMessage(getConfig().getString("messages.no_permission"));
                    return false;
                }
                loadConfig();
                sender.sendMessage("§aКонфигурация перезагружена!");
                return true;
            }

            if (!(sender instanceof Player)) {
                sender.sendMessage("Только игроки могут использовать эту команду.");
                return false;
            }

            Player player = (Player) sender;
            if (!player.hasPermission("expchange.use")) {
                player.sendMessage(getConfig().getString("messages.no_permission"));
                return false;
            }

            // Проверка кулдауна
            if (hasCooldown(player)) {
                long timeLeft = getCooldownTimeLeft(player);
                player.sendMessage(getConfig().getString("messages.cooldown")
                    .replace("%time%", String.valueOf(timeLeft)));
                return false;
            }

            int totalXp = getTotalExperience(player);
            
            // Проверка минимального количества опыта
            if (totalXp < minXpForConversion) {
                player.sendMessage(getConfig().getString("messages.not_enough_xp_for_conversion")
                    .replace("%min_xp%", String.valueOf(minXpForConversion)));
                return false;
            }

            // Проверяем аргументы для процента
            int percentage = 100;
            if (args.length > 0) {
                try {
                    percentage = Integer.parseInt(args[0]);
                    if (percentage <= 0 || percentage > 100) {
                        player.sendMessage("§cПроцент должен быть от 1 до 100!");
                        return false;
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage("§cНеверный формат процента!");
                    return false;
                }
            }

            // Сначала вычисляем количество опыта для конвертации на основе процента
            int xpToTake = (int) (totalXp * (percentage / 100.0));
            
            // Рассчитываем комиссию
            int commissionAmount = (int) (xpToTake * commission);
            
            // Вычисляем итоговое количество опыта для книги
            int xpToGive = xpToTake - commissionAmount;
            
            if (xpToGive <= 0) {
                player.sendMessage("§cСлишком мало опыта для конвертации!");
                return false;
            }

            // Проверка максимального значения
            if (xpToGive > maxXpPerBook) {
                player.sendMessage(getConfig().getString("messages.too_much_xp"));
                return false;
            }

            // Проверяем, есть ли место в инвентаре
            if (player.getInventory().firstEmpty() == -1) {
                player.sendMessage(getConfig().getString("messages.inventory_full", "§cОсвободите место в инвентаре!"));
                return false;
            }

            // Снимаем полное количество опыта у игрока
            setTotalExperience(player, totalXp - xpToTake);
            
            // Создаем книгу с опытом за вычетом комиссии
            ItemStack book = createExpBook(xpToGive);
            player.getInventory().addItem(book);
            
            // Эффекты
            if (soundEffectsEnabled) {
                player.playSound(player.getLocation(), Sound.valueOf(createSound), 1.0f, 1.0f);
            }
            
            if (particleEffectsEnabled) {
                player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation().add(0, 1, 0), 50, 0.5, 0.5, 0.5, 0.1);
            }

            setCooldown(player);
            
            // Отправляем подробное сообщение
            player.sendMessage(getConfig().getString("messages.exchange_success")
                .replace("%xp_total%", String.valueOf(xpToTake))
                .replace("%commission_percent%", String.format("%.1f%%", commission * 100))
                .replace("%commission_amount%", String.valueOf(commissionAmount))
                .replace("%xp_final%", String.valueOf(xpToGive)));
            
            return true;
        } else if (command.getName().equalsIgnoreCase("givexpbook")) {
            if (!sender.hasPermission("expchange.admin")) {
                sender.sendMessage(getConfig().getString("messages.no_permission"));
                return false;
            }

            if (args.length < 2) {
                sender.sendMessage("§cИспользование: /givexpbook <игрок> <количество>");
                return false;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage("§cИгрок не найден!");
                return false;
            }

            try {
                int xp = Integer.parseInt(args[1]);
                ItemStack book = createExpBook(xp);
                target.getInventory().addItem(book);
                sender.sendMessage("§aКнига с " + xp + " опыта выдана игроку " + target.getName());
                return true;
            } catch (NumberFormatException e) {
                sender.sendMessage("§cНеверное количество опыта!");
                return false;
            }
        }
        return false;
    }

    @EventHandler
    public void onPlayerUseItem(PlayerInteractEvent event) {
        if (event.getItem() != null && event.getItem().getType() == itemMaterial) {
            event.setCancelled(true);
            
            ItemMeta meta = event.getItem().getItemMeta();
            if (meta != null && meta.getPersistentDataContainer().has(new NamespacedKey(this, "xp_amount"), PersistentDataType.INTEGER)) {
                // Проверяем, есть ли у книги необходимое зачарование
                if (bookGlowing && !meta.hasEnchant(Enchantment.DURABILITY)) {
                    event.getPlayer().sendMessage(getConfig().getString("messages.invalid_book"));
                    return;
                }
                
                int xpAmount = meta.getPersistentDataContainer().get(new NamespacedKey(this, "xp_amount"), PersistentDataType.INTEGER);
                
                if (xpAmount <= 0) {
                    event.getPlayer().sendMessage(getConfig().getString("messages.invalid_book"));
                    return;
                }
                
                if (xpAmount > maxXpPerBook) {
                    event.getPlayer().sendMessage(getConfig().getString("messages.too_much_xp"));
                    return;
                }
                
                Player player = event.getPlayer();
                player.giveExp(xpAmount);
                event.getItem().setAmount(event.getItem().getAmount() - 1);
                
                if (soundEffectsEnabled) {
                    player.playSound(player.getLocation(), Sound.valueOf(useSound), 1.0f, 1.0f);
                }
                
                if (particleEffectsEnabled) {
                    player.getWorld().spawnParticle(Particle.ENCHANTMENT_TABLE, 
                        player.getLocation().add(0, 1, 0), 50, 0.5, 0.5, 0.5, 0.1);
                }
                
                player.sendMessage(getConfig().getString("messages.use_success")
                    .replace("%xp%", String.valueOf(xpAmount)));
            }
        }
    }

    private String[] replacePlaceholders(String[] lore, int xpAmount) {
        String[] newLore = Arrays.copyOf(lore, lore.length);
        for (int i = 0; i < newLore.length; i++) {
            newLore[i] = newLore[i].replace("%xp%", String.valueOf(xpAmount));
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
        }

        book.setItemMeta(meta);
        return book;
    }

    private void setXpAmount(ItemMeta meta, int xpAmount) {
        meta.getPersistentDataContainer().set(new NamespacedKey(this, "xp_amount"), PersistentDataType.INTEGER, xpAmount);
    }

    private void setBookDisplayNameAndLore(ItemMeta meta, int xpAmount) {
        meta.setDisplayName(bookDisplayName.replace("%xp%", String.valueOf(xpAmount)));
        meta.setLore(Arrays.asList(replacePlaceholders(bookLore, xpAmount)));
    }

    private void applyGlowing(ItemMeta meta) {
        if (bookGlowing) {
            meta.addEnchant(Enchantment.DURABILITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
    }

    private boolean hasCooldown(Player player) {
        if (!cooldowns.containsKey(player.getUniqueId())) {
            return false;
        }
        return System.currentTimeMillis() < cooldowns.get(player.getUniqueId());
    }

    private void setCooldown(Player player) {
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis() + (cooldownTime * 1000L));
    }

    private long getCooldownTimeLeft(Player player) {
        return (cooldowns.get(player.getUniqueId()) - System.currentTimeMillis()) / 1000;
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
        if (args.length == 1) {
            return Arrays.asList("reload");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("givexpbook")) {
            return Arrays.asList("<игрок>", "<количество>");
        }
        return null;
    }
}