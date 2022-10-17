package de.whitescan.playerplot.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import com.google.common.collect.ImmutableList;

import de.whitescan.playerplot.PlayerPlot;

public class ConfigLoader {

	private static final String pluginFolder = "PlayerPlot";

	private static ImmutableList<String> configs = new ImmutableList.Builder<String>().add("config").build();

	private static ImmutableList<String> languages = new ImmutableList.Builder<String>().add("cs").add("de").add("en")
			.add("es").add("fr").add("it").add("ko").add("lt").add("pl").add("ru").build();

	public static void load() {
		try {
			for (String config : configs) {
				File target = new File("plugins/" + pluginFolder, config + ".yml");
				if (!target.exists()) {
					load("config/" + config + ".yml", target);
				}
			}
			for (String lang : languages) {
				File target = new File("plugins/" + pluginFolder + "/locale", lang + ".yml");
				if (!target.exists()) {
					load("locale/" + lang + ".yml", target);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void load(String resource, File file) throws IOException {
		file.getParentFile().mkdirs();
		InputStream in = PlayerPlot.getPlugin().getResource(resource);
		Files.copy(in, file.toPath());
		in.close();
	}

}
