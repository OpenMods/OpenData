package openeye.logic;

import static argo.jdom.JsonNodeFactories.*;

import java.io.File;
import java.lang.reflect.Field;
import java.util.*;
import java.util.logging.Level;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;
import openeye.Log;
import argo.jdom.JsonNode;
import argo.jdom.JsonRootNode;

import com.google.common.base.Strings;
import com.google.common.collect.*;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.ModMetadata;
import cpw.mods.fml.common.discovery.ASMDataTable;
import cpw.mods.fml.common.discovery.ContainerType;
import cpw.mods.fml.common.discovery.ModCandidate;
import cpw.mods.fml.common.versioning.ArtifactVersion;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import cpw.mods.fml.relauncher.ReflectionHelper;

public class ModMetaCollector {

	private static final String TYPE_MOD_INFO = "mod_info";

	private static class TweakMeta {
		private final String pluginName;
		private final String className;

		private TweakMeta(String pluginName, String className) {
			this.pluginName = Strings.nullToEmpty(pluginName);
			this.className = Strings.nullToEmpty(className);
		}

		public JsonNode serialize() {
			return object(
					field("plugin", string(pluginName)),
					field("class", string(className)));
		}
	}

	private static class ModMeta {
		private final String modId;
		private final String name;
		private final String version;
		private final ModMetadata metadata;

		private ModMeta(ModContainer container) {
			this.modId = Strings.nullToEmpty(container.getModId());
			this.name = Strings.nullToEmpty(container.getName());
			this.version = Strings.nullToEmpty(container.getVersion());
			this.metadata = container.getMetadata();
		}

		private static JsonNode processStringList(Collection<String> strings) {
			List<JsonNode> result = Lists.newArrayList();
			for (String s : strings)
				result.add(string(Strings.nullToEmpty(s)));
			return array(result);
		}

		private static JsonNode processVersionList(Collection<ArtifactVersion> versions) {
			if (versions == null) return array();
			List<JsonNode> result = Lists.newArrayList();
			for (ArtifactVersion version : versions) {
				result.add(object(
						field("label", string(version.getLabel())),
						field("version", string(version.getRangeString()))
						));
			}
			return array(result);
		}

		public JsonNode serialize() {
			return object(field("modId", string(modId)),
					field("name", string(name)),
					field("version", string(version)),
					field("description", string(Strings.nullToEmpty(metadata.description))),
					field("url", string(Strings.nullToEmpty(metadata.url))),
					field("updateUrl", string(Strings.nullToEmpty(metadata.updateUrl))),
					field("credits", string(Strings.nullToEmpty(metadata.credits))),
					field("parent", string(Strings.nullToEmpty(metadata.parent))),
					field("authors", processStringList(metadata.authorList)),
					field("requiredMods", processVersionList(metadata.requiredMods)),
					field("dependants", processVersionList(metadata.dependants)),
					field("parent", string(Strings.nullToEmpty(metadata.parent))));
		}
	}

	private static class FileMeta {
		public final Set<String> classTransformers = Sets.newHashSet();
		public final Map<String, ModMeta> mods = Maps.newHashMap();
		public final List<TweakMeta> tweakers = Lists.newArrayList();
		public final Set<String> packages = Sets.newHashSet();
		public final File container;

		public FileMeta(File container) {
			this.container = container;
		}

		private String fingerprint;

		public String fingerprint() {
			if (fingerprint == null) {
				fingerprint = createFingerprint(container);
			}
			return fingerprint;
		}

		public JsonNode getSignatureNode() {
			String signature = fingerprint();
			return string(Strings.isNullOrEmpty(signature)? "error" : signature);
		}

