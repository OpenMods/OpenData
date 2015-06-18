package openeye.asm;

import net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;

public class MethodMatcher {
	private final String clsName;
	private final String description;
	private final String srgName;
	private final String mcpName;

	public MethodMatcher(String clsName, String description, String mcpName, String srgName) {
		this.clsName = clsName;
		this.description = description;
		this.srgName = srgName;
		this.mcpName = mcpName;
	}

	public boolean match(String methodName, String methodDesc) {
		if (!methodDesc.equals(description)) return false;
		if (methodName.equals(mcpName)) return true;
		if (!VisitorHelper.useSrgNames()) return false;
		String mapped = FMLDeobfuscatingRemapper.INSTANCE.mapMethodName(clsName, methodName, methodDesc);
		return mapped.equals(srgName);
	}
}
