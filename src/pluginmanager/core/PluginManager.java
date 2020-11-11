package pluginmanager.core;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;

import pluginmanager.api.annotations.EventHandler;
import pluginmanager.api.annotations.EventHandlerSubscriber;
import pluginmanager.api.annotations.Plugin;
import pluginmanager.api.event.Event;
import pluginmanager.api.event.PluginManagerEvent;
import pluginmanager.api.exceptions.MalformedPluginException;
import pluginmanager.api.exceptions.StoredException;
import pluginmanager.loading.JarLoader;
import pluginmanager.util.ConsoleHandler;

/**
 * 
 * This is the main class of the PluginManager.
 * 
 * @author alexander
 *
 */
public class PluginManager {
	
	private final ArrayList<PluginWrapper> PLUGINS = new ArrayList<PluginWrapper>();
	private final ArrayList<Class<Annotation>> CUSTOM_ANNOTATIONS = new ArrayList<Class<Annotation>>();
	private final ArrayList<Class<? extends Event>> EVENTS = new ArrayList<Class<? extends Event>>();
	private final HashMap<Class<? extends Event>, ArrayList<Method>> EVENTLISTENERS = new HashMap<Class<? extends Event>, ArrayList<Method>>();
	private final ArrayList<File> pluginFiles = new ArrayList<File>();
	
	
	/**
	 * Add any classes extending pluginmanager.api.event.Event here, any methods with the @EventHandler annotation will check if the
	 * parameter of the annotation matches an event either specified by this library or by you.
	 * 
	 * @param param
	 */
	public void injectEvent(Class<? extends Event> param) {
		this.EVENTS.add(param);
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
						ConsoleHandler.println("This class is a Plugin main class.");
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
				wrapper = new PluginWrapper(pluginMain, eventHandlerSubscribers, customAnnotatedClasses);
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
		
		//iterate through all the plugins, select all methods matching the EventType and add them to their respective EventList
		for(PluginWrapper plugin : this.PLUGINS) {
			
			ConsoleHandler.println("Checking plugin " + plugin.getMainClass().getAnnotation(Plugin.class).name() + " for EventHandlers");
			
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
		
		if(exceptions.recordedExceptions().length != 0) throw exceptions;
		
		sendEvent(new PluginManagerEvent.InitializationEvent(this));
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
	public Event sendEvent(Event event) throws StoredException {
		
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
		
		return event;
	}
}