package github.xCykrix.dynamicLights.event;

import github.xCykrix.dynamicLights.DynamicLights;
import github.xCykrix.dynamicLights.util.PlayerUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
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
      if (PlayerUtil.getLockStatus(event.getPlayer())) {
        event.getPlayer().sendMessage(DynamicLights.translate("prevent-block-place"));
        event.setCancelled(true);
      }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerQuitEvent(PlayerQuitEvent event) {
    DynamicLights.manager.removePlayerLightEnabled(event.getPlayer().getUniqueId());
    // DynamicLights.manager.removeLightFromLocationRegion(event.getPlayer().getUniqueId());
  }

  @EventHandler(ignoreCancelled = true)
  public void onPlayerGameModeChangeEvent(PlayerGameModeChangeEvent event) {
    DynamicLights.manager.updatePlayerState(event.getPlayer(), event.getNewGameMode());
  }

  @EventHandler
  public void onPlayerJoinEvent(PlayerJoinEvent event) {
    DynamicLights.manager.updatePlayerState(event.getPlayer(), event.getPlayer().getGameMode());
  }
}
