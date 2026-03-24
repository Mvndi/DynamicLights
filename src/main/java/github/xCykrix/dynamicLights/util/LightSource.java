package github.xCykrix.dynamicLights.util;

import java.io.File;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class LightSource {
  private final Map<Material, Integer> levelOfLights = new EnumMap<>(Material.class);
  private final Set<Material> submersibleLights = new HashSet<>();
  private final Set<Material> lockedLights = new HashSet<>();

  private final JavaPlugin plugin;

  public LightSource(JavaPlugin plugin) { this.plugin = plugin; }

  // @Override
  public void initialize() {
    // YamlDocument lights = DynamicLights.configuration.getYAMLFile("lights.yml");
    YamlConfiguration lights = YamlConfiguration.loadConfiguration(new File(this.plugin.getDataFolder(), "lights.yml"));


    if (lights == null) {
      throw new RuntimeException("lights.yml is corrupted or contains invalid formatting. Failed to load plugin.");
    }

    // Register Light Levels
    this.levelOfLights.clear();
    // Map<Object, Block<?>> levels = lights.getSection("levels").getStoredValue();
    Map<String, Object> levels = lights.getConfigurationSection("levels").getValues(false);
    for (Map.Entry<String, Object> entry : levels.entrySet()) {
      String materialString = entry.getKey();
      Object levelObject = entry.getValue();
      try {
        int level = Integer.parseInt(levelObject.toString());
        Material material = Material.valueOf(materialString);
        this.levelOfLights.put(material, level);
      } catch (Exception exception) {
        this.plugin.getLogger().warning("Unable to register level for '" + materialString + "'. " + exception.getMessage());
      }
    }
    this.plugin.getLogger().info("Registered " + this.levelOfLights.size() + " items for Dynamic Lights.");

    // Register Submersible Lights
    this.submersibleLights.clear();
    List<String> submersibles = lights.getStringList("submersibles");
    for (String material : submersibles) {
      try {
        this.submersibleLights.add(Material.valueOf(material));
      } catch (Exception exception) {
        this.plugin.getLogger().warning("Unable to register submersible for '" + material + "'. " + exception.getMessage());
      }
    }
    this.plugin.getLogger().info("Registered " + this.submersibleLights.size() + " items for Dynamic Submersible Lights.");

    // Register Lockable Lights
    this.lockedLights.clear();
    List<String> lockables = lights.getStringList("lockables");
    for (String material : lockables) {
      try {
        this.lockedLights.add(Material.valueOf(material));
      } catch (Exception exception) {
        this.plugin.getLogger().warning("Unable to register lockable for '" + material + "'. " + exception.getMessage());
      }
    }
    this.plugin.getLogger().info("Registered " + this.lockedLights.size() + " items for Dynamic Locked Lights.");
  }

  // @Override
  public void shutdown() {
    this.lockedLights.clear();
    this.submersibleLights.clear();
    this.levelOfLights.clear();
  }

  public boolean hasLightLevel(Material material) { return levelOfLights.containsKey(material); }

  public int getLightLevel(Material... materials) {
    return Arrays.stream(materials)
        .mapToInt(mat -> levelOfLights.getOrDefault(mat, 0))
        .max()
        .orElse(0);
}

  public boolean isSubmersible(Material... materials) {
    return Arrays.stream(materials).anyMatch(submersibleLights::contains);
  }

  public boolean isProtectedLight(Material offHand) { return lockedLights.contains(offHand); }
}
