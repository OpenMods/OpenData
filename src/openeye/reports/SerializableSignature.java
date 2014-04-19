package openeye.reports;

import com.google.gson.annotations.SerializedName;

public class SerializableSignature {
	@SerializedName("signature")
	public String signature;

	@SerializedName("filename")
	public String filename;
}