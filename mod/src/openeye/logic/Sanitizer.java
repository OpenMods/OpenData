package openeye.logic;

import java.io.File;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Deque;
import java.util.Enumeration;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.world.World;
import net.minecraft.world.storage.ISaveHandler;
import openeye.Log;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class Sanitizer {

	public interface ITransformer {
		public String transform(String input);
	}

	private static class SimpleReplace implements ITransformer {
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

	private static class PropertyReplace extends SimpleReplace {
		public PropertyReplace(String property, String value) {
			super(System.getProperty(property), value);
		}
	}

	private static class PathReplace implements ITransformer {
		private final String targetNormal;
		private final String targetReversed;
		private final String value;

		public PathReplace(String target, String value) {
			this.targetNormal = target;
			if (File.separatorChar == '\\') targetReversed = targetNormal.replace('\\', '/');
			else if (File.separatorChar == '/') targetReversed = targetNormal.replace('/', '\\');
			else targetReversed = null;

			this.value = value;
		}

		@Override
		public String transform(String input) {
			String tmp = input.replace(targetNormal, value);
			if (targetReversed != null) tmp = input.replace(targetReversed, value);
			return tmp;
		}

		@Override
		public String toString() {
			return String.format("'%s'->'%s'", targetNormal, value);
		}
	}

	private static class IpReplace implements ITransformer {
		private Pattern pattern = Pattern.compile("(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})[:~]\\d+");

		@Override
		public String transform(String input) {
			Matcher match = pattern.matcher(input);
			return match.replaceAll("[ip:port]");
		}
	}

	public static ITransformer replace(Object target, String value) {
		if (target != null) {
			String s = target.toString();
			if (!Strings.isNullOrEmpty(s)) return new SimpleReplace(s, value);
		}
		return null;
	}

	public static ITransformer path(String target, String value) {
		if (!Strings.isNullOrEmpty(target)) return new PathReplace(target, value);
		return null;
	}

	public static ITransformer replaceNoDuplicates(Object target, String value) {
		if (target != null) {
			String s = target.toString();
			if (!Strings.isNullOrEmpty(s) && !ALREADY_REPLACED.contains(s)) {
				ALREADY_REPLACED.add(s);
				return new SimpleReplace(s, value);
			}
		}
		return null;
	}

	private static final Deque<ITransformer> TRANSFORMERS = Lists.newLinkedList();

	private static final Set<String> ALREADY_REPLACED = Sets.newHashSet();

	static {
		addLocalAddresses();

		addLast(new PathReplace(System.getProperty("user.dir"), "[workdir]"));
		addLast(new PathReplace(System.getProperty("user.home"), "[home]"));
		addLast(new PropertyReplace("user.name", "[user]"));
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
			addLast(replace(ip, "[local ip]"));

		for (String host : hosts)
			addLast(replace(host, "[host]"));

		addLast(new IpReplace());
	}

	public static void addWorldNames(World world) {
		final ISaveHandler saveHandler = world.getSaveHandler();

		String worldDir = saveHandler.getWorldDirectoryName();
		Sanitizer.addFirst(replaceNoDuplicates(worldDir, "[save dir]"));

		try {
			File dummy = saveHandler.getMapFileFromName("dummy");
			if (dummy != null) {
				String parent = dummy.getParent();
				if (parent != null) Sanitizer.addFirst(new PathReplace(parent, "[save dir]"));
			}
		} catch (Throwable t) {
			Log.warn(t, "Failed to get sanitizer name for world");
		}

		String worldName = world.getWorldInfo().getWorldName();
		Sanitizer.addFirst(replaceNoDuplicates(worldName, "[world name]"));
	}

	public static void addFirst(ITransformer transformer) {
		if (transformer != null) TRANSFORMERS.addFirst(transformer);
	}

	public static void addLast(ITransformer transformer) {
		if (transformer != null) TRANSFORMERS.addLast(transformer);
	}

	public static String sanitize(String input) {
		if (Strings.isNullOrEmpty(input)) return "";

		for (ITransformer transformer : TRANSFORMERS)
			input = transformer.transform(input);

		return input;
	}
}
