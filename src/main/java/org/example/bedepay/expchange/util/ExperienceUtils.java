package org.example.bedepay.expchange.util;

import org.bukkit.entity.Player;

/**
 * Утилитарный класс для работы с опытом игроков
 */
public class ExperienceUtils {

    /**
     * Получить общий опыт игрока
     */
    public static int getTotalExperience(Player player) {
        int level = player.getLevel();
        float progress = player.getExp();
        
        int totalExp = getExpToLevel(level);
        int expToNextLevel = getExpToNextLevel(level);
        
        return totalExp + Math.round(progress * expToNextLevel);
    }

    /**
     * Установить общий опыт игрока
     */
    public static void setTotalExperience(Player player, int exp) {
        if (exp < 0) {
            exp = 0;
        }
        
        player.setTotalExperience(0);
        player.setLevel(0);
        player.setExp(0);
        
        int iterations = 0;
        final int MAX_ITERATIONS = 100;
        
        while (exp > 0 && iterations < MAX_ITERATIONS) {
            int currentLevel = player.getLevel();
            int expToLevel = getExpToNextLevel(currentLevel);
            
            if (expToLevel <= 0) {
                break;
            }
            
            if (exp >= expToLevel) {
                exp -= expToLevel;
                player.giveExp(expToLevel);
            } else {
                player.giveExp(exp);
                exp = 0;
            }
            
            iterations++;
        }
    }

    /**
     * Получить количество опыта до следующего уровня
     */
    public static int getExpToNextLevel(int level) {
        if (level <= 15) {
            return 2 * level + 7;
        } else if (level <= 30) {
            return 5 * level - 38;
        } else {
            return 9 * level - 158;
        }
    }

    /**
     * Получить общий опыт для достижения указанного уровня
     */
    public static int getExpToLevel(int targetLevel) {
        if (targetLevel <= 0) {
            return 0;
        }
        
        int totalExp = 0;
        
        if (targetLevel <= 16) {
            totalExp = targetLevel * targetLevel + 6 * targetLevel;
        } else if (targetLevel <= 31) {
            totalExp = (int) (2.5 * targetLevel * targetLevel - 40.5 * targetLevel + 360);
        } else {
            totalExp = (int) (4.5 * targetLevel * targetLevel - 162.5 * targetLevel + 2220);
        }
        
        return Math.max(0, totalExp);
    }

    /**
     * Получить уровень по количеству опыта
     */
    public static int getLevelFromExp(int exp) {
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
} 