package pluginmanager.loading;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import pluginmanager.util.ConsoleHandler;

public class JarDiscoverer {
	public static File[] getJars(File directory, boolean searchSubdirectories) throws IllegalArgumentException, MalformedURLException {
		
		ConsoleHandler.println("Looking for plugin jars in directory " + directory.toString());
		
		if (!directory.isDirectory()) throw new IllegalArgumentException("The path " + directory.toString() + " is not a directory");
		
		ArrayList<File> files = new ArrayList<File>();
		
		File[] entries = directory.listFiles();
		
		for(File file : entries) {
			if(file.isDirectory() && searchSubdirectories) {
				ConsoleHandler.println("Found subdirectory, adding subdirectory to list of jar-fetching");
				Collections.addAll(files, JarDiscoverer.getJars(file, true));
			} else {
				ConsoleHandler.println("Checking file " + file.toString());
				if(file.toURI().toURL().toString().endsWith(".jar")) {
					ConsoleHandler.println("Found jar file " + file.toString() + " to add to plugin list");
					files.add(file);
				} else {
					ConsoleHandler.println("File " + file.toString() + " is not a jar file");
					//do nothing
				}
			}
		}
		
		ConsoleHandler.println("Found " + files.size() + " jar files to load in directory " + directory.toString() + ", including subdirectories: " + searchSubdirectories);
		
		return files.toArray(new File[files.size()]);
	}
	
	public static String[] getClasses(File jar) {
		
		ConsoleHandler.println("Getting classes in jar " + jar.toString());
		
		JarFile file = null;
		ArrayList<String> classes = new ArrayList<String>();
		try {
			file = new JarFile(jar);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		Enumeration<JarEntry> entries = file.entries();
		while(entries.hasMoreElements()) {
			JarEntry entry = entries.nextElement();
			
			if(!entry.isDirectory()) {
				if(!entry.getName().endsWith(".class")) continue;
				classes.add(entry.getName().replaceAll("/", "\\.").substring(0, entry.getName().replaceAll("/", "\\.").lastIndexOf('.')));
			}
		}
		
		ConsoleHandler.println("Found " + classes.size() + " classes to load in Jarfile " + jar.toString());
		
		return classes.toArray(new String[classes.size()]);
	}
}
