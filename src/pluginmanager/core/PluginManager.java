package pluginmanager.core;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import configurationutil.type.Configuration;
import pluginmanager.api.annotations.EventHandler;
import pluginmanager.api.annotations.EventHandlerSubscriber;
import pluginmanager.api.annotations.Plugin;
import pluginmanager.api.event.Event;
import pluginmanager.api.event.PluginManagerEvent;
import pluginmanager.api.exceptions.DependencyMissingException;
import pluginmanager.api.exceptions.MalformedPluginException;
import pluginmanager.api.exceptions.StoredException;
import pluginmanager.loading.ConfigurationDiscoverer;
import pluginmanager.loading.JarLoader;
import pluginmanager.util.ConsoleHandler;

/**
 * 
 * This is the main class of the PluginManager.
 * 
 * @author alexander
 *
 */
@Plugin(id = "pluginmanager", version = "1.0.0", name = "Plugin Manager")
public class PluginManager {
	
	private final List<PluginWrapper> PLUGINS = new ArrayList<PluginWrapper>();
	private final List<Class<Annotation>> CUSTOM_ANNOTATIONS = new ArrayList<Class<Annotation>>();
	private final List<Class<? extends Event>> EVENTS = new ArrayList<Class<? extends Event>>();
	private final Map<Class<? extends Event>, ArrayList<Method>> EVENTLISTENERS = new HashMap<Class<? extends Event>, ArrayList<Method>>();
	private final List<File> pluginFiles = new ArrayList<File>();
	
	
	/**
	 * Add any classes extending pluginmanager.api.event.Event here, any methods with the @EventHandler annotation will check if the
	 * parameter of the annotation matches an event either specified by this library or by you. In order for this to take effect during runtime,
	 * you need to call refreshEvents();
	 * 
	 * @param param
	 */
	public void injectEvent(Class<? extends Event> param) {
		this.EVENTS.add(param);
	}
	
	/**
	 * Resort all the events so that they can be quickly loaded at runtime
	 * 
	 */
	public void refreshEvents() {
		for(PluginWrapper plugin : this.PLUGINS) {
			
			try {
				ConsoleHandler.println("Checking plugin " + plugin.getMainClass().getAnnotation(Plugin.class).id() + " for EventHandlers");
			} catch (Exception e) {
				System.out.println("This should never happen");
				e.printStackTrace();
			}
				
			for(Class<?> eventReceiver : plugin.getEventHandlers()) {
				for(Method method : eventReceiver.getMethods()) {
					if (method.isAnnotationPresent(EventHandler.class)) {
						ConsoleHandler.println("Checking method " + method.toString());
						Parameter param = method.getParameters()[0];
						for(Class<?> event : this.EVENTS) {
							if(param.getType().isAssignableFrom(event)) {
								this.EVENTLISTENERS.get(param.getType()).add(method);
								ConsoleHandler.println("Adding method " + method.toString() + " to EventListener type " + this.EVENTLISTENERS.get(param.getType()).toString() + " which now contains " + this.EVENTLISTENERS.get(param.getType()).size() + " eventHandlers");
							}
						}
					}
				}
			}
		}
	}
	
	/**
	 * Adds custom annotations to look out for when loading a plugin.
	 * 
	 * @param clazz
	 */
	public void addCustomAnnotationToCheckFor(Class<Annotation> clazz) {
		this.CUSTOM_ANNOTATIONS.add(clazz);
	}
	
	public void addPluginToLoad(File file) {
		pluginFiles.add(file);
	}
	
	public boolean isPluginLoaded(String id) {
		boolean is = false;
		
		for(PluginWrapper wrapper : this.PLUGINS) {
			if(wrapper.getID().contentEquals(id)) is = true;
		}
		
		return is;
	}
	
