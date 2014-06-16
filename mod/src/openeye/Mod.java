package openeye;

import java.io.File;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.RunnableFuture;

import openeye.logic.*;
import openeye.notes.CommandNotes;
import openeye.reports.FileSignature;
import openeye.utils.NameCollector;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import cpw.mods.fml.client.FMLFileResourcePack;
import cpw.mods.fml.client.FMLFolderResourcePack;
import cpw.mods.fml.common.DummyModContainer;
import cpw.mods.fml.common.LoadController;
import cpw.mods.fml.common.ModMetadata;
import cpw.mods.fml.common.event.*;

public class Mod extends DummyModContainer {

	private LoadController controller;

	public Mod() {
		super(new ModMetadata());
		ModMetadata meta = getMetadata();
		meta.modId = "OpenEye";
		meta.name = "OpenEye";
		meta.version = "@VERSION@";
		meta.authorList = Arrays.asList("boq", "Mikee");
		meta.url = "http://openmods.info/";
		meta.description = "We see you...";
	}

	private SenderWorker worker;

	@Override
	public boolean registerBus(EventBus bus, LoadController controller) {
		this.controller = controller;
		bus.register(this);
		return true;
	}

	@Subscribe
	public void onModConstruct(FMLConstructionEvent evt) {
		Proxy.instance().first();

		ModCollectorFactory factory = new ModCollectorFactory();
		RunnableFuture<ModMetaCollector> collector = factory.createCollector(
				evt.getASMHarvestedData(),
				Bootstrap.instance.getLoader(),
				Bootstrap.instance.getTweakers());

		startMetadataCollection(collector);

		ThrowableLogger.enableResolving(collector);

		if (Proxy.instance().isSnooperEnabled()) {
			worker = new SenderWorker(collector, StateHolder.state());
			worker.start();
		} else Log.info("Snooper disabled, OpenEye will not send or receive any data from server");
	}

	private static void startMetadataCollection(RunnableFuture<ModMetaCollector> collector) {
		Thread modCollector = new Thread(collector);
		modCollector.setName("OpenEye mod meta collector");
		modCollector.start();
	}

	@Subscribe
	public void onInit(FMLInitializationEvent evt) {
		// give thread enough time to receive IMC
		if (worker != null) {
			worker.waitForFirstMsg();
			handleUnwantedFiles();
		}

		NameCollector.register();
		Proxy.instance().init();
	}

	private void handleUnwantedFiles() {
		Collection<FileSignature> dangerousMods = worker.listDangerousFiles();

		if (!dangerousMods.isEmpty()) {
			for (FileSignature signature : dangerousMods)
				Log.warn("File suggested for deletion: %s (%s)", signature.filename, signature.signature);
		}
	}

	public static void crash1() {
		try {
			File mcDir = Bootstrap.instance.getMcLocation();
			throw new ProtocolException("128.0.0.1 deep one: " + new File(mcDir, "hello.txt"));
		} catch (Exception e) {
			throw new RuntimeException("u wot m8: 127.0.0.4:5262", e);
		}
	}

	public static void crash2() {
		try {
			crash1();
		} catch (RuntimeException e) {
			throw new RuntimeException(e);
		}
	}

	@Subscribe
	public void onInit(FMLPostInitializationEvent evt) {
		if (Config.crashOnStartup) try {
			crash2();
		} catch (RuntimeException e) {
			controller.errorOccurred(this, new RuntimeException("Goodbye, cruel world!", e));
		}
	}

	@Subscribe
	public void onServerStart(FMLServerStartingEvent evt) {
		evt.registerServerCommand(new CommandNotes(evt.getServer().getFile(".")));
	}

	@Override
	public File getSource() {
		File injectedSource = Bootstrap.instance.getSelfLocation();

		if (injectedSource != null) return injectedSource;

		// looks like we are in dev (or broken) env
		URL url = getClass().getResource(".");

		try {
			File rootFile = new File(url.toURI());
			if (rootFile.getName().equals("openeye")) rootFile = rootFile.getParentFile();
			return rootFile;
		} catch (Exception e) {
			Log.info(e, "Failed to extract source from URL %s", url);
		}

		return null;
	}

	@Override
	public Class<?> getCustomResourcePackClass() {
		File source = getSource();
		if (source == null) {
			Log.warn("Failed to get source, resource pack missing");
			return null;
		}
		return source.isDirectory()? FMLFolderResourcePack.class : FMLFileResourcePack.class;
	}
}
