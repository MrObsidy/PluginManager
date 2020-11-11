package test;

import java.io.File;
import java.net.MalformedURLException;

import pluginmanager.api.exceptions.StoredException;
import pluginmanager.core.PluginManager;
import pluginmanager.discoverer.JarDiscoverer;
import pluginmanager.util.ConsoleHandler;

public class Test {

	public static void main(String[] args) {
		PluginManager manager = new PluginManager();
		
		ConsoleHandler.setOutputting(true);
		
		File[] loadableJars = null;
		try {
			loadableJars = JarDiscoverer.getJars(new File("/windows/daten/GAMEDEV/Java/TestPlugin/build/"), true);
		} catch (IllegalArgumentException | MalformedURLException e1) {
			e1.printStackTrace();
			System.exit(-1);
		}
		
		for(File file : loadableJars) {
			manager.addPluginToLoad(file);
		}
		
		try {
			manager.initialize();
		} catch (StoredException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

}
