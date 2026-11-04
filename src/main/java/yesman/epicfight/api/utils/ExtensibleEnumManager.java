package yesman.epicfight.api.utils;
import yesman.epicfight.EpicFight;

import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.network.chat.Component;
import yesman.epicfight.main.EpicFightMod;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ExtensibleEnumManager<T extends ExtensibleEnum> {
    private final Int2ObjectMap<T> enumMapByOrdinal = new Int2ObjectLinkedOpenHashMap<>();
    private final Map<String, T> enumMapByName = new LinkedHashMap<> ();
    private final Map<String, Class<?>> enums = new ConcurrentHashMap<> ();
    private final String enumName;
    private int lastOrdinal = 0;

    public ExtensibleEnumManager(String enumName) {
        this.enumName = enumName;
    }

    public void registerEnumCls(String modid, Class<? extends ExtensibleEnum> cls) {
        if (this.enums.containsKey(modid)) {
            EpicFight.LOGGER.error("{} is already registered in {}", modid, this.enumName);
        }

        EpicFight.LOGGER.debug("Registered Extensible Enum {} in ", this.enumName);

        this.enums.put(modid, cls);
    }

    public void loadEnum() {
        List<String> orderByModid = new ArrayList<>(this.enums.keySet());
        Collections.sort(orderByModid);
        Class<?> cls = null;

        try {
            for (String modid : orderByModid) {
                cls = this.enums.get(modid);

                Method m = cls.getMethod("values");
                m.invoke(null);

                EpicFight.LOGGER.debug("Loaded enums in {}", cls);
            }
        } catch (ClassCastException e) {
            EpicFight.LOGGER.error("{} is not an Extensible Enum!", cls.getCanonicalName(), e);
        } catch (NoSuchMethodException e) {
            EpicFight.LOGGER.error("{} is not an Enum class!", cls.getCanonicalName(), e);
        } catch (SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            EpicFight.LOGGER.warn("Error while loading extensible enum {}", cls.getCanonicalName(), e);
        }

        EpicFight.LOGGER.debug("All enums are loaded: {} {}", this.enumName, this.enumMapByName.values());
    }

    public int assign(T value) {
        int lastOrdinal = this.lastOrdinal;
        String enumName = ParseUtil.toLowerCase(value.toString());

        if (this.enumMapByName.containsKey(enumName)) {
            throw new IllegalArgumentException("Enum identifier " + enumName + " already exists in " + this.enumName);
        }

        this.enumMapByOrdinal.put(lastOrdinal, value);
        this.enumMapByName.put(enumName, value);
        ++this.lastOrdinal;

        return lastOrdinal;
    }

    public T getOrThrow(int id) throws NoSuchElementException {
        if (!this.enumMapByOrdinal.containsKey(id)) {
            throw new NoSuchElementException("Enum id " + id + " does not exist in " + this.enumName);
        }

        return this.enumMapByOrdinal.get(id);
    }

    public T getOrThrow(String name) throws NoSuchElementException {
        String key = ParseUtil.toLowerCase(name);

        if (!this.enumMapByName.containsKey(key)) {
            // Addon enum classes may register via registerEnumCls after the first
            // loadEnum pass (mod construction order is not guaranteed). Pull in any
            // pending enum classes before declaring the identifier missing.
            loadPendingEnums();
        }

        if (!this.enumMapByName.containsKey(key)) {
            throw new NoSuchElementException("Enum identifier " + key + " does not exist in " + this.enumName);
        }

        return this.enumMapByName.get(key);
    }

    public T get(int id) {
        return this.enumMapByOrdinal.get(id);
    }

    public T get(String name) {
        String key = ParseUtil.toLowerCase(name);
        T value = this.enumMapByName.get(key);

        if (value == null) {
            loadPendingEnums();
            value = this.enumMapByName.get(key);
        }

        return value;
    }

    private int loadedEnumCount;

    private void loadPendingEnums() {
        if (this.enums.size() != this.loadedEnumCount) {
            loadEnum();
            this.loadedEnumCount = this.enums.size();
        }
    }

    public Collection<T> universalValues() {
        return this.enumMapByOrdinal.values();
    }

    public String toTranslated(ExtensibleEnum e) {
        return Component.translatable(String.format("%s.%s.%s", EpicFight.MODID, this.enumName, ParseUtil.toLowerCase(e.toString()))).getString();
    }
}