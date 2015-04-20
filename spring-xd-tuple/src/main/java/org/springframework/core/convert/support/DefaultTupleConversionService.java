/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.core.convert.support;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.ResolvableType;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalConverter;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.xd.tuple.Tuple;

/**
 * Base {@link ConversionService} implementation suitable for use with {@link Tuple}.
 * 
 * @author David Turanski
 * @author Michael Minella
 */
public class DefaultTupleConversionService implements ConfigurableConversionService {

	private final Converters converters = new Converters();

	private final Map<ConverterCacheKey, GenericConverter> converterCache =
			new ConcurrentHashMap<>(64);

	private static final GenericConverter NO_OP_CONVERTER = new NoOpConverter("NO_OP");
	private static final GenericConverter NO_MATCH = new NoOpConverter("NO_MATCH");

	private static final List<ConverterEntry> defaultConvertersByType = new ArrayList<>(12);
	private static final List<ConverterFactoryEntry> defaultConverterFactories = new ArrayList<>(4);

	static {
		Converter toString = new ObjectToStringConverter();
		defaultConvertersByType.add(new ConverterEntry(String.class, Character.class, new StringToCharacterConverter()));
		defaultConvertersByType.add(new ConverterEntry(Number.class, Character.class, new NumberToCharacterConverter()));
		defaultConvertersByType.add(new ConverterEntry(String.class, Boolean.class, new StringToBooleanConverter()));
		defaultConvertersByType.add(new ConverterEntry(String.class, Locale.class, new StringToLocaleConverter()));
		defaultConvertersByType.add(new ConverterEntry(String.class, Properties.class, new StringToPropertiesConverter()));
		defaultConvertersByType.add(new ConverterEntry(Properties.class, String.class, new PropertiesToStringConverter()));
		defaultConvertersByType.add(new ConverterEntry(String.class, UUID.class, new StringToUUIDConverter()));

		defaultConvertersByType.add(new ConverterEntry(UUID.class, String.class, toString));
		defaultConvertersByType.add(new ConverterEntry(Number.class, String.class, toString));
		defaultConvertersByType.add(new ConverterEntry(Locale.class, String.class, toString));
		defaultConvertersByType.add(new ConverterEntry(Boolean.class, String.class, toString));
		defaultConvertersByType.add(new ConverterEntry(Character.class, String.class, toString));

		defaultConverterFactories.add(new ConverterFactoryEntry(Number.class, Number.class, new NumberToNumberConverterFactory()));
		defaultConverterFactories.add(new ConverterFactoryEntry(String.class, Number.class, new StringToNumberConverterFactory()));
		defaultConverterFactories.add(new ConverterFactoryEntry(Character.class, Number.class, new CharacterToNumberFactory()));
		defaultConverterFactories.add(new ConverterFactoryEntry(String.class, Enum.class, new StringToEnumConverterFactory()));
	}

	public DefaultTupleConversionService() {
		for (ConverterFactoryEntry defaultConverterFactory : defaultConverterFactories) {
			this.addConverterFactory(defaultConverterFactory.getSource(), defaultConverterFactory.getTarget(), defaultConverterFactory.getConverterFactory());
		}

		for (ConverterEntry converterEntry : defaultConvertersByType) {
			this.addConverter(converterEntry.getSource(), converterEntry.getTarget(), converterEntry.getConverter());
		}

		this.addConverter(new ArrayToCollectionConverter(this));
		this.addConverter(new CollectionToArrayConverter(this));
		this.addConverter(new XdArrayToArrayConverter(this));
		this.addConverter(new CollectionToCollectionConverter(this));
		this.addConverter(new MapToMapConverter(this));
		this.addConverter(new ArrayToStringConverter(this));
		this.addConverter(new StringToArrayConverter(this));
		this.addConverter(new ArrayToObjectConverter(this));
		this.addConverter(new ObjectToArrayConverter(this));
		this.addConverter(new CollectionToStringConverter(this));
		this.addConverter(new StringToCollectionConverter(this));
		this.addConverter(new ObjectToCollectionConverter(this));
		this.addConverter(new ObjectToObjectConverter());
		this.addConverter(new IdToEntityConverter(this));
		this.addConverter(new FallbackObjectToStringConverter());

		this.addConverter(Enum.class, String.class, new EnumToStringConverter(this));
	}

