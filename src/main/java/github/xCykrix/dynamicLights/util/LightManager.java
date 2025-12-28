package github.xCykrix.dynamicLights.util;

import github.xCykrix.dynamicLights.DynamicLights;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.block.data.type.Light;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class LightManager {
  private final LightSource source;
  private final HashMap<UUID, ScheduledTask> tasks = new HashMap<>();
  private final HashMap<String, Location> lastLightLocation = new HashMap<>();

  public final ConcurrentMap<String, Boolean> toggles;
  public final ConcurrentMap<String, Boolean> locks;
  private final long refresh;
  public final boolean toggle;

  private JavaPlugin plugin;

  private final List<Location> lights;

  public LightManager(JavaPlugin plugin) {
    this.plugin = plugin;
    // YamlDocument config = DynamicLights.configuration.getYAMLFile("config.yml");
    // if (config == null) {
    // throw new RuntimeException("config.yml is corrupted or contains invalid formatting. Failed to load plugin.");
    // }

    this.source = DynamicLights.source;
    // TODO reenable file storing of toggles & locks
    // this.toggles = DynamicLights.h2.get().openMap("lightToggleStatus");
    // this.locks = DynamicLights.h2.get().openMap("lightLockStatus");
    this.toggles = new ConcurrentHashMap<>();
    this.locks = new ConcurrentHashMap<>();
    this.refresh = plugin.getConfig().getLong("update-rate");
    this.toggle = plugin.getConfig().getBoolean("default-toggle-state");

    this.lights = Collections.synchronizedList(new LinkedList<>());

    // @NotNull ScheduledTask runAtFixedRate(@NotNull Plugin plugin, @NotNull Consumer<ScheduledTask> task, long initialDelay, long period,
    // @NotNull TimeUnit unit);
    Bukkit.getAsyncScheduler().runAtFixedRate(plugin, st -> tick(), 0L, refresh, TimeUnit.MILLISECONDS);
  }


  // @Override
  public void shutdown() { clearLight(); }


  public void tick() {
    if (!plugin.isEnabled()) {
      clearLight();
      return;
    }

    // plugin.getLogger().info("tick with enabled plugin");

    Set<Location> actualLocations = new HashSet<>();

    // For each online player, check if we should add a light.
    for (Player targetPlayer : Bukkit.getOnlinePlayers()) {
      // plugin.getLogger().info("player " + targetPlayer.getName() + " is online");
      Optional<Location> opLocation = run(targetPlayer);
      if (opLocation.isPresent()) {
        actualLocations.add(opLocation.get());
      }
    }

    // Remove all old light except for the one in actualLocations
    synchronized (this.lights) {
      for (Location location : this.lights) {
        if (!actualLocations.contains(location)) {
          Bukkit.getRegionScheduler().run(plugin, location, st -> this.removeLight(location));
        }
      }
      this.lights.clear();
      this.lights.addAll(actualLocations);
    }
  }

  public void clearLight() {
    synchronized (this.lights) {
      for (Location location : lights) {
        Bukkit.getRegionScheduler().run(plugin, location, st -> this.removeLight(location));
      }
    }
  }

  private Optional<Location> run(Player player) {
    if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
      // plugin.getLogger().info("creative or spectator");
      return Optional.empty();
    }
    if (!(this.toggles.getOrDefault(player.getUniqueId().toString(), this.toggle))) {
      // plugin.getLogger().info("toggle false");plugin.getLogger().info
      return Optional.empty();
    }

    Material mainHand = getMaterialOrAir(player.getInventory().getItemInMainHand());
    Material offHand = getMaterialOrAir(player.getInventory().getItemInOffHand());
    Material helmet = getMaterialOrAir(player.getInventory().getHelmet());
    Material chestplate = getMaterialOrAir(player.getInventory().getChestplate());
    Material legging = getMaterialOrAir(player.getInventory().getLeggings());
    Material boot = getMaterialOrAir(player.getInventory().getBoots());
    int lightLevel = source.getLightLevel(mainHand, offHand, helmet, chestplate, legging, boot);
    if (lightLevel > 0) {
      // plugin.getLogger().info("lightLevel > 0 " + lightLevel);
      Location eyeLocation = player.getEyeLocation();
      Bukkit.getRegionScheduler().run(plugin, eyeLocation, st -> this.addLight(eyeLocation, lightLevel));
      return Optional.of(eyeLocation);
    }
    // plugin.getLogger().info("lightLevel <= 0");
    return Optional.empty();
  }

  public void removePlayer(UUID uid) {
    synchronized (this.tasks) {
      if (this.tasks.containsKey(uid)) {
        this.tasks.get(uid).cancel();
        this.tasks.remove(uid);
      }
    }
  }

  private boolean acceptableBlock(Block block) {
    Material type = block.getType();
    return type == Material.AIR || type == Material.CAVE_AIR  || type == Material.LIGHT || type == Material.WATER;
    // TODO reenable water lights
    // || type == Material.WATER;
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

  public void addLight(Location location, int lightLevel) {
    if (lightLevel == 0) {
      return;
    }


    World world = location.getWorld();
    Block block = world.getBlockAt(location);

    
    // Only AIR or LIGHT or WATER can be replaced.
    block = getClosestAcceptableBlock(block);

    if (block == null) {
        return;
      }

    if (block.getType() == Material.LIGHT) {
        // return if existing light level if the same
        Light existingLight = (Light) block.getBlockData();
        if (existingLight.getLevel() == lightLevel) {
          return;
        }
      }
    
      

    location = block.getLocation();

    Light light = (Light) Material.LIGHT.createBlockData();
    switch (block.getType()) {
      case AIR, CAVE_AIR -> {
        light.setWaterlogged(false);
        light.setLevel(lightLevel);
      }
      case WATER -> {
        light.setWaterlogged(true);
        light.setLevel(lightLevel);
      }
      default -> {
      }
    }
    lights.add(location);
    location.getWorld().setBlockData(location, light);
  }

  public void removeLight(Location location) {
    Block b = location.getWorld().getBlockAt(location);
    if (b.getType() == Material.LIGHT) {
      if (b.getBlockData() instanceof Light light) {
        if (light.isWaterlogged()) {
          b.setType(Material.WATER);
        }else{
          b.setType(Material.AIR);
        }
      }
    }
    lights.remove(location);
  }

  public boolean valid(Player player, Material mainHand, Material offHand, Material helmet, Material chestplate, Material legging,
      Material boot) {
    boolean hasLightLevel = false;
    hasLightLevel = source.hasLightLevel(mainHand) ? true : hasLightLevel;
    hasLightLevel = source.hasLightLevel(offHand) ? true : hasLightLevel;
    hasLightLevel = source.hasLightLevel(helmet) ? true : hasLightLevel;
    hasLightLevel = source.hasLightLevel(chestplate) ? true : hasLightLevel;
    hasLightLevel = source.hasLightLevel(legging) ? true : hasLightLevel;
    hasLightLevel = source.hasLightLevel(boot) ? true : hasLightLevel;

    if (!hasLightLevel) {
      return false;
    }
    Block currentLocation = player.getEyeLocation().getBlock();
    if (currentLocation.getType() == Material.AIR || currentLocation.getType() == Material.CAVE_AIR) {
      return true;
    }
    if (currentLocation instanceof Waterlogged currentLocationWaterlogged && currentLocationWaterlogged.isWaterlogged()) {
      return false;
    }
    if (currentLocation.getType() == Material.WATER) {
      return source.isSubmersible(mainHand, offHand, helmet, chestplate, legging, boot);
    }
    return false;
  }

  public Location getLastLocation(String uuid) { return lastLightLocation.getOrDefault(uuid, null); }

  public void setLastLocation(String uuid, Location location) { lastLightLocation.put(uuid, location); }

  public void removeLastLocation(String uuid) { lastLightLocation.remove(uuid); }

  private Material getMaterialOrAir(ItemStack item) {
    if (item == null)
      return Material.AIR;
    else
      return item.getType();
  }
}
