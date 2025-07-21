package org.example.bedepay.expchange.manager;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Менеджер кулдаунов игроков
 */
public class CooldownManager {
    private Map<UUID, Map<String, Long>> typedCooldowns = new HashMap<>();

    public boolean hasCooldown(Player player, String type) {
        if (!typedCooldowns.containsKey(player.getUniqueId())) {
            return false;
        }
        Map<String, Long> playerCooldowns = typedCooldowns.get(player.getUniqueId());
        return playerCooldowns.containsKey(type) && System.currentTimeMillis() < playerCooldowns.get(type);
    }

    public void setCooldown(Player player, String type, int seconds) {
        UUID playerUUID = player.getUniqueId();
        if (!typedCooldowns.containsKey(playerUUID)) {
            typedCooldowns.put(playerUUID, new HashMap<>());
        }
        typedCooldowns.get(playerUUID).put(type, System.currentTimeMillis() + (seconds * 1000L));
        
        // Очищаем старые кулдауны при добавлении нового
        cleanupExpiredCooldowns(playerUUID);
    }

    public long getCooldownTimeLeft(Player player, String type) {
        Map<String, Long> playerCooldowns = typedCooldowns.get(player.getUniqueId());
        if (playerCooldowns != null && playerCooldowns.containsKey(type)) {
            return (playerCooldowns.get(type) - System.currentTimeMillis()) / 1000;
        }
        return 0;
    }

    public void removeCooldown(Player player, String type) {
        UUID playerUUID = player.getUniqueId();
        Map<String, Long> playerCooldowns = typedCooldowns.get(playerUUID);
        if (playerCooldowns != null) {
            playerCooldowns.remove(type);
            if (playerCooldowns.isEmpty()) {
                typedCooldowns.remove(playerUUID);
            }
        }
    }

    public void removeAllCooldowns(Player player) {
        typedCooldowns.remove(player.getUniqueId());
    }

    private void cleanupExpiredCooldowns(UUID playerUUID) {
        Map<String, Long> playerCooldowns = typedCooldowns.get(playerUUID);
        if (playerCooldowns != null) {
            long currentTime = System.currentTimeMillis();
            playerCooldowns.entrySet().removeIf(entry -> entry.getValue() < currentTime);
            
            // Если у игрока больше нет активных кулдаунов, удаляем запись
            if (playerCooldowns.isEmpty()) {
                typedCooldowns.remove(playerUUID);
            }
        }
    }

    public void clear() {
        typedCooldowns.clear();
    }
} 