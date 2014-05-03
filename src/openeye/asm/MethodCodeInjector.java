package openeye.asm;

import org.objectweb.asm.MethodVisitor;

public abstract class MethodCodeInjector {

	public final String name;

	public final MethodMatcher matcher;

	public abstract MethodVisitor createVisitor(MethodVisitor parent);

	public MethodCodeInjector(String name, MethodMatcher matcher) {
		this.name = name;
		this.matcher = matcher;
	}
}
