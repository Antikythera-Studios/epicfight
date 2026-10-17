package yesman.epicfight.platform.neoforged.common;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class ModConfigSpec {
    private final Map<String, Object> defaults;
    private final Map<String, String> comments;
    private final List<ConfigValue<?>> values;
    private CommentedFileConfig fileConfig;

    private ModConfigSpec(Map<String, Object> defaults, Map<String, String> comments, List<ConfigValue<?>> values) {
        this.defaults = defaults;
        this.comments = comments;
        this.values = values;
    }

    public void loadConfig(Path path) {
        this.fileConfig = CommentedFileConfig.builder(path).sync().build();
        this.fileConfig.load();
        for (var entry : defaults.entrySet()) {
            if (!fileConfig.contains(entry.getKey())) {
                fileConfig.set(entry.getKey(), entry.getValue());
            }
        }
        for (var entry : comments.entrySet()) {
            fileConfig.setComment(entry.getKey(), entry.getValue());
        }
        fileConfig.save();
    }

    void save() {
        if (fileConfig != null) fileConfig.save();
    }

    @SuppressWarnings("unchecked")
    <T> T getRaw(String path) {
        if (fileConfig != null && fileConfig.contains(path)) return fileConfig.get(path);
        return (T) defaults.get(path);
    }

    void setRaw(String path, Object value) {
        if (fileConfig != null) fileConfig.set(path, value);
    }

    // --- Value holders ---

    public static class ConfigValue<T> {
        ModConfigSpec spec;
        final String path;
        final T defaultValue;

        ConfigValue(String path, T defaultValue) {
            this.path = path;
            this.defaultValue = defaultValue;
        }

        @SuppressWarnings("unchecked")
        public T get() {
            if (spec == null) return defaultValue;
            Object val = spec.getRaw(path);
            if (val == null) return defaultValue;
            if (defaultValue instanceof Integer && val instanceof Number n) return (T) Integer.valueOf(n.intValue());
            if (defaultValue instanceof Double && val instanceof Number n) return (T) Double.valueOf(n.doubleValue());
            try { return (T) val; } catch (ClassCastException e) { return defaultValue; }
        }

        public void set(T value) {
            if (spec != null) spec.setRaw(path, value);
        }

        public void save() {
            if (spec != null) spec.save();
        }

        public T getDefault() {
            return defaultValue;
        }
    }

    public static class BooleanValue extends ConfigValue<Boolean> {
        BooleanValue(String path, boolean defaultValue) { super(path, defaultValue); }
    }

    public static class IntValue extends ConfigValue<Integer> {
        IntValue(String path, int defaultValue) { super(path, defaultValue); }

        @Override
        public Integer get() {
            if (spec == null) return defaultValue;
            Object val = spec.getRaw(path);
            if (val instanceof Number n) return n.intValue();
            return defaultValue;
        }
    }

    public static class DoubleValue extends ConfigValue<Double> {
        DoubleValue(String path, double defaultValue) { super(path, defaultValue); }

        @Override
        public Double get() {
            if (spec == null) return defaultValue;
            Object val = spec.getRaw(path);
            if (val instanceof Number n) return n.doubleValue();
            return defaultValue;
        }
    }

    public static class EnumValue<T extends Enum<T>> extends ConfigValue<T> {
        private final Class<T> enumClass;

        @SuppressWarnings("unchecked")
        EnumValue(String path, T defaultValue) {
            super(path, defaultValue);
            this.enumClass = (Class<T>) defaultValue.getClass();
        }

        @Override
        public T get() {
            if (spec == null) return defaultValue;
            Object val = spec.getRaw(path);
            if (val instanceof String s) {
                try { return Enum.valueOf(enumClass, s); } catch (IllegalArgumentException e) { return defaultValue; }
            }
            return defaultValue;
        }

        @Override
        public void set(T value) {
            if (spec != null) spec.setRaw(path, value.name());
        }
    }

    // --- Builder ---

    public static class Builder {
        private final Map<String, Object> defaults = new LinkedHashMap<>();
        private final Map<String, String> comments = new LinkedHashMap<>();
        private final List<ConfigValue<?>> values = new ArrayList<>();
        private String pendingComment;

        public Builder comment(String comment) {
            this.pendingComment = comment;
            return this;
        }

        private void track(String path, Object defVal, ConfigValue<?> v) {
            defaults.put(path, defVal);
            if (pendingComment != null) { comments.put(path, pendingComment); pendingComment = null; }
            values.add(v);
        }

        public BooleanValue define(String path, boolean defaultValue) {
            BooleanValue v = new BooleanValue(path, defaultValue);
            track(path, defaultValue, v);
            return v;
        }

        public BooleanValue define(String path, Supplier<Boolean> defaultSupplier) {
            return define(path, defaultSupplier.get().booleanValue());
        }

        public <T> ConfigValue<T> define(String path, T defaultValue) {
            ConfigValue<T> v = new ConfigValue<>(path, defaultValue);
            track(path, defaultValue, v);
            return v;
        }

        public <T extends Enum<T>> EnumValue<T> defineEnum(String path, T defaultValue) {
            EnumValue<T> v = new EnumValue<>(path, defaultValue);
            track(path, defaultValue.name(), v);
            return v;
        }

        public IntValue defineInRange(String path, int defaultValue, int min, int max) {
            IntValue v = new IntValue(path, defaultValue);
            track(path, defaultValue, v);
            return v;
        }

        public DoubleValue defineInRange(String path, double defaultValue, double min, double max) {
            DoubleValue v = new DoubleValue(path, defaultValue);
            track(path, defaultValue, v);
            return v;
        }

        public <T> ConfigValue<List<? extends T>> defineList(String path, List<? extends T> defaultValue, Supplier<?> newElementDefault, Predicate<Object> validator) {
            ConfigValue<List<? extends T>> v = new ConfigValue<>(path, defaultValue);
            track(path, new ArrayList<>(defaultValue), v);
            return v;
        }

        public ModConfigSpec build() {
            ModConfigSpec spec = new ModConfigSpec(defaults, comments, values);
            for (ConfigValue<?> v : values) v.spec = spec;
            return spec;
        }
    }
}