	@Override
	public boolean canConvert(Class<?> sourceType, Class<?> targetType) {
		Assert.notNull(targetType, "targetType to convert to cannot be null");
		return canConvert((sourceType != null ? TypeDescriptor.valueOf(sourceType) : null),
				TypeDescriptor.valueOf(targetType));
	}

	@Override
	public boolean canConvert(TypeDescriptor sourceType, TypeDescriptor targetType) {
		Assert.notNull(targetType, "targetType to convert to cannot be null");
		if (sourceType == null) {
			return true;
		}
		GenericConverter converter = getConverter(sourceType, targetType);
		return (converter != null);
	}


	@Override
	@SuppressWarnings("unchecked")
	public <T> T convert(Object source, Class<T> targetType) {
		Assert.notNull(targetType,"The targetType to convert to cannot be null");
		return (T) convert(source, TypeDescriptor.forObject(source), TypeDescriptor.valueOf(targetType));
	}

	@Override
	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		Assert.notNull(targetType,"The targetType to convert to cannot be null");
		if (sourceType == null) {
			Assert.isTrue(source == null, "The source must be [null] if sourceType == [null]");
			return handleResult(sourceType, targetType, null);
		}
		if (source != null && !sourceType.getObjectType().isInstance(source)) {
			throw new IllegalArgumentException("The source to convert from must be an instance of " +
					sourceType + "; instead it was a " + source.getClass().getName());
		}
		GenericConverter converter = getConverter(sourceType, targetType);
		if (converter != null) {
			Object result = invokeConverter(converter, source, sourceType, targetType);
			return handleResult(sourceType, targetType, result);
		}
		return handleConverterNotFound(source, sourceType, targetType);
	}

	@Override
	public void addConverter(Converter<?, ?> converter) {
		Assert.notNull(converter);
		ResolvableType[] typeInfo = getRequiredTypeInfo(converter, Converter.class);
		Assert.notNull(typeInfo, "Unable to the determine sourceType <S> and targetType " +
				"<T> which your Converter<S, T> converts between; declare these generic types.");
		addConverter(new ConverterAdapter(converter, typeInfo[0], typeInfo[1]));
	}

	@Override
	public void addConverter(Class<?> sourceType, Class<?> targetType, Converter<?, ?> converter) {
		addConverter(new ConverterAdapter(converter, ResolvableType.forClass(sourceType), ResolvableType.forClass(targetType)));
	}

	@Override
	public void addConverter(GenericConverter converter) {
		this.converters.add(converter);
		invalidateCache();
	}

	@Override
	public void addConverterFactory(ConverterFactory<?, ?> converterFactory) {
		ResolvableType[] typeInfo = getRequiredTypeInfo(converterFactory, ConverterFactory.class);
		Assert.notNull("Unable to the determine sourceType <S> and targetRangeType R which your " +
				"ConverterFactory<S, R> converts between; declare these generic types.");
		addConverter(new ConverterFactoryAdapter(converterFactory,
				new GenericConverter.ConvertiblePair(typeInfo[0].resolve(), typeInfo[1].resolve())));

	}

	public void addConverterFactory(Class source, Class target, ConverterFactory converterFactory) {
		addConverter(new ConverterFactoryAdapter(converterFactory,
				new GenericConverter.ConvertiblePair(source, target)));
	}

	public boolean canBypassConvert(TypeDescriptor sourceType, TypeDescriptor targetType) {
		Assert.notNull(targetType, "targetType to convert to cannot be null");
		if (sourceType == null) {
			return true;
		}
		GenericConverter converter = getConverter(sourceType, targetType);
		return (converter == NO_OP_CONVERTER);
	}

	@Override
	public void removeConvertible(Class<?> sourceType, Class<?> targetType) {
		this.converters.remove(sourceType, targetType);
		invalidateCache();
	}

	private ResolvableType[] getRequiredTypeInfo(Object converter, Class<?> genericIfc) {
		ResolvableType resolvableType = ResolvableType.forClass(converter.getClass()).as(genericIfc);
		ResolvableType[] generics = resolvableType.getGenerics();
		if (generics.length < 2) {
			return null;
		}
		Class<?> sourceType = generics[0].resolve();
		Class<?> targetType = generics[1].resolve();
		if (sourceType == null || targetType == null) {
			return null;
		}
		return generics;
	}

	/**
	 * Hook method to lookup the converter for a given sourceType/targetType pair.
	 * First queries this ConversionService's converter cache.
	 * On a cache miss, then performs an exhaustive search for a matching converter.
	 * If no converter matches, returns the default converter.
	 * Subclasses may override.
	 * @param sourceType the source type to convert from
	 * @param targetType the target type to convert to
	 * @return the generic converter that will perform the conversion, or {@code null} if
	 * no suitable converter was found
	 */
	protected GenericConverter getConverter(TypeDescriptor sourceType, TypeDescriptor targetType) {
		ConverterCacheKey key = new ConverterCacheKey(sourceType, targetType);
		GenericConverter converter = this.converterCache.get(key);
		if (converter != null) {
			return (converter != NO_MATCH ? converter : null);
		}

		converter = this.converters.find(sourceType, targetType);
		if (converter == null) {
			converter = getDefaultConverter(sourceType, targetType);
		}

		if (converter != null) {
			this.converterCache.put(key, converter);
			return converter;
		}

		this.converterCache.put(key, NO_MATCH);
		return converter;
	}

	private Object handleResult(TypeDescriptor sourceType, TypeDescriptor targetType, Object result) {
		if (result == null) {
			assertNotPrimitiveTargetType(sourceType, targetType);
		}
		return result;
	}

	private void assertNotPrimitiveTargetType(TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (targetType.isPrimitive()) {
			throw new ConversionFailedException(sourceType, targetType, null,
					new IllegalArgumentException("A null value cannot be assigned to a primitive type"));
		}
	}

	private Object handleConverterNotFound(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (source == null) {
			assertNotPrimitiveTargetType(sourceType, targetType);
			return source;
		}
		if (sourceType.isAssignableTo(targetType) && targetType.getObjectType().isInstance(source)) {
			return source;
		}
		throw new ConverterNotFoundException(sourceType, targetType);
	}

	public Object invokeConverter(GenericConverter converter, Object source, TypeDescriptor sourceType,
			TypeDescriptor targetType) {
		try {
			return converter.convert(source, sourceType, targetType);
		}
		catch (ConversionFailedException ex) {
			throw ex;
		}
		catch (Exception ex) {
			throw new ConversionFailedException(sourceType, targetType, source, ex);
		}
	}
	protected GenericConverter getDefaultConverter(TypeDescriptor sourceType, TypeDescriptor targetType) {
		return (sourceType.isAssignableTo(targetType) ? NO_OP_CONVERTER : null);
	}

	private void invalidateCache() {
		this.converterCache.clear();
	}

	/**
	 * Adapts a {@link Converter} to a {@link GenericConverter}.
	 */
	@SuppressWarnings("unchecked")
	private static final class ConverterAdapter implements ConditionalGenericConverter {

		private final Converter<Object, Object> converter;

		private final ConvertiblePair typeInfo;

		private final ResolvableType targetType;

		public ConverterAdapter(Converter<?, ?> converter, ResolvableType sourceType, ResolvableType targetType) {
			this.converter = (Converter<Object, Object>) converter;
			this.typeInfo = new ConvertiblePair(sourceType.resolve(Object.class), targetType.resolve(Object.class));
			this.targetType = targetType;
		}

		@Override
		public Set<ConvertiblePair> getConvertibleTypes() {
			return Collections.singleton(this.typeInfo);
		}

		@Override
		public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
			// Check raw type first...
			if (!this.typeInfo.getTargetType().equals(targetType.getObjectType())) {
				return false;
			}
			// Full check for complex generic type match required?
			ResolvableType rt = targetType.getResolvableType();
			if (!(rt.getType() instanceof Class) && !rt.isAssignableFrom(this.targetType) &&
					!this.targetType.hasUnresolvableGenerics()) {
				return false;
			}
			return !(this.converter instanceof ConditionalConverter) ||
					((ConditionalConverter) this.converter).matches(sourceType, targetType);
		}

		@Override
		public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
			if (source == null) {
				return null;
			}
			return this.converter.convert(source);
		}

		@Override
		public String toString() {
			return this.typeInfo + " : " + this.converter;
		}
	}

	/**
	 * Manages all converters registered with the service.
	 */
	private static class Converters {

		private final Set<GenericConverter> globalConverters = new LinkedHashSet<GenericConverter>();

		private final Map<GenericConverter.ConvertiblePair, ConvertersForPair> converters =
				new LinkedHashMap<GenericConverter.ConvertiblePair, ConvertersForPair>(36);

		public void add(GenericConverter converter) {
			Set<GenericConverter.ConvertiblePair> convertibleTypes = converter.getConvertibleTypes();
			if (convertibleTypes == null) {
				Assert.state(converter instanceof ConditionalConverter,
						"Only conditional converters may return null convertible types");
				this.globalConverters.add(converter);
			}
			else {
				for (GenericConverter.ConvertiblePair convertiblePair : convertibleTypes) {
					ConvertersForPair convertersForPair = getMatchableConverters(convertiblePair);
					convertersForPair.add(converter);
				}
			}
		}

		private ConvertersForPair getMatchableConverters(GenericConverter.ConvertiblePair convertiblePair) {
			ConvertersForPair convertersForPair = this.converters.get(convertiblePair);
			if (convertersForPair == null) {
				convertersForPair = new ConvertersForPair();
				this.converters.put(convertiblePair, convertersForPair);
			}
			return convertersForPair;
		}

		public void remove(Class<?> sourceType, Class<?> targetType) {
			this.converters.remove(new GenericConverter.ConvertiblePair(sourceType, targetType));
		}

		/**
		 * Find a {@link GenericConverter} given a source and target type.
		 * <p>This method will attempt to match all possible converters by working
		 * through the class and interface hierarchy of the types.
		 * @param sourceType the source type
		 * @param targetType the target type
		 * @return a matching {@link GenericConverter}, or {@code null} if none found
		 */
		public GenericConverter find(TypeDescriptor sourceType, TypeDescriptor targetType) {
			// Search the full type hierarchy
			List<Class<?>> sourceCandidates = getClassHierarchy(sourceType.getType());
			List<Class<?>> targetCandidates = getClassHierarchy(targetType.getType());
			for (Class<?> sourceCandidate : sourceCandidates) {
				for (Class<?> targetCandidate : targetCandidates) {
					GenericConverter.ConvertiblePair convertiblePair = new GenericConverter.ConvertiblePair(sourceCandidate, targetCandidate);
					GenericConverter converter = getRegisteredConverter(sourceType, targetType, convertiblePair);
					if (converter != null) {
						return converter;
					}
				}
			}
			return null;
		}

		private GenericConverter getRegisteredConverter(TypeDescriptor sourceType,
				TypeDescriptor targetType, GenericConverter.ConvertiblePair convertiblePair) {

			// Check specifically registered converters
			ConvertersForPair convertersForPair = this.converters.get(convertiblePair);
			if (convertersForPair != null) {
				GenericConverter converter = convertersForPair.getConverter(sourceType, targetType);
				if (converter != null) {
					return converter;
				}
			}
			// Check ConditionalGenericConverter that match all types
			for (GenericConverter globalConverter : this.globalConverters) {
				if (((ConditionalConverter)globalConverter).matches(sourceType, targetType)) {
					return globalConverter;
				}
			}
			return null;
		}

		/**
		 * Returns an ordered class hierarchy for the given type.
		 * @param type the type
		 * @return an ordered list of all classes that the given type extends or implements
		 */
		private List<Class<?>> getClassHierarchy(Class<?> type) {
			List<Class<?>> hierarchy = new ArrayList<Class<?>>(20);
			Set<Class<?>> visited = new HashSet<Class<?>>(20);
			addToClassHierarchy(0, ClassUtils.resolvePrimitiveIfNecessary(type), false, hierarchy, visited);
			boolean array = type.isArray();
			int i = 0;
			while (i < hierarchy.size()) {
				Class<?> candidate = hierarchy.get(i);
				candidate = (array ? candidate.getComponentType() : ClassUtils.resolvePrimitiveIfNecessary(candidate));
				Class<?> superclass = candidate.getSuperclass();
				if (superclass != null && superclass != Object.class && superclass != Enum.class) {
					addToClassHierarchy(i + 1, candidate.getSuperclass(), array, hierarchy, visited);
				}
				addInterfacesToClassHierarchy(candidate, array, hierarchy, visited);
				i++;
			}

			if (Enum.class.isAssignableFrom(type)) {
				addToClassHierarchy(hierarchy.size(), Enum.class, array, hierarchy, visited);
				addToClassHierarchy(hierarchy.size(), Enum.class, false, hierarchy, visited);
				addInterfacesToClassHierarchy(Enum.class, array, hierarchy, visited);
			}

			addToClassHierarchy(hierarchy.size(), Object.class, array, hierarchy, visited);
			addToClassHierarchy(hierarchy.size(), Object.class, false, hierarchy, visited);
			return hierarchy;
		}

		private void addInterfacesToClassHierarchy(Class<?> type, boolean asArray,
				List<Class<?>> hierarchy, Set<Class<?>> visited) {

			for (Class<?> implementedInterface : type.getInterfaces()) {
				addToClassHierarchy(hierarchy.size(), implementedInterface, asArray, hierarchy, visited);
			}
		}

		private void addToClassHierarchy(int index, Class<?> type, boolean asArray,
				List<Class<?>> hierarchy, Set<Class<?>> visited) {

			if (asArray) {
				type = Array.newInstance(type, 0).getClass();
			}
			if (visited.add(type)) {
				hierarchy.add(index, type);
			}
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("ConversionService converters =\n");
			for (String converterString : getConverterStrings()) {
				builder.append('\t').append(converterString).append('\n');
			}
			return builder.toString();
		}

		private List<String> getConverterStrings() {
			List<String> converterStrings = new ArrayList<String>();
			for (ConvertersForPair convertersForPair : converters.values()) {
				converterStrings.add(convertersForPair.toString());
			}
			Collections.sort(converterStrings);
			return converterStrings;
		}
	}

	/**
	 * Manages converters registered with a specific {@link GenericConverter.ConvertiblePair}.
	 */
	private static class ConvertersForPair {

		private final LinkedList<GenericConverter> converters = new LinkedList<GenericConverter>();

		public void add(GenericConverter converter) {
			this.converters.addFirst(converter);
		}

		public GenericConverter getConverter(TypeDescriptor sourceType, TypeDescriptor targetType) {
			for (GenericConverter converter : this.converters) {
				if (!(converter instanceof ConditionalGenericConverter) ||
						((ConditionalGenericConverter) converter).matches(sourceType, targetType)) {
					return converter;
				}
			}
			return null;
		}

		@Override
		public String toString() {
			return StringUtils.collectionToCommaDelimitedString(this.converters);
		}
	}

	/**
	 * Adapts a {@link ConverterFactory} to a {@link GenericConverter}.
	 */
	@SuppressWarnings("unchecked")
	private static final class ConverterFactoryAdapter implements ConditionalGenericConverter {

		private final ConverterFactory<Object, Object> converterFactory;

		private final ConvertiblePair typeInfo;

		public ConverterFactoryAdapter(ConverterFactory<?, ?> converterFactory, ConvertiblePair typeInfo) {
			this.converterFactory = (ConverterFactory<Object, Object>) converterFactory;
			this.typeInfo = typeInfo;
		}

		@Override
		public Set<ConvertiblePair> getConvertibleTypes() {
			return Collections.singleton(this.typeInfo);
		}

		@Override
		public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
			boolean matches = true;
			if (this.converterFactory instanceof ConditionalConverter) {
				matches = ((ConditionalConverter) this.converterFactory).matches(sourceType, targetType);
			}
			if (matches) {
				Converter<?, ?> converter = this.converterFactory.getConverter(targetType.getType());
				if (converter instanceof ConditionalConverter) {
					matches = ((ConditionalConverter) converter).matches(sourceType, targetType);
				}
			}
			return matches;
		}

		@Override
		public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
			if (source == null) {
				return null;
			}
			return this.converterFactory.getConverter(targetType.getObjectType()).convert(source);
		}

		@Override
		public String toString() {
			return this.typeInfo + " : " + this.converterFactory;
		}
	}

	/**
	 * Internal converter that performs no operation.
	 */
	private static class NoOpConverter implements GenericConverter {

		private final String name;

		public NoOpConverter(String name) {
			this.name = name;
		}

		@Override
		public Set<ConvertiblePair> getConvertibleTypes() {
			return null;
		}

		@Override
		public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
			return source;
		}

		@Override
		public String toString() {
			return this.name;
		}
	}

	private static class GenericConverterEntry {
		private final Class source;
		private final Class target;
		private final GenericConverter converter;

		public GenericConverterEntry(Class source, Class target, GenericConverter converter) {
			Assert.notNull(source);
			Assert.notNull(target);
			Assert.notNull(converter);

			this.source = source;
			this.target = target;
			this.converter = converter;
		}

		public Class getSource() {
			return source;
		}

		public Class getTarget() {
			return target;
		}

		public GenericConverter getConverter() {
			return converter;
		}
	}

	private static class ConverterEntry {
		private final Class source;
		private final Class target;
		private final Converter converter;

		public ConverterEntry(Class source, Class target, Converter converter) {
			Assert.notNull(source);
			Assert.notNull(target);
			Assert.notNull(converter);

			this.source = source;
			this.target = target;
			this.converter = converter;
		}

		public Class getSource() {
			return source;
		}

		public Class getTarget() {
			return target;
		}

		public Converter getConverter() {
			return converter;
		}
	}

	private static class ConverterFactoryEntry {
		private final Class source;
		private final Class target;
		private final ConverterFactory converter;

		public ConverterFactoryEntry(Class source, Class target, ConverterFactory converter) {
			Assert.notNull(source);
			Assert.notNull(target);
			Assert.notNull(converter);

			this.source = source;
			this.target = target;
			this.converter = converter;
		}

		public Class getSource() {
			return source;
		}

		public Class getTarget() {
			return target;
		}

		public ConverterFactory getConverterFactory() {
			return converter;
		}
	}

	/**
	 * Key for use with the converter cache.
	 */
	private static final class ConverterCacheKey {

		private final TypeDescriptor sourceType;

		private final TypeDescriptor targetType;

		public ConverterCacheKey(TypeDescriptor sourceType, TypeDescriptor targetType) {
			this.sourceType = sourceType;
			this.targetType = targetType;
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof ConverterCacheKey)) {
				return false;
			}
			ConverterCacheKey otherKey = (ConverterCacheKey) other;
			return ObjectUtils.nullSafeEquals(this.sourceType, otherKey.sourceType) &&
					ObjectUtils.nullSafeEquals(this.targetType, otherKey.targetType);
		}

		@Override
		public int hashCode() {
			return ObjectUtils.nullSafeHashCode(this.sourceType) * 29 +
					ObjectUtils.nullSafeHashCode(this.targetType);
		}

		@Override
		public String toString() {
			return "ConverterCacheKey [sourceType = " + this.sourceType +
					", targetType = " + this.targetType + "]";
		}
	}
}
