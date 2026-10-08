package com.sounaks.indiangold.rates;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Plug-in jars in ~/.indiangold/plugins/, built here the way docs/PLUGINS.md describes. */
class PluginsTest
{
	@TempDir Path dataDir;
	@TempDir Path work;

	private Path plugins() throws IOException
	{
		return Files.createDirectories(dataDir.resolve(ProviderRegistry.PLUGINS_FOLDER));
	}

	private static void jar(Path jar, Map<String, byte[]> files) throws IOException
	{
		try(JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar)))
		{
			for(Map.Entry<String, byte[]> file : files.entrySet())
			{
				out.putNextEntry(new JarEntry(file.getKey()));
				out.write(file.getValue());
				out.closeEntry();
			}
		}
	}

	private static byte[] text(String text)
	{
		return text.getBytes(StandardCharsets.UTF_8);
	}

	@Test
	void definitionOnlyPlugInsNeedNoCode() throws Exception
	{
		jar(plugins().resolve("my-sources.jar"), Map.of(
				ProviderRegistry.PLUGIN_DEFINITIONS, text("# my sources\nproviders/my-shop.properties\nproviders/broken.properties\nproviders/missing.properties\n"),
				"providers/my-shop.properties", text("id=my-shop\nname=My shop\ntype=web-page\nurl=https://shop.test/rates\nmetal.gold.select=#gold\nmetal.gold.unit=g\nmetal.gold.per=10\ncurrency=INR\n"),
				"providers/broken.properties", text("id=broken\ntype=json-api\n")));

		ProviderRegistry registry = ProviderRegistry.load(dataDir);

		ProviderRegistry.Entry shop = registry.find("my-shop").orElseThrow();
		assertEquals(ProviderRegistry.Origin.PLUGIN, shop.origin());
		assertEquals("my-sources.jar", shop.file().orElseThrow().getFileName().toString());
		assertTrue(shop.provider().isWebPage());
		String problems = String.join("\n", registry.problems());
		assertTrue(problems.contains("my-sources.jar/providers/broken.properties: name is missing"), problems);
		assertTrue(problems.contains("providers/missing.properties is listed but not in the jar"), problems);
	}

	@Test
	void javaPlugInsAreFoundThroughServiceLoaderAndFetch() throws Exception
	{
		Path source = Files.createDirectories(work.resolve("src/example"));
		Files.writeString(source.resolve("FixedGold.java"), """
				package example;
				import com.sounaks.indiangold.rates.*;
				import java.util.*;
				public class FixedGold implements RateProvider {
				    public String id() { return "fixed-gold"; }
				    public String name() { return "Fixed gold"; }
				    public String description() { return "Always 100 USD per gram."; }
				    public Set<Metal> metals() { return EnumSet.of(Metal.GOLD); }
				    public RateSnapshot fetch(FetchContext context) {
				        Quote gold = Quote.of(Metal.GOLD, 100, 1, MassUnit.GRAM, "USD", context.clock().instant(), id());
				        return new RateSnapshot(id(), Map.of(Metal.GOLD, gold), Optional.empty(), context.clock().instant());
				    }
				}
				""");
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		Path classes = Files.createDirectories(work.resolve("classes"));
		int result = compiler.run(null, null, null, "-d", classes.toString(), "-cp", System.getProperty("java.class.path"), source.resolve("FixedGold.java").toString());
		assertEquals(0, result, "the example plug-in compiles against the app");
		jar(plugins().resolve("fixed-gold.jar"), Map.of(
				"META-INF/services/com.sounaks.indiangold.rates.RateProvider", text("example.FixedGold\n"),
				"example/FixedGold.class", Files.readAllBytes(classes.resolve("example/FixedGold.class"))));

		ProviderRegistry registry = ProviderRegistry.load(dataDir);

		RateProvider plugin = registry.find("fixed-gold").orElseThrow().provider();
		assertEquals(ProviderRegistry.Origin.PLUGIN, registry.find("fixed-gold").orElseThrow().origin());
		RateSnapshot snapshot = plugin.fetch(LocalServer.context(null));
		assertEquals(100.0, snapshot.quotes().get(Metal.GOLD).pricePerGram(), 1e-9);
		assertEquals(List.of("gold-api", "westmetall", "currency-api", "metals-dev", "manual"),
				registry.entries().stream().filter(e -> e.origin() == ProviderRegistry.Origin.BUILT_IN).map(e -> e.provider().id()).toList(),
				"built-in providers are not loaded a second time through the plug-in's class loader");
	}

	@Test
	void reloadPicksUpNewJarsAndReportsClashingIds() throws Exception
	{
		ProviderRegistry registry = ProviderRegistry.load(dataDir);
		registry.register(new RateServiceTest.FakeProvider("registered", 0, java.time.Duration.ofMinutes(1)));
		assertTrue(registry.find("later").isEmpty());

		jar(plugins().resolve("later.jar"), Map.of(
				ProviderRegistry.PLUGIN_DEFINITIONS, text("later.properties\nclash.properties\n"),
				"later.properties", text("id=later\nname=Later\ntype=json-api\nurl=https://x.test/\nmetal.gold.path=/g\n"),
				"clash.properties", text("id=gold-api\nname=Mine\ntype=json-api\nurl=https://x.test/\nmetal.gold.path=/g\n")));
		registry.reload();

		assertTrue(registry.find("later").isPresent());
		assertTrue(registry.find("registered").isPresent(), "providers registered in code are kept");
		assertEquals(ProviderRegistry.Origin.BUILT_IN, registry.find("gold-api").orElseThrow().origin());
		assertTrue(String.join("\n", registry.problems()).contains("\"gold-api\" is already used"));
		assertEquals(dataDir.resolve("plugins"), registry.pluginsFolder().orElseThrow());
	}

	@Test
	void unreadableJarsAreReportedNotFatal() throws Exception
	{
		try(OutputStream out = Files.newOutputStream(plugins().resolve("not-a-jar.jar")))
		{
			out.write(text("this is not a zip file"));
		}

		ProviderRegistry registry = ProviderRegistry.load(dataDir);

		assertTrue(registry.find("gold-api").isPresent());
	}
}
