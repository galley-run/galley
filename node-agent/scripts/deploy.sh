#!/bin/sh
# Deploy Galley CLI artifacts to resources directory.
# Usage:
#   scripts/deploy.sh [-y]

set -eu

# Parse command line arguments
YES_TO_ALL=false
while [ $# -gt 0 ]; do
  case "$1" in
    -y|--yes)
      YES_TO_ALL=true
      shift
      ;;
    *)
      echo "Unknown option: $1"
      echo "Usage: $0 [-y|--yes]"
      exit 1
      ;;
  esac
done

ROOT="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
DIST="${DIST:-$ROOT/dist}"
GETROOT="${GETROOT:-$ROOT/../src/main/resources/getroot}"
GETROOT_BIN="$GETROOT/bin"
VERSION_FILE="$ROOT/VERSION"

# Read version from VERSION file
if [ -f "$VERSION_FILE" ]; then
  GALLEY_VERSION="$(cat "$VERSION_FILE" | xargs)"
  # Add 'v' prefix if not present
  case "$GALLEY_VERSION" in
    v*) ;;
    *) GALLEY_VERSION="v$GALLEY_VERSION" ;;
  esac
else
  echo "Error: VERSION file not found at $VERSION_FILE"
  exit 1
fi

TARGET="$GETROOT_BIN/$GALLEY_VERSION"

# Ask if user wants to build/rebuild
if [ "$YES_TO_ALL" = true ]; then
  answer="y"
  new_version=""
else
  if [ ! -d "$DIST" ]; then
    echo "Error: dist directory not found at $DIST"
    printf "Do you want to build now? [y/N] "
  else
    printf "Do you want to (re)build before deploying? [y/N] "
  fi
  read -r answer
fi

case "$answer" in
  [Yy]*)
    if [ "$YES_TO_ALL" = true ]; then
      new_version=""
    else
      printf "Enter new version (leave empty to keep $GALLEY_VERSION): "
      read -r new_version
    fi
    if [ -n "$new_version" ]; then
      "$ROOT/scripts/build.sh" "$new_version"
      # Re-read version after build
      GALLEY_VERSION="$(cat "$VERSION_FILE" | xargs)"
      TARGET="$GETROOT_BIN/$GALLEY_VERSION"
    else
      "$ROOT/scripts/build.sh"
    fi
    ;;
  *)
    if [ ! -d "$DIST" ]; then
      echo "Cancelled."
      exit 1
    fi
    ;;
esac

echo "==> Deploying artifacts for version $GALLEY_VERSION"
echo "    from: $DIST"
echo "    to:   $TARGET"
mkdir -p "$TARGET"

# Copy all files from dist to target version directory
cp -v "$DIST"/* "$TARGET/"

# Update install.sh with current version and copy to getroot (not getroot/bin)
sed "s/GALLEY_VERSION=\"\${GALLEY_VERSION:-v[^}]*}\"/GALLEY_VERSION=\"\${GALLEY_VERSION:-$GALLEY_VERSION}\"/" \
  "$ROOT/install.sh" > "$GETROOT/install.sh"
chmod +x "$GETROOT/install.sh"

# Copy VERSION file to getroot as 'latest'
cp -v "$VERSION_FILE" "$GETROOT/latest"

# Create/update 'latest' symlink to current version in getroot/bin
LATEST_LINK="$GETROOT_BIN/latest"
if [ -L "$LATEST_LINK" ]; then
  rm "$LATEST_LINK"
fi
ln -s "$GALLEY_VERSION" "$LATEST_LINK"
echo "    - Updated 'latest' symlink to $GALLEY_VERSION"

echo "==> Done. Artifacts deployed:"
echo "    - Binaries to: $TARGET"
echo "    - install.sh to: $GETROOT"
echo "    - VERSION file to: $GETROOT/latest"
echo "    - Latest symlink: $LATEST_LINK -> $GALLEY_VERSION"
