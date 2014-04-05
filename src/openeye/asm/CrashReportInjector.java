package openeye.asm;

import net.minecraft.launchwrapper.IClassTransformer;

public class CrashReportInjector implements IClassTransformer {

	@Override
	public byte[] transform(final String name, String transformedName, byte[] bytes) {
		return bytes;
	}

}
