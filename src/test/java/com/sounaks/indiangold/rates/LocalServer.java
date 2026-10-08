package com.sounaks.indiangold.rates;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/** A local HTTP server for provider tests: serves fixed responses and records what was requested. */
final class LocalServer implements AutoCloseable
{
	record Reply(int status, String body)
	{
	}

	record Request(String pathAndQuery, String userAgent)
	{
	}

	private final HttpServer server;
	private final Map<String, Reply> replies = new ConcurrentHashMap<>();
	final List<Request> requests = new CopyOnWriteArrayList<>();

	LocalServer() throws IOException
	{
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/", exchange -> {
			String path = exchange.getRequestURI().getRawPath() + (exchange.getRequestURI().getRawQuery() == null ? "" : "?" + exchange.getRequestURI().getRawQuery());
			requests.add(new Request(path, exchange.getRequestHeaders().getFirst("User-Agent")));
			Reply reply = replies.getOrDefault(path, replies.getOrDefault(exchange.getRequestURI().getRawPath(), new Reply(404, "not found")));
			byte[] body = reply.body().getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(reply.status(), body.length);
			try(OutputStream out = exchange.getResponseBody())
			{
				out.write(body);
			}
		});
		server.start();
	}

	String base()
	{
		return "http://127.0.0.1:" + server.getAddress().getPort();
	}

	LocalServer reply(String path, int status, String body)
	{
		replies.put(path, new Reply(status, body));
		return this;
	}

	long count(String pathPrefix)
	{
		return requests.stream().filter(r -> r.pathAndQuery().startsWith(pathPrefix)).count();
	}

	@Override
	public void close()
	{
		server.stop(0);
	}

	static String resource(String name)
	{
		try(InputStream in = LocalServer.class.getResourceAsStream("/rates/" + name))
		{
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
		catch(IOException e)
		{
			throw new UncheckedIOException(e);
		}
	}

	/** Loads a built-in definition and points its URLs at this server: "https://host/x" becomes base + "/host/x". */
	ProviderDefinition builtIn(String id)
	{
		Properties properties = new Properties();
		try(InputStream in = ProviderDefinition.class.getResourceAsStream("/providers/" + id + ".properties"))
		{
			properties.load(in);
		}
		catch(IOException e)
		{
			throw new UncheckedIOException(e);
		}
		for(String key : properties.stringPropertyNames())
		{
			String value = properties.getProperty(key);
			if((key.equals("url") || key.endsWith(".url") || key.equals("url.fallback")) && value.startsWith("https://"))
				properties.setProperty(key, base() + "/" + value.substring("https://".length()));
		}
		return ProviderDefinition.of(properties);
	}

	static FetchContext context(String apiKey)
	{
		Map<String, String> values = new ConcurrentHashMap<>();
		if(apiKey != null) values.put(ProviderSettings.API_KEY, apiKey);
		ProviderSettings settings = new ProviderSettings()
		{
			@Override
			public Optional<String> get(String name)
			{
				return Optional.ofNullable(values.get(name));
			}

			@Override
			public void put(String name, String value)
			{
				values.put(name, value);
			}
		};
		return new FetchContext(settings, new HttpFetcher("IndianGold/5.0 (+https://github.com/sounak3/indiangold)", Duration.ofSeconds(5),
				Clock.systemUTC()), Clock.systemUTC());
	}
}
