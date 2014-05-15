package openeye.logic;

import java.util.SortedSet;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;

public class Sanitizer {

	public interface ITransformer {
		public String transform(String input);
	}

	private static class TransformerWrapper implements Comparable<TransformerWrapper>, ITransformer {

		private final int priority;

		private final ITransformer transformer;

		private TransformerWrapper(int priority, ITransformer transformer) {
			this.priority = priority;
			this.transformer = transformer;
		}

		@Override
		public int compareTo(TransformerWrapper o) {
			return o.priority - priority;
		}

		@Override
		public String transform(String input) {
			return transformer.transform(input);
		}

		@Override
		public String toString() {
			return "[" + priority + "]" + transformer;
		}
	}

	public Sanitizer() {
		this(null);
	}

	public Sanitizer(Sanitizer parent) {
		this.parent = parent;
	}

	private final Sanitizer parent;

	private final SortedSet<TransformerWrapper> pre = Sets.newTreeSet();
	private final SortedSet<TransformerWrapper> post = Sets.newTreeSet();

	public void addPre(int priority, ITransformer transformer) {
		if (transformer != null) pre.add(new TransformerWrapper(priority, transformer));
	}

	public void addPost(int priority, ITransformer transformer) {
		if (transformer != null) post.add(new TransformerWrapper(priority, transformer));
	}

	public String sanitize(String input) {
		if (Strings.isNullOrEmpty(input)) return "";

		for (ITransformer transformer : pre)
			input = transformer.transform(input);

		if (parent != null) input = parent.sanitize(input);

		for (ITransformer transformer : post)
			input = transformer.transform(input);

		return input;
	}
}
