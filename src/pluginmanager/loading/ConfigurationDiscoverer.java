package pluginmanager.loading;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import configurationutil.file.ConfigurationFileHandler;
import configurationutil.type.Configuration;
import pluginmanager.util.ConsoleHandler;

public class ConfigurationDiscoverer {
	public static File[] getConfigurations(File directory, boolean searchSubdirectories) throws IllegalArgumentException, MalformedURLException {
		
		ConsoleHandler.println("Looking for plugin jars in directory " + directory.toString());
		
		if (!directory.isDirectory()) throw new IllegalArgumentException("The path " + directory.toString() + " is not a directory");
		
		ArrayList<File> files = new ArrayList<File>();
		
		File[] entries = directory.listFiles();
		
		for(File file : entries) {
			if(file.isDirectory() && searchSubdirectories) {
				ConsoleHandler.println("Found subdirectory, adding subdirectory to list of configuration-fetching");
				Collections.addAll(files, ConfigurationDiscoverer.getConfigurations(file, true));
			} else {
				ConsoleHandler.println("Checking file " + file.toString());
				if(file.toURI().toURL().toString().endsWith(".ccf")) {
					ConsoleHandler.println("Found configuration file " + file.toString() + " to add to configuration list");
					files.add(file);
				} else {
					ConsoleHandler.println("File " + file.toString() + " is not a configuration file");
					//do nothing
				}
			}
		}
		
		ConsoleHandler.println("Found " + files.size() + " configuration files to load in directory " + directory.toString() + ", including subdirectories: " + searchSubdirectories);
		
		return files.toArray(new File[files.size()]);
	}
	
	public static Configuration[] parseConfigurations(File[] files) throws IOException {
		List<Configuration> conf = new ArrayList<Configuration>();
		
		for(File file : files) {
			conf.add(ConfigurationFileHandler.readConfigurationFromFile(file));
		}
		
		return conf.toArray(new Configuration[conf.size()]);
	}
}
