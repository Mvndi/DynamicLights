package github.xCykrix.dynamicLights.util;

import github.xCykrix.dynamicLights.DynamicLights;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

public class PlayerUtil {
    private static NamespacedKey lightToggleStatusKey = new NamespacedKey("dynamiclights", "toggle-state");
    private static NamespacedKey lightLockStatusKey = new NamespacedKey("dynamiclights", "lock-state");
    public static boolean getToggleStatus(Player player) {
        return player.getPersistentDataContainer().getOrDefault(lightToggleStatusKey, PersistentDataType.BOOLEAN,
                DynamicLights.getInstance().getConfig().getBoolean("default-toggle-state"));
    }
    public static void switchToggleStatus(Player player) {
        player.getPersistentDataContainer().set(lightToggleStatusKey, PersistentDataType.BOOLEAN, !getToggleStatus(player));
    }

    public static boolean getLockStatus(Player player) {
        return player.getPersistentDataContainer().getOrDefault(lightLockStatusKey, PersistentDataType.BOOLEAN,
                DynamicLights.getInstance().getConfig().getBoolean("default-lock-state"));
    }

    public static void switchLockStatus(Player player) {
        player.getPersistentDataContainer().set(lightLockStatusKey, PersistentDataType.BOOLEAN, !getLockStatus(player));
    }
}
