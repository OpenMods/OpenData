package openeye;

import java.io.File;
import java.util.Arrays;

import openeye.logic.InjectedDataStore;
import openeye.logic.MainWorker;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import cpw.mods.fml.common.DummyModContainer;
import cpw.mods.fml.common.LoadController;
import cpw.mods.fml.common.ModMetadata;
import cpw.mods.fml.common.event.FMLConstructionEvent;
import cpw.mods.fml.common.event.FMLInitializationEvent;

public class Mod extends DummyModContainer {

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
		bus.register(this);
		return true;
	}

	@Subscribe
	public void onModConstruct(FMLConstructionEvent evt) {
		worker = new MainWorker();
		worker.start(InjectedDataStore.instance, evt.getASMHarvestedData());

	}

	@Subscribe
	public void onInit(FMLInitializationEvent evt) {
		// give thread enough time to receive IMC
		if (worker != null) worker.waitForFirstMsg();
	}

	@Override
	public File getSource() {
		return InjectedDataStore.instance.getSelfLocation();
	}

}
