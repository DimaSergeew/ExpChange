package org.example.bedepay.expchange.model;

import org.bukkit.Particle;

/**
 * Класс для тиеров книг опыта
 */
public class BookTier {
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
        if (name == null || name.trim().isEmpty()) {
            return Particle.ENCHANTMENT_TABLE;
        }
        
        try {
            return Particle.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Particle.ENCHANTMENT_TABLE;
        }
    }
} 