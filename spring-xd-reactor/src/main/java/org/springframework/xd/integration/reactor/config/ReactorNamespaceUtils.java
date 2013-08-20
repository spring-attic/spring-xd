package org.springframework.xd.integration.reactor.config;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * @author Jon Brisbin
 */
public abstract class ReactorNamespaceUtils {

	public static final String REACTOR_ENV_BEAN = "xd.reactor.env";

	protected ReactorNamespaceUtils() {
	}

	public static BeanDefinitionBuilder createBeanDefitionBuilder(Class<?> componentType, Element element) {
		String envRef = element.getAttribute("env");
		if (!StringUtils.hasText(envRef)) {
			envRef = REACTOR_ENV_BEAN;
		}

		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(componentType);
		builder.addConstructorArgReference(envRef);

		return builder;
	}

}
