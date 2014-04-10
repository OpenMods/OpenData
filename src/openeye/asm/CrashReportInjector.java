package openeye.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import openeye.asm.VisitorHelper.TransformProvider;

import org.objectweb.asm.ClassVisitor;

public class CrashReportInjector implements IClassTransformer {

	@Override
	public byte[] transform(final String name, String transformedName, byte[] bytes) {
		if (bytes == null) return bytes;

		if (transformedName.equals("net.minecraft.crash.CrashReport")) return VisitorHelper.apply(bytes, 0, new TransformProvider() {
			@Override
			public ClassVisitor createVisitor(ClassVisitor cv) {
				return new CrashHandlerTransformer(cv, name);
			}
		});

		return bytes;
	}

}
