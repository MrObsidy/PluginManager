package pluginmanager.api.exceptions;

import java.util.ArrayList;

public class StoredException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1023701717667601148L;
	
	private final ArrayList<Exception> recordedExceptions = new ArrayList<Exception>();
	
	public StoredException() {
		super("The following exceptions have occured:");
	}
	
	public void addException(Exception e) {
		this.recordedExceptions.add(e);
	}
	
	public Exception[] recordedExceptions() {
		return this.recordedExceptions.toArray(new Exception[this.recordedExceptions.size()]);
	}
	
	@Override
	public void printStackTrace() {
		super.printStackTrace();
		System.err.println("Recorded Exceptions: " + recordedExceptions.size());
		System.err.println("Exceptions: ");
		for(Exception e : this.recordedExceptions) {
			e.printStackTrace();
		}
	}
}
