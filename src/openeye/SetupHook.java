package openeye;

import java.util.Map;

import openeye.logic.LoadDataStore;
import cpw.mods.fml.relauncher.IFMLCallHook;

public class SetupHook implements IFMLCallHook {

	@Override
	public Void call() throws Exception {
		return null;
	}

	@Override
	public void injectData(Map<String, Object> data) {
		LoadDataStore.instance.populateFromSetupClass(data);
	}

}
