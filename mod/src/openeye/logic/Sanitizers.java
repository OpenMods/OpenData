package openeye.logic;

import java.io.File;
import java.net.*;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.world.World;
import net.minecraft.world.storage.ISaveHandler;
import openeye.Log;
import openeye.logic.Sanitizer.ITransformer;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class Sanitizers {
	public static final int PRIORITY_SAVE_DIR = 1300;
	public static final int PRIORITY_MINECRAFT_DIR = 1200;
	public static final int PRIORITY_WORK_DIR = 1100;
	public static final int PRIORITY_HOME = 1000;

	public static final int PRIORITY_WORLD_NAME = 900;
	public static final int PRIORITY_SAVE_DIR_NAME = 800;

	public static final int PRIORITY_LOCAL_IP = 700;
	public static final int PRIORITY_IP_PORT = 600;

	public static final int PRIORITY_LOCAL_HOST = 500;
	public static final int PRIORITY_LOCAL_PLAYER = 400;
	public static final int PRIORITY_PLAYER_NAME = 300;
	public static final int PRIORITY_PLAYER_ID = 200;
	public static final int PRIORITY_SYSTEM_USER = 100;

	static class SimpleReplace implements ITransformer {
		private final String target;
		private final String value;

		public SimpleReplace(String target, String value) {
			this.target = target;
			this.value = value;
		}

		@Override
		public String transform(String input) {
			return input.replace(target, value);
		}

		@Override
		public String toString() {
			return String.format("'%s'->'%s'", target, value);
		}
	}

	private static class PropertyReplace extends Sanitizers.SimpleReplace {
		public PropertyReplace(String property, String value) {
			super(System.getProperty(property), value);
		}
	}

	private static class PathReplace implements ITransformer {
		private final String targetNormal;
		private final String targetReversed;
		private final String targetDoubled;
		private final String value;

		public PathReplace(String target, String value) {
			this.targetNormal = target;
			if (File.separatorChar == '\\') {
				targetReversed = targetNormal.replace('\\', '/');
				targetDoubled = targetNormal.replace("\\", "\\\\");
			}
			else if (File.separatorChar == '/') {
				targetReversed = targetNormal.replace('/', '\\');
				targetDoubled = targetNormal.replace("/", "\\\\");
			} else {
				targetReversed = null;
				targetDoubled = null;
			}

			this.value = value;
		}

		@Override
		public String transform(String input) {
			input = input.replace(targetNormal, value);
			if (targetReversed != null) input = input.replace(targetReversed, value);
			if (targetDoubled != null) input = input.replace(targetDoubled, value);
			return input;
		}

		@Override
		public String toString() {
			return String.format("'%s'->'%s'", targetNormal, value);
		}
	}

	private static class SimpleRegexReplace implements ITransformer {
		private final Pattern pattern;
		private final String replacement;

		public SimpleRegexReplace(String pattern, String replacement) {
			this.pattern = Pattern.compile(pattern);
			this.replacement = replacement;
		}

		@Override
		public String transform(String input) {
			Matcher match = pattern.matcher(input);
			return match.replaceAll(replacement);
		}

		@Override
		public String toString() {
			return "regex: " + replacement;
		}
	}

	private static final Set<String> ALREADY_REPLACED = Sets.newHashSet();

	private static final Map<Class<? extends Throwable>, Sanitizer> THROWABLE_SANITIZERS = Maps.newHashMap();

	public static final Sanitizer mainSanitizer = new Sanitizer();

	public static void addThrowableSanitizer(Class<? extends Throwable> cls, Sanitizer sanitizer) {
		THROWABLE_SANITIZERS.put(cls, sanitizer);
	}

	public static Sanitizer getSanitizerForThrowable(Class<? extends Throwable> cls) {
		Sanitizer result = THROWABLE_SANITIZERS.get(cls);
		return result != null? result : mainSanitizer;
	}

	public static ITransformer path(String target, String value) {
		if (!Strings.isNullOrEmpty(target)) return new PathReplace(target, value);
		return null;
	}

	public static ITransformer replace(Object target, String value) {
		if (target != null) {
			String s = target.toString();
			if (s != null && s.length() > 2) return new Sanitizers.SimpleReplace(s, value);
		}
		return null;
	}

	public static ITransformer replaceNoDuplicates(Object target, String value) {
		if (target != null) {
			String s = target.toString();
			if (s != null && s.length() > 2 && !ALREADY_REPLACED.contains(s)) {
				ALREADY_REPLACED.add(s);
				return new Sanitizers.SimpleReplace(s, value);
			}
		}
		return null;
	}

	private static void addLocalAddresses() {
		Set<String> ips = Sets.newHashSet();
		Set<String> hosts = Sets.newHashSet();
		try {
			Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
			while (interfaces.hasMoreElements()) {
				NetworkInterface intf = interfaces.nextElement();
				Enumeration<InetAddress> addresses = intf.getInetAddresses();
				while (addresses.hasMoreElements()) {
					InetAddress address = addresses.nextElement();
					if (address != null) {
						ips.add(address.getHostAddress());

						String host = address.getHostName();
						if (!ips.contains(host)) hosts.add(host);
					}
				}
			}
		} catch (Throwable t) {
			Log.warn(t, "Failed to get local IP adresses for sanitization");
		}

		for (String ip : ips)
			mainSanitizer.addPre(PRIORITY_LOCAL_IP, replace(ip, "[local ip]"));

		for (String host : hosts)
			mainSanitizer.addPre(PRIORITY_LOCAL_HOST, replace(host, "[host]"));

		mainSanitizer.addPre(PRIORITY_IP_PORT, new SimpleRegexReplace("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}[:~]\\d+", "[ip+port]"));
	}

	public static void addWorldNames(World world) {
		final ISaveHandler saveHandler = world.getSaveHandler();

		String worldDir = saveHandler.getWorldDirectoryName();
		mainSanitizer.addPre(PRIORITY_SAVE_DIR_NAME, replaceNoDuplicates(worldDir, "[save dir]"));

		try {
			File dummy = saveHandler.getMapFileFromName("dummy");
			if (dummy != null) {
				String parent = dummy.getParent();
				if (parent != null) mainSanitizer.addPre(PRIORITY_SAVE_DIR, new PathReplace(parent, "[save dir]"));
			}
		} catch (Throwable t) {
			Log.warn(t, "Failed to get sanitizer name for world");
		}

		String worldName = world.getWorldInfo().getWorldName();
		mainSanitizer.addPre(PRIORITY_WORLD_NAME, replaceNoDuplicates(worldName, "[world name]"));
	}

	static {
		addLocalAddresses();

		mainSanitizer.addPre(PRIORITY_WORK_DIR, new PathReplace(System.getProperty("user.dir"), "[workdir]"));
		mainSanitizer.addPre(PRIORITY_HOME, new PathReplace(System.getProperty("user.home"), "[home]"));
		mainSanitizer.addPre(PRIORITY_SYSTEM_USER, new PropertyReplace("user.name", "[user]"));

		Sanitizer ipSanitizer = new Sanitizer(mainSanitizer);
		ipSanitizer.addPre(1000, new SimpleRegexReplace("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}", "[ip]"));

		addThrowableSanitizer(ProtocolException.class, ipSanitizer);
		addThrowableSanitizer(UnknownHostException.class, ipSanitizer);
	}

	public static void addMinecraftPath(File mcLocation) {
		mainSanitizer.addPre(PRIORITY_MINECRAFT_DIR, path(mcLocation.getAbsolutePath(), "[minecraft_dir]"));
	}

	public static void addPlayerName(String username) {
		mainSanitizer.addPre(PRIORITY_LOCAL_PLAYER, Sanitizers.replace(username, "[local player]"));
	}
}
