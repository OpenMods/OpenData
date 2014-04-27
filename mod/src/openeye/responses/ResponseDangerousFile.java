package openeye.responses;

import java.io.File;

import openeye.logic.IContext;
import openeye.notes.NoteCollector;

public class ResponseDangerousFile implements IResponse {
	public static final String TYPE = "dangerous_file";

	@Override
	public String getType() {
		return TYPE;
	}

	public String signature;

	@Override
	public void execute(IContext context) {
		context.markDangerousSignature(signature);

		File file = context.getFileForSignature(signature);
		NoteCollector.INSTANCE.addDangerousFile(file, this);
	}

}
