package pluginmanager.util;

public class ConsoleHandler {
	private static boolean isOutputting = true;
	
	public static void setOutputting(boolean outputting) {
		isOutputting = outputting;
	}
	
	public static boolean getOutputting() {
		return isOutputting;
	}
	
	public static void println(String line) {
		if(isOutputting) {
			System.out.println("[PluginManager] " + line);
		}
	}
}