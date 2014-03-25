/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.xd.analytics.ml.pmml;

import javax.xml.transform.sax.SAXSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dmg.pmml.PMML;
import org.jpmml.model.ImportFilter;
import org.jpmml.model.JAXBUtil;
import org.springframework.util.Assert;
import org.xml.sax.InputSource;

/**
 * An abstract implementation of a {@link org.springframework.xd.analytics.ml.pmml.PmmlResolver}
 * that can build a {@link org.dmg.pmml.PMML} instance form a given {@link org.xml.sax.InputSource}.
 * Sub-classes can customize the resolving process by implementing the {@link #getPmmlText(String)} method.
 *
 * @author Thomas Darimont
 */
public abstract class AbstractPmmlResolver implements PmmlResolver {

	protected final Log log = LogFactory.getLog(this.getClass());

	/**
	 * Performs the actual resolving process.
	 * Sub-classes should override this method to implement other resolve mechanisms.
	 *
	 * @param name
	 * @return
	 * @throws Exception
	 */
	protected abstract InputSource getPmmlText(String name) throws Exception;

	/**
	 * Returns an {@link org.dmg.pmml.PMML} instance form the given {@code name}.
	 *
	 * @param name must not be {@literal null}.
	 * @return
	 */
	public PMML getPmml(final String name) {

		Assert.notNull("name", name);

		try {

			InputSource pmmlText = getPmmlText(name);

			//ImportFilter handles PMML version differences
			SAXSource transformedSource = ImportFilter.apply(pmmlText);

			return JAXBUtil.unmarshalPMML(transformedSource);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
}
