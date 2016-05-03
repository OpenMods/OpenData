package openeye.logic;

import openeye.Log;
import openeye.storage.IDataSource;

public class StateHolder {
	private static ModState state = new ModState();

	private static void storeState(ModState state, Storages storages) {
		IDataSource<ModState> stateStorage = storages.state.getById(Storages.STATE_FILE_ID);
		stateStorage.store(state);
	}

	public static void init(final Storages storages) {
		try {
			IDataSource<ModState> stateStorage = storages.state.getById(Storages.STATE_FILE_ID);
			ModState storedState = stateStorage.retrieve();
			if (storedState != null) state = storedState;
		} catch (Throwable t) {
			Log.warn(t, "Failed to get mod state, reinitializing");
		}

		Thread stateDump = new Thread() {
			@Override
			public void run() {
				try {
					storeState(state, storages);
				} catch (Throwable t) {
					System.err.println("[OpenEye] Failed to store state");
					t.printStackTrace();
				}
			}
		};

		Runtime.getRuntime().addShutdownHook(stateDump);
	}

	public static ModState state() {
		return state;
	}
}
