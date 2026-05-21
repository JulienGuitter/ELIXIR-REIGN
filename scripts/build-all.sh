#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${ROOT_DIR}/.env"
DRY_RUN="false"
BUILD_VERSION=""

usage() {
  echo "Usage: $0 --version <version> [--env-file /path/to/.env] [--dry-run]"
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --version)
      if [[ $# -lt 2 || "$2" == --* ]]; then
        echo "Missing value for --version"
        usage
        exit 1
      fi
      BUILD_VERSION="$2"
      shift 2
      ;;
    --env-file)
      if [[ $# -lt 2 || "$2" == --* ]]; then
        echo "Missing value for --env-file"
        usage
        exit 1
      fi
      ENV_FILE="$2"
      shift 2
      ;;
    --dry-run)
      DRY_RUN="true"
      shift
      ;;
    *)
      echo "Unknown argument: $1"
      usage
      exit 1
      ;;
  esac
done

if [[ -z "$BUILD_VERSION" ]]; then
  echo "Missing required argument: --version"
  usage
  exit 1
fi

if [[ ! "$BUILD_VERSION" =~ ^[0-9A-Za-z][0-9A-Za-z._+-]*$ ]]; then
  echo "Invalid version: $BUILD_VERSION"
  echo "Allowed characters: letters, digits, dot, underscore, plus and hyphen."
  exit 1
fi

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
APP_NAME="${APP_NAME:-ELIXIR-REIGN}"
IMC_APP_NAME="${IMC_APP_NAME:-IMC-CRET}"
APP_VERSION="$BUILD_VERSION"

update_gradle_property() {
  local key="$1"
  local value="$2"
  local file="${ROOT_DIR}/gradle.properties"

  PROPERTY_KEY="$key" PROPERTY_VALUE="$value" perl -0pi -e '
    BEGIN {
      $key = $ENV{"PROPERTY_KEY"};
      $value = $ENV{"PROPERTY_VALUE"};
    }
    if (!s/^\Q$key\E=.*/$key=$value/m) {
      $_ .= "\n$key=$value\n";
    }
  ' "$file"
}

if [[ "$DRY_RUN" == "true" ]]; then
  echo "Dry run: would set projectVersion=${APP_VERSION} in gradle.properties"
else
  update_gradle_property "projectVersion" "$APP_VERSION"
fi

GRADLE_ARGS=(
  "--console=plain"
  "-PprojectVersion=${APP_VERSION}"
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
  ":android:assembleDebug"
  ":android:assembleRelease"
  ":imc-cret:assembleDebug"
  ":imc-cret:assembleRelease"
)

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

copy_android_apks() {
  local module_dir="$1"
  local artifact_name="$2"
  local artifact_suffix="$3"

  if [[ ! -d "${ROOT_DIR}/${module_dir}/build/outputs/apk" ]]; then
    return
  fi

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

    cp -f "$apk_file" "${OUTPUT_DIR}/${artifact_name}-${APP_VERSION}-${artifact_suffix}-${apk_variant}.apk"
  done < <(find "${ROOT_DIR}/${module_dir}/build/outputs/apk" -type f -name "*.apk")
}

copy_android_apks "android" "$APP_NAME" "android"
copy_android_apks "imc-cret" "$IMC_APP_NAME" "imc-cret"

echo "Artifacts copied to: $OUTPUT_DIR"
ls -1 "$OUTPUT_DIR" | sed 's/^/ - /'
