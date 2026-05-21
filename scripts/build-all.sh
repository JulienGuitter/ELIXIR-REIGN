#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${ROOT_DIR}/.env"
DRY_RUN="false"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --env-file)
      ENV_FILE="$2"
      shift 2
      ;;
    --dry-run)
      DRY_RUN="true"
      shift
      ;;
    *)
      echo "Unknown argument: $1"
      echo "Usage: $0 [--env-file /path/to/.env] [--dry-run]"
      exit 1
      ;;
  esac
done

if [[ -f "$ENV_FILE" ]]; then
  set -a
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  set +a
else
  echo "No .env file found at: $ENV_FILE"
  echo "Continuing with defaults and current environment."
fi

GRADLEW="${ROOT_DIR}/gradlew"
OUTPUT_DIR="${OUTPUT_DIR:-${ROOT_DIR}/build/release-artifacts}"
ANDROID_BUILD_TASKS="${ANDROID_BUILD_TASKS:-${ANDROID_BUILD_TASK:-:android:assembleRelease}}"
APP_NAME="${APP_NAME:-ELIXIR-REIGN}"
APP_VERSION="${APP_VERSION:-$(grep -E '^projectVersion=' "${ROOT_DIR}/gradle.properties" | head -n 1 | cut -d'=' -f2-)}"
APP_VERSION="${APP_VERSION:-0.0.0}"

GRADLE_ARGS=(
  "--console=plain"
)

if [[ -n "${ELIXIR_DEFAULT_SERVER_HOST:-}" ]]; then
  GRADLE_ARGS+=("-PelixirDefaultServerHost=${ELIXIR_DEFAULT_SERVER_HOST}")
fi

if [[ -n "${ELIXIR_DEFAULT_SERVER_PORT:-}" ]]; then
  GRADLE_ARGS+=("-PelixirDefaultServerPort=${ELIXIR_DEFAULT_SERVER_PORT}")
fi

TASKS=(
  ":server:jar"
  ":lwjgl3:jarLinux"
  ":lwjgl3:jarWin"
)

read -r -a ANDROID_TASK_ARRAY <<< "$ANDROID_BUILD_TASKS"
if [[ ${#ANDROID_TASK_ARRAY[@]} -eq 0 ]]; then
  ANDROID_TASK_ARRAY=(":android:assembleRelease")
fi
TASKS+=("${ANDROID_TASK_ARRAY[@]}")

if [[ "$DRY_RUN" == "true" ]]; then
  GRADLE_ARGS+=("--dry-run")
fi

echo "Running Gradle tasks: ${TASKS[*]}"
"$GRADLEW" "${GRADLE_ARGS[@]}" "${TASKS[@]}"

if [[ "$DRY_RUN" == "true" ]]; then
  echo "Dry run finished."
  exit 0
fi

mkdir -p "$OUTPUT_DIR"

# Server fat jar
server_jar_file="$(find "${ROOT_DIR}/server/build/libs" -maxdepth 1 -type f -name "*.jar" | sort | tail -n 1 || true)"
if [[ -n "$server_jar_file" ]]; then
  cp -f "$server_jar_file" "${OUTPUT_DIR}/${APP_NAME}-${APP_VERSION}-serv.jar"
fi

# Desktop jars for Linux and Windows
linux_jar_file="$(find "${ROOT_DIR}/lwjgl3/build/libs" -maxdepth 1 -type f -name "*-linux.jar" | sort | tail -n 1 || true)"
if [[ -n "$linux_jar_file" ]]; then
  cp -f "$linux_jar_file" "${OUTPUT_DIR}/${APP_NAME}-${APP_VERSION}-pc-linux.jar"
fi
win_jar_file="$(find "${ROOT_DIR}/lwjgl3/build/libs" -maxdepth 1 -type f -name "*-win.jar" | sort | tail -n 1 || true)"
if [[ -n "$win_jar_file" ]]; then
  cp -f "$win_jar_file" "${OUTPUT_DIR}/${APP_NAME}-${APP_VERSION}-pc-win.jar"
fi

# Android APK(s)
if [[ -d "${ROOT_DIR}/android/build/outputs/apk" ]]; then
  while IFS= read -r apk_file; do
    apk_base_name="$(basename "$apk_file")"
    apk_variant="release"

    if [[ "$apk_base_name" == *"release-unsigned"* ]]; then
      apk_variant="release-unsigned"
    elif [[ "$apk_base_name" == *"debug"* ]]; then
      apk_variant="debug"
    elif [[ "$apk_base_name" == *"release"* ]]; then
      apk_variant="release"
    fi

    cp -f "$apk_file" "${OUTPUT_DIR}/${APP_NAME}-${APP_VERSION}-android-${apk_variant}.apk"
  done < <(find "${ROOT_DIR}/android/build/outputs/apk" -type f -name "*.apk")
fi

echo "Artifacts copied to: $OUTPUT_DIR"
ls -1 "$OUTPUT_DIR" | sed 's/^/ - /'

