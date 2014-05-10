package openeye.logic;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Deque;
import java.util.Enumeration;
import java.util.Set;

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
		return new SimpleReplace(target.toString(), value);
	}

	private static final Deque<ITransformer> TRANSFORMERS = Lists.newLinkedList();

	static {
		addLocalAddresses();

		TRANSFORMERS.addLast(new PropertyReplace("user.dir", "[workdir]"));
		TRANSFORMERS.addLast(new PropertyReplace("user.home", "[home]"));
		TRANSFORMERS.addLast(new PropertyReplace("user.name", "[user]"));
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
			TRANSFORMERS.addLast(replace(ip, "[local ip]"));

		for (String host : hosts)
			TRANSFORMERS.addLast(replace(host, "[host]"));
	}

	public static void addFirst(ITransformer transformer) {
		TRANSFORMERS.addFirst(transformer);
	}

	public static void addLast(ITransformer transformer) {
		TRANSFORMERS.addLast(transformer);
	}

	public static String sanitize(String input) {
		if (Strings.isNullOrEmpty(input)) return "";

		for (ITransformer transformer : TRANSFORMERS)
			input = transformer.transform(input);

		return input;
	}
}
