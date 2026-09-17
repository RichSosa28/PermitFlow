#!/usr/bin/env sh
set -eu
version=8.11.1
base_dir="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
gradle_home="$base_dir/.gradle-dist/gradle-$version"
if [ ! -x "$gradle_home/bin/gradle" ]; then
  mkdir -p "$base_dir/.gradle-dist"
  curl -fsSL "https://services.gradle.org/distributions/gradle-$version-bin.zip" -o "$base_dir/.gradle-dist/gradle.zip"
  unzip -q "$base_dir/.gradle-dist/gradle.zip" -d "$base_dir/.gradle-dist"
  rm "$base_dir/.gradle-dist/gradle.zip"
fi
exec "$gradle_home/bin/gradle" "$@"
