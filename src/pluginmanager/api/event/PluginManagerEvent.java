package pluginmanager.api.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import configurationutil.type.Configuration;

public abstract class PluginManagerEvent extends Event {
	
	public PluginManagerEvent(Object sender) {
		super(sender);
	}

	/**
	 * This event gets called whenever a plugin is loaded from disk. Registering dependencies must be registered to this event, or they will not be registered!
	 * 
	 * @author alexander
	 *
	 */
	public static class InitializationEvent extends PluginManagerEvent {
		
		private List<String> dependencies = new ArrayList<String>();
		private List<Class<? extends Event>> newEvents = new ArrayList<Class<? extends Event>>();
		
		public InitializationEvent(Object sender) {
			super(sender);
		}
		
		public List<String> getDependencies() {
			return this.dependencies;
		}
		
		/**
		 * Use this method to set any dependencies your plugin depends on. 
		 * 
		 * @param id - the id of the plugin you depend on
		 * @param version - the version of the plugin you depend on
		 * @param lowerVersion - the minimum Version of the required Plugin (pass null if any version is fine)
		 * @param upperVersion - the maximum Version of the required plugin (pass null if any version is fine)
		 */
		public void addDependency(String id, String lowerVersion, String upperVersion) {
			
			if(lowerVersion == null) lowerVersion = "any";
			if(upperVersion == null) upperVersion = "any";
			
			dependencies.add(id + "@" + lowerVersion + ":" + upperVersion);
		}
		
		public void addEvent(Class<? extends Event> event) {
			this.newEvents.add(event);
		}

		public List<Class<? extends Event>> getEvents() {
			return this.newEvents;
		}
	}
	
	public static class ConfigurationLoadingEvent extends PluginManagerEvent {
		
		private Map<String, Configuration> configurations = new HashMap<String, Configuration>();
		
		public ConfigurationLoadingEvent(Object sender) {
			super(sender);
		}
		
		public void addConfiguration(String pluginId, Configuration configuration) {
			this.configurations.put(pluginId, configuration);
		}
		
		public Configuration getConfiguration(String pluginId) {
			return this.configurations.get(pluginId);
		}
	}
	
	public static class DependencyRegisteringEvent extends PluginManagerEvent {
		
		private Map<String, ArrayList<String>> dependencies = new HashMap<String, ArrayList<String>>();
		
		public DependencyRegisteringEvent(Object sender) {
			super(sender);
		}
		
		public void registerDependency(String neededByID, String dependingOnID) {
			
			if(this.dependencies.get(neededByID) == null) {
				this.dependencies.put(neededByID, new ArrayList<String>());
			}
			
			this.dependencies.get(neededByID).add(dependingOnID);
		}
		
		public List<String> getDependenciesForPlugin(String neededByID) {
			return this.dependencies.get(neededByID);
		}
	}
}