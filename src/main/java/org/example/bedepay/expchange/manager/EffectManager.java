package org.example.bedepay.expchange.manager;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.example.bedepay.expchange.model.BookTier;

/**
 * Менеджер эффектов (звуки и частицы)
 */
public class EffectManager {
    private final JavaPlugin plugin;
    private final ConfigManager configManager;

    public EffectManager(JavaPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public void playEffects(Player player, boolean isCreate, int xpAmount) {
        if (configManager.isSoundEffectsEnabled()) {
            playSoundEffect(player, isCreate);
        }
        
        if (configManager.isParticleEffectsEnabled()) {
            playParticleEffect(player, isCreate, xpAmount);
        }
    }

    private void playSoundEffect(Player player, boolean isCreate) {
        try {
            String soundName = isCreate ? configManager.getCreateSound() : configManager.getUseSound();
            Sound sound = Sound.valueOf(soundName);
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Неверный звук: " + (isCreate ? configManager.getCreateSound() : configManager.getUseSound()));
        }
    }

    private void playParticleEffect(Player player, boolean isCreate, int xpAmount) {
        BookTier tier = getTierForXp(xpAmount);
        Particle particle = tier != null ? tier.getParticle() : 
            (isCreate ? Particle.PORTAL : Particle.ENCHANTMENT_TABLE);
        
        spawnSpiralParticle(player, particle, isCreate);
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

    private void spawnSpiralParticle(Player player, Particle particle, boolean isCreation) {
        if (player == null || player.getWorld() == null || particle == null) {
            return;
        }
        
        try {
            final double radius = 1.0;
            final int particles = 40;
            final double height = 2.0;
            
            for (int i = 0; i < particles; i++) {
                double angle = (double) i / particles * Math.PI * 2;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                double y = (double) i / particles * height;
                
                try {
                    player.getWorld().spawnParticle(
                        particle,
                        player.getLocation().add(x, y, z),
                        3, 0.05, 0.05, 0.05, 0.01
                    );
                } catch (Exception e) {
                    // Игнорируем ошибки отдельных частиц
                }
            }
            
            // Дополнительные эффекты для создания книги
            if (isCreation) {
                try {
                    player.getWorld().spawnParticle(
                        Particle.EXPLOSION_NORMAL,
                        player.getLocation().add(0, 1, 0),
                        10, 0.5, 0.5, 0.5, 0.1
                    );
                } catch (Exception e) {
                    // Игнорируем ошибки с дополнительными эффектами
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка при создании эффектов частиц: " + e.getMessage());
        }
    }
} 