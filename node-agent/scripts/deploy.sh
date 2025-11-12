#!/bin/sh
# Deploy Galley CLI artifacts to resources directory.
# Usage:
#   scripts/deploy.sh

set -eu

ROOT="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
DIST="${DIST:-$ROOT/dist}"
GETROOT_BASE="${GETROOT_BASE:-$ROOT/../src/main/resources/getroot}"
POM_XML="$ROOT/../pom.xml"

# Read version from pom.xml
if [ -f "$POM_XML" ]; then
  GALLEY_VERSION="$(grep -m1 '<version>' "$POM_XML" | sed 's/.*<version>\(.*\)<\/version>.*/\1/' | xargs)"
  # Add 'v' prefix if not present
  case "$GALLEY_VERSION" in
    v*) ;;
    *) GALLEY_VERSION="v$GALLEY_VERSION" ;;
  esac
else
  GALLEY_VERSION="${GALLEY_VERSION:-v0.1.0}"
fi

TARGET="$GETROOT_BASE/$GALLEY_VERSION"

if [ ! -d "$DIST" ]; then
  echo "Error: dist directory not found at $DIST"
  echo "Run scripts/build.sh first."
  exit 1
fi

echo "==> Deploying artifacts for version $GALLEY_VERSION"
echo "    from: $DIST"
echo "    to:   $TARGET"
mkdir -p "$TARGET"

# Copy all files from dist to target version directory
cp -v "$DIST"/* "$TARGET/"

# Update install.sh with current version and copy to getroot base
sed "s/GALLEY_VERSION=\"\${GALLEY_VERSION:-v[^}]*}\"/GALLEY_VERSION=\"\${GALLEY_VERSION:-$GALLEY_VERSION}\"/" \
  "$ROOT/install.sh" > "$GETROOT_BASE/install.sh"
chmod +x "$GETROOT_BASE/install.sh"

# Also copy install.sh to version directory
cp -v "$GETROOT_BASE/install.sh" "$TARGET/"

echo "==> Done. Artifacts deployed:"
echo "    - Binaries to: $TARGET"
echo "    - install.sh to: $GETROOT_BASE"
