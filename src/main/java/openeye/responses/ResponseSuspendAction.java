package openeye.responses;

import openeye.logic.IContext;
import openeye.logic.StateHolder;
import openeye.notes.NoteCollector;
import openeye.protocol.responses.ResponseSuspend;

public class ResponseSuspendAction extends ResponseSuspend implements IExecutableResponse {

	@Override
	public void execute(IContext context) {
		long now = System.currentTimeMillis();
		// duration in seconds
		final long suspendUntilTimestamp = now + duration * 1000;
		StateHolder.state().suspendUntilTimestamp = suspendUntilTimestamp;
		NoteCollector.INSTANCE.addSuspendNote(suspendUntilTimestamp, reason);
	}

}
