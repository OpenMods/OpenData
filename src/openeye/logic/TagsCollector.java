package openeye.logic;

import java.util.Map;
import java.util.Set;

import net.minecraft.launchwrapper.Launch;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

public class TagsCollector {

	private static final String ELEMENT_NAME = "openeye.tags";
	private static final Splitter TAG_SPLITTER = Splitter.on(',');

	public static Set<String> collectSystemTags() {
		Set<String> result = Sets.newHashSet();
		addBlackboardTags(result);
		addEnvTags(result);
		addArgsTags(result);
		return result;
	}

	private static void addArgsTags(Set<String> result) {
		@SuppressWarnings("unchecked")
		Map<String, String> args = (Map<String, String>)Launch.blackboard.get("launchArgs");
		String tags = args.get(ELEMENT_NAME);
		if (tags != null) Iterables.addAll(result, TAG_SPLITTER.split(tags));
	}

	private static void addEnvTags(Set<String> result) {
		Map<String, String> env = System.getenv();
		if (env.containsKey(ELEMENT_NAME)) {
			String tags = env.get(ELEMENT_NAME);
			Iterables.addAll(result, TAG_SPLITTER.split(tags));
		}
	}

	@SuppressWarnings("unchecked")
	private static void addBlackboardTags(Set<String> result) {
		Object tags = Launch.blackboard.get(ELEMENT_NAME);
		if (tags instanceof Set) result.addAll((Set<String>)tags);
	}

}
