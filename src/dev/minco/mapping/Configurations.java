package dev.minco.mapping;

import java.util.Arrays;
import java.util.List;

import org.gradle.api.plugins.JavaPlugin;

class Configurations {
	public static final List<String> builtinCfgPrefixes = Arrays.asList(
		JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME,
		JavaPlugin.API_CONFIGURATION_NAME);

	public static String makeFirstUpper(String cfgName) {
		return cfgName.substring(0, 1).toUpperCase() + cfgName.substring(1);
	}
}
