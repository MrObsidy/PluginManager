package pluginmanager.api.event;

import java.lang.reflect.Method;
import java.util.ArrayList;

public abstract class Event {
	private final Object SENDER;
	private final ArrayList<Method> handledBy = new ArrayList<Method>();
	
	public Event(Object sender) {
		this.SENDER = sender;
	}
	
	public Object getSender() {
		return this.SENDER;
	}
	
	public void addHandler(Method handler) {
		this.handledBy.add(handler);
	}
	
	public Method[] getHandlers() {
		return this.handledBy.toArray(new Method[this.handledBy.size()]);
	}
}