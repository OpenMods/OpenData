package openeye.responses;

import java.io.File;

import openeye.logic.IContext;
import openeye.notes.NoteCollector;

import com.google.gson.annotations.SerializedName;

public class ResponseRemoveFile implements IResponse {
	public static final String TYPE = "remove_file_suggestion";

	@Override
	public String getType() {
		return TYPE;
	}

	@SerializedName("signature")
	public String signature;

	@SerializedName("url")
	public String url;

	@Override
	public void execute(IContext context) {
		context.markUnwantedSignature(signature);

		File file = context.getFileForSignature(signature);
		NoteCollector.INSTANCE.addNote(file, this);
	}

}
