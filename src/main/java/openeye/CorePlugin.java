package openeye;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin.Name;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin.TransformerExclusions;
import java.util.Map;
import openeye.logic.Bootstrap;

@Name("OpenEyePlugin")
@TransformerExclusions("openeye")
public class CorePlugin implements IFMLLoadingPlugin {

	@Override
	public String getAccessTransformerClass() {
		return null;
	}

	@Override
	public String[] getASMTransformerClass() {
		return new String[] { "openeye.asm.MultiTransformer" };
	}

	@Override
	public String getModContainerClass() {
		return "openeye.Mod";
	}

	@Override
	public String getSetupClass() {
		return "openeye.SetupHook";
	}

	@Override
	public void injectData(Map<String, Object> data) {
		Bootstrap.instance.populateFromInject(data);
	}
}