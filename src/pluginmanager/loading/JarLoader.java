package pluginmanager.loading;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;

import pluginmanager.discoverer.JarDiscoverer;
import pluginmanager.util.ConsoleHandler;

public class JarLoader {
	public static Class<?> loadClass(String className, File file) throws ClassNotFoundException, IOException {
		ConsoleHandler.println("Loading class " + className + " from jar file " + file.toString());
		
		URLClassLoader loader = new URLClassLoader(new URL[] {file.toURI().toURL()}, ClassLoader.getSystemClassLoader());
		
		Class<?> classToLoad = Class.forName(className, true, loader);

		loader.close();
		
		return classToLoad;
	}
	
	public static Class<?>[] loadJar(File file) throws ClassNotFoundException, IOException {
		ConsoleHandler.println("Loading jar file " + file.toString() + " into JVM");
		String[] classes = JarDiscoverer.getClasses(file);
		
		ArrayList<Class<?>> classObjects = new ArrayList<Class<?>>();
		
		for(String clazz : classes) {
			classObjects.add(JarLoader.loadClass(clazz, file));
		}
		
		return classObjects.toArray(new Class<?>[classObjects.size()]);
	}
}