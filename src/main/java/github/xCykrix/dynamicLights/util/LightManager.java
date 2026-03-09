package github.xCykrix.dynamicLights.util;

import github.xCykrix.dynamicLights.DynamicLights;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Light;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class LightManager {
  private final LightSource source;
  private final Map<UUID, Location> lastLightLocation = new ConcurrentHashMap<>(200);

  private final Set<UUID> playerLightEnabled;

  private JavaPlugin plugin;

  public LightManager(JavaPlugin plugin) {
    this.plugin = plugin;
    this.source = DynamicLights.source;

    playerLightEnabled = ConcurrentHashMap.newKeySet();

    Bukkit.getAsyncScheduler().runAtFixedRate(plugin, st -> tick(), 0L, plugin.getConfig().getLong("update-rate"), TimeUnit.MILLISECONDS);
  }


  // @Override
  public void shutdown() { clearAllLights(); }

  public void addPlayerLightEnabled(UUID uuid) { playerLightEnabled.add(uuid); }

  public void removePlayerLightEnabled(UUID uuid) { playerLightEnabled.remove(uuid); }

  public boolean isPlayerLightEnabled(UUID uuid) { return playerLightEnabled.contains(uuid); }

  // Player emit light if (SURVIVAL or ADVENTURE) and they have it enabled.
  public void updatePlayerState(Player player, GameMode mode) {
    if ((mode == GameMode.SURVIVAL || mode == GameMode.ADVENTURE) && PlayerUtil.getToggleStatus(player)) {
      addPlayerLightEnabled(player.getUniqueId());
    } else {
      removePlayerLightEnabled(player.getUniqueId());
      // removeLightFromLocationRegion(player.getUniqueId());
    }
  }

  public void tick() {
    if (!plugin.isEnabled()) {
      clearAllLights();
      return;
    }

    // For each online player that have the right game mode, check if we should add a light or move the existing one
    for (UUID targetPlayerUUID : playerLightEnabled) {
      updatePlayerLight(targetPlayerUUID);
    }

    // Remove each light that should not be there
    // With a short delay between each tick(), the light is added back when removed from somewhere else,
    // so we need to remove it after the main for loop.
    // It might call to remove the light many time if the region scheduler is not fast enough, but it's fine I guess.
    for (UUID targetPlayerUUID : lastLightLocation.keySet()) {
      if (!playerLightEnabled.contains(targetPlayerUUID)) {
        this.removeLightFromLocationRegion(targetPlayerUUID);
      }
    }
  }

  public void clearAllLights() {
    for (Map.Entry<UUID, Location> entry : lastLightLocation.entrySet()) {
      this.removeLightFromLocationRegion(entry.getKey());
    }
  }

  public void updatePlayerLight(UUID targetPlayerUUID) {
    Player player = Bukkit.getPlayer(targetPlayerUUID);
    if (player == null) {
      removeLightFromLocationRegion(targetPlayerUUID);
    } else {
      Location playerLocation = player.getEyeLocation();
      Bukkit.getRegionScheduler().run(plugin, playerLocation, st -> updateLightToNewLocation(player, playerLocation));
    }
  }

  /**
   * Find the best location to place a light and update the light if needed.
   */
  private Optional<Location> updateLightToNewLocation(Player player, Location eyeLocation) {
    Material mainHand = getMaterialOrAir(player.getInventory().getItemInMainHand());
    Material offHand = getMaterialOrAir(player.getInventory().getItemInOffHand());
    Material helmet = getMaterialOrAir(player.getInventory().getHelmet());
    Material chestplate = getMaterialOrAir(player.getInventory().getChestplate());
    Material legging = getMaterialOrAir(player.getInventory().getLeggings());
    Material boot = getMaterialOrAir(player.getInventory().getBoots());
    int lightLevel = source.getLightLevel(mainHand, offHand, helmet, chestplate, legging, boot);
    if (lightLevel > 0) {
      // plugin.getLogger().info("lightLevel > 0 " + lightLevel);
      Block bestBlock = getClosestAcceptableBlock(eyeLocation.getBlock());
      if (bestBlock == null) {
        this.removeLightFromLocationRegion(player.getUniqueId());
        return Optional.empty();
      }
      Location bestBlockLocation = bestBlock.getLocation();
      if (bestBlockLocation == null) {
        this.removeLightFromLocationRegion(player.getUniqueId());
        return Optional.empty();
      }

      // Update the light in Minecraft & int lastLightLocation
      Location last = lastLightLocation.getOrDefault(player.getUniqueId(), null);
      if (last != null) {
        if (last.equals(bestBlockLocation) && bestBlock.getType() == Material.LIGHT
            && bestBlock.getBlockData() instanceof Light existingLight && existingLight.getLevel() == lightLevel) {
          // The light is already at the right location with the right level, no need to remove or to add
          return Optional.empty();
        }
        this.removeLightFromLocationRegion(player.getUniqueId());
      }

      if (!player.isOnline()) { // player might have log off in the meantime
        return Optional.empty();
      }
      lastLightLocation.put(player.getUniqueId(), bestBlockLocation); // replace ligh location
      this.addLight(bestBlock, lightLevel, source.isSubmersible(mainHand, offHand, helmet, chestplate, legging, boot));

      return Optional.of(bestBlockLocation);
    }
    // plugin.getLogger().info("lightLevel <= 0");
    this.removeLightFromLocationRegion(player.getUniqueId());
    return Optional.empty();
  }

  private boolean acceptableBlock(Block block) {
    Material type = block.getType();
    return type == Material.AIR || type == Material.CAVE_AIR || type == Material.LIGHT || type == Material.WATER;
  }

  private Block getClosestAcceptableBlock(Block block) {
    List<Block> possibleLocation = List.of(block, block.getRelative(BlockFace.NORTH), block.getRelative(BlockFace.EAST),
        block.getRelative(BlockFace.SOUTH), block.getRelative(BlockFace.WEST), block.getRelative(BlockFace.UP),
        block.getRelative(BlockFace.DOWN));

    for (Block relativeBlock : possibleLocation) {
      if (acceptableBlock(relativeBlock)) {
        return relativeBlock;
      }
    }
    return null;
  }

  public void addLight(Block block, int lightLevel, boolean isSubmersible) {
    Light light = (Light) Material.LIGHT.createBlockData();
    switch (block.getType()) {
      case AIR, CAVE_AIR -> {
        light.setWaterlogged(false);
        light.setLevel(lightLevel);
      }
      case WATER -> {
        if (isSubmersible) {
          light.setWaterlogged(true);
          light.setLevel(lightLevel);
        } else {
          return;
        }
      }
      default -> {
        // do nothing
      }
    }

    block.getWorld().setBlockData(block.getLocation(), light);

    // DynamicLights.getInstance().getLogger().info("Added light at " + block.getLocation());
  }

  private void removeLight(UUID playerUuid, Location location) {
    // Location location = lastLightLocation.get(playerUuid);
    if (location != null) {
      World world = location.getWorld();
      if (world != null) {
        Block b = world.getBlockAt(location);
        if (b.getBlockData() instanceof Light light) {
          if (light.isWaterlogged()) {
            b.setType(Material.WATER);
          } else {
            b.setType(Material.AIR);
          }
        }
      }
    }
    // Remove only if its the current last location, else we want to keep that value to remove it later when we will have to remove the
    // light.
    lastLightLocation.remove(playerUuid, location);
    // DynamicLights.getInstance().getLogger().info("Removed light at " + location);
  }

  public void removeLightFromLocationRegion(UUID playerUuid) {
    if (playerUuid == null || !lastLightLocation.containsKey(playerUuid)) {
      return;
    }

    Location location = lastLightLocation.get(playerUuid);
    // Since it's planned for later, we need to send the current last location before it will be override when placing the new light.
    Bukkit.getRegionScheduler().run(plugin, location, st -> this.removeLight(playerUuid, location));
  }

  private Material getMaterialOrAir(ItemStack item) {
    if (item == null)
      return Material.AIR;
    else
      return item.getType();
  }
}
