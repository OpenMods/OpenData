package openeye.logic;

import java.util.Deque;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

public class Sanitizer {

	public interface ITransformer {
		public String transform(String input);
	}

	private static class SimpleReplace implements ITransformer {
		private final String target;
		private final String value;

		public SimpleReplace(String target, String value) {
			this.target = target;
			this.value = value;
		}

		@Override
		public String transform(String input) {
			return input.replace(target, value);
		}

		@Override
		public String toString() {
			return String.format("'%s'->'%s'", target, value);
		}

	}

	private static class PropertyReplace extends SimpleReplace {
		public PropertyReplace(String property, String value) {
			super(System.getProperty(property), value);
		}
	}

	public static ITransformer replace(Object target, String value) {
		return new SimpleReplace(target.toString(), value);
	}

	private static final Deque<ITransformer> TRANSFORMERS = Lists.newLinkedList();

	static {
		TRANSFORMERS.addLast(new PropertyReplace("user.dir", "[workdir]"));
		TRANSFORMERS.addLast(new PropertyReplace("user.home", "[home]"));
		TRANSFORMERS.addLast(new PropertyReplace("user.name", "[user]"));
	}

	public static void addFirst(ITransformer transformer) {
		TRANSFORMERS.addFirst(transformer);
	}

	public static void addLast(ITransformer transformer) {
		TRANSFORMERS.addLast(transformer);
	}

	public static String sanitize(String input) {
		if (Strings.isNullOrEmpty(input)) return "";

		for (ITransformer transformer : TRANSFORMERS)
			input = transformer.transform(input);

		return input;
	}
}
