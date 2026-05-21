# ELIXIR-REIGN

A [libGDX](https://libgdx.com/) project generated with [gdx-liftoff](https://github.com/libgdx/gdx-liftoff).

A sample project implementing an isometric pixel-art game with procedural terrain. Includes launchers for each platform and a small but winnable game.

## Platforms

- `core`: Main module with the application logic shared by all platforms.
- `lwjgl3`: Primary desktop platform using LWJGL3; was called 'desktop' in older docs.
- `android`: Android mobile platform. Needs Android SDK.
- `server`: A separate application without access to the `core` module.
- `shared`: A common module shared by `core` and `server` platforms.

## Gradle

This project uses [Gradle](https://gradle.org/) to manage dependencies.
The Gradle wrapper was included, so you can run Gradle tasks using `gradlew.bat` or `./gradlew` commands.
Useful Gradle tasks and flags:

- `--continue`: when using this flag, errors will not stop the tasks from running.
- `--daemon`: thanks to this flag, Gradle daemon will be used to run chosen tasks.
- `--offline`: when using this flag, cached dependency archives will be used.
- `--refresh-dependencies`: this flag forces validation of all dependencies. Useful for snapshot versions.
- `android:lint`: performs Android project validation.
- `build`: builds sources and archives of every project.
- `cleanEclipse`: removes Eclipse project data.
- `cleanIdea`: removes IntelliJ project data.
- `clean`: removes `build` folders, which store compiled classes and built archives.
- `eclipse`: generates Eclipse project data.
- `idea`: generates IntelliJ project data.
- `lwjgl3:jar`: builds application's runnable jar, which can be found at `lwjgl3/build/libs`.
- `lwjgl3:run`: starts the application.
- `server:run`: runs the server application.
- `test`: runs unit tests (if any).

## Unified Build Script

To build server + desktop (Linux/Windows) + Android APK in one command:

1. Copy `.env.example` to `.env` and adjust values.
2. Run:

```bash
./scripts/build-all.sh --version 1.0.0
```

By default it runs:

- `:server:jar`
- `:lwjgl3:jarLinux`
- `:lwjgl3:jarWin`
- `:android:assembleRelease`
- `:imc-cret:assembleRelease` (or tasks set in `.env` with `ANDROID_BUILD_TASKS`)

Artifacts are copied to `build/release-artifacts`.

Default copied names:

- `ELIXIR-REIGN-<version>-serv.jar`
- `ELIXIR-REIGN-<version>-pc-linux.jar`
- `ELIXIR-REIGN-<version>-pc-win.jar`
- `ELIXIR-REIGN-<version>-android-debug.apk` (if built)
- `ELIXIR-REIGN-<version>-android-release.apk` or `ELIXIR-REIGN-<version>-android-release-unsigned.apk`
- `IMC-CRET-<version>-imc-cret-debug.apk` (if built)
- `IMC-CRET-<version>-imc-cret-release.apk` or `IMC-CRET-<version>-imc-cret-release-unsigned.apk`

Optional checks:

```bash
./scripts/build-all.sh --version 1.0.0 --dry-run
./scripts/build-all.sh --version 1.0.0 --env-file /path/to/.env
```

Note that most tasks that are not specific to a single project can be run with `name:` prefix, where the `name` should be replaced with the ID of a specific project.
For example, `core:clean` removes `build` folder only from the `core` project.
