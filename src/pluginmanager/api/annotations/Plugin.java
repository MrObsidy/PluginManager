package pluginmanager.api.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.Documented;

/**
 * Classes annotated with this annotation will be loaded as a plugin. Only one class per jar can be annotated with this annotations, otherwise
 * PluginManager:initialize() will throw an exception.
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
