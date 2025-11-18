
#!/bin/sh
# Build Galley CLI for multiple Linux targets and generate SHASUMS.
# Usage:
#   scripts/build.sh [version]
# If version is provided, it will be written to the VERSION file.
# Otherwise, version is read from the VERSION file in the project root.

set -eu

ROOT="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
OUT="${OUT:-$ROOT/dist}"
PKG="${PKG:-./cmd/galley}"
VERSION_FILE="$ROOT/VERSION"

# Check if version is provided as argument
if [ -n "${1:-}" ]; then
  VERSION="$1"
  # Add 'v' prefix if not present
  case "$VERSION" in
    v*) ;;
    *) VERSION="v$VERSION" ;;
  esac
  echo "==> Writing new version $VERSION to VERSION file"
  echo "$VERSION" > "$VERSION_FILE"
# Otherwise read from VERSION file
elif [ -f "$VERSION_FILE" ]; then
  VERSION="$(cat "$VERSION_FILE" | xargs)"
  # Add 'v' prefix if not present
  case "$VERSION" in
    v*) ;;
    *) VERSION="v$VERSION" ;;
  esac
else
  echo "Error: VERSION file not found at $VERSION_FILE and no version provided"
  exit 1
fi

echo "==> Building version: $VERSION"

# Clean dist directory before building
if [ -d "$OUT" ]; then
  echo "==> Cleaning dist directory"
  rm -rf "$OUT"
fi
mkdir -p "$OUT"

LDFLAGS="-s -w -X main.Version=$VERSION -X main.commit=$(git -C "$ROOT" rev-parse --short HEAD 2>/dev/null || echo unknown) -X main.date=$(date -u +%Y-%m-%dT%H:%M:%SZ)"

build_one() {
  GOOS="$1"; GOARCH="$2"; GOARM="${3:-}"
  SUFFIX="$GOOS-$GOARCH"
  BIN="galley-$SUFFIX"
  if [ "$GOARCH" = "arm" ] && [ -n "$GOARM" ]; then
    SUFFIX="$GOOS-armv$GOARM"
    BIN="galley-$SUFFIX"
  fi
  echo "--> $SUFFIX"
  echo $PKG
  env CGO_ENABLED=0 GOOS="$GOOS" GOARCH="$GOARCH" GOARM="$GOARM" \
    go build -trimpath -ldflags "$LDFLAGS" -o "$OUT/$BIN" "$PKG"

  # Package tar.gz for CDN
  (
    cd "$OUT"
    TAR="galley-${VERSION}-$SUFFIX.tar.gz"
    chmod 0755 "$BIN"
    tar -czf "$TAR" "$BIN"
  )
}

build_one linux amd64
build_one linux arm64
build_one linux arm 7

# SHASUMS
(
  cd "$OUT"
  rm -f SHA256SUMS
  shasum -a 256 * > SHA256SUMS
  echo "==> Wrote SHA256SUMS"
)

echo "==> Done. Artifacts in $OUT"
