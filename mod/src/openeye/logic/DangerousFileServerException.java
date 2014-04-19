package openeye.logic;

import java.util.Collection;
import java.util.List;

import openeye.reports.FileSignature;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

public class DangerousFileServerException extends RuntimeException implements INotStoredCrash {

	private static final long serialVersionUID = 3261748350312647720L;

	private static String prepareMessage(Collection<FileSignature> dangerousFiles) {
		StringBuilder builder = new StringBuilder("OpenEye has detected files marked as dangerous by authors: ");

		List<String> mods = Lists.newArrayList();
		for (FileSignature signature : dangerousFiles)
			mods.add(String.format("%s(%s)", signature.filename, signature.signature));

		builder.append(Joiner.on(",").join(mods));

		builder.append(". Replace those files with safe ones or contact OpenMods team, if you think it's false-positive");

		return builder.toString();
	}

	public DangerousFileServerException(Collection<FileSignature> dangerousFiles) {
		super(prepareMessage(dangerousFiles));
	}
}
