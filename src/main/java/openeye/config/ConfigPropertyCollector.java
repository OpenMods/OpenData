package openeye.config;

import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

public class ConfigPropertyCollector {

	private abstract static class ConfigPropertyHolder implements IConfigPropertyHolder {

		private final String name;
		private final String category;
		private final String comment;

		public ConfigPropertyHolder(String name, String category, String comment) {
			this.name = name;
			this.category = category;
			this.comment = comment;
		}

		@Override
		public String name() {
			return name;
		}

		@Override
		public String category() {
			return category;
		}

		@Override
		public String comment() {
			return comment;
		}
	}

	private static class InstancePropertyHolder extends ConfigPropertyHolder {

		private final Field field;
		private final Object target;

		public InstancePropertyHolder(Field field, Object target, String name, String category, String comment) {
			super(name, category, comment);
			this.field = field;
			this.target = target;
		}

		@Override
		public Object getValue() {
			try {
				return field.get(target);
			} catch (Throwable t) {
				throw Throwables.propagate(t);
			}
		}

		@Override
		public void setValue(Object value) {
			try {
				field.set(target, value);
			} catch (Throwable t) {
				throw Throwables.propagate(t);
			}
		}

		@Override
		public Class<?> getType() {
			return field.getType();
		}
	}

	private static IConfigPropertyHolder createHolder(ConfigProperty annotation, Field field, Object target) {
		String name = annotation.name();
		String category = annotation.category();

		if (Strings.isNullOrEmpty(name)) name = field.getName();
		if (Strings.isNullOrEmpty(category)) category = null;

		return new InstancePropertyHolder(field, target, name, category, annotation.comment());
	}

	public static List<IConfigPropertyHolder> collectFromClass(Class<?> cls) {
		List<IConfigPropertyHolder> result = Lists.newArrayList();

		for (Field f : cls.getFields()) {
			if (!Modifier.isStatic(f.getModifiers())) continue;
			ConfigProperty property = f.getAnnotation(ConfigProperty.class);
			if (property != null) result.add(createHolder(property, f, null));
		}

		return result;
	}

	public static List<IConfigPropertyHolder> collectFromInstance(Object target, boolean excludeStatic) {
		List<IConfigPropertyHolder> result = Lists.newArrayList();

		for (Field f : target.getClass().getFields()) {
			boolean isStatic = Modifier.isStatic(f.getModifiers());
			if (excludeStatic && isStatic) continue;
			ConfigProperty property = f.getAnnotation(ConfigProperty.class);
			if (property != null) result.add(createHolder(property, f, isStatic? null : target));
		}

		return result;
	}
}
