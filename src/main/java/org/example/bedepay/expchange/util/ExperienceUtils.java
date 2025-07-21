package org.example.bedepay.expchange.util;

import org.bukkit.entity.Player;

/**
 * Современный утилитарный класс для работы с опытом игроков
 * Использует встроенные методы Paper API вместо кастомных вычислений
 */
public class ExperienceUtils {

    /**
     * Получить общий опыт игрока (современный Paper API метод)
     * Использует встроенный calculateTotalExperiencePoints()
     */
    public static int getTotalExperience(Player player) {
        return player.calculateTotalExperiencePoints();
    }

    /**
     * Установить общий опыт игрока (современный Paper API метод)
     * Использует встроенный setExperienceLevelAndProgress()
     */
    public static void setTotalExperience(Player player, int exp) {
        if (exp < 0) {
            exp = 0;
        }
        
        // Используем современный Paper API метод
        player.setExperienceLevelAndProgress(exp);
    }

    /**
     * Получить количество опыта до следующего уровня
     * Оставляем для совместимости, но используем встроенные методы
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
     * Используется для расчетов в CommandHandler
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
     * Вспомогательный метод для расчетов
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

    /**
     * Проверить, поддерживает ли текущая версия Paper современные методы опыта
     */
    public static boolean isPaperModernExperienceSupported() {
        try {
            // Проверяем наличие современного метода Paper API
            Player.class.getMethod("calculateTotalExperiencePoints");
            Player.class.getMethod("setExperienceLevelAndProgress", int.class);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
} 