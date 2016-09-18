package openeye.responses;

import java.io.File;
import openeye.logic.IContext;
import openeye.notes.NoteCollector;
import openeye.protocol.responses.ResponseRemoveFile;

public class ResponseRemoveFileAction extends ResponseRemoveFile implements IExecutableResponse {

	@Override
	public void execute(IContext context) {
		context.markUnwantedSignature(signature);

		File file = context.getFileForSignature(signature);
		NoteCollector.INSTANCE.addNote(file, this);
	}

}
