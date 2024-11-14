package me.lukasabbe.trustedafk;

import net.fabricmc.loader.api.FabricLoader;
import org.yaml.snakeyaml.Yaml;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class Config {
    public int afkTime = 5;
    public boolean optInDefault = false;
    public void loadConfig() {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve("trusted-afk-mod-config.yml");
        if(!Files.exists(configPath))createConfig(configPath);
        Yaml yaml = new Yaml();
        try{
            Map<String, Object> configMap = yaml.load(new FileReader(configPath.toFile()));
            if(configMap.containsKey("afk-time")){
                afkTime = (int) configMap.get("afk-time");
            }
            if(configMap.containsKey("opt-in-default")){
                optInDefault = (boolean) configMap.get("opt-in-default");
            }

        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void createConfig(Path configPath){
        FabricLoader.getInstance().getModContainer("trustedafk").ifPresent(modContainer -> {
            Path path = modContainer.findPath("trusted-afk-mod-config.yml").orElseThrow();
            try {
                Files.copy(path,configPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
    public void reloadConfig(){
        loadConfig();
    }
}
