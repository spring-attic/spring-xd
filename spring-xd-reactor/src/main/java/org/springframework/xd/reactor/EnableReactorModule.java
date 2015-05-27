package org.springframework.xd.reactor;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * Add this annotation to an {@code Bean}, likely a {@link ReactiveModule} class to have
 * the imported Spring XD Reactor configuration
 *
 * @author Stephane Maldini
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(ReactorModuleConfiguration.class)
@Configuration
public @interface EnableReactorModule {

}
