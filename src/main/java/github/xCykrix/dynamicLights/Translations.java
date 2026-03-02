package github.xCykrix.dynamicLights;

import com.google.common.collect.Maps;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.minimessage.translation.MiniMessageTranslationStore;
import net.kyori.adventure.translation.GlobalTranslator;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.codehaus.plexus.util.FileUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Translations {
    private final Path localeFolder = DynamicLights.getInstance().getDataPath().resolve("locale");
    private final Key name = Key.key("minigamelib", "locale");
    private MiniMessageTranslationStore storage = MiniMessageTranslationStore.create(name);

    public void reload() {
        GlobalTranslator.translator().removeSource(storage);
        storage = MiniMessageTranslationStore.create(name);

        if (!Files.isDirectory(localeFolder)) {
            try {
                Files.createDirectories(localeFolder);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }

        // create default en.yml if not exists
        Path defaultLocaleFile = localeFolder.resolve("en.yml");
        if (!Files.exists(defaultLocaleFile)) {
            DynamicLights.getInstance().saveResource("locale/en.yml", false);
        }

        List<Locale> registered = new ArrayList<>(1);
        registered.add(Locale.ENGLISH);

        try (var str = Files.list(localeFolder)) {
            str.forEach(path -> {
                String name = path.getFileName().toString();
                if (!name.endsWith(".yml")) {
                    return;
                }

                String localeName = FileUtils.basename(name, ".yml");

                Locale locale = Locale.forLanguageTag(localeName);
                if (locale != null) load(locale);
                registered.add(locale);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

        Map<String, String> english = getEnglishKeys();

        Locale.availableLocales().forEach(locale -> {
            if (locale.getLanguage().isEmpty()) {
                return;
            }

            if (registered.contains(locale)) {
                return;
            }

            storage.registerAll(locale, english);
        });

        GlobalTranslator.translator().addSource(storage);
    }

    public Component english(TranslatableComponent component) {
        Component c = storage.translate(component, Locale.ENGLISH);
        return c == null ? component : c;
    }

    public Component translate(TranslatableComponent component, Locale locale) {
        Component c = storage.translate(component, locale);
        return c == null ? storage.translate(component, Locale.ENGLISH) : c;
    }

    private void load(Locale locale) {
        Path localeFile = localeFolder.resolve(locale.getLanguage() + ".yml");
        if (!Files.exists(localeFile)) {
            try {
                Files.createFile(localeFile);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }

        Map<String, String> map = Maps.newHashMap();
        FileConfiguration configuration = YamlConfiguration.loadConfiguration(localeFile.toFile());

        configuration.getValues(true).forEach((k, o) -> {
            if (o instanceof MemorySection) {
                return;
            }

            map.put(k, (String) o);
        });

        storage.registerAll(locale, map);
    }

    private Map<String, String> getEnglishKeys() {
        Map<String, String> map = Maps.newHashMap();
        FileConfiguration configuration = YamlConfiguration.loadConfiguration(localeFolder.resolve("en.yml").toFile());

        configuration.getValues(true).forEach((k, o) -> {
            if (o instanceof MemorySection) {
                return;
            }

            map.put(k, (String) o);
        });

        return map;
    }
}
