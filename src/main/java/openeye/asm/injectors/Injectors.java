package openeye.asm.injectors;

import com.google.common.collect.Multimap;
import java.io.File;
import net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import openeye.asm.MethodCodeInjector;
import openeye.asm.MethodMatcher;
import openeye.asm.VisitorHelper;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

public class Injectors {

	private static final String CRASH_REPORT_CLS = "net.minecraft.crash.CrashReport";
	private static final String TILE_ENTITY_CLS = "net.minecraft.tileentity.TileEntity";
	private static final String ENTITY_LIST_CLS = "net.minecraft.entity.EntityList";
	private static final String ENTITY_CLS = "net.minecraft.entity.Entity";
	private static final String NBT_TAG_COMPOUND_CLS = "net.minecraft.nbt.NBTTagCompound";
	private static final String WORLD_CLS = "net.minecraft.world.World";
	private static final String CHUNK_CLS = "net.minecraft.world.chunk.Chunk";
	private static final String ANVIL_CHUNK_LOADER = "net.minecraft.world.chunk.storage.AnvilChunkLoader";

	public static String getClassName(String name) {
		name = name.replace('.', '/');
		return VisitorHelper.useSrgNames()? FMLDeobfuscatingRemapper.INSTANCE.unmap(name) : name;
	}

	public static void setupInjectors(Multimap<String, MethodCodeInjector> injectors) {
		String nbtTagCompoundName = getClassName(NBT_TAG_COMPOUND_CLS);
		Type nbtTagCompoundType = Type.getObjectType(nbtTagCompoundName);

		String worldName = getClassName(WORLD_CLS);
		Type worldType = Type.getObjectType(worldName);

		String entityName = getClassName(ENTITY_CLS);
		Type entityType = Type.getObjectType(entityName);

		String tileEntityName = getClassName(TILE_ENTITY_CLS);
		Type tileEntityType = Type.getObjectType(tileEntityName);

		String chunkName = getClassName(CHUNK_CLS);
		Type chunkType = Type.getObjectType(chunkName);

		{
			String crashHandlerName = getClassName(CRASH_REPORT_CLS);
			Type fileType = Type.getType(File.class);

			Type methodType = Type.getMethodType(Type.BOOLEAN_TYPE, fileType);

			MethodMatcher matcher = new MethodMatcher(crashHandlerName, methodType.getDescriptor(), "saveToFile", "func_147149_a");

			injectors.put(CRASH_REPORT_CLS, new MethodCodeInjector("crash_handler", matcher) {
				@Override
				public MethodVisitor createVisitor(MethodVisitor parent) {
					return new CrashHandlerInjector(parent);
				}
			});
		}

		{
			Type methodType = Type.getMethodType(tileEntityType, worldType, nbtTagCompoundType);

			MethodMatcher matcher = new MethodMatcher(tileEntityName, methodType.getDescriptor(), "create", "func_190200_a");

			injectors.put(TILE_ENTITY_CLS, new MethodCodeInjector("tile_entity_load", matcher) {
				@Override
				public MethodVisitor createVisitor(MethodVisitor parent) {
					return new ExceptionHandlerInjector(parent, "java/lang/Throwable", "tile_entity_construct", "tile_entity_read");
				}
			});
		}

		{
			String entityListName = getClassName(ENTITY_LIST_CLS);

			{
				Type methodType = Type.getMethodType(entityType, nbtTagCompoundType, worldType);
				MethodMatcher matcher = new MethodMatcher(entityListName, methodType.getDescriptor(), "createEntityFromNBT", "func_75615_a");
				injectors.put(ENTITY_LIST_CLS, new MethodCodeInjector("entity_read", matcher) {
					@Override
					public MethodVisitor createVisitor(MethodVisitor parent) {
						return new ExceptionHandlerInjector(parent, "java/lang/Exception", "entity_read");
					}
				});
			}

			{
				Type methodType = Type.getMethodType(entityType, Type.getType(Class.class), worldType);
				MethodMatcher matcher = new MethodMatcher(entityListName, methodType.getDescriptor(), "newEntity", "func_191304_a");
				injectors.put(ENTITY_LIST_CLS, new MethodCodeInjector("entity_create", matcher) {
					@Override
					public MethodVisitor createVisitor(MethodVisitor parent) {
						return new ExceptionHandlerInjector(parent, "java/lang/Exception", "entity_create", "entity_create"); // seems to be split in two
					}
				});
			}
		}

		{
			String chunkLoaderName = getClassName(ANVIL_CHUNK_LOADER);

			Type methodType = Type.getMethodType(Type.VOID_TYPE, chunkType, worldType, nbtTagCompoundType);

			MethodMatcher matcher = new MethodMatcher(chunkLoaderName, methodType.getDescriptor(), "writeChunkToNBT", "func_75820_a");

			injectors.put(ANVIL_CHUNK_LOADER, new MethodCodeInjector("chunk_write", matcher) {
				@Override
				public MethodVisitor createVisitor(MethodVisitor parent) {
					return new ExceptionHandlerInjector(parent, "java/lang/Exception", "entity_write", "tile_entity_write");
				}
			});
		}
	}
}
