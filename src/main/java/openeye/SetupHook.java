package openeye;

import cpw.mods.fml.relauncher.IFMLCallHook;
import java.util.Map;
import openeye.logic.Bootstrap;

public class SetupHook implements IFMLCallHook {

	@Override
	public Void call() throws Exception {
		Bootstrap.instance.startup();
		return null;
	}

	@Override
	public void injectData(Map<String, Object> data) {
		Bootstrap.instance.populateFromSetupClass(data);
	}

}