		public JsonRootNode serialize(String type) {
			JsonNode pathNode = string(container.getName());

			JsonNode sizeNode;
			try {
				long size = container.length();
				sizeNode = number(size);
			} catch (Throwable t) {
				Log.info(t, "Can't get size info for file %s", container);
				sizeNode = string("error");
			}

			JsonNode signatureNode = getSignatureNode();

			List<JsonNode> modNodes = Lists.newArrayList();
			for (ModMeta meta : mods.values())
				modNodes.add(meta.serialize());

			List<JsonNode> classTransformerNodes = Lists.newArrayList();
			for (String cls : classTransformers)
				classTransformerNodes.add(string(cls));

			List<JsonNode> tweakerNodes = Lists.newArrayList();
			for (TweakMeta tweaker : tweakers)
				tweakerNodes.add(tweaker.serialize());

			List<JsonNode> packageNodes = Lists.newArrayList();
			for (String pkg : packages)
				packageNodes.add(string(pkg));

			return object(
					field("type", string(type)),
					field("filename", pathNode),
					field("size", sizeNode),
					field("signature", signatureNode),
					field("mods", array(modNodes)),
					field("classTransformers", array(classTransformerNodes)),
					field("tweakers", array(tweakerNodes)),
					field("packages", array(packageNodes)));
		}
	}

	private final Map<File, FileMeta> files = Maps.newHashMap();

	private final Map<String, FileMeta> signatures = Maps.newHashMap();

	private final long operationDuration;

	ModMetaCollector(InjectedDataStore store, ASMDataTable table) {
		long start = System.nanoTime();
		Collection<ModCandidate> allCandidates = stealCandidates(table);
		collectFilesFromModCandidates(allCandidates);
		collectFilesFromClassTransformers(store, table);
		collectFilesFromTweakers(store, table);
		collectFilesFromModContainers(table);

		fillSignaturesMap();
		operationDuration = System.nanoTime() - start;
		Log.info("Collecting data took %.4f ms", operationDuration / 1000000.0d);
	}

	private static FileMeta fromModCandidate(ModCandidate candidate) {
		FileMeta fileMeta = new FileMeta(candidate.getModContainer());
		fileMeta.packages.addAll(candidate.getContainedPackages());
		for (ModContainer c : candidate.getContainedMods())
			fileMeta.mods.put(c.getModId(), new ModMeta(c));

		return fileMeta;
	}

	private static String createFingerprint(File file) {
		try {
			return Files.hash(file, Hashing.sha256()).toString();
		} catch (Throwable t) {
			Log.log(Level.INFO, t, "Can't hash file %s", file);
			return "error";
		}
	}

	private FileMeta getOrCreateData(File file) {
		FileMeta data = files.get(file);
		if (data == null) {
			data = new FileMeta(file);
			files.put(file, data);
		}

		return data;
	}

	private void collectFilesFromModCandidates(Collection<ModCandidate> candidates) {
		for (ModCandidate c : candidates) {
			File modContainer = c.getModContainer();
			if (!files.containsKey(modContainer) &&
					!c.isMinecraftJar() &&
					c.getSourceType() == ContainerType.JAR) {
				FileMeta meta = fromModCandidate(c);
				files.put(modContainer, meta);
			}
		}
	}

	private static String extractPackage(String className) {
		int pkgIdx = className.lastIndexOf('.');
		if (pkgIdx == -1) return null;
		return className.substring(0, pkgIdx);
	}

	private static Set<ModCandidate> getCandidatesForClass(ASMDataTable table, String cls) {
		String packageName = extractPackage(cls);
		if (Strings.isNullOrEmpty(packageName)) return null;
		return table.getCandidatesFor(packageName);
	}

	private interface IFileMetaVisitor {
		public void visit(FileMeta fileMeta);
	}

	private void visitMeta(ASMDataTable table, String cls, IFileMetaVisitor visitor) {
		Set<ModCandidate> candidates = getCandidatesForClass(table, cls);
		if (candidates != null) {
			for (ModCandidate c : candidates) {
				File container = c.getModContainer();
				if (container.isDirectory()) continue;
				FileMeta fileMeta = files.get(container);
				if (fileMeta == null) {
					fileMeta = fromModCandidate(c);
					files.put(container, fileMeta);
				}
				visitor.visit(fileMeta);
			}
		}
	}

