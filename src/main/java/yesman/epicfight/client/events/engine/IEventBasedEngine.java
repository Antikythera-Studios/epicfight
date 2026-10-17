package yesman.epicfight.client.events.engine;



public interface IEventBasedEngine {
	public void gameEventBus(Object gameEventBus);
	
	public void modEventBus(Object modEventBus);
	
	public static void init(Object gameEventBus, Object modEventBus) {
		RenderEngine.getInstance().gameEventBus(gameEventBus);
		RenderEngine.getInstance().modEventBus(modEventBus);
		ControlEngine.getInstance().gameEventBus(gameEventBus);
		ControlEngine.getInstance().modEventBus(modEventBus);
	}
}
