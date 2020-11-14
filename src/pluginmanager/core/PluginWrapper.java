package pluginmanager.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;

public class PluginWrapper {
	
	private final String pluginID;
	private final String pluginVersion;
	private final String pluginName;
	private final Class<?> pluginClass;
	private final ArrayList<Class<?>> eventHandlerSubscribers;
	private final HashMap<String, ArrayList<Class<?>>> customAnnotated;
	private final Object pluginInstance;
	
	public PluginWrapper(Class<?> pluginClass, ArrayList<Class<?>> subscribers, HashMap<String, ArrayList<Class<?>>>
	withCustomAnnotation, String id, String version, String name) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
	
		this.pluginClass = pluginClass;
		this.eventHandlerSubscribers = subscribers;
		this.customAnnotated = withCustomAnnotation;
		this.pluginInstance = this.getNewPluginInstance();
		this.pluginID = id;
		this.pluginVersion = version;
		this.pluginName = name;
	}
	
	/**
	 * Creates a new Instance of this plugin. Internal use only!
	 * 
	 * @return
	 * @throws InvocationTargetException 
	 * @throws IllegalArgumentException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws SecurityException 
	 * @throws NoSuchMethodException 
	 */
	Object getNewPluginInstance() throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		Object  newInstance = null;

		Constructor<?> constructor = pluginClass.getConstructor();
		newInstance = constructor.newInstance();
		
		return newInstance;
	}
	
	/**
	 * Get all eventHandlers in this plugin
	 * 
	 * @return an Array of EventHandlers
	 */
	public Class<?>[] getEventHandlers() {
		return this.eventHandlerSubscribers.toArray(new Class<?>[this.eventHandlerSubscribers.size()]);
	}
	
	/**
	 * Get all classses with custom Annotations
	 * 
	 * @return
	 */
	public HashMap<String, ArrayList<Class<?>>> getCustomAnnotatedClasses() {
		return this.customAnnotated;
	}
	
	public Class<?> getMainClass() {
		return this.pluginClass;
	}
	
	public Object getInstance() {
		return this.pluginInstance;
	}
	
	public String getID() {
		return this.pluginID;
	}
	
	public String getVersion() {
		return this.pluginVersion;
	}
	
	public String getName() {
		return this.pluginName;
	}
	
	/**
	 * Gets  all classes in this plugin with the Annotation with the name name.
	 * 
	 * @param name
	 * @return an Array of classes or an empty array if no classes with that annotation were found.
	 */
	public Class<?>[] getClassesWithAnnotation(String name) {
		if (this.customAnnotated.get(name) == null) {
			return new Class<?>[0];
		} else {
			return this.customAnnotated.get(name).toArray(new Class<?>[this.customAnnotated.get(name).size()]);
		}
	}


}
