package net.kyrptonaught.inventorysorter.config;

import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonObject;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.kyrptonaught.inventorysorter.InventorySorterMod;

import java.io.*;
import java.nio.file.Files;
import java.util.stream.Collectors;

public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().create();
    private static final Jankson JANKSON = Jankson.builder().build();
    private final File configFile, ignoreFile;
    public ConfigOptions config;
    public IgnoreList blacklist;

    public ConfigManager() {
        File dir = new File(FabricLoader.getInstance().getConfigDirectory() + "/inventorysorter");
        if (!Files.exists(dir.toPath())) {
            try {
                Files.createDirectories(dir.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        File oldConfig = new File(FabricLoader.getInstance().getConfigDirectory(), "inventorysorter.json5");
        if (oldConfig.exists())
            oldConfig.renameTo(new File(dir, "config.json5"));
        this.configFile = new File(dir, "config.json5");
        this.ignoreFile = new File(dir, "blacklist.json5");

    }

    public void saveAll() {
        save(configFile, true);
        save(ignoreFile, false);
    }

    public void loadAll() {
        load(configFile, true);
        load(ignoreFile, false);
    }

    public Gson getGSON() {
        return GSON;
    }

    private void save(File saveFile, boolean isConfig) {
        try {
            if (!saveFile.exists() && !saveFile.createNewFile()) {
                System.out.println(InventorySorterMod.MOD_ID + " Failed to save config! Overwriting with default config.");
                resetToDefault(isConfig);
                return;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try (FileOutputStream out = new FileOutputStream(saveFile, false)) {
            String result = JANKSON.toJson(isConfig ? config : blacklist).toJson(true, true, 0);
            if (!saveFile.exists())
                saveFile.createNewFile();
            out.write(result.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(InventorySorterMod.MOD_ID + " Failed to save config! Overwriting with default config.");
            resetToDefault(isConfig);
        }
    }

    private void load(File saveFile, boolean isConfig) {
        if (!saveFile.exists() || !saveFile.canRead()) {
            System.out.println(InventorySorterMod.MOD_ID + " Config not found! Creating one.");
            resetToDefault(isConfig);
            save(saveFile, isConfig);
            return;
        }
        boolean failed = false;
        try {
            JsonObject configJson = JANKSON.load(saveFile);
            String regularized = configJson.toJson(false, false, 0);
            if (isConfig) config = GSON.fromJson(regularized, ConfigOptions.class);
            else blacklist = GSON.fromJson(regularized, IgnoreList.class);
        } catch (Exception e) {
            e.printStackTrace();
            failed = true;
        }
        if (failed || (isConfig && config == null) || (!isConfig && blacklist == null)) {
            System.out.println(InventorySorterMod.MOD_ID + " Failed to load config! Overwriting with default config.");
            resetToDefault(isConfig);
        }
        save(saveFile, isConfig);
    }

    public void loadDefaultBlacklist() {
        String file = "/data/blacklistedInventories.json";
        InputStream in = InventorySorterMod.class.getResourceAsStream(file);
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        String lines = reader.lines().collect(Collectors.joining());
        blacklist.defaultBlacklist = GSON.fromJson(lines, IgnoreList.DefaultList.class).defaultBlacklist;
    }

    private void resetToDefault(boolean isConfig) {
        if (isConfig) config = new ConfigOptions();
        else {
            blacklist = new IgnoreList();
            loadDefaultBlacklist();
        }
    }
}
