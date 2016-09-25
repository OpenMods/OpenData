package openeye.asm.injectors;

import com.google.common.base.Throwables;
import java.io.Writer;
import openeye.Log;
import openeye.asm.CallHack;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

public class CrashHandlerInjector extends MethodVisitor {
	private final Method streamClose;
	private final Method callTarget;
	private final Type callHackType;

	public CrashHandlerInjector(MethodVisitor mv) {
		super(Opcodes.ASM5, mv);

		try {
			streamClose = new Method("closeQuietly", Type.VOID_TYPE, new Type[] { Type.getType(Writer.class) });
			callHackType = Type.getType(CallHack.class);
			callTarget = Method.getMethod(CallHack.class.getMethod("callFromCrashHandler", Object.class));
		} catch (Throwable t) {
			throw Throwables.propagate(t);
		}
	}

	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean intf) {
		super.visitMethodInsn(opcode, owner, name, desc, intf);
		if (streamClose.getName().equals(name) && streamClose.getDescriptor().equals(desc)) {
			Log.debug("Adding handler for 'crash_handler'");
			visitVarInsn(Opcodes.ALOAD, 0);
			visitMethodInsn(Opcodes.INVOKESTATIC, callHackType.getInternalName(), callTarget.getName(), callTarget.getDescriptor(), false);
		}
	}
}
