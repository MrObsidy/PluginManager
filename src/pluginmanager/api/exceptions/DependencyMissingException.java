package pluginmanager.api.exceptions;

public class DependencyMissingException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1863919930742553356L;

	public DependencyMissingException(String reason) {
		super(reason);
	}
}
