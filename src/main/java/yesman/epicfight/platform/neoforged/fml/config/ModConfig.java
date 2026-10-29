package yesman.epicfight.platform.neoforged.fml.config;

import yesman.epicfight.platform.neoforged.common.ModConfigSpec;

import java.nio.file.Path;

public class ModConfig {
    public enum Type { COMMON, SERVER, CLIENT }

    private final Type type;
    private final Path fullPath;

    public ModConfig(Type type, ModConfigSpec spec, Path configDir, String modId) {
        this.type = type;
        this.fullPath = configDir.resolve(modId + "-" + type.name().toLowerCase() + ".toml");
        spec.loadConfig(this.fullPath);
    }

    public Type getType() { return type; }
    public Path getFullPath() { return fullPath; }
}
