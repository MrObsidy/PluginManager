package pluginmanager.api.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.Documented;

/**
 * 
 * 
 * @author alexander
 *
 */
@Target(ElementType.TYPE)
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface Plugin {
	String name();
	
	String id();
	
	String version();
	
	boolean canBeLoadedAtRuntime() default false;
}
