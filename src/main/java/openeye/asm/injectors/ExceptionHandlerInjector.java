package openeye.asm.injectors;

import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import java.util.Map;
import openeye.Log;
import openeye.asm.CallHack;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

public class ExceptionHandlerInjector extends MethodVisitor {

	private final Method callTarget;
	private final Type callHackType;

	private final String[] excNames;
	private final Map<Label, String> excLabels = Maps.newIdentityHashMap();
	int currentLabel;
	private boolean skipHandlers;

	public ExceptionHandlerInjector(MethodVisitor mv, String... excNames) {
		super(Opcodes.ASM4, mv);

		this.excNames = excNames;

		try {
			callHackType = Type.getType(CallHack.class);
			callTarget = Method.getMethod(CallHack.class.getMethod("callForSilentException", Throwable.class, String.class));
		} catch (Throwable t) {
			throw Throwables.propagate(t);
		}
	}

	@Override
	public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
		super.visitTryCatchBlock(start, end, handler, type);

		if (!skipHandlers && "java/lang/Exception".equals(type)) {
			try {
				String name = excNames[currentLabel++];
				excLabels.put(handler, name);
			} catch (ArrayIndexOutOfBoundsException e) {
				Log.warn("Invalid method structure, more than two exception handlers. Aborting");
				skipHandlers = true;
			}
		}
	}

	@Override
	public void visitLabel(Label label) {
		super.visitLabel(label);

		if (!skipHandlers) {
			String name = excLabels.get(label);
			if (name != null) addHandler(name);
		}
	}

	private void addHandler(String location) {
		Log.info("Adding handler for '%s'", location);
		super.visitInsn(Opcodes.DUP);
		super.visitLdcInsn(location);
		super.visitMethodInsn(Opcodes.INVOKESTATIC, callHackType.getInternalName(), callTarget.getName(), callTarget.getDescriptor());
	}
}
