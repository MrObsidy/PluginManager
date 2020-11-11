package pluginmanager.api.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Only classes with this annotation will be searched for methods with an @EventHandler annotation.
 * 
 * @author alexander
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE) 
public @interface EventHandlerSubscriber {
	
}