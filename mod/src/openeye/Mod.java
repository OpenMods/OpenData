package openeye;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

import openeye.logic.Config;
import openeye.logic.InjectedDataStore;
import openeye.logic.MainWorker;
import openeye.reports.FileSignature;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import cpw.mods.fml.common.DummyModContainer;
import cpw.mods.fml.common.LoadController;
import cpw.mods.fml.common.ModMetadata;
import cpw.mods.fml.common.event.FMLConstructionEvent;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;

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
	}

	private void handleDangerousFiles() {
		Collection<FileSignature> dangerousMods = worker.listDangerousFiles();

		if (!dangerousMods.isEmpty()) {
			for (FileSignature signature : dangerousMods)
				Log.warn("Dangerous file detected: %s (%s)", signature.filename, signature.signature);
			controller.errorOccurred(this, Proxy.instance().signalDangerousFiles(dangerousMods));
		}
	}

	@Subscribe
	public void onInit(FMLPostInitializationEvent evt) {
		if (Config.crashOnStartup) controller.errorOccurred(this, new RuntimeException("derp"));
	}

	@Override
	public File getSource() {
		return InjectedDataStore.instance.getSelfLocation();
	}

}
