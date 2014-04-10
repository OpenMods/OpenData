package openeye.asm;

import java.io.File;
import java.io.FileWriter;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.Method;

import com.google.common.base.Throwables;

import cpw.mods.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;

public class CrashHandlerTransformer extends ClassVisitor {

	private final MethodMatcher saveToFileMatcher;

	public CrashHandlerTransformer(ClassVisitor cv, String obfClassName) {
		super(Opcodes.ASM4, cv);

		String logHandlerName = "net/minecraft/logging/ILogAgent";

		if (VisitorHelper.useSrgNames()) {
			logHandlerName = FMLDeobfuscatingRemapper.INSTANCE.unmap(logHandlerName);
		}

		Type logHandlerType = Type.getObjectType(logHandlerName);
		Type fileType = Type.getType(File.class);

		Type methodType = Type.getMethodType(Type.BOOLEAN_TYPE, fileType, logHandlerType);

		saveToFileMatcher = new MethodMatcher(obfClassName, methodType.getDescriptor(), "saveToFile", "func_71508_a");
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		MethodVisitor parent = super.visitMethod(access, name, desc, signature, exceptions);
		return saveToFileMatcher.match(name, desc)? new CallInjector(parent) : parent;
	}

	protected static class CallInjector extends MethodVisitor {

		private final Method fileWriterClose;
		private final Method callTarget;
		private final Type callHackType;

		public CallInjector(MethodVisitor mv) {
			super(Opcodes.ASM4, mv);

			try {
				fileWriterClose = Method.getMethod(FileWriter.class.getMethod("close"));
				callHackType = Type.getType(CallHack.class);
				callTarget = Method.getMethod(CallHack.class.getMethod("callFromCrashHandler", Object.class));
			} catch (Throwable t) {
				throw Throwables.propagate(t);
			}
		}

		@Override
		public void visitMethodInsn(int opcode, String owner, String name, String desc) {
			super.visitMethodInsn(opcode, owner, name, desc);
			if (fileWriterClose.getName().equals(name) && fileWriterClose.getDescriptor().equals(desc)) {
				visitVarInsn(Opcodes.ALOAD, 0);
				visitMethodInsn(Opcodes.INVOKESTATIC, callHackType.getInternalName(), callTarget.getName(), callTarget.getDescriptor());
			}
		}
	}

}
