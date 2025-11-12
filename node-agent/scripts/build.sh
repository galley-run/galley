
#!/bin/sh
# Build Galley CLI for multiple Linux targets and generate SHASUMS.
# Usage:
#   scripts/build.sh [version]
# If version is omitted, we derive it from `git describe --tags --always`.

set -eu

ROOT="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
OUT="${OUT:-$ROOT/dist}"
PKG="${PKG:-./cmd/galley}"
VERSION="${1:-${VERSION:-}}"

if [ -z "${VERSION}" ]; then
  if command -v git >/dev/null 2>&1 && git -C "$ROOT" rev-parse --git-dir >/dev/null 2>&1; then
    VERSION="$(git -C "$ROOT" describe --tags --always --dirty 2>/dev/null || echo v0.0.0)"
  else
    VERSION="v0.0.0"
  fi
fi

echo "==> Building version: $VERSION"
mkdir -p "$OUT"

LDFLAGS="-s -w -X main.version=$VERSION -X main.commit=$(git -C "$ROOT" rev-parse --short HEAD 2>/dev/null || echo unknown) -X main.date=$(date -u +%Y-%m-%dT%H:%M:%SZ)"

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
