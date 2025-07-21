package org.example.bedepay.expchange;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.example.bedepay.expchange.handler.CommandHandler;
import org.example.bedepay.expchange.handler.ExpChangeEventHandler;
import org.example.bedepay.expchange.manager.*;
public final class ExpChange extends JavaPlugin {

    // Менеджеры
    private ConfigManager configManager;
    private MessageManager messageManager;
    private CooldownManager cooldownManager;
    private BookManager bookManager;
    private EffectManager effectManager;
    
    // Обработчики
    private CommandHandler commandHandler;
    private ExpChangeEventHandler eventHandler;

    @Override
    public void onEnable() {
        // Инициализация менеджеров
        initializeManagers();
        
        // Инициализация обработчиков
        initializeHandlers();
        
        // Регистрация событий
        Bukkit.getPluginManager().registerEvents(eventHandler, this);
        
        // Регистрация команд (используем классический способ для совместимости)
        registerCommands();
        
        // Загрузка конфигурации
        saveDefaultConfig();
        configManager.loadConfig();
        messageManager.loadMessages();
        
        // Отображение баннера
        displayBanner();
    }

    @Override
    public void onDisable() {
        // Очищаем все данные для предотвращения memory leaks
        if (cooldownManager != null) {
            cooldownManager.clear();
        }
        if (eventHandler != null) {
            eventHandler.clear();
        }
        getLogger().info("ExpChange выключен!");
    }

    private void initializeManagers() {
        configManager = new ConfigManager(this);
        messageManager = new MessageManager(this, configManager);
        cooldownManager = new CooldownManager();
        bookManager = new BookManager(this, configManager);
        effectManager = new EffectManager(this, configManager);
    }

    private void initializeHandlers() {
        commandHandler = new CommandHandler(this, configManager, messageManager, 
                                          cooldownManager, bookManager, effectManager);
        eventHandler = new ExpChangeEventHandler(this, configManager, messageManager,
                                               bookManager, effectManager, cooldownManager);
    }

    private void registerCommands() {
        // Проверяем, зарегистрированы ли команды
        if (getCommand("expchange") != null) {
            getCommand("expchange").setTabCompleter(commandHandler);
            getCommand("expchange").setExecutor(commandHandler);
        } else {
            getLogger().severe("Команда 'expchange' не найдена! Проверьте файл plugin.yml");
        }
        
        if (getCommand("givexpbook") != null) {
            getCommand("givexpbook").setExecutor(commandHandler);
        } else {
            getLogger().severe("Команда 'givexpbook' не найдена! Проверьте файл plugin.yml");
        }
        
        if (getCommand("exphelp") != null) {
            getCommand("exphelp").setTabCompleter(commandHandler);
            getCommand("exphelp").setExecutor(commandHandler);
        } else {
            getLogger().severe("Команда 'exphelp' не найдена! Проверьте файл plugin.yml");
        }
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
            "§b║           §fВерсия: §a" + this.getDescription().getVersion() + "    §fАвтор: §aBedePay§b           ║",
            "§b║                                                       ║",
            "§b║           §6Новые возможности:                        §b║",
            "§b║           §7• Команда /expchange 30 lvl all           §b║",
            "§b║           §7• Улучшенная защита от дюпов              §b║",
            "§b║           §7• Оптимизированная архитектура            §b║",
            "§b║           §7• Современные Paper API методы            §b║",
            "§b╚═══════════════════════════════════════════════════════╝"
        };
        
        for (String line : banner) {
            getServer().getConsoleSender().sendMessage(line);
        }
        
        getLogger().info("ExpChange успешно запущен! Наслаждайтесь игрой!");
    }

    // Геттеры для доступа к менеджерам из других классов (если потребуется)
    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }

    public BookManager getBookManager() {
        return bookManager;
    }

    public EffectManager getEffectManager() {
        return effectManager;
    }
}