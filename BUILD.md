# Building IndianGold

This document describes how IndianGold is built, tested and packaged with Jenkins: the two pipelines, what goes into the installers, how the app stores its data, and how the jobs and the dev trigger are set up.

The build server and the three agents are shared with DeskStop (`desktime`). Their setup (the Jenkins container, agent connections, secrets rotation, backups) is documented once, in [desktime's BUILD.md](https://github.com/sounak3/desktime/blob/main/BUILD.md), and is not repeated here.

## Pipelines at a glance

| Jenkins job | Pipeline file | Purpose | Trigger | Output |
|---|---|---|---|---|
| `indiangold` | [`Jenkinsfile`](Jenkinsfile) | **Release.** Builds and tests the jar from source, then packages native installers on each OS. | Manual: *Build Now* | `IndianGold-<ver>.msi`, `indiangold_<ver>-release_amd64.deb`, `IndianGold-<ver>.dmg`, `indiangold.jar` (archived in Jenkins) |
| `indiangold dev` | [`Jenkinsfile.dev`](Jenkinsfile.dev) | **CI / testing.** Copies the jar your IDE just built onto all three test machines, then runs the unit tests. No installers. | Automatic, whenever `target/indiangold.jar` changes on dell5558 | `indiangold_latest.jar` on the Desktop of lin, mac and win (also archived in Jenkins) |

Typical workflow:

1. Build in the IDE (`mvn package`). This writes `target/indiangold.jar`.
2. `indiangold dev` runs automatically. It drops `indiangold_latest.jar` on the Desktop of all three machines and runs the unit tests on a snapshot of your working copy.
3. Test the jar on Linux, macOS and Windows.
4. To release a new version, set `<version>` in `pom.xml` (for example `5.1-SNAPSHOT` → installers versioned `5.1`). Commit and push to `master`.
5. Run `indiangold` (release). Download the installers from the build page and upload them to GitHub Releases.

Each build's description shows what it was built from:

- **Release:** `v5.0 @ 2e1154f0`, meaning the version and the commit it was built from.
- **Dev:** `2e1154f`, or `2e1154f + uncommitted changes` if the IDE build included work that wasn't committed yet.

### Version

`pom.xml` is the only place the version is set.

- The release pipeline reads `project.version`, removes `-SNAPSHOT`, and uses the result as the installer version. The MSI format accepts only numeric versions (`5.0`, `5.1.2`), and the pipeline stops with an error otherwise.
- The window title (`Indian Gold v5.0`, or `IGv5` with the calculator hidden) comes from the same version: Maven writes it into `indiangold-version.properties` inside the jar. Running the classes without Maven's resource filtering shows `dev` instead.

### Release pipeline flow

```
Build jar (lin)                         Package (parallel)
  checkout scm                     ┌──> Windows (win): jlink → jpackage --type msi (WiX 3.14)
  version from pom.xml             │
  mvn clean verify (tests) ─stash─>├──> Ubuntu  (lin): jlink → jpackage --type deb
  app/ + extras/ icons             └──> Mac OS  (mac): jlink → jpackage --type dmg
```

Only `lin` checks out the repository. `win` and `mac` receive the jar, the license and the icons through `stash`/`unstash`, so they need neither git nor GitHub access. If a unit test fails, the release stops before packaging.

The `app/` folder is jpackage's `--input`, and everything in it ships inside the installer:

| File in installer | Source in repo |
|---|---|
| `indiangold.jar` | `target/indiangold.jar` (shaded jar with jsoup and Gson, built by Maven). It also contains the default `units.dat`. |
| `LICENSE.txt` | `LICENSE.md` (GPL v3, renamed as jpackage's license file) |

Icons come from `extras/` (`IndianGold.ico` for Windows, `IndianGold.png` for Linux, `IndianGold.icns` for macOS) and are passed with `--icon`, so they are not copied into `app/`. They are generated from `extras/IndianGold-icon.png` (1024×1024, drawn by `extras/DrawIcon.java`) with `extras/create-icons.sh`, which needs ImageMagick and python3. Pass `--redraw` to redraw the master image first.

#### The minimal runtime

Each agent builds a runtime with `jlink`, containing the modules `jdeps` finds in the jar (currently `java.base`, `java.desktop`, `java.net.http` and `java.sql`, the last for Gson) plus two it can't see:

| Module | Why |
|---|---|
| `jdk.crypto.ec` | Elliptic-curve TLS. Without it HTTPS requests to the rate sources fail with `Received close_notify during handshake`. |
| `jdk.localedata` | Number formats for non-English locales (for example `1.234.567,89` in German, Bengali digits), so the installed app formats like `java -jar` on a full JDK. Adds about 11 MB. |

They are set once, in `EXTRA_MODULES` at the top of the `Jenkinsfile`. `jdeps` runs with `--multi-release 21` because jsoup is a multi-release jar, and with `--ignore-missing-deps` because jsoup refers to optional `org.jspecify` annotations that aren't on the classpath. jsoup's `module-info.class` is filtered out of the shaded jar, so jdeps treats it as a plain classpath jar.

#### jpackage settings

| Setting | Value |
|---|---|
| Name | `IndianGold` (Linux package name `indiangold`, installed in `/opt/indiangold`) |
| Vendor | Sounak Choudhury |
| Description | Weight and price calculator for Indian units (ratti, tola, bhori) with metal market rates |
| Windows | `--win-dir-chooser --win-menu --win-menu-group IndianGold --win-shortcut` |
| Linux | `--linux-app-category utils --linux-menu-group "Utility;Calculator" --linux-shortcut --resource-dir extras/linux --java-options "--add-opens=java.desktop/sun.awt.X11=ALL-UNNAMED"`: the desktop entry from `extras/linux/IndianGold.desktop` adds `StartupWMClass=IndianGold`, and the option lets the app set that window class (see *Dock and taskbar* below) |
| macOS | `--mac-app-category finance --mac-package-name IndianGold` |

The Jenkinsfile is the only packaging definition. There is no jpackage configuration in `pom.xml`.

## Where the app stores its data

Everything the app writes is per user, in `~/.indiangold/` (`%USERPROFILE%\.indiangold\` on Windows):

| File | Contents |
|---|---|
| `units.dat` | Units and their milligram values and all settings as a Java properties file: country, currency, taxes, display options, the units rates are shown in, gold purity rows, the rate sources (order, on/off) and each source's own settings, including **API keys in plain text** (the file is readable only by the user) |
| `units.dat.bak` | The previous version of `units.dat` |
| `rates.properties` (+ `.bak`) | The last prices and exchange rates from each source, errors, and request counts for sources with a monthly quota, so the rate bar works offline and at start |
| `plugins/*.jar` | Optional plug-ins with more rate sources (see [docs/PLUGINS.md](docs/PLUGINS.md)); loaded at start and by "Reload plug-ins" |
| `indiangold.png` | The icon for the Linux desktop entry below |

The calculator's last weight, rate, units, making charge and discount are saved in `units.dat` (`$last.*`) when the window closes, and restored at start.

On Linux, when the plain jar runs (not an installed package), the app also writes `~/.local/share/applications/indiangold.desktop`, pointing at the running jar, so docks can show the window and the app can be pinned.

On start, the first usable file of these is loaded:

1. `~/.indiangold/units.dat`
2. `~/.indiangold/units.dat.bak`
3. `units.dat` next to the jar. Versions before 5.0 saved there, so existing settings carry over on the first start; the file is only read, never changed.
4. The default `units.dat` bundled in the jar
5. Two built-in units (troy ounce and pound)

Version 4.x kept the last metal prices in `units.dat` (`@gold=…` in USD per troy ounce, base metals per pound); on the first start of 5.0 they move to `rates.properties`.

A file that can't be parsed, or has no units (as an interrupted save would leave it), is skipped. Loading never writes a file. Saves always go to `~/.indiangold`: a temp file is written and then swapped in, after the old file is copied to `.bak`, so a crash can't leave a truncated file. Running `java -jar indiangold.jar` from any folder therefore works, including a read-only install folder.

## Market rate sources

The rate bar gets its prices from pluggable sources, in `com.sounaks.indiangold.rates`:

| Source | Kind | Key | Metals | Notes |
|---|---|---|---|---|
| Gold-API.com | JSON API | none | gold, silver, platinum, palladium, copper (COMEX, per lb) | live; one request per metal, so at most every 5 minutes; "no rate limiting", commercial use allowed |
| Westmetall | web page | none | LME copper, aluminium, nickel, zinc, lead, tin | official LME cash prices, published once a day; read at most hourly where robots.txt allows; the user accepts a disclaimer first |
| Currency-API (fawazahmed0) | JSON API | none | gold, silver, platinum, palladium + exchange rates for 300+ currencies | daily; jsDelivr with a Cloudflare Pages mirror; public domain |
| Metals.Dev | JSON API | the user's own | all 9 metals + 170 currencies in one request | free plan: 100 requests a month, one account per person; scheduled at 10:30 and 16:30 by default |
| Manual entry | Java | – | any | prices typed in with a date |

For each metal the first enabled source (in the user's order) with a price less than 48 hours old wins. Sources with a monthly quota run only at their scheduled times (up to three a day, with one catch-up at start), and today's share is the remaining quota divided by the days left in the month; manual refreshes show it and ask first. Gold rows such as 22K are pure gold × fineness, so they work with every source.

**Adding a source:**

- *As a plug-in* (by anyone, without a new release): a jar in `~/.indiangold/plugins/` with definition files (`type=json-api` reads values with JSON pointers, `type=web-page` with CSS selectors) and/or Java classes implementing `RateProvider`. The developer guide is [docs/PLUGINS.md](docs/PLUGINS.md); the app links to it from Settings → Market rates ("How to write a plug-in"), and `PluginsTest` builds both kinds the way the guide describes.
- *Built in:* add a definition to `src/main/resources/providers/` and to `providers/index.list` (the order is the default priority), or a Java class listed in `src/main/resources/META-INF/services/com.sounaks.indiangold.rates.RateProvider`, plus a test against a recorded response in `ProvidersTest`.

Rules every source follows: web pages are fetched at most once an hour, only where robots.txt allows it, and the User-Agent is always `IndianGold/<version> (+https://github.com/sounak3/indiangold)`. **Never make a source pose as a browser** to get past a site's blocking; goodreturns.in was left out for that reason. kitco.com and goldprice.org were left out because their terms forbid automated access.

Country defaults (currency, the unit for each metal group, gold rows, taxes) come from `src/main/resources/country-defaults.csv`; legacy and non-ISO currency codes are mapped in `currency-migrations.properties`.

## Parameters

| Job | Parameter | Default | Notes |
|---|---|---|---|
| `indiangold dev` | `JAR_PATH` | `/home/sounak/Documents/NetBeansProjects/IndianGold/target/indiangold.jar` | The IDE-built jar on the lin host. The working copy two folders above it is used for the commit info and the unit tests. |

The release job has no parameters. Its version comes from `pom.xml`.

## Build infrastructure

The same as for desktime; see [desktime's BUILD.md](https://github.com/sounak3/desktime/blob/main/BUILD.md) for the controller (`docker run` command, root URL, `--add-host` entries), the agents and how they connect, rotating agent secrets, and backups.

| Label | Host | Used by |
|---|---|---|
| `lin` | dell5558 (192.168.1.14), also runs the Jenkins controller | release: Build jar, Ubuntu; dev: Collect jar, lin, Unit tests |
| `mac` | macos.local (192.168.1.16), Intel, macOS 13 | release: Mac OS; dev: mac |
| `win` | winos.local (192.168.1.17), Windows 7 VM | release: Windows; dev: win |

### Software required on each agent

All three agents need **JDK 21 or newer** (`maven.compiler.release=21`; `jlink --compress=zip-6` exists only from JDK 21).

| Agent | Needs |
|---|---|
| `lin` | JDK 21 (`java`, `jdeps`, `jlink`, `jpackage`), Maven, git, `dpkg-deb` and `fakeroot` (for `jpackage --type deb`), `unzip`, `sha256sum`, `rsync`, `~/Desktop` |
| `win` | JDK 21 with `JAVA_HOME` set system-wide (the stages call `"%JAVA_HOME%\bin\jpackage"` etc.), `java` on `PATH`, **.NET Framework 4.8** for WiX 3.14, internet access to github.com (WiX is downloaded on every release), `certutil` (built in), `%USERPROFILE%\Desktop` |
| `mac` | JDK 21 on the non-interactive SSH `PATH`, `hdiutil` and `shasum` (built in), system sleep off (`sudo pmset -a sleep 0`), `~/Desktop` |

**Comodo Internet Security** on the Windows VM blocks unknown programs and `.bat` files. A Windows step that hangs with no output is usually Comodo; allow the folders `c:\jenkins\` and the JDK, not single files. Details are in desktime's BUILD.md.

## Jenkins configuration

### Plugins the pipelines depend on

Pipeline and Pipeline: Declarative, Git, JUnit, Workspace Cleanup (`cleanWs`), File Operations (WiX download and unzip on Windows), Pipeline: Basic Steps (`stash`, `archiveArtifacts`, `timeout`) and SSH Build Agents (for `mac`). All are already installed for desktime.

### Job setup

Both jobs read their pipeline from the repository:

*Configure → Pipeline → Definition: **Pipeline script from SCM*** → SCM: Git → Repository URL `https://github.com/sounak3/indiangold.git`, Credentials: *none* (public repository) → Branch `*/master` → Script Path `Jenkinsfile` (release) or `Jenkinsfile.dev` (dev) → *Lightweight checkout* ✓.

`indiangold dev` has a 15-second quiet period, set in `Jenkinsfile.dev`.

### Dev trigger (on dell5558)

A systemd user path unit watches the jar and asks Jenkins to start `indiangold dev`:

| File | Role |
|---|---|
| `~/.config/systemd/user/indiangold-dev-trigger.path` | watches `target/indiangold.jar` |
| `~/.config/systemd/user/indiangold-dev-trigger.service` | runs `curl -X POST …/job/indiangold%20dev/buildWithParameters` (a job with parameters rejects plain `/build` with HTTP 400); skipped if the jar doesn't exist, for example right after `mvn clean` |
| `~/.config/indiangold-dev-trigger.env` (mode 600) | `JENKINS_USER` and `JENKINS_TOKEN` (a Jenkins API token; the same one desktime's trigger uses) |

To check that it works: `systemctl --user start indiangold-dev-trigger.service`, then `journalctl --user -u indiangold-dev-trigger.service -n 20`.

A single Maven build writes the jar more than once (the shade plugin replaces it). The 15-second quiet period merges those triggers into one build. Each deploy stage times out after 5 minutes, so a powered-off VM only fails its own branch. Each stage prints the SHA-256 of the copy on the Desktop, so you can check from the log that all three machines got the same jar.

Because of this trigger, don't run `mvn package` in the working copy unless you want the jar pushed to all three machines. Experiment in a copy instead (`rsync` without `target` and `.git`).

## Dock and taskbar

Linux docks show a running window only when they can match its window class to a `.desktop` file. Java names the class after the main class (`com-sounaks-indiangold-IndianGold`), so the window used to be missing from the dock, also when minimized. `DesktopIntegration` now sets the class to `IndianGold` (this needs `java.desktop/sun.awt.X11` opened: the jar's manifest has `Add-Opens`, the installed app gets `--add-opens`). The installed desktop entry says `StartupWMClass=IndianGold`, and plain-jar runs write their own entry (see *Where the app stores its data*). Windows carry the icon in 16–256 px; on macOS the dock icon is set with `java.awt.Taskbar`.

Some docks only show pinned apps (on dell5558, Dash2Dock Lite did not show GNOME Calculator either while it ran); there, pin IndianGold from the app grid.

**Never use `java.awt.Desktop` or `java.awt.Taskbar` on Linux.** They load GTK into the app's process; started from a snap-packaged program (e.g. VS Code), GTK then loads the snap's libraries and the app dies with `symbol lookup error ... __libc_pthread_init`. `Links` opens pages, folders and mail with `xdg-open` in a separate process instead, without the snap's environment variables.

## Known issues and recommendations

1. **Free sources can change or disappear.** Gold-API.com and Currency-API are provided "as is", and a web page source breaks when the site is redesigned. The rate bar keeps the last prices (greyed after 48 hours), the source's status shows the error, and other sources fill in; a broken definition can be fixed in the editor without a new release.
2. **Publishing is manual.** Jenkins only archives the installers. Uploading them to GitHub Releases happens outside Jenkins.
3. **Installers are unsigned.** The MSI has no Authenticode signature (Windows SmartScreen will warn). The DMG is not notarized; open it with right-click → *Open*.
4. **The DMG is Intel-only,** because the `mac` agent has an Intel JDK. Apple Silicon Macs run it through Rosetta.
5. **Test machines need Java 21** to run the jar from the dev pipeline. Each deploy stage prints `java -version`.
6. **Windows 7 is unsupported by Java 21.** The MSI itself installs and runs on Windows 10/11; see desktime's BUILD.md.
7. **What you test isn't exactly what you ship.** `indiangold dev` tests your IDE build, which may include uncommitted changes. The release rebuilds from `master`. The build descriptions record the commit, so you can see whether they match.
8. **The copyright year in the jpackage options is fixed** (`2012-2026`) and needs updating in the `Jenkinsfile` each year.
9. **The license wording differs.** The README and `LICENSE.md` say GPL version 3; the Java source headers say "version 3 or (at your option) any later version".
