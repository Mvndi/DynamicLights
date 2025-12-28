package github.xCykrix.dynamicLights.command;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Subcommand;
import github.xCykrix.dynamicLights.DynamicLights;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

@CommandAlias("dynamiclights|dynamiclight|dl")
public class DynamicLightsCommand extends co.aikar.commands.BaseCommand {// extends DevkitSimpleState {

  private static JavaPlugin plugin;

  public DynamicLightsCommand(JavaPlugin plugin) { this.plugin = plugin; }

  @Subcommand("reload")
  @Description("Reloads the plugin config and data files")
  @CommandPermission("dynamiclights.reload")
  public static void onReload(CommandSender commandSender) {
    try {
      // Objects.requireNonNull(DynamicLights.configuration.getYAMLFile("lights.yml")).reload();
      YamlConfiguration lights = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "lights.yml"));
      DynamicLights.source.initialize();
      commandSender.sendMessage(DynamicLights.translate("reload"));
    } catch (NullPointerException ex) {
      commandSender.sendMessage(DynamicLights.translate("reload-error"));
      plugin.getLogger().severe("Failed to reload lights.yml.");
      plugin.getLogger().severe(getStackTrace(ex));
    }
  }

  public static String getStackTrace(Throwable throwable) {
    if (throwable == null) {
      return "";
    } else {
      StringWriter sw = new StringWriter();
      throwable.printStackTrace(new PrintWriter(sw, true));
      return sw.toString();
    }
  }

  @Subcommand("toggle")
  @Description("Toggles Dynamic Lights on and off")
  @CommandPermission("dynamiclights.toggle")
  public static void onToggle(CommandSender commandSender) {
    if (commandSender instanceof Player player) {
      String uuid = player.getUniqueId().toString();
      boolean current = DynamicLights.manager.toggles.getOrDefault(uuid, DynamicLights.manager.toggle);
      if (!current) {
        player.sendMessage(DynamicLights.translate("toggle-on"));
        DynamicLights.manager.toggles.put(uuid, true);
      } else {
        player.sendMessage(DynamicLights.translate("toggle-off"));
        DynamicLights.manager.toggles.put(uuid, false);
      }
    }
  }

  @Subcommand("lock")
  @Description("Prevents lights from being placed in the offhand")
  @CommandPermission("dynamiclights.lock")
  public static void onLock(CommandSender commandSender) {
    if (commandSender instanceof Player player) {
      String uuid = player.getUniqueId().toString();
      boolean current = DynamicLights.manager.locks.getOrDefault(uuid, true);
      if (!current) {
        player.sendMessage(DynamicLights.translate("enable-lock"));
        DynamicLights.manager.locks.put(uuid, true);
      } else {
        player.sendMessage(DynamicLights.translate("disable-lock"));
        DynamicLights.manager.locks.put(uuid, false);
      }
    }
  }
}
