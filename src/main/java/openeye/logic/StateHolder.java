package openeye.logic;

import com.google.common.base.Preconditions;
import openeye.Log;
import openeye.storage.IDataSource;

public class StateHolder {
	private static ModState state = new ModState();

	private static Runnable saveCallback;

	private static void storeState(ModState state, Storages storages) {
		IDataSource<ModState> stateStorage = storages.state.getById(Storages.STATE_FILE_ID);
		stateStorage.store(state);
	}

	public static void init(final Storages storages) {
		Preconditions.checkState(saveCallback == null, "Double initialization of state storage");
		try {
			IDataSource<ModState> stateStorage = storages.state.getById(Storages.STATE_FILE_ID);
			ModState storedState = stateStorage.retrieve();
			if (storedState != null) state = storedState;
		} catch (Throwable t) {
			Log.warn(t, "Failed to get mod state, reinitializing");
		}

		saveCallback = () -> {
			try {
				storeState(state, storages);
			} catch (Throwable t) {
				System.err.println("[OpenEye] Failed to store state");
				t.printStackTrace();
			}
		};

		Runtime.getRuntime().addShutdownHook(new Thread(saveCallback));
	}

	public static void save() {
		Preconditions.checkState(saveCallback != null, "State holder not initialized");
		saveCallback.run();
	}

	public static ModState state() {
		return state;
	}
}
