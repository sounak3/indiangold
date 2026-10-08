/*
 * Copyright (C) 2026 Sounak Choudhury
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.sounaks.indiangold.rates;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * All rate providers the app knows: the ones built into the app (definition files listed in /providers/index.list and
 * Java providers registered with ServiceLoader) and plug-ins, which are jar files in ~/.indiangold/plugins/.
 * <p>
 * A plug-in jar may contain Java providers (classes implementing {@link RateProvider}, listed in
 * META-INF/services/com.sounaks.indiangold.rates.RateProvider) and definition files (listed, one resource path per
 * line, in {@value #PLUGIN_DEFINITIONS}). See docs/PLUGINS.md.
 * @author Sounak Choudhury
 */
public final class ProviderRegistry
{
	public static final String PLUGINS_FOLDER = "plugins";
	/** The list of definition files inside a plug-in jar. */
	public static final String PLUGIN_DEFINITIONS = "META-INF/indiangold/providers.list";

	/** Where a provider comes from. */
	public enum Origin
	{
		BUILT_IN, PLUGIN
	}

	/**
	 * A known provider.
	 * @param provider The provider.
	 * @param origin Where it comes from.
	 * @param file The plug-in jar it came from, for plug-ins.
	 * @param registered Whether it was added in code with {@link #register(RateProvider)}.
	 */
	public record Entry(RateProvider provider, Origin origin, Optional<Path> file, boolean registered)
	{
		public Entry(RateProvider provider, Origin origin, Optional<Path> file)
		{
			this(provider, origin, file, false);
		}

		/** The definition behind the provider, if it is defined by one. */
		public Optional<ProviderDefinition> definition()
		{
			return provider instanceof DefinitionProvider dp ? Optional.of(dp.definition()) : Optional.empty();
		}
	}

	// Replaced as a whole by reload(), so the rate thread always sees a complete set.
	private volatile Map<String, Entry> entries = new LinkedHashMap<>();
	private volatile List<String> problems = new ArrayList<>();
	private final Path dataDir;

	private ProviderRegistry(Path dataDir)
	{
		this.dataDir = dataDir;
	}

	/**
	 * Loads every provider.
	 * @param dataDir The app's data folder (~/.indiangold); may be null to load only built-in providers.
	 * @return The registry; {@link #problems()} lists anything that could not be loaded.
	 */
	public static ProviderRegistry load(Path dataDir)
	{
		ProviderRegistry registry = new ProviderRegistry(dataDir);
		registry.reload();
		return registry;
	}

	/**
	 * Loads every provider again, e.g. after the user put a new plug-in into the plugins folder. Providers added
	 * with {@link #register(RateProvider)} are kept.
	 */
	public synchronized void reload()
	{
		Map<String, Entry> registered = new LinkedHashMap<>();
		entries.forEach((id, entry) -> { if(entry.registered()) registered.put(id, entry); });
		ProviderRegistry fresh = new ProviderRegistry(dataDir);
		fresh.loadBuiltInDefinitions();
		Set<String> builtInClasses = fresh.loadServices(ProviderRegistry.class.getClassLoader(), Origin.BUILT_IN, null, Set.of());
		if(dataDir != null) fresh.loadPlugins(dataDir.resolve(PLUGINS_FOLDER), builtInClasses);
		registered.forEach((id, entry) -> fresh.addEntry(entry, "registered"));
		entries = fresh.entries;
		problems = fresh.problems;
	}

	/** The folder for plug-in jars, ~/.indiangold/plugins/; may not exist yet. */
	public Optional<Path> pluginsFolder()
	{
		return Optional.ofNullable(dataDir).map(dir -> dir.resolve(PLUGINS_FOLDER));
	}

	private void loadBuiltInDefinitions()
	{
		try(InputStream index = ProviderRegistry.class.getResourceAsStream("/providers/index.list"))
		{
			if(index == null) return;
			for(String name : lines(index))
			{
				try(InputStream in = ProviderRegistry.class.getResourceAsStream("/providers/" + name))
				{
					if(in == null) { problems.add("Built-in definition " + name + " is missing."); continue; }
					Properties properties = new Properties();
					properties.load(in);
					add(ProviderDefinition.of(properties), Origin.BUILT_IN, null, name);
				}
			}
		}
		catch(IOException e)
		{
			problems.add("Cannot read the built-in providers: " + e.getMessage());
		}
	}

