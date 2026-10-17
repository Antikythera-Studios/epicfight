package yesman.epicfight.api.utils;
import yesman.epicfight.EpicFight;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.collider.Collider;
import yesman.epicfight.api.exception.AnimationInvokeException;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.client.gui.datapack.screen.DatapackEditScreen;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.gameasset.ColliderPreset;
import yesman.epicfight.main.EpicFightSharedConstants;

import javax.annotation.Nullable;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class InstantiateInvoker {
	private static final BiMap<String, Class<?>> PRIMITIVE_KEYWORDS = HashBiMap.create();
	private static final BiMap<Class<?>, Class<?>> RETURN_TYPE_MAPPER = HashBiMap.create();
	private static final Map<Class<?>, Function<String, Object>> STRING_TO_OBJECT_PARSER = Maps.newHashMap();
	
	static {
		registerPrimitive("B", byte.class, Byte::parseByte);
		registerPrimitive("C", char.class, (s) -> s.charAt(0));
		registerPrimitive("D", double.class, Double::parseDouble);
		registerPrimitive("F", float.class, Float::parseFloat);
		registerPrimitive("I", int.class, Integer::parseInt);
		registerPrimitive("J", long.class, Long::parseLong);
		registerPrimitive("S", short.class, Short::parseShort);
		registerPrimitive("Z", boolean.class, Boolean::parseBoolean);
		
		registerReturnTypeMapper(Armature.class, AssetAccessor.class);
		
		registerKeyword(String.class, (s) -> s);
		registerKeyword(Collider.class, (s) -> ColliderPreset.get(ResourceLocation.parse(s)));
		registerKeyword(Joint.class, (s) -> {
			String[] armature$joint = s.split("\\.");
			AssetAccessor<? extends Armature> armature = getArmature(armature$joint[0]);
			return armature.get().searchJointByName(armature$joint[1]);
		});
		registerKeyword(Armature.class, (s) -> getArmature(s));
		registerKeyword(InteractionHand.class, InteractionHand::valueOf);
	}
	
	private static AssetAccessor<? extends Armature> getArmature(String id) {
		if (EpicFightSharedConstants.isPhysicalClient()) {
			return DatapackEditScreen.getCurrentScreen() != null ? DatapackEditScreen.getArmature(id) : Armatures.getOrCreate(ResourceLocation.parse(id), Armature::new);
		} else {
			return Armatures.getOrCreate(ResourceLocation.parse(id), Armature::new);
		}
	}
	
	public static void registerPrimitive(String keyword, Class<?> clz, Function<String, Object> decoder) {
		PRIMITIVE_KEYWORDS.put(keyword, clz);
		STRING_TO_OBJECT_PARSER.put(clz, decoder);
	}
	
	public static void registerReturnTypeMapper(Class<?> clz, Class<?> returnClz) {
		RETURN_TYPE_MAPPER.put(clz, returnClz);
	}
	
	public static void registerKeyword(Class<?> clz, Function<String, Object> decoder) {
		STRING_TO_OBJECT_PARSER.put(clz, decoder);
	}
	
	@SuppressWarnings("unchecked")
	public static <T> Result<T> invoke(String invocationCommand, @Nullable Class<T> hint) throws Exception {
		if (invocationCommand.matches("\\(.+\\)") || invocationCommand.matches("\\(.+\\)\\#.+")) { // invoke instance
			return (Result<T>)invokeInstance(invocationCommand, hint);
		} else if (invocationCommand.matches("\\[.+\\]") || invocationCommand.matches("\\[.+\\]\\#.+")) { // invoke array
			return (Result<T>)invokeArray(invocationCommand, hint);
		} else {
			String[] param = splitExceptWrapper(invocationCommand, '#', true);
			String sValue = param[0];
			String sType = param[1];

            if ("IMeshRenderBoost".equals(sType)) {
                sType = "I"; // Bad Implementation: I > IMeshRenderBoost is caused by replace all but kept to preserve backward compatibility
            }

			if (PRIMITIVE_KEYWORDS.containsKey(sType)) {
				Class<T> type = (Class<T>)PRIMITIVE_KEYWORDS.get(sType);
				return Result.of(type, (T)STRING_TO_OBJECT_PARSER.get(type).apply(sValue));
			}
			
			Class<T> type = (Class<T>)Class.forName(sType);
			
			if (STRING_TO_OBJECT_PARSER.containsKey(type)) {
				if (RETURN_TYPE_MAPPER.containsKey(type)) {
					return Result.of((Class<T>)RETURN_TYPE_MAPPER.get(type), (T)STRING_TO_OBJECT_PARSER.get(type).apply(sValue));
				}
				
				return Result.of(type, (T)STRING_TO_OBJECT_PARSER.get(type).apply(sValue));
			}
			
			throw new AnimationInvokeException("Can't find the matching type for the command " + invocationCommand);
		}
	}
	
	/**
	 * @param hint has a higher priority than class specified in invocation command
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static <T> Result<T> invokeInstance(String invocationCommand, @Nullable Class<?> hint) throws Exception {
		if (!invocationCommand.matches("\\(.+\\)") && !invocationCommand.matches("\\(.+\\)\\#.+")) {
			throw new IllegalStateException("Invalid instantiate invocation command: " + invocationCommand);
		}
		
		String[] param$type = splitExceptWrapper(invocationCommand, '#', true);
		String sParams = param$type[0];

        if (param$type.length > 1 && "yesman.epicfight.api.animation.types.BasicAttackAnimation".equals(param$type[1])) {
            param$type[1] = "yesman.epicfight.api.animation.types.ComboAttackAnimation"; // Class name changed: BasicAttackAnimation(1.20.1) -> "ComboAttackAnimation"
        }

		Class<?> type = param$type.length > 1 ? Class.forName(param$type[1]) : hint;
		
		if (type == null) {
			throw new AnimationInvokeException("Can't find the type in command " + invocationCommand);
		}
		
		String[] params = splitExceptWrapper(sParams, ',', false);
		Object[] oArgs = new Object[params.length];
		Class[] oArgClss = new Class[params.length];
		
		for (int i = 0; i < params.length; i++) {
			Result<?> result = invoke(params[i], null);
			oArgs[i] = result.result;
			oArgClss[i] = result.type;
			
			for (Class<?> reservedClass : STRING_TO_OBJECT_PARSER.keySet()) {
				if (!result.type.equals(reservedClass) && reservedClass.isAssignableFrom(result.type)) {
					oArgClss[i] = reservedClass;
				}
			}
		}
		
		Constructor constructor = null;
		
		try {
			if (hint == null) {
				throw new NoSuchMethodException();
			}
			
			constructor = hint.getConstructor(oArgClss);
		} catch (NoSuchMethodException e) {
			//EpicFight.LOGGER.debug("Can't find the matching constructor for the hint class " + hint + ". Use the given class " + type);
			constructor = type.getConstructor(oArgClss);
		}
		
		return Result.of((Class<T>)type, (T)constructor.newInstance(oArgs));
	}
	
	/**
	 * @param hint has a higher priority than class specified in invocation command
	 */
	@SuppressWarnings("unchecked")
	public static <T> Result<T[]> invokeArray(String invocationCommand, @Nullable Class<?> hint) throws Exception {
		if (!invocationCommand.matches("\\[.+\\]") && !invocationCommand.matches("\\[.+\\]\\#.+")) {
			throw new AnimationInvokeException("Invalid array invocation command: " + invocationCommand);
		}
		
		String[] param$type = splitExceptWrapper(invocationCommand, '#', true);
		String sParams = param$type[0];
		Class<?> type = param$type.length > 1 ? Class.forName(param$type[1]) : hint;
		
		if (type == null) {
			throw new AnimationInvokeException("Can't find the type in command " + invocationCommand);
		}
		
		String[] params = splitExceptWrapper(sParams, ',', false);
		List<T> resultArray = Lists.newArrayList();
		T[] result = (T[]) Array.newInstance(type, params.length);
		
		for (int i = 0; i < params.length; i++) {
			T obj = (T)invoke(params[i], type).result;
			
			if (obj.getClass() != type) {
				throw new AnimationInvokeException("Heterogeneous array elements for the command " + invocationCommand);
			}
			
			resultArray.add(obj);
			result[i] = obj;
		}
		
		return Result.of((Class<T[]>)type.arrayType(), resultArray.toArray(result));
	}
	
	private static String[] splitExceptWrapper(String sArgs, char keyword, boolean skipWrapper) {
		List<String> sArgsList = Lists.newArrayList();
		
		int arrayNestCounter = 0;
		StringBuilder sb = new StringBuilder();
		
		for (int i = 0; i < sArgs.length(); i++) {
			char c = sArgs.charAt(i);
			
			if (c == keyword) {
				if (arrayNestCounter < 1) {
					sArgsList.add(sb.toString());
					sb.setLength(0);
				} else {
					sb.append(c);
				}
			} else if (c == '[' || c == '(') {
				if (!skipWrapper || arrayNestCounter > 0) {
					sb.append(c);
				}
				
				arrayNestCounter++;
			} else if (c == ']' || c == ')') {
				arrayNestCounter--;
				
				if (!skipWrapper || arrayNestCounter > 0) {
					sb.append(c);
				}
			} else {
				sb.append(c);
			}
		}
		
		if (!sb.isEmpty()) {
			sArgsList.add(sb.toString());
		}
		
		return sArgsList.toArray(new String[0]);
	}
	
	public static class Result<T> {
		final Class<T> type;
		final T result;
		
		Result(Class<T> type, T result) {
			this.type = type;
			this.result = result;
		}
		
		public static <T> Result<T> of(Class<T> type, T value) {
			return new Result<>(type, value);
		}
		
		public Class<T> getType() {
			return this.type;
		}
		
		public T getResult() {
			return this.result;
		}
	}
}