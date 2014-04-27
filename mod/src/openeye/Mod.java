package openeye;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;

import openeye.logic.Config;
import openeye.logic.InjectedDataStore;
import openeye.logic.MainWorker;
import openeye.notes.CommandNotes;
import openeye.reports.FileSignature;

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

	private MainWorker worker;

	@Override
	public boolean registerBus(EventBus bus, LoadController controller) {
		this.controller = controller;
		bus.register(this);
		return true;
	}

	@Subscribe
	public void onModConstruct(FMLConstructionEvent evt) {
		worker = new MainWorker(Proxy.instance().isSnooperEnabled());
		worker.start(InjectedDataStore.instance, evt.getASMHarvestedData());
	}

	@Subscribe
	public void onInit(FMLInitializationEvent evt) {
		// give thread enough time to receive IMC
		if (worker != null) {
			worker.waitForFirstMsg();
			handleDangerousFiles();
		}

		Proxy.instance().init();
	}

	private void handleDangerousFiles() {
		Collection<FileSignature> dangerousMods = worker.listDangerousFiles();

		if (!dangerousMods.isEmpty()) {
			for (FileSignature signature : dangerousMods)
				Log.warn("Dangerous file detected: %s (%s)", signature.filename, signature.signature);

			if (Config.haltOnDangerousFiles) controller.errorOccurred(this, Proxy.instance().signalDangerousFiles(dangerousMods));
		}
	}

	public static void crash1() {
		try {
			throw new RuntimeException("deep one");
		} catch (RuntimeException e) {
			throw new RuntimeException("u wot m8", e);
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
		File injectedSource = InjectedDataStore.instance.getSelfLocation();
		URL cpSource = getClass().getProtectionDomain().getCodeSource().getLocation();
		try {
			return new File(cpSource.toURI());
		} catch (Exception e) {
			Log.warn(e, "Failed to extract source from URL %s, using injected path %s", cpSource, injectedSource);
		}
		return injectedSource;
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
