#!/usr/bin/env bash
# Builds every platform module that exists and files the resulting jar(s) into
# releases/<version>/<platform>/[<minecraft-version>/]<jar> — a durable, version-and-platform
# archive, since a build tool's own output dir (target/, build/libs/) gets wiped/overwritten on
# every rebuild and only ever holds whatever was built most recently.
#
# Module directory naming convention:
#   <platform>                e.g. "paper"           -> one jar, deliberately spans multiple MC
#                                                         versions (see paper/pom.xml's comment on
#                                                         why one jar covers 1.21.11 through 26.x)
#   <platform>-<mc-version>   e.g. "fabric-1.21.1",      -> platforms whose loader ties a build
#                                  "fabric-26.1",            tightly to one specific MC version's
#                                  "forge-26.1"               mappings/API need a separate module
#                                                            PER targeted MC version, each its own
#                                                            complete build, each producing its own
#                                                            jar filed under its own MC-version
#                                                            subfolder. Add a new module directory
#                                                            per MC version you want to support —
#                                                            this script discovers it automatically,
#                                                            nothing else to configure.
#
# Each module's own build file version is the single source of truth for that jar's version —
# nothing to keep in sync across files by hand.
#
# Usage: ./release.sh   (run from anywhere; paths are resolved relative to this script)

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

build_module() {
  local module_dir="$1"
  local name
  name="$(basename "$module_dir")"

  # Split "platform" or "platform-mcversion" -> platform, mcversion (mcversion empty if absent).
  local platform="$name" mc_version=""
  if [[ "$name" =~ ^([a-z]+)-([0-9].*)$ ]]; then
    platform="${BASH_REMATCH[1]}"
    mc_version="${BASH_REMATCH[2]}"
  fi

  local jar="" version=""

  if [ -f "$module_dir/pom.xml" ]; then
    echo "== building $name (Maven) =="
    (cd "$module_dir" && mvn -q -B package)
    version="$(cd "$module_dir" && mvn -q -B help:evaluate -Dexpression=project.version -DforceStdout)"
    # The shade plugin leaves 3 jars in target/: the real shaded one (plain "<artifact>-<version>.jar"),
    # a duplicate "-shaded.jar", and the pre-shade "original-*.jar" — only the first ships.
    for candidate in "$module_dir"/target/*.jar; do
      [ -e "$candidate" ] || continue
      case "$(basename "$candidate")" in
        original-*|*-shaded.jar) continue ;;
      esac
      jar="$candidate"
      break
    done
  elif [ -f "$module_dir/build.gradle" ] || [ -f "$module_dir/build.gradle.kts" ]; then
    if [ ! -x "$module_dir/gradlew" ]; then
      echo "!! $name/ has no Gradle wrapper yet (run 'gradle wrapper --gradle-version <ver>' in"
      echo "   it once it can configure successfully — see the module's own build.gradle/README"
      echo "   notes if it currently can't; that's usually an external blocker, not this script)."
      return
    fi
    echo "== building $name (Gradle) =="
    (cd "$module_dir" && ./gradlew --no-daemon build)
    version="$(grep -E '^mod_version=' "$module_dir/gradle.properties" | cut -d= -f2)"
    if [ -z "$version" ]; then
      echo "!! couldn't read mod_version from $module_dir/gradle.properties"
      exit 1
    fi
    # Loom leaves the real remapped jar plus a "-sources.jar" (and sometimes a "-dev.jar"
    # intermediate) in build/libs/ — only the plain "<name>-<version>.jar" ships.
    for candidate in "$module_dir"/build/libs/*.jar; do
      [ -e "$candidate" ] || continue
      case "$(basename "$candidate")" in
        *-sources.jar|*-dev.jar) continue ;;
      esac
      jar="$candidate"
      break
    done
  else
    echo "-- skipping $name/ (no pom.xml or build.gradle found)"
    return
  fi

  if [ -z "$jar" ]; then
    echo "!! no jar found in $module_dir after build — something's wrong, check the build output above"
    exit 1
  fi

  local dest="$ROOT_DIR/releases/$version/$platform"
  [ -n "$mc_version" ] && dest="$dest/$mc_version"
  mkdir -p "$dest"
  cp -v "$jar" "$dest/"
  echo "-> $dest"
  echo
}

shopt -s nullglob
for module_dir in "$ROOT_DIR"/*/; do
  module_dir="${module_dir%/}"
  name="$(basename "$module_dir")"
  [[ "$name" =~ ^(paper|fabric|forge)(-[0-9].*)?$ ]] || continue
  build_module "$module_dir"
done

echo "All releases on disk:"
find "$ROOT_DIR/releases" -name "*.jar" 2>/dev/null | sort
