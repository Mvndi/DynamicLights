package github.xCykrix.dynamicLights;

import co.aikar.commands.PaperCommandManager;
import github.xCykrix.dynamicLights.command.DynamicLightsCommand;
import github.xCykrix.dynamicLights.event.PlayerHandler;
import github.xCykrix.dynamicLights.util.LightManager;
import github.xCykrix.dynamicLights.util.LightSource;
import org.bukkit.plugin.java.JavaPlugin;

public final class DynamicLights extends JavaPlugin {
  // Core APIs.
  // public static ConfigurationPlugin configuration;
  // public static AdventurePlugin adventure;
  // public static CommandPlugin command;

  // Third Party APIs.
  // public static H2MVStorePlugin h2;

  // Internal APIs.
  // public static LanguageFile language;
  public static LightSource source;
  public static LightManager manager;

  // @Override
  protected void pre() {
    // configuration = this.register(new ConfigurationPlugin(this));
    // adventure = this.register(new AdventurePlugin(this));
    // command = this.register(new CommandPlugin(this));
    // h2 = this.register(new H2MVStorePlugin(this));
  }

  // @Override
  public void initialize() {

    // save default config files
    saveDefaultConfig();
    saveResource("language.yml", false);
    saveResource("lights.yml", false);


    // Register Configurations
    // configuration.register(new Resource("config.yml", null, this.getResource("config.yml")))
    // .register(new Resource("lights.yml", null, this.getResource("lights.yml"))).registerLanguageFile(this.getResource("language.yml"));
    // language = configuration.getLanguageFile();

    // Register Internal APIs.
    source = new LightSource(this);
    source.initialize();
    manager = new LightManager(this);
    // manager.initialize();

    // Register Events
    this.getServer().getPluginManager().registerEvents(new PlayerHandler(this), this);

    // Register Commands
    PaperCommandManager manager = new PaperCommandManager(this);
    manager.registerCommand(new DynamicLightsCommand(this));
  }

  @Override
  public void onEnable() { initialize(); }

  // @Override
  public void shutdown() {
    manager.shutdown();
    source.shutdown();
  }

  public static String translate(String key) {
    // TODO fix language file access & color interpretation
    return key;
  }
}