	public boolean isPluginLoadedAtVersion(String id, String version) {
		boolean is = false;
		
		for(PluginWrapper wrapper : this.PLUGINS) {
			if(wrapper.getID().equals(id) && wrapper.getVersion().equals(version)) is = true;
		}
		
		
		return is;
	}
	
	public boolean isPluginLoadedAtOrAboveVersion(String id, String minimumVersion) {
		boolean is = false;
		
		for(PluginWrapper wrapper : this.PLUGINS) {
			if(wrapper.getID().equals(id)) {
				String[] version = wrapper.getVersion().split(Pattern.quote("."));
				int major = Integer.parseInt(version[0]);
				int minor = Integer.parseInt(version[1]);
				int patch = Integer.parseInt(version[2]);
				
				String[] minVersion = minimumVersion.split(Pattern.quote("."));
				
				int minMajor = Integer.parseInt(minVersion[0]);
				int minMinor = Integer.parseInt(minVersion[1]);
				int minPatch = Integer.parseInt(minVersion[2]);
				
				if(major > minMajor) is = true;
				if(major == minMajor && minor > minMinor) is = true;
				if(major == minMajor && minor == minMinor && patch >= minPatch) is = true;
			}
		}
		
		return is;
	}
	
	public boolean isPluginLoadedAtOrBelowVersion(String id, String maximumVersion) {
		boolean is = false;
		
		for(PluginWrapper wrapper : this.PLUGINS) {
			if(wrapper.getID().equals(id)) {
				String[] version = wrapper.getVersion().split(Pattern.quote("."));
				int major = Integer.parseInt(version[0]);
				int minor = Integer.parseInt(version[1]);
				int patch = Integer.parseInt(version[2]);
				
				String[] maxVersion = maximumVersion.split(Pattern.quote("."));
				
				int maxMajor = Integer.parseInt(maxVersion[0]);
				int maxMinor = Integer.parseInt(maxVersion[1]);
				int maxPatch = Integer.parseInt(maxVersion[2]);
				
				if(major < maxMajor) is = true;
				if(major == maxMajor && minor < maxMinor) is = true;
				if(major == maxMajor && minor == maxMinor && patch <= maxPatch) is = true;
			}
		}
		
		return is;
	}
	
	/**
	 * Loads configurations from disk and relays them to the plugins.
	 * 
	 * @param directory - the configuration folder directory
	 * @param includeSubdirectories - include subdirectories when searching directory
	 * @param otherConfigs - any other configurations you wish to load
	 * 
	 * @throws StoredException
	 */
	public void loadConfigurations(File directory, boolean includeSubdirectories, Configuration[] otherConfigs) throws StoredException  {
		StoredException ex = new StoredException();
		
		File[] files = null;
		
		try {
			files = ConfigurationDiscoverer.getConfigurations(directory, includeSubdirectories);
		} catch (IllegalArgumentException | MalformedURLException e) {
			ex.addException(e);
		}
		
		Configuration[] configs = null;
		
		try {
			configs = ConfigurationDiscoverer.parseConfigurations(files);
		} catch (IOException e) {
			ex.addException(e);
		}
		
		PluginManagerEvent.ConfigurationLoadingEvent confLoadEvent = new PluginManagerEvent.ConfigurationLoadingEvent(this);
		
		for(Configuration config : configs) {
			try {
				confLoadEvent.addConfiguration(config.getSubConfiguration("pluginid").getValue(), config);
			} catch (Exception e) {
				ex.addException(e);
			}
		}
		
		for(Configuration config : otherConfigs) {
			try {
				confLoadEvent.addConfiguration(config.getSubConfiguration("pluginid").getValue(), config);
			} catch (Exception e) {
				ex.addException(e);
			}
		}
		
		if(ex.recordedExceptions().length != 0) throw ex;
		
		this.sendEvent(confLoadEvent);
	}
	
	public void addMethodToEventBus(Class<? extends Event> eventType, Method method) throws StoredException {
		this.EVENTLISTENERS.get(eventType).add(method);
	}
	