	private void registerClassTransformer(ASMDataTable table, final String cls) {
		visitMeta(table, cls, new IFileMetaVisitor() {
			@Override
			public void visit(FileMeta fileMeta) {
				fileMeta.classTransformers.add(cls);
			}
		});
	}

	private static Collection<ModCandidate> stealCandidates(ASMDataTable table) {
		// I'm very sorry for that
		try {
			Multimap<String, ModCandidate> packageMap = ReflectionHelper.getPrivateValue(ASMDataTable.class, table, "packageMap");
			if (packageMap != null) return packageMap.values();
		} catch (Throwable t) {
			Log.warn(t, "Can't get ModCandidate map, data will be missing from report");

		}
		return ImmutableList.of();
	}

	private void collectFilesFromModContainers(ASMDataTable table) {
		File dummyEntry = new File("minecraft.jar");
		for (ModContainer c : Loader.instance().getModList()) {
			File f = c.getSource();
			if (f != null && !f.equals(dummyEntry) && !f.isDirectory()) {
				FileMeta meta = getOrCreateData(f);
				meta.mods.put(c.getModId(), new ModMeta(c));
			}
		}
	}

	private void collectFilesFromTweakers(InjectedDataStore store, ASMDataTable table) {
		List<ITweaker> tweakers = store.getTweakers();

		try {
			Class<?> coreModWrapper = Class.forName("cpw.mods.fml.relauncher.CoreModManager$FMLPluginWrapper");
			Field nameField = coreModWrapper.getField("name");
			nameField.setAccessible(true);
			Field pluginField = coreModWrapper.getField("coreModInstance");
			pluginField.setAccessible(true);
			Field locationField = coreModWrapper.getField("location");
			locationField.setAccessible(true);

			for (ITweaker tweaker : tweakers) {
				try {
					File location = (File)locationField.get(tweaker);
					if (location.isDirectory()) continue;
					String name = (String)nameField.get(tweaker);
					IFMLLoadingPlugin plugin = (IFMLLoadingPlugin)pluginField.get(tweaker);

					FileMeta meta = getOrCreateData(location);
					meta.tweakers.add(new TweakMeta(name, plugin.getClass().getName()));
				} catch (Throwable t) {
					Log.warn(t, "Can't get data for tweaker %s", tweaker);
				}
			}
		} catch (Throwable t) {
			Log.warn(t, "Can't get tweaker data");
		}
	}

	private void collectFilesFromClassTransformers(InjectedDataStore store, ASMDataTable table) {
		LaunchClassLoader loaders = store.getLoader();
		if (loaders != null) {
			List<IClassTransformer> transformers = loaders.getTransformers();
			for (IClassTransformer transformer : transformers)
				registerClassTransformer(table, transformer.getClass().getName());
		} else {
			Log.warn("LaunchClassLoader not available");
		}
	}

	private void fillSignaturesMap() {
		for (FileMeta meta : files.values())
			signatures.put(meta.fingerprint(), meta);
	}

	public JsonRootNode getSignatures() {
		List<JsonNode> result = Lists.newArrayList();
		for (FileMeta meta : files.values())
			result.add(meta.getSignatureNode());

		return array(result);
	}

	public JsonRootNode getModInfo(String signature) {
		FileMeta meta = signatures.get(signature);
		return meta != null? meta.serialize(TYPE_MOD_INFO) : null;
	}

	// debug function for now
	public JsonRootNode dumpAllFiles() {
		List<JsonNode> result = Lists.newArrayList();
		for (FileMeta meta : files.values())
			result.add(meta.serialize(TYPE_MOD_INFO));

		return array(result);
	}

	public long getCollectingDuration() {
		return operationDuration;
	}
}
