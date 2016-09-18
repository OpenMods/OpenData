package openeye.logic;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.ModMetadata;
import cpw.mods.fml.common.discovery.ASMDataTable;
import cpw.mods.fml.common.discovery.ContainerType;
import cpw.mods.fml.common.discovery.ModCandidate;
import cpw.mods.fml.common.versioning.ArtifactVersion;
import cpw.mods.fml.common.versioning.VersionRange;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import cpw.mods.fml.relauncher.ReflectionHelper;
import java.io.File;
import java.lang.reflect.Field;
import java.net.URL;
import java.security.CodeSource;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;
import openeye.Log;
import openeye.protocol.Artifact;
import openeye.protocol.FileSignature;
import openeye.protocol.reports.ReportCrash.FileState;
import openeye.protocol.reports.ReportCrash.ModState;
import openeye.protocol.reports.ReportFileContents;
import openeye.protocol.reports.ReportFileInfo;
import openeye.protocol.reports.ReportFileInfo.SerializableMod;
import openeye.protocol.reports.ReportFileInfo.SerializableTweak;

public class ModMetaCollector {

	private static class TweakMeta {
		private final String pluginName;
		private final String className;

		private TweakMeta(String pluginName, String className) {
			this.pluginName = Strings.nullToEmpty(pluginName);
			this.className = Strings.nullToEmpty(className);
		}

		public SerializableTweak toSerializable() {
			SerializableTweak result = new SerializableTweak();
			result.plugin = pluginName;
			result.cls = className;
			return result;
		}
	}

	private static class ModMeta {
		private final String modId;
		private final String name;
		private final String version;
		private final ModMetadata metadata;
		private final ModContainer container;

		private ModMeta(ModContainer container) {
			this.container = container;
			this.modId = Strings.nullToEmpty(container.getModId());
			this.name = Strings.nullToEmpty(container.getName());
			this.version = Strings.nullToEmpty(container.getVersion());
			this.metadata = container.getMetadata();
		}

		private static <T> Collection<T> safeCopy(Collection<T> input) {
			if (input == null) return ImmutableList.of();
			return ImmutableList.copyOf(input);
		}

		private static Collection<Artifact> copyArtifacts(Collection<ArtifactVersion> input) {
			if (input == null) return ImmutableList.of();

			ImmutableList.Builder<Artifact> result = ImmutableList.builder();

			for (ArtifactVersion version : input) {
				final Artifact tmp = new Artifact();
				tmp.label = version.getLabel();
				tmp.version = version.getVersionString();
				result.add(tmp);
			}

			return result.build();
		}

		public SerializableMod toSerializable() {
			SerializableMod result = new SerializableMod();
			result.modId = modId;
			result.name = name;
			result.version = version;

			VersionRange mcVersionRange = container.acceptableMinecraftVersionRange();
			if (mcVersionRange != null) result.mcVersion = Strings.nullToEmpty(mcVersionRange.toString());

			result.description = Strings.nullToEmpty(metadata.description);
			result.url = Strings.nullToEmpty(metadata.url);
			result.updateUrl = Strings.nullToEmpty(metadata.updateUrl);
			result.credits = Strings.nullToEmpty(metadata.credits);
			result.parent = Strings.nullToEmpty(metadata.parent);
			result.authors = safeCopy(metadata.authorList);
			result.requiredMods = copyArtifacts(metadata.requiredMods);
			result.dependants = copyArtifacts(metadata.dependants);
			result.dependencies = copyArtifacts(metadata.dependencies);
			return result;
		}

		public ModState getState() {
			ModState state = new ModState();
			state.modId = modId;
			state.state = Loader.instance().getModState(container).toString();
			return state;
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

		private String signature;

		public String signature() {
			if (signature == null) {
				signature = calculateSignature(container);
			}
			return signature;
		}

		public Long getSize() {
			try {
				return container.length();
			} catch (Throwable t) {
				Log.info(t, "Can't get size info for file %s", container);
			}
			return null;
		}

		public FileState getStates() {
			FileState result = new FileState();
			result.signature = signature();

			List<ModState> mods = Lists.newArrayList();

			for (ModMeta meta : this.mods.values())
				mods.add(meta.getState());

			result.mods = mods;
			return result;
		}

		public ReportFileInfo generateReport() {
			ReportFileInfo report = new ReportFileInfo();
			report.signature = signature();
			report.size = getSize();

			{
				ImmutableList.Builder<SerializableMod> modsBuilder = ImmutableList.builder();
				for (ModMeta m : mods.values())
					modsBuilder.add(m.toSerializable());
				report.mods = modsBuilder.build();
			}

			{
				ImmutableList.Builder<SerializableTweak> tweaksBuilder = ImmutableList.builder();
				for (TweakMeta t : tweakers)
					tweaksBuilder.add(t.toSerializable());
				report.tweakers = tweaksBuilder.build();
			}

			report.classTransformers = ImmutableList.copyOf(classTransformers);
			report.packages = ImmutableList.copyOf(packages);

			return report;
		}

		public ReportFileContents generateFileContentsReport() {
			ReportFileContents report = new ReportFileContents();
			report.signature = signature();
			ReportBuilders.fillFileContents(container, report);
			return report;
		}

		public FileSignature createFileSignature() {
			FileSignature result = new FileSignature();
			result.signature = signature();
			result.filename = container.getName();
			return result;
		}
	}