	public void loadPluginAtRuntime(File path) throws StoredException {
		StoredException exceptions = new StoredException();
		
		Class<?>[] classes = null;
		try {
			classes = JarLoader.loadJar(path);
		} catch (ClassNotFoundException | IOException e) {
			exceptions.addException(e);
		}
		ArrayList<Class<?>> eventHandlerSubscribers = new ArrayList<Class<?>>();
		HashMap<String, ArrayList<Class<?>>> customAnnotatedClasses = new HashMap<String, ArrayList<Class<?>>>();
		Class<?> pluginMain = null;
		
		//prepare a holder for each custom annotation
		for(Class<Annotation> customAnnotation : this.CUSTOM_ANNOTATIONS) {
			customAnnotatedClasses.put(customAnnotation.getName(), new ArrayList<Class<?>>());
		}
		
		for(Class<?> clazz : classes) {				
			ConsoleHandler.println("Inspecting " + clazz.getName() + " for annotations, " + clazz.getAnnotations().length + " annotations present");
			
			Annotation[] annotations = clazz.getAnnotations();
			
			for(Annotation annotation : annotations) {
				
									
				if(annotation.annotationType().isAssignableFrom(Plugin.class)) {
					ConsoleHandler.println("This class is a Plugin main class.");
					if(pluginMain != null ) {
						exceptions.addException(new MalformedPluginException("Multiple plugins detected in jar file " + path.toString()));
					} else {
						if(!((Plugin) annotation).canBeLoadedAtRuntime()) {
							exceptions.addException(new MalformedPluginException("This plugin may not be loaded at runtime!"));
						}
						pluginMain = clazz;
					}
				}
				
				if(annotation.annotationType().isAssignableFrom(EventHandlerSubscriber.class)) {
					ConsoleHandler.println("This class is an EventHandlerSubscriber");
					eventHandlerSubscribers.add(clazz);
				}
				
				for(Class<Annotation> customAnnotation : this.CUSTOM_ANNOTATIONS) {
					if(annotation.annotationType().isAssignableFrom(customAnnotation)) {
						ConsoleHandler.println("This class has a custom annotation: " + annotation.annotationType().toString());
						customAnnotatedClasses.get(annotation.annotationType().toString()).add(clazz);
					}
				}
			}
		}
		
		if(exceptions.recordedExceptions().length != 0) throw exceptions;
		
	}
	
