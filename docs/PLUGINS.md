# Writing a rate source plug-in for IndianGold

IndianGold shows metal prices from *rate sources*. Besides the built-in ones (Gold-API.com, Westmetall, Currency-API, Metals.Dev, manual entry), you can add your own as a **plug-in**: a `.jar` file in the plug-ins folder.

There are two kinds of plug-in, and one jar can contain both:

| Kind | You write | Good for |
|---|---|---|
| **Definition plug-in** | a `.properties` file per source, no code | JSON APIs and simple web pages |
| **Java plug-in** | a class implementing `RateProvider` | anything else: several requests, sign-in, unusual formats, calculations |

## Installing and testing a plug-in

1. Put the jar in the plug-ins folder:
   - Linux and macOS: `~/.indiangold/plugins/`
   - Windows: `%USERPROFILE%\.indiangold\plugins\`

   In the app: *Settings → Market rates → Plug-ins folder...* creates and opens it.
2. Press **Reload plug-ins** (or restart the app). New sources appear in the source list, switched off. Anything that could not be loaded is listed with the reason.
3. Select the source and press **Test**: it fetches once and shows the prices it read.
4. Tick **On** to use it. Move it up to give it priority: for each metal, the first enabled source with a price less than 48 hours old is used.

Plug-ins run inside the app with the user's rights. Only install plug-ins you trust, and say so to the people you give yours to.

## Rules every source must follow

- **Identify honestly.** Always fetch through `context.http()` (Java) or a definition. Both send `User-Agent: IndianGold/<version> (+https://github.com/sounak3/indiangold)`. Never pose as a browser, and never work around a site that blocks automated access.
- **Respect the site.** Web pages are fetched at most once an hour, and only where the site's `robots.txt` allows it (`context.http().getPage(...)` checks it for you). Check the site's terms of use and link them (`terms=` / `termsPage()`).
- **Respect quotas.** If the API has a monthly limit, declare it (`quota.monthly=` / `monthlyQuota()`). The app then fetches only at scheduled times and shows the user how many requests are left.
- **Don't share keys.** API keys are the user's own; they enter them in the app. Never put a key in a plug-in.
- **Fail with a message.** On failure throw `RateException` with a sentence the user understands; it is shown in the source list. The app keeps the last prices.

## Definition plug-ins (no code)

### Jar layout

```
my-sources.jar
├── META-INF/indiangold/providers.list     ← one definition path per line; # starts a comment
└── providers/
    ├── my-api.properties
    └── my-page.properties
```

`providers.list`:

```
# My rate sources
providers/my-api.properties
providers/my-page.properties
```

Build it with the JDK's `jar` tool (or any zip tool):

```sh
jar cf my-sources.jar META-INF providers
```

### Input and output of a JSON API source

The source fetches its `url` (with `{apikey}` replaced by the user's key, URL-encoded), reads the JSON response, and picks values with [JSON pointers](https://www.rfc-editor.org/rfc/rfc6901).

Say the API returns:

```json
{
  "updated": "2026-10-07T09:30:00Z",
  "base": "USD",
  "prices": { "XAU": 4165.30, "XAG": 61.48 },
  "fx": { "INR": 96.42, "EUR": 0.886 },
  "error": null
}
```

Then this definition reads gold and silver (US dollars per troy ounce), the exchange rates, and the date:

```properties
id=my-api
name=My metals API
type=json-api
description=Gold and silver from my-api.example; free key, 1000 requests a month.
url=https://api.my-api.example/v1/latest?key={apikey}
apikey=required
signup=https://my-api.example/signup
terms=https://my-api.example/terms
quota.monthly=1000
schedule=09:00,13:00,17:00
currency=USD
asOf.path=/updated
error.path=/error
metal.gold.path=/prices/XAU
metal.gold.unit=toz
metal.silver.path=/prices/XAG
metal.silver.unit=toz
fx.path=/fx
fx.direction=perUsd
```

The built-in definitions are complete examples:
[gold-api](../src/main/resources/providers/gold-api.properties) (one URL per metal),
[currency-api](../src/main/resources/providers/currency-api.properties) (inverted values, fallback URL),
[metals-dev](../src/main/resources/providers/metals-dev.properties) (API key, quota, error codes, usage).

### A web page source

```properties
id=my-page
name=My jeweller's board
type=web-page
description=Today's 22K rate from my-jeweller.example (web page).
url=https://my-jeweller.example/rates
terms=https://my-jeweller.example/terms
currency=INR
number.decimal=.
number.grouping=,
asOf.select=#rates .date
asOf.format=d MMM yyyy
asOf.locale=en
metal.gold.select=#rates tr:has(td:containsOwn(24K)) td:eq(1)
metal.gold.unit=g
metal.gold.per=10
```

Selectors use [jsoup's CSS syntax](https://jsoup.org/cookbook/extracting-data/selector-syntax); the first number in the selected element's text is used. See [westmetall](../src/main/resources/providers/westmetall.properties) for a real example.

### All keys

Only `id`, `name`, `type`, an address and one metal are required.

| Key | Meaning |
|---|---|
| `id` | lower-case letters, digits and dashes; unique among all sources |
| `name`, `description` | shown in the source list; say what it offers and what it costs |
| `type` | `json-api` or `web-page` |
| `url`, `url.fallback` | the address; the fallback is tried if the first can't be reached or answers with a server error |
| `apikey=required` | the URLs contain `{apikey}`; the user enters the key in the app |
| `signup`, `terms` | pages for "Get a free key..." and "Terms of use" |
| `currency` | ISO code of the prices (default `USD`) |
| `minInterval` | minutes between fetches, or an ISO duration such as `PT6H`; at least 60 for web pages |
| `quota.monthly`, `schedule` | requests allowed per month, and the default fetch times (e.g. `10:30,16:30`) |
| `asOf.path` / `asOf.select`, `asOf.format`, `asOf.locale` | where the price date is; without a format, ISO dates and times and epoch seconds or milliseconds are understood |
| `error.path`, `error.code.path`, `error.kind.CODE` | where a JSON API puts its error message and code; `error.kind.1101=API_KEY` (or `QUOTA`, `NETWORK`, `NOT_ALLOWED`, `UNEXPECTED_RESPONSE`, `CONFIGURATION`) tells the app what kind of problem it is |
| `number.decimal`, `number.grouping` | how numbers are written on a web page (default `.` and `,`) |
| `metal.KEY.path` / `metal.KEY.select` | where the price is; KEY is one of `gold`, `silver`, `platinum`, `palladium`, `copper`, `aluminum`, `nickel`, `zinc`, `lead`, `tin` |
| `metal.KEY.unit`, `metal.KEY.per` | the price is per `per` units of `unit`: `mg`, `g`, `kg`, `t`, `toz` (troy ounce), `lb`, `oz`, `tola` (11.664 g), `tael_hk` (37.429 g), `tael` (37.5 g), `baht` (15.244 g) |
| `metal.KEY.invert=true` | the value is units of metal per currency unit (e.g. ounces per dollar) |
| `metal.KEY.currency`, `metal.KEY.url` | a different currency or address for this metal only |
| `fx.path`, `fx.url`, `fx.direction` | an object of currency code → rate; `perUsd` (units per US dollar) or `usdPerUnit` (dollars per unit) |
| `usage.url`, `usage.plan.path`, `usage.total.path`, `usage.used.path` | how to ask the API how much of the quota is used ("Check usage") |

Gold is always the price of **pure** gold; the app derives 22K, 18K and the other rows from it. If a source only quotes 22K, set `metal.gold.per` to match and divide by the fineness in a Java plug-in instead.

## Java plug-ins

### The interface

Implement `com.sounaks.indiangold.rates.RateProvider` with a public no-argument constructor. The required methods:

```java
public interface RateProvider {
    String id();                 // unique, lower-case letters, digits, dashes
    String name();               // shown in the source list
    String description();        // one line: what it offers and what it costs
    Set<Metal> metals();         // which rows it can fill
    RateSnapshot fetch(FetchContext context) throws RateException;
    // ... optional methods with defaults, below
}
```

Optional methods (override as needed):

| Method | Default | Override when |
|---|---|---|
| `boolean needsApiKey()` | `false` | the user must enter a key; read it with `context.settings().apiKey()` |
| `Optional<URI> signupPage()`, `termsPage()` | empty | there is a page to get a key, or terms of use |
| `boolean providesFx()` | `false` | the snapshot includes exchange rates |
| `boolean isWebPage()` | `false` | you read a web page; the user is shown a disclaimer |
| `Duration minInterval()` | 1 minute | the source must not be asked more often (use at least 1 hour for web pages) |
| `int monthlyQuota()`, `int requestsPerFetch()` | 0 (no limit), 1 | the source has a monthly request limit |
| `List<LocalTime> defaultSchedule()` | none | the default fetch times for a source with a quota |
| `Optional<Usage> usage(FetchContext)` | empty | the source can report how much of its quota is used |

`fetch` is called on a background thread, never on the Swing thread. Don't open windows from it.

### Input: `FetchContext`

| Part | What it gives you |
|---|---|
| `context.settings()` | the user's settings for your source: `apiKey()`, and `get(name)` / `put(name, value)` for anything else you want to remember |
| `context.http()` | `get(uri)` → status and body, whatever the status; `getOk(uri)` → body, or a `RateException` with a clear message (401 → key refused, 429 → quota, ...); `getPage(uri)` → like `getOk`, but only if `robots.txt` allows the page |
| `context.clock()` | the time to use for timestamps (tests replace it) |

### Output: `RateSnapshot`

```java
Quote gold = Quote.of(Metal.GOLD,
        4165.30,            // the price as quoted
        1,                  // for this many units
        MassUnit.TROY_OUNCE,// of this unit
        "USD",              // in this currency
        asOf,               // when the source says the price is valid
        id());
FxRates fx = new FxRates(Map.of("INR", 96.42, "EUR", 0.886), asOf, id()); // units per US dollar

return new RateSnapshot(id(), Map.of(Metal.GOLD, gold), Optional.of(fx), context.clock().instant());
```

A snapshot may hold only some of your metals; missing ones keep their last price or come from other sources. If nothing usable came back, throw `new RateException(RateException.Kind.UNEXPECTED_RESPONSE, "The page of My source has changed.")` instead. The kinds are `NETWORK`, `API_KEY`, `QUOTA`, `NOT_ALLOWED`, `UNEXPECTED_RESPONSE` and `CONFIGURATION`.

You can also parse JSON or HTML yourself: Gson (`com.google.gson`) and jsoup (`org.jsoup`) are inside the app, so a plug-in can use them without bundling them.

### A complete example

`src/example/FixedGold.java`:

```java
package example;

import com.sounaks.indiangold.rates.*;
import java.util.*;

public class FixedGold implements RateProvider {
    public String id() { return "fixed-gold"; }
    public String name() { return "Fixed gold"; }
    public String description() { return "Always 100 USD per gram; an example."; }
    public Set<Metal> metals() { return EnumSet.of(Metal.GOLD); }

    public RateSnapshot fetch(FetchContext context) throws RateException {
        Quote gold = Quote.of(Metal.GOLD, 100, 1, MassUnit.GRAM, "USD", context.clock().instant(), id());
        return new RateSnapshot(id(), Map.of(Metal.GOLD, gold), Optional.empty(), context.clock().instant());
    }
}
```

`META-INF/services/com.sounaks.indiangold.rates.RateProvider`, one class name per line:

```
example.FixedGold
```

### Building it

You need JDK 21 and IndianGold's jar to compile against: `indiangold.jar` from a release, from `target/` after `mvn package`, or from an installation (`/opt/indiangold/lib/app/` on Linux, `C:\Program Files\IndianGold\app\` on Windows, `IndianGold.app/Contents/app/` on macOS).

With the JDK tools:

```sh
javac --release 21 -cp indiangold.jar -d classes src/example/FixedGold.java
jar cf fixed-gold.jar -C classes . META-INF
```

With Maven, install the app's jar in your local repository once, and depend on it with `provided` scope (it is not bundled into the plug-in):

```sh
mvn install:install-file -Dfile=indiangold.jar -DgroupId=com.sounaks -DartifactId=indiangold -Dversion=5.0 -Dpackaging=jar
```

```xml
<dependency>
    <groupId>com.sounaks</groupId>
    <artifactId>indiangold</artifactId>
    <version>5.0</version>
    <scope>provided</scope>
</dependency>
```

Put the services file in `src/main/resources/META-INF/services/`. If your plug-in needs other libraries, shade them into its jar.

`PluginsTest` in this repository builds exactly this example and a definition plug-in, and loads them the way the app does.