	private final Map<File, FileMeta> files = Maps.newHashMap();

	private final Map<String, FileMeta> signatures = Maps.newHashMap();

	private final long operationDuration;

	private final ASMDataTable table;

	ModMetaCollector(ASMDataTable table, LaunchClassLoader loader, Collection<ITweaker> tweakers) {
		Log.info("Starting mod metadatadata collection");
		this.table = table;
		long start = System.nanoTime();
		Collection<ModCandidate> allCandidates = stealCandidates(table);
		collectFilesFromModCandidates(allCandidates);
		collectFilesFromClassTransformers(loader, table);
		collectFilesFromTweakers(tweakers, table);
		collectFilesFromModContainers(table);

		fillSignaturesMap();
		operationDuration = System.nanoTime() - start;
		Log.info("Collection of mod metadata finished. Duration: %.4f ms", operationDuration / 1000000.0d);
	}

	private static FileMeta fromModCandidate(ModCandidate candidate) {
		FileMeta fileMeta = new FileMeta(candidate.getModContainer());
		fileMeta.packages.addAll(candidate.getContainedPackages());
		for (ModContainer c : candidate.getContainedMods())
			fileMeta.mods.put(c.getModId(), new ModMeta(c));

		return fileMeta;
	}

	private static String calculateSignature(File file) {
		try {
			return "sha256:" + Files.hash(file, Hashing.sha256()).toString();
		} catch (Throwable t) {
			Log.warn(t, "Can't hash file %s", file);
			return null;
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

	private void collectFilesFromTweakers(Collection<ITweaker> tweakers, ASMDataTable table) {
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
					if (location == null || location.isDirectory()) continue;
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

	private void collectFilesFromClassTransformers(LaunchClassLoader loader, ASMDataTable table) {
		if (loader != null) {
			List<IClassTransformer> transformers = loader.getTransformers();
			for (IClassTransformer transformer : transformers)
				registerClassTransformer(table, transformer.getClass().getName());
		} else {
			Log.warn("LaunchClassLoader not available");
		}
	}

	private void fillSignaturesMap() {
		for (FileMeta meta : files.values())
			signatures.put(meta.signature(), meta);
	}

	public List<String> listSignatures() {
		List<String> result = Lists.newArrayList();
		for (FileMeta meta : files.values())
			result.add(meta.signature);

		return result;
	}

	public List<FileState> collectStates() {
		List<FileState> result = Lists.newArrayList();

		for (FileMeta meta : files.values())
			result.add(meta.getStates());

		return result;
	}

	public List<FileSignature> getAllFiles() {
		List<FileSignature> result = Lists.newArrayList();
		for (FileMeta meta : files.values()) {
			FileSignature tmp = new FileSignature();
			tmp.signature = meta.signature();
			tmp.filename = meta.container.getName();
			result.add(meta.createFileSignature());
		}

		return result;
	}

	public Set<String> getAllSignatures() {
		Set<String> result = Sets.newHashSet();

		for (FileMeta meta : files.values())
			result.add(meta.signature());

		return result;
	}

	public long getCollectingDuration() {
		return operationDuration;
	}

	public ReportFileInfo generateFileReport(String signature) {
		FileMeta meta = signatures.get(signature);
		return meta != null? meta.generateReport() : null;
	}

	public ReportFileContents generateFileContentsReport(String signature) {
		FileMeta meta = signatures.get(signature);
		return meta != null? meta.generateFileContentsReport() : null;
	}

	public Set<String> getModsForSignature(String signature) {
		FileMeta meta = signatures.get(signature);
		if (meta != null) return ImmutableSet.copyOf(meta.mods.keySet());
		else return ImmutableSet.of();
	}

	public FileSignature getFileForSignature(String signature) {
		FileMeta meta = signatures.get(signature);
		return meta != null? meta.createFileSignature() : null;
	}

	public File getContainerForSignature(String signature) {
		FileMeta meta = signatures.get(signature);
		return meta != null? meta.container : null;
	}

	public static class ClassSource {
		public String loadedFrom;
		public final Set<String> containingClasses = Sets.newHashSet();
	}

	public ClassSource identifyClassSource(String className) {
		String packageName = extractPackage(className);
		ClassSource result = new ClassSource();

		if (packageName.startsWith("net.minecraft") ||
				packageName.startsWith("net.minecraftforge") ||
				packageName.startsWith("cpw.mods.fml"))
			return null;

		try {
			Class<?> cls = Class.forName(className);
			CodeSource src = cls.getProtectionDomain().getCodeSource();
			if (src != null) {
				URL sourceUrl = src.getLocation();
				File sourceFile = new File(sourceUrl.toURI());
				FileMeta meta = files.get(sourceFile);
				if (meta != null) result.loadedFrom = meta.signature();
			}
		} catch (Throwable t) {
			// NO-OP - nothing to save
		}

		Set<ModCandidate> candidates = table.getCandidatesFor(packageName);
		for (ModCandidate c : candidates) {
			File container = c.getModContainer();
			if (container != null) {
				FileMeta meta = files.get(container);
				if (meta != null) result.containingClasses.add(meta.signature());
			}
		}

		return result;
	}
}
