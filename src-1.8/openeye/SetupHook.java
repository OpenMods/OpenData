package openeye;

import java.util.Map;

import openeye.logic.Bootstrap;
import net.minecraftforge.fml.relauncher.IFMLCallHook;

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