	/**
	 * Call this after you have set the plugin directory and plugin configuration directory. Also, register the eventHandler types.
	 * This method does all the heavy lifting (loading Jars, adding them to classpath, registering them with this pluginmanager, etc.)
	 * @throws PluginInitializationException 
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 * @throws SecurityException 
	 * @throws NoSuchMethodException 
	 * @throws InvocationTargetException 
	 * @throws IllegalArgumentException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws MalformedPluginException 
	 */
	public void initialize() throws StoredException  {		
		
		ConsoleHandler.println("-----");
		ConsoleHandler.println("Initializing plugin loading...");
		ConsoleHandler.println("-----");
		
		StoredException exceptions = new StoredException();
		
		for(File jar : pluginFiles) {
			Class<?>[] classes = null;
			try {
				classes = JarLoader.loadJar(jar);
			} catch (ClassNotFoundException | IOException e) {
				exceptions.addException(e);
			}
			ArrayList<Class<?>> eventHandlerSubscribers = new ArrayList<Class<?>>();
			HashMap<String, ArrayList<Class<?>>> customAnnotatedClasses = new HashMap<String, ArrayList<Class<?>>>();
			Class<?> pluginMain = null;
			
			//prepare a holder for each custom annotation
			for(Class<Annotation> customAnnotation : this.CUSTOM_ANNOTATIONS) {
				customAnnotatedClasses.put(customAnnotation.getName(), new ArrayList<Class<?>>());
			}
			
			for(Class<?> clazz : classes) {				
				ConsoleHandler.println("Inspecting " + clazz.getName() + " for annotations, " + clazz.getAnnotations().length + " annotations present");
				
				Annotation[] annotations = clazz.getAnnotations();
				
				for(Annotation annotation : annotations) {
					
										
					if(annotation.annotationType().isAssignableFrom(Plugin.class)) {
						ConsoleHandler.println("This class is a Plugin main class, PluginID: " + ((Plugin) annotation).id());
						if(pluginMain != null ) {
							exceptions.addException(new MalformedPluginException("Multiple plugins detected in jar file " + jar.toString()));
						} else {
							pluginMain = clazz;
						}
					}
					
					if(annotation.annotationType().isAssignableFrom(EventHandlerSubscriber.class)) {
						ConsoleHandler.println("This class is an EventHandlerSubscriber");
						eventHandlerSubscribers.add(clazz);
					}
					
					for(Class<Annotation> customAnnotation : this.CUSTOM_ANNOTATIONS) {
						if(annotation.annotationType().isAssignableFrom(customAnnotation)) {
							ConsoleHandler.println("This class has a custom annotation: " + annotation.annotationType().toString());
							customAnnotatedClasses.get(annotation.annotationType().toString()).add(clazz);
						}
					}
				}
			}
			
			if(pluginMain == null) {
				//no plugin was found
				exceptions.addException(new MalformedPluginException("No plugin entry was found in plugin : " + jar.toString()));
			}
			
			PluginWrapper wrapper = null;
			
			try {
				wrapper = new PluginWrapper(pluginMain, eventHandlerSubscribers, customAnnotatedClasses, pluginMain.getAnnotation(Plugin.class).id(), pluginMain.getAnnotation(Plugin.class).version(), pluginMain.getAnnotation(Plugin.class).name());
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException | SecurityException | NullPointerException e) {
				exceptions.addException(e);
			}
			
			this.PLUGINS.add(wrapper);
			
			injectDefaultEvents();
			
			for(Class<? extends Event> eventType : this.EVENTS) {
				this.EVENTLISTENERS.put(eventType, new ArrayList<Method>());
			}
		}
		
		//register itself, to make room for version checking of the plugin manager
		try {
			PluginWrapper pluginManagerWrapper = new PluginWrapper(PluginManager.class, new ArrayList<Class<?>>(), new HashMap<String, ArrayList<Class<?>>>(), "pluginmanager", "1.0.0", "Plugin Manager");
			this.PLUGINS.add(pluginManagerWrapper);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e1) {
			e1.printStackTrace();
			//this should never happen
		}
		
		//iterate through all the plugins, select all methods matching the EventType and add them to their respective EventList
		this.refreshEvents();
		
		if(exceptions.recordedExceptions().length != 0) throw exceptions;
		
		PluginManagerEvent.InitializationEvent inEv = new PluginManagerEvent.InitializationEvent(this);
		
		sendEvent(inEv);
		
		ConsoleHandler.println("Checking for dependencies...");
		ConsoleHandler.println("Dependencies: " + inEv.getDependencies().size());
		
		boolean metDependencies = true;
		ArrayList<String> missingDependencies = new ArrayList<String>();
		
		for(String dep : inEv.getDependencies()) {
			ConsoleHandler.println("Checking dependency: " + dep);
			String[] split = dep.split(Pattern.quote("@"));
			String[] version = split[1].split(Pattern.quote(":"));
			String id = split[0];
			String lower = version[0];
			String upper = version[1];
			split = null;
			version = null;
			
			if(lower.equals("any") && !upper.equals("any")) {
				metDependencies = metDependencies & this.isPluginLoadedAtOrBelowVersion(id, upper);
				if(!this.isPluginLoadedAtOrBelowVersion(id, upper)) missingDependencies.add(dep);
			} else if(upper.equals("any") && !lower.equals("any")) {
				metDependencies = metDependencies & this.isPluginLoadedAtOrAboveVersion(id, lower);
				if(!this.isPluginLoadedAtOrAboveVersion(id, lower)) missingDependencies.add(dep);
			} else if(lower.equals("any") && upper.equals("any")) {
				metDependencies = metDependencies & this.isPluginLoaded(id);
				if(!this.isPluginLoaded(id)) missingDependencies.add(dep);
			} else {
				metDependencies = metDependencies & this.isPluginLoadedAtOrAboveVersion(id, lower) & this.isPluginLoadedAtOrBelowVersion(id, upper);
				if(!(this.isPluginLoadedAtOrBelowVersion(id, upper) & this.isPluginLoadedAtOrAboveVersion(id, lower))) missingDependencies.add(dep);
			}
		}
		
		if(!metDependencies) {
			for(String md : missingDependencies) {	
				exceptions.addException(new DependencyMissingException("Missing a dependency: " + md));
			}
			throw exceptions;
		}
		
		ConsoleHandler.println("Done checking dependencies.");
		ConsoleHandler.println("Registering plugin-injected events...");
		
		for(Class<? extends Event> event : inEv.getEvents()) {
			this.injectEvent(event);
		}
		
		this.refreshEvents();
		
		ConsoleHandler.println("Done registering plugin-injected events.");
		ConsoleHandler.println("Done initialiting.");
	}
	
