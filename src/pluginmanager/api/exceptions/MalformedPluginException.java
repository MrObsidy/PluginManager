package pluginmanager.api.exceptions;

public class MalformedPluginException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3892115905957953868L;

	public MalformedPluginException(String reason) {
		super(reason);
	}
}
