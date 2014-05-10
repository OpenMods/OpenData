package openeye.logic;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Deque;
import java.util.Enumeration;
import java.util.Set;

import net.minecraft.world.World;
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

	public static ITransformer replace(Object target, String value) {
		if (target != null) {
			String s = target.toString();
			if (!Strings.isNullOrEmpty(s)) return new SimpleReplace(s, value);
		}
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

		addLast(new PropertyReplace("user.dir", "[workdir]"));
		addLast(new PropertyReplace("user.home", "[home]"));
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
	}

	public static void addWorldNames(World world) {
		String worldDir = world.getSaveHandler().getWorldDirectoryName();
		Sanitizer.addFirst(replaceNoDuplicates(worldDir, "[save dir]"));

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
