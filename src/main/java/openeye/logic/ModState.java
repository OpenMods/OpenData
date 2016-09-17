package openeye.logic;

import com.google.common.collect.Sets;
import java.util.Set;

public class ModState {
	public Set<String> installedMods = Sets.newHashSet();

	public boolean mainMenuInfoDisplayed;

	public boolean infoNotesDisplayed;

	public long suspendUntilTimestamp;
}
