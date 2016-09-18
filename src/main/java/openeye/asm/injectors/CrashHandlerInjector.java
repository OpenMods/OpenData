package openeye.asm.injectors;

import com.google.common.base.Throwables;
import java.io.FileWriter;
import openeye.asm.CallHack;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

public class CrashHandlerInjector extends MethodVisitor {
	private final Method fileWriterClose;
	private final Method callTarget;
	private final Type callHackType;

	public CrashHandlerInjector(MethodVisitor mv) {
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
