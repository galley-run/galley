
#!/bin/sh
# Galley install.sh - POSIX sh
# - Perform security updates, force reboot if needed, auto-resume
# - Download/verify galley CLI binary (Go static) to /usr/local/bin/galley
# - Prepare systemd units when necessary
set -eu

GALLEY_BIN="/usr/local/bin/galley"
GALLEY_URL="${GALLEY_URL:-https://cloud.galley.run}"
GALLEY_STATE_DIR="/var/lib/galley"
GALLEY_RESUME_ARGS_FILE="$GALLEY_STATE_DIR/resume.args"
GALLEY_RESUME_WRAPPER="/usr/local/bin/galley-install-resume"
GALLEY_RESUME_UNIT="galley-install-resume.service"
GALLEY_VERSION="${GALLEY_VERSION:-v0.1.0}"
TMPDIR="${TMPDIR:-/tmp}"

# Try to read config file for download_base if it exists
read_config_download_base() {
  CONFIG_FILE="$HOME/.galley/config"
  if [ -f "$CONFIG_FILE" ]; then
    # Simple YAML parser for download_base
    grep "^download_base:" "$CONFIG_FILE" | sed 's/download_base:[[:space:]]*//' | tr -d '"' | xargs
  fi
}

# Check SUDO_USER's home for config if running as root
if [ "$(id -u)" -eq 0 ] && [ -n "${SUDO_USER:-}" ]; then
  USER_HOME=$(eval echo "~$SUDO_USER")
  CONFIG_FILE="$USER_HOME/.galley/config"
  if [ -f "$CONFIG_FILE" ]; then
    CONFIG_DOWNLOAD_BASE=$(grep "^download_base:" "$CONFIG_FILE" | sed 's/download_base:[[:space:]]*//' | tr -d '"' | xargs)
    CONFIG_CLIENT_URL=$(grep "^client_url:" "$CONFIG_FILE" | sed 's/client_url:[[:space:]]*//' | tr -d '"' | xargs)
  fi
elif [ -f "$HOME/.galley/config" ]; then
  CONFIG_DOWNLOAD_BASE=$(grep "^download_base:" "$HOME/.galley/config" | sed 's/download_base:[[:space:]]*//' | tr -d '"' | xargs)
  CONFIG_CLIENT_URL=$(grep "^client_url:" "$HOME/.galley/config" | sed 's/client_url:[[:space:]]*//' | tr -d '"' | xargs)
fi

# Priority: env var > config file > default
GALLEY_DOWNLOAD_BASE="${GALLEY_DOWNLOAD_BASE:-${CONFIG_DOWNLOAD_BASE:-https://get.galley.run}}"
GALLEY_CLIENT_URL="${GALLEY_CLIENT_URL:-${CONFIG_CLIENT_URL:-https://cloud.galley.run}}"

require_root_or_sudo() {
  if [ "$(id -u)" -ne 0 ]; then
    if command -v sudo >/dev/null 2>&1; then
      exec sudo -E sh "$0" "$@"
    else
      echo "Root privileges required. Install sudo or re-run as root." >&2
      exit 1
    fi
  fi
}

arch_triplet() {
  UNAME_M="$(uname -m)"
  case "$UNAME_M" in
    x86_64|amd64) echo "linux-amd64" ;;
    aarch64|arm64) echo "linux-arm64" ;;
    armv7l|armv7) echo "linux-armv7" ;;
    *) echo "unsupported" ;;
  esac
}

download_cli() {
  T="$(arch_triplet)"
  if [ "$T" = "unsupported" ]; then
    echo "Unsupported architecture $(uname -m)" >&2
    exit 1
  fi
  URL="$GALLEY_DOWNLOAD_BASE/bin/$GALLEY_VERSION/galley-$T"
  TMP="$TMPDIR/galley.$$"
  echo "Downloading galley CLI from $URL ..."
  curl -fL "$URL" -o "$TMP"
  chmod 0755 "$TMP"
  mv "$TMP" "$GALLEY_BIN"
}

main() {
  require_root_or_sudo "$@"

  # Ensure CLI is installed
  if ! command -v "$GALLEY_BIN" >/dev/null 2>&1; then
    download_cli
  fi

  echo "============================================================================="
  echo ""
  echo "Galley CLI installed at $GALLEY_BIN."
  echo ""
  echo "----------------------------------------------------------------------------"
  echo ""
  echo "Next steps:"
  echo ""
  echo "Start preparing this node for Galley:"
  echo "  sudo galley node prepare"
  echo ""
  echo "or ask the Galley Node Agent what is possible by executing just:"
  echo "  galley"
  echo ""
  echo "============================================================================="
  echo ""
}

main "$@"
