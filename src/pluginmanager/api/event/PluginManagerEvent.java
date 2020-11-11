package pluginmanager.api.event;

public abstract class PluginManagerEvent extends Event {
	
	public PluginManagerEvent(Object sender) {
		super(sender);
	}

	/**
	 * 
	 * 
	 * @author alexander
	 *
	 */
	public static class InitializationEvent extends PluginManagerEvent {

		public InitializationEvent(Object sender) {
			super(sender);
		}
		
	}
	
	public static class ConfigurationLoadingEvent extends PluginManagerEvent {

		public ConfigurationLoadingEvent(Object sender) {
			super(sender);
		}
		
	}
}