package openeye.responses;

import openeye.logic.IContext;
import openeye.notes.NoteCollector;
import openeye.protocol.responses.ResponseKnownCrash;

public class ResponseKnownCrashAction extends ResponseKnownCrash implements IExecutableResponse {

	@Override
	public void execute(IContext context) {
		NoteCollector.INSTANCE.addNote(this);
	}
}
