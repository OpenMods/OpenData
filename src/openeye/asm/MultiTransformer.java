package openeye.asm;

import java.util.Collection;
import java.util.Map;

import net.minecraft.launchwrapper.IClassTransformer;
import openeye.asm.VisitorHelper.TransformProvider;
import openeye.asm.injectors.Injectors;

import org.objectweb.asm.ClassVisitor;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class MultiTransformer implements IClassTransformer {

	private Multimap<String, MethodCodeInjector> injectors = HashMultimap.create();

	public MultiTransformer() {
		Injectors.setupInjectors(injectors);
	}

	@Override
	public byte[] transform(final String name, String transformedName, byte[] bytes) {
		if (bytes == null) return bytes;

		for (Map.Entry<String, Collection<MethodCodeInjector>> clsInjectors : injectors.asMap().entrySet()) {
			if (transformedName.equals(clsInjectors.getKey())) {
				final Collection<MethodCodeInjector> methodInjector = clsInjectors.getValue();
				bytes = VisitorHelper.apply(bytes, 0, new TransformProvider() {
					@Override
					public ClassVisitor createVisitor(ClassVisitor cv) {
						return new SingleClassTransformer(cv, name, methodInjector);
					}
				});
			}
		}

		return bytes;
	}
}
