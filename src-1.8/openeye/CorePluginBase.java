package openeye;

import java.util.Map;

import openeye.logic.Bootstrap;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

public abstract class CorePluginBase implements IFMLLoadingPlugin {

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
