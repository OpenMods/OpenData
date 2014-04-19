package openeye.reports;

import com.google.gson.annotations.SerializedName;

public class FileSignature {
	@SerializedName("signature")
	public String signature;

	@SerializedName("filename")
	public String filename;
}