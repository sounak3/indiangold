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
| `indiangold.jar` | `target/indiangold.jar` (shaded jar with jsoup, built by Maven). It also contains the default `units.dat`. |
| `LICENSE.txt` | `LICENSE.md` (GPL v3, renamed as jpackage's license file) |

Icons come from `extras/` (`IndianGold.ico` for Windows, `IndianGold.png` for Linux, `IndianGold.icns` for macOS) and are passed with `--icon`, so they are not copied into `app/`. They are generated from `extras/IndianGold-icon.png` (1024×1024, drawn by `extras/DrawIcon.java`) with `extras/create-icons.sh`, which needs ImageMagick and python3. Pass `--redraw` to redraw the master image first.

#### The minimal runtime

Each agent builds a runtime with `jlink`, containing the modules `jdeps` finds in the jar plus two it can't see:

| Module | Why |
|---|---|
| `jdk.crypto.ec` | Elliptic-curve TLS. Without it the HTTPS rate fetch fails with `Received close_notify during handshake`. |
| `jdk.localedata` | Number formats for non-English locales (for example `1.234.567,89` in German, Bengali digits), so the installed app formats like `java -jar` on a full JDK. Adds about 11 MB. |

They are set once, in `EXTRA_MODULES` at the top of the `Jenkinsfile`. `jdeps` runs with `--multi-release 21` because jsoup is a multi-release jar, and with `--ignore-missing-deps` because jsoup refers to optional `org.jspecify` annotations that aren't on the classpath. jsoup's `module-info.class` is filtered out of the shaded jar, so jdeps treats it as a plain classpath jar.

#### jpackage settings

| Setting | Value |
|---|---|
| Name | `IndianGold` (Linux package name `indiangold`, installed in `/opt/indiangold`) |
| Vendor | Sounak Choudhury |
| Description | Weight and price calculator for Indian units (ratti, tola, bhori) with metal market rates |
| Windows | `--win-dir-chooser --win-menu --win-menu-group IndianGold --win-shortcut` |
| Linux | `--linux-app-category utils --linux-menu-group "Utility;Calculator" --linux-shortcut` |
| macOS | `--mac-app-category finance --mac-package-name IndianGold` |

The Jenkinsfile is the only packaging definition. There is no jpackage configuration in `pom.xml`.

## Where the app stores its data

Everything the app writes is per user, in `~/.indiangold/` (`%USERPROFILE%\.indiangold\` on Windows):

| File | Contents |
|---|---|
| `units.dat` | Units and their milligram values, the last fetched metal rates, and all settings (currency, taxes, rate bar, decimals) as a Java properties file |
| `units.dat.bak` | The previous version of `units.dat` |

On start, the first usable file of these is loaded:

1. `~/.indiangold/units.dat`
2. `~/.indiangold/units.dat.bak`
3. `units.dat` next to the jar. Versions before 5.0 saved there, so existing settings carry over on the first start; the file is only read, never changed.
4. The default `units.dat` bundled in the jar
5. Two built-in units (troy ounce and pound)

A file that can't be parsed, or has no units (as an interrupted save would leave it), is skipped. Loading never writes a file. Saves always go to `~/.indiangold`: a temp file is written and then swapped in, after the old file is copied to `.bak`, so a crash can't leave a truncated file. Running `java -jar indiangold.jar` from any folder therefore works, including a read-only install folder.

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

## Known issues and recommendations

1. **The precious and base metal rates are broken.** `RateBar` scrapes kitco.com and kitcometals.com. kitco's page now has 6 tables, so `doc.select("table").get(32)` throws `IndexOutOfBoundsException` on the timer thread, and kitcometals.com redirects to kitco with nothing the scraper matches. The USD conversion from x-rates.com still works. The fix needs a new rate source, and the fetch should move off the Swing event thread; it is planned as separate work.
2. **Publishing is manual.** Jenkins only archives the installers. Uploading them to GitHub Releases happens outside Jenkins.
3. **Installers are unsigned.** The MSI has no Authenticode signature (Windows SmartScreen will warn). The DMG is not notarized; open it with right-click → *Open*.
4. **The DMG is Intel-only,** because the `mac` agent has an Intel JDK. Apple Silicon Macs run it through Rosetta.
5. **Test machines need Java 21** to run the jar from the dev pipeline. Each deploy stage prints `java -version`.
6. **Windows 7 is unsupported by Java 21.** The MSI itself installs and runs on Windows 10/11; see desktime's BUILD.md.
7. **What you test isn't exactly what you ship.** `indiangold dev` tests your IDE build, which may include uncommitted changes. The release rebuilds from `master`. The build descriptions record the commit, so you can see whether they match.
8. **The copyright year in the jpackage options is fixed** (`2012-2026`) and needs updating in the `Jenkinsfile` each year.
9. **The license wording differs.** The README and `LICENSE.md` say GPL version 3; the Java source headers say "version 3 or (at your option) any later version".
