package openeye.logic;

import java.util.Collection;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraftforge.fml.common.discovery.ASMDataTable;

public class ModCollectorFactory {

	public RunnableFuture<ModMetaCollector> createCollector(final ASMDataTable table, final LaunchClassLoader loader, final Collection<ITweaker> tweakers) {
		return new FutureTask<>(() -> {
			return new ModMetaCollector(table, loader, tweakers);
		});
	}
}