	private void loadPlugins(Path folder, Set<String> builtInClasses)
	{
		if(!Files.isDirectory(folder)) return;
		List<Path> jars = new ArrayList<>();
		try(DirectoryStream<Path> files = Files.newDirectoryStream(folder, "*.jar"))
		{
			files.forEach(jars::add);
		}
		catch(IOException e)
		{
			problems.add("Cannot read " + folder + ": " + e.getMessage());
		}
		Collections.sort(jars);
		for(Path jar : jars)
		{
			String jarName = jar.getFileName().toString();
			try
			{
				// One class loader per jar, so each jar's resources are read from that jar. Plug-ins run as code with the
				// user's rights; the settings window says so next to them.
				@SuppressWarnings("resource") // stays open while the plug-in's classes are in use
				URLClassLoader loader = new URLClassLoader(new URL[] { jar.toUri().toURL() }, ProviderRegistry.class.getClassLoader());
				URL list = loader.findResource(PLUGIN_DEFINITIONS);
				if(list != null)
				{
					try(InputStream in = list.openStream())
					{
						for(String name : lines(in))
						{
							URL definition = loader.findResource(name.startsWith("/") ? name.substring(1) : name);
							if(definition == null) { problems.add(jarName + ": " + name + " is listed but not in the jar."); continue; }
							try(InputStream definitionIn = definition.openStream())
							{
								Properties properties = new Properties();
								properties.load(definitionIn);
								add(ProviderDefinition.of(properties), Origin.PLUGIN, jar, jarName + "/" + name);
							}
						}
					}
				}
				loadServices(loader, Origin.PLUGIN, jar, builtInClasses);
			}
			catch(IOException | IllegalArgumentException e)
			{
				problems.add(jarName + " could not be read: " + e.getMessage());
			}
		}
	}

	/**
	 * Loads the Java providers a class loader can see.
	 * @param skip Classes already loaded from the app itself; a plug-in's loader sees them again through its parent.
	 * @return The names of the classes loaded.
	 */
	private Set<String> loadServices(ClassLoader loader, Origin origin, Path jar, Set<String> skip)
	{
		Set<String> loaded = new HashSet<>();
		var iterator = ServiceLoader.load(RateProvider.class, loader).stream().iterator();
		String source = jar == null ? "A provider" : jar.getFileName().toString();
		while(true)
		{
			try
			{
				if(!iterator.hasNext()) break;
				ServiceLoader.Provider<RateProvider> service = iterator.next();
				if(skip.contains(service.type().getName())) continue;
				addProvider(service.get(), origin, jar, service.type().getName());
				loaded.add(service.type().getName());
			}
			catch(ServiceConfigurationError | RuntimeException | LinkageError e)
			{
				problems.add(source + " could not be loaded: " + e.getMessage());
			}
		}
		return loaded;
	}

	private void add(ProviderDefinition definition, Origin origin, Path jar, String source)
	{
		if(!definition.isValid())
		{
			problems.add(source + ": " + String.join(" ", definition.problems()));
			return;
		}
		addProvider(definition.createProvider(), origin, jar, source);
	}

	private void addProvider(RateProvider provider, Origin origin, Path jar, String source)
	{
		addEntry(new Entry(provider, origin, Optional.ofNullable(jar)), source);
	}

	private void addEntry(Entry entry, String source)
	{
		Entry existing = entries.get(entry.provider().id());
		if(existing != null)
		{
			problems.add(source + ": the id \"" + entry.provider().id() + "\" is already used by " + existing.provider().name() + "; give it another id.");
			return;
		}
		entries.put(entry.provider().id(), entry);
	}

	private static List<String> lines(InputStream in) throws IOException
	{
		List<String> lines = new ArrayList<>();
		BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
		for(String line; (line = reader.readLine()) != null;)
		{
			String trimmed = line.trim();
			if(!trimmed.isEmpty() && !trimmed.startsWith("#")) lines.add(trimmed);
		}
		return lines;
	}

	/** Adds a provider written in Java, e.g. by a test. */
	void register(RateProvider provider)
	{
		Map<String, Entry> copy = new LinkedHashMap<>(entries);
		copy.put(provider.id(), new Entry(provider, Origin.PLUGIN, Optional.empty(), true));
		entries = copy;
	}

	/** All providers, built-in ones first in their default order. */
	public List<Entry> entries()
	{
		return List.copyOf(entries.values());
	}

	public Optional<Entry> find(String id)
	{
		return Optional.ofNullable(entries.get(id));
	}

	/** Plug-ins and definitions that could not be loaded, with the reason. */
	public List<String> problems()
	{
		return Collections.unmodifiableList(problems);
	}
}
