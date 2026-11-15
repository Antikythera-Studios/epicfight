package yesman.epicfight.api.utils;
import yesman.epicfight.EpicFight;

public class GLConstants {
	/**
	 * Extracted the GL constants to avoid direct using of gl classes since it causes issues
	 */
	public static final int GL_BYTE = 5120;
	public static final int GL_SHORT = 5122;
	public static final int GL_INT = 5124;
	public static final int GL_FLOAT = 5126;
	public static final int GL_DOUBLE = 5130;
	public static final int GL_ARRAY_BUFFER = 34962;
	public static final int GL_ELEMENT_ARRAY_BUFFER = 34963;
	public static final int GL_STATIC_DRAW = 35044;
	public static final int GL_VERTEX_ARRAY_BINDING = 34229;
	public static final int GL_VERTEX_ARRAY_BUFFER_BINDING = 34966;
	
	/*
	public static void checkAllMatches() {
		if (GL_BYTE != GL11.GL_BYTE) {
			EpicFight.LOGGER.warn("GL_BYTE doesn't match " + GL_BYTE + " " + GL11.GL_BYTE);
		}
		
		if (GL_SHORT != GL11.GL_SHORT) {
			EpicFight.LOGGER.warn("GL_SHORT doesn't match " + GL_SHORT + " " + GL11.GL_SHORT);
		}
		
		if (GL_INT != GL11.GL_INT) {
			EpicFight.LOGGER.warn("GL_INT doesn't match " + GL_INT + " " + GL11.GL_INT);
		}
		
		if (GL_FLOAT != GL11.GL_FLOAT) {
			EpicFight.LOGGER.warn("GL_FLOAT doesn't match " + GL_FLOAT + " " + GL11.GL_FLOAT);
		}
		
		if (GL_DOUBLE != GL11.GL_DOUBLE) {
			EpicFight.LOGGER.warn("GL_DOUBLE doesn't match " + GL_DOUBLE + " " + GL11.GL_DOUBLE);
		}
		
		if (GL_ARRAY_BUFFER != GL15.GL_ARRAY_BUFFER) {
			EpicFight.LOGGER.warn("GL_FLOAT doesn't match " + GL_ARRAY_BUFFER + " " + GL15.GL_ARRAY_BUFFER);
		}
		
		if (GL_ELEMENT_ARRAY_BUFFER != GL15.GL_ELEMENT_ARRAY_BUFFER) {
			EpicFight.LOGGER.warn("GL_ELEMENT_ARRAY_BUFFER doesn't match " + GL_ELEMENT_ARRAY_BUFFER + " " + GL15.GL_ELEMENT_ARRAY_BUFFER);
		}
		
		if (GL_STATIC_DRAW != GL15.GL_STATIC_DRAW) {
			EpicFight.LOGGER.warn("GL_STATIC_DRAW doesn't match " + GL_STATIC_DRAW + " " + GL15.GL_ARRAY_BUFFER);
		}
		
		if (GL_VERTEX_ARRAY_BINDING != GL30.GL_VERTEX_ARRAY_BINDING) {
			EpicFight.LOGGER.warn("GL_VERTEX_ARRAY_BINDING doesn't match " + GL_VERTEX_ARRAY_BINDING + " " + GL30.GL_VERTEX_ARRAY_BINDING);
		}
		
		if (GL_VERTEX_ARRAY_BUFFER_BINDING != GL30.GL_VERTEX_ARRAY_BUFFER_BINDING) {
			EpicFight.LOGGER.warn("GL_VERTEX_ARRAY_BINDING doesn't match " + GL_VERTEX_ARRAY_BUFFER_BINDING + " " + GL30.GL_VERTEX_ARRAY_BUFFER_BINDING);
		}
	}
	*/
}
