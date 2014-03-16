package org.springframework.xd.analytics.model.jpmml;

import static org.springframework.xd.tuple.TupleBuilder.tuple;

import javax.xml.transform.sax.SAXSource;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.dmg.pmml.PMML;
import org.jpmml.model.ImportFilter;
import org.jpmml.model.JAXBUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.ReflectionUtils;
import org.springframework.xd.tuple.Tuple;
import org.xml.sax.InputSource;

/**
 * Author: Thomas Darimont
 */
public abstract class AbstractJpmmlAnalyticalModelEvaluatorTests {

	static final String ANALYTICS_MODELS_LOCATION = "analytics/models/";

	protected JpmmlAnalyticalModelEvaluator analyticalModelEvaluator;


	protected void useModel(String modelName, Set<String> inputFieldNames, List<String> outputFieldNames) throws Exception {

		JpmmlAnalyticalModel model = new JpmmlAnalyticalModel(loadPmmlModel(modelName));
		model.setInputFields(inputFieldNames);
		model.setOutputFieldsNames(outputFieldNames);
		model.init();

		analyticalModelEvaluator.setModel(model);
	}

	protected static PMML loadPmmlModel(String modelName) throws Exception {

		InputSource pmmlStream = new InputSource(new ClassPathResource(ANALYTICS_MODELS_LOCATION + modelName).getInputStream());
		SAXSource transformedSource = ImportFilter.apply(pmmlStream);

		return JAXBUtil.unmarshalPMML(transformedSource);
	}

	protected static Tuple objectToTuple(final Object o) {

		final List<String> fieldNames = new ArrayList<String>();
		final List<Object> fieldValues = new ArrayList<Object>();

		ReflectionUtils.doWithFields(o.getClass(), new ReflectionUtils.FieldCallback() {
			@Override
			public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {

				if(field.isSynthetic()){
					return;
				}

				Value value = field.getAnnotation(Value.class);
				fieldNames.add(value == null ? field.getName() : value.value());
				fieldValues.add(field.get(o));
			}
		});

		return tuple().ofNamesAndValues(fieldNames,fieldValues);
	}
}
