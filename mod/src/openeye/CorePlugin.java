package openeye;

import java.util.Map;

import openeye.logic.LoadDataStore;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin.TransformerExclusions;

@TransformerExclusions("openeye")
public class CorePlugin implements IFMLLoadingPlugin {

	@Override
	public String[] getASMTransformerClass() {
		return new String[] { "openeye.asm.CrashReportInjector" };
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
		LoadDataStore.instance.populateFromInject(data);
	}

	@Override
	@Deprecated
	public String[] getLibraryRequestClass() {
		return null;
	}

}