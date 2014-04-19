package openeye.logic;

import java.util.Set;

import openeye.config.ConfigProperty;

import com.google.common.collect.ImmutableSet;

public class Config {

	@ConfigProperty(category = "debug")
	public static boolean crashOnStartup = false;

	@ConfigProperty(category = "data")
	public static Set<String> tags = ImmutableSet.of();

	@ConfigProperty(category = "data")
	public static boolean scanOnly = false;

	@ConfigProperty(category = "debug")
	public static boolean pingOnInitialReport = false;
}
