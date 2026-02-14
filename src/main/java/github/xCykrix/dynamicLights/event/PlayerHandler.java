package github.xCykrix.dynamicLights.event;

import github.xCykrix.dynamicLights.DynamicLights;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.java.JavaPlugin;

public class PlayerHandler implements Listener {
  public PlayerHandler(JavaPlugin plugin) {}

  @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
  public void playerBlockPlaceEvent(BlockPlaceEvent event) {
    if (event.getPlayer().isSneaking()) {
      return;
    }

    if (event.getHand() == EquipmentSlot.OFF_HAND) {
      if (!DynamicLights.source.isProtectedLight(event.getItemInHand().getType())) {
        return;
      }
      if (DynamicLights.manager.locks.getOrDefault(event.getPlayer().getUniqueId().toString(), DynamicLights.manager.toggle)) {
        event.getPlayer().sendMessage(DynamicLights.translate("prevent-block-place"));
        event.setCancelled(true);
      }
    }
  }

  // No need we do it for each online player
  // @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  // public void onPlayerJoinEvent(PlayerJoinEvent event) {
  // DynamicLights.manager.addPlayer(event.getPlayer());
  // }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerQuitEvent(PlayerQuitEvent event) {
    DynamicLights.manager.removeLightFromLocationRegion(event.getPlayer().getUniqueId());
  }
}
