package openeye;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin.Name;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin.TransformerExclusions;

@Name("OpenEyePlugin")
@TransformerExclusions("openeye")
public class CorePlugin extends CorePluginBase {

	@Override
	public String getAccessTransformerClass() {
		return null;
	}

}