	/**
	 * convenienve method
	 * 
	 */
	private void injectDefaultEvents() {
		this.injectEvent(PluginManagerEvent.ConfigurationLoadingEvent.class);
		this.injectEvent(PluginManagerEvent.InitializationEvent.class);
	}

	/**
	 * Returns a new Instance of this plugin. Do not use this unless you know exactly what you're doing, changes done to the
	 * Object returned by this function are not necessarily global!
	 * 
	 * @param name
	 * @return a new instance of the plugin or null if no plugin with the name name was found.
	 * @throws SecurityException 
	 * @throws NoSuchMethodException 
	 * @throws InvocationTargetException 
	 * @throws IllegalArgumentException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	@Deprecated
	public Object getNewInstanceOfPlugin(String name) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		for(PluginWrapper wrapper : this.PLUGINS) {
			Class<?> mainClass = wrapper.getMainClass();
			if(mainClass.getAnnotation(Plugin.class).name().equals(name)) {
				return wrapper.getNewPluginInstance();
			}
		}
		return null;
	}
	
	/**
	 * Gets the Instance of the specified plugin.
	 * 
	 * @param name
	 * @return
	 */
	public Object getPluginInstance(String name) {
		for(PluginWrapper wrapper : this.PLUGINS) {
			Class<?> mainClass = wrapper.getMainClass();
			if(mainClass.getAnnotation(Plugin.class).name().equals(name)) {
				return wrapper.getInstance();
			}
		}
		return null;
	}
	
	/**
	 * Send an event to all the plugins. Only EventHandlers which's parameter matches the type of event you send will receive the event.
	 * 
	 * @param event
	 * @return
	 * @throws StoredException 
	 * @throws InvocationTargetException 
	 * @throws IllegalArgumentException 
	 * @throws IllegalAccessException 
	 */
	public void sendEvent(Event event) throws StoredException {
		
		ConsoleHandler.println("Sending event " + event.getClass().toString());
		
		ArrayList<Method> methods = this.EVENTLISTENERS.get(event.getClass());
		
		StoredException exception = new StoredException();
		
		if(methods != null) {
			for(Method method : methods) {
				try {
					method.invoke(null, event);
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NullPointerException e) {
					ConsoleHandler.println("Error invoking eventHandler on " + method.toString() + ", is the method static?");
					exception.addException(e);
				}
				event.addHandler(method);
			}
		}
		
		if(exception.recordedExceptions().length != 0) throw exception;
		
		exception = null;		
	}
}