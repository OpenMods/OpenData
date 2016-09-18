package openeye.asm;

import java.util.Collection;
import openeye.Log;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class SingleClassTransformer extends ClassVisitor {

	private final Collection<MethodCodeInjector> methodInjectors;

	public SingleClassTransformer(ClassVisitor cv, String obfClassName, Collection<MethodCodeInjector> injectors) {
		super(Opcodes.ASM4, cv);

		this.methodInjectors = injectors;
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		MethodVisitor parent = super.visitMethod(access, name, desc, signature, exceptions);
		for (MethodCodeInjector injector : methodInjectors)
			if (injector.matcher.match(name, desc)) {
				Log.info("Applying method transformer %s for method %s(%s)", injector.name, name, desc);
				return injector.createVisitor(parent);
			}

		return parent;
	}

}
