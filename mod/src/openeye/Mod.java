package openeye;

import java.io.File;
import java.util.Arrays;

import openeye.logic.InjectedDataStore;
import openeye.logic.ReportSender;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import cpw.mods.fml.common.DummyModContainer;
import cpw.mods.fml.common.LoadController;
import cpw.mods.fml.common.ModMetadata;
import cpw.mods.fml.common.event.FMLConstructionEvent;

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

	@Override
	public boolean registerBus(EventBus bus, LoadController controller) {
		bus.register(this);
		return true;
	}

	@Subscribe
	public void onModConstruct(final FMLConstructionEvent evt) {
		new ReportSender().start(InjectedDataStore.instance, evt.getASMHarvestedData());

	}

	@Override
	public File getSource() {
		return InjectedDataStore.instance.getSelfLocation();
	}

}
