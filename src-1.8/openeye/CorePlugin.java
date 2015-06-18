package openeye;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin.Name;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin.TransformerExclusions;

@Name("OpenEyePlugin")
@TransformerExclusions("openeye")
public class CorePlugin extends CorePluginBase {

	@Override
	public String getAccessTransformerClass() {
		return null;
	}

}
