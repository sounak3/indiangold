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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

/**
 * All rate providers the app knows: definitions built into the jar, Java providers registered with ServiceLoader,
 * the user's own definition files in ~/.indiangold/providers/ and plug-in jars in ~/.indiangold/plugins/.
 * @author Sounak Choudhury
 */
public final class ProviderRegistry
{
	public static final String PROVIDERS_FOLDER = "providers";
	public static final String PLUGINS_FOLDER = "plugins";

	/** Where a provider comes from; built-in ones can be duplicated but not changed. */
	public enum Origin
	{
		BUILT_IN, USER_DEFINITION, PLUGIN
	}

	/**
	 * A known provider.
	 * @param provider The provider.
	 * @param origin Where it comes from.
	 * @param file The user's definition file, for USER_DEFINITION.
	 */
	public record Entry(RateProvider provider, Origin origin, Optional<Path> file)
	{
		/** The definition behind the provider, if it is defined by one. */
		public Optional<ProviderDefinition> definition()
		{
			return provider instanceof DefinitionProvider dp ? Optional.of(dp.definition()) : Optional.empty();
		}
	}

	private final Map<String, Entry> entries = new LinkedHashMap<>();
	private final List<String> problems = new ArrayList<>();
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
		registry.loadBuiltInDefinitions();
		registry.loadServices(ProviderRegistry.class.getClassLoader(), Origin.BUILT_IN);
		if(dataDir != null)
		{
			registry.loadUserDefinitions(dataDir.resolve(PROVIDERS_FOLDER));
			registry.loadPlugins(dataDir.resolve(PLUGINS_FOLDER));
		}
		return registry;
	}

	private void loadBuiltInDefinitions()
	{
		try(InputStream index = ProviderRegistry.class.getResourceAsStream("/" + PROVIDERS_FOLDER + "/index.list"))
		{
			if(index == null) return;
			BufferedReader reader = new BufferedReader(new InputStreamReader(index, StandardCharsets.UTF_8));
			for(String line; (line = reader.readLine()) != null;)
			{
				String name = line.trim();
				if(name.isEmpty() || name.startsWith("#")) continue;
				Properties properties = new Properties();
				try(InputStream in = ProviderRegistry.class.getResourceAsStream("/" + PROVIDERS_FOLDER + "/" + name))
				{
					if(in == null) { problems.add("Built-in definition " + name + " is missing."); continue; }
					properties.load(in);
				}
				add(ProviderDefinition.of(properties), Origin.BUILT_IN, null, name);
			}
		}
		catch(IOException e)
		{
			problems.add("Cannot read the built-in providers: " + e.getMessage());
		}
	}

	private void loadUserDefinitions(Path folder)
	{
		if(!Files.isDirectory(folder)) return;
		try(DirectoryStream<Path> files = Files.newDirectoryStream(folder, "*.properties"))
		{
			List<Path> sorted = new ArrayList<>();
			files.forEach(sorted::add);
			Collections.sort(sorted);
			for(Path file : sorted)
			{
				Optional<Properties> properties = SafeFiles.readProperties(file);
				if(properties.isEmpty()) { problems.add("Cannot read " + file.getFileName() + "."); continue; }
				add(ProviderDefinition.of(properties.get()), Origin.USER_DEFINITION, file, file.getFileName().toString());
			}
		}
		catch(IOException e)
		{
			problems.add("Cannot read " + folder + ": " + e.getMessage());
		}
	}

	private void loadPlugins(Path folder)
	{
		if(!Files.isDirectory(folder)) return;
		List<URL> jars = new ArrayList<>();
		try(DirectoryStream<Path> files = Files.newDirectoryStream(folder, "*.jar"))
		{
			for(Path jar : files) jars.add(jar.toUri().toURL());
		}
		catch(IOException e)
		{
			problems.add("Cannot read " + folder + ": " + e.getMessage());
		}
		if(jars.isEmpty()) return;
		// Plug-ins run as code with the user's rights; the settings window says so next to them.
		loadServices(new URLClassLoader(jars.toArray(URL[]::new), ProviderRegistry.class.getClassLoader()), Origin.PLUGIN);
	}

	private void loadServices(ClassLoader loader, Origin origin)
	{
		ServiceLoader<RateProvider> services = ServiceLoader.load(RateProvider.class, loader);
		var iterator = services.stream().iterator();
		while(true)
		{
			try
			{
				if(!iterator.hasNext()) break;
				ServiceLoader.Provider<RateProvider> service = iterator.next();
				// The parent loader's services show up again through a plug-in loader; keep only new ones.
				if(origin == Origin.PLUGIN && service.type().getClassLoader() != loader) continue;
				addProvider(service.get(), origin, null, service.type().getName());
			}
			catch(ServiceConfigurationError | RuntimeException | LinkageError e)
			{
				problems.add("A " + (origin == Origin.PLUGIN ? "plug-in" : "provider") + " could not be loaded: " + e.getMessage());
			}
		}
	}

	private void add(ProviderDefinition definition, Origin origin, Path file, String source)
	{
		if(!definition.isValid())
		{
			problems.add(source + ": " + String.join(" ", definition.problems()));
			return;
		}
		addProvider(definition.createProvider(), origin, file, source);
	}

	private void addProvider(RateProvider provider, Origin origin, Path file, String source)
	{
		Entry existing = entries.get(provider.id());
		if(existing != null)
		{
			problems.add(source + ": the id \"" + provider.id() + "\" is already used by " + existing.provider().name() + "; give it another id.");
			return;
		}
		entries.put(provider.id(), new Entry(provider, origin, Optional.ofNullable(file)));
	}

	/** Adds a provider written in Java, e.g. by a test. */
	void register(RateProvider provider)
	{
		addProvider(provider, Origin.PLUGIN, null, provider.getClass().getName());
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

	/** Definitions and plug-ins that could not be loaded, with the reason. */
	public List<String> problems()
	{
		return Collections.unmodifiableList(problems);
	}

	/**
	 * Saves a user definition as ~/.indiangold/providers/ID.properties. Reload the registry afterwards.
	 * @param definition The definition; it must be valid and must not reuse a built-in id.
	 * @return The file written.
	 * @throws IOException If it cannot be written.
	 * @throws IllegalArgumentException If the definition is invalid or its id belongs to a built-in provider.
	 */
	public Path save(ProviderDefinition definition) throws IOException
	{
		if(!definition.isValid()) throw new IllegalArgumentException(String.join(" ", definition.problems()));
		Entry existing = entries.get(definition.id());
		if(existing != null && existing.origin() != Origin.USER_DEFINITION)
			throw new IllegalArgumentException("The id \"" + definition.id() + "\" belongs to the built-in " + existing.provider().name() + "; choose another id.");
		if(dataDir == null) throw new IOException("No data folder to save into.");
		Path file = dataDir.resolve(PROVIDERS_FOLDER).resolve(definition.id() + ".properties");
		SafeFiles.writeProperties(file, definition.toProperties(), "IndianGold rate source: " + definition.name());
		return file;
	}

	/**
	 * Deletes a user definition file; built-in providers cannot be deleted.
	 * @param id The provider id.
	 * @throws IOException If the file cannot be deleted.
	 */
	public void delete(String id) throws IOException
	{
		Entry entry = entries.get(id);
		if(entry == null || entry.file().isEmpty()) throw new IllegalArgumentException("Only your own definitions can be deleted.");
		Files.deleteIfExists(entry.file().get());
		Files.deleteIfExists(SafeFiles.backupOf(entry.file().get()));
	}
}
