
#!/bin/sh
# Galley install.sh - POSIX sh
# - Perform security updates, force reboot if needed, auto-resume
# - Download/verify galley CLI binary (Go static) to /usr/local/bin/galley
# - Prepare systemd units when necessary
set -eu

GALLEY_BIN="/usr/local/bin/galley"
GALLEY_URL="${GALLEY_URL:-https://galley.run}"
GALLEY_STATE_DIR="/var/lib/galley"
GALLEY_RESUME_ARGS_FILE="$GALLEY_STATE_DIR/resume.args"
GALLEY_RESUME_WRAPPER="/usr/local/bin/galley-install-resume"
GALLEY_RESUME_UNIT="galley-install-resume.service"
GALLEY_VERSION="${GALLEY_VERSION:-v0.1.0-alpha-2}"
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
  fi
elif [ -f "$HOME/.galley/config" ]; then
  CONFIG_DOWNLOAD_BASE=$(grep "^download_base:" "$HOME/.galley/config" | sed 's/download_base:[[:space:]]*//' | tr -d '"' | xargs)
fi

# Priority: env var > config file > default
GALLEY_DOWNLOAD_BASE="${GALLEY_DOWNLOAD_BASE:-${CONFIG_DOWNLOAD_BASE:-https://get.galley.run}}"

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

detect_distro() {
  DISTRO_FAMILY=""
  PKG_MGR=""
  if [ -r /etc/os-release ]; then
    . /etc/os-release
    case "${ID_LIKE:-$ID}" in
      *debian*|*ubuntu*|*raspbian*)
        DISTRO_FAMILY="deb"
        PKG_MGR="apt"
        ;;
      *rhel*|*fedora*|*centos*|*rocky*|*almalinux*)
        DISTRO_FAMILY="rhel"
        PKG_MGR="dnf"
        ;;
      *suse*|*sle*)
        DISTRO_FAMILY="suse"
        PKG_MGR="zypper"
        ;;
      *)
        echo "Unsupported distro family for ID=$ID ID_LIKE=$ID_LIKE" >&2
        exit 1
        ;;
    esac
  else
    echo "/etc/os-release not found" >&2
    exit 1
  fi
}

sync_time() {
  if command -v timedatectl >/dev/null 2>&1; then
    timedatectl set-ntp true 2>/dev/null || true
  fi
  if command -v ntpdate >/dev/null 2>&1; then
    ntpdate -s time.nist.gov 2>/dev/null || ntpdate -s pool.ntp.org 2>/dev/null || true
  elif command -v chronyd >/dev/null 2>&1; then
    systemctl restart chronyd 2>/dev/null || true
  elif command -v systemd-timesyncd >/dev/null 2>&1; then
    systemctl restart systemd-timesyncd 2>/dev/null || true
  fi
  sleep 2
}

update_security_no_reboot() {
  case "$PKG_MGR" in
    apt)
      export DEBIAN_FRONTEND=noninteractive
      apt-get update -y
      if command -v unattended-upgrades >/dev/null 2>&1; then
        unattended-upgrades -v || true
      fi
      apt-get install -y --only-upgrade $(apt-get -s upgrade 2>/dev/null | awk '/^Inst/ {print $2}') || true
      apt-get -y dist-upgrade --no-install-recommends
      ;;
    dnf)
      if dnf --help 2>/dev/null | grep -q -- "--security"; then
        dnf -y update --security || dnf -y update
      else
        dnf -y update
      fi
      ;;
    zypper)
      zypper -n refresh
      zypper -n patch --category security || zypper -n patch
      ;;
  esac
}

needs_reboot() {
  if [ -f /var/run/reboot-required ] || [ -f /run/reboot-required ]; then
    return 0
  fi
  if command -v needs-restarting >/dev/null 2>&1; then
    if needs-restarting -r >/dev/null 2>&1; then :; else return 0; fi
  fi
  if [ "$PKG_MGR" = "zypper" ] && command -v zypper >/dev/null 2>&1; then
    if zypper ps -s 2>/dev/null | grep -qi "deleted"; then return 0; fi
  fi
  if command -v rpm >/dev/null 2>&1; then
    if rpm -q kernel >/dev/null 2>&1; then
      RUN_KVER="$(uname -r || true)"
      LAST_KVER="$(rpm -q kernel --qf '%{VERSION}-%{RELEASE}.%{ARCH}\n' 2>/dev/null | sort -V | tail -n1 || true)"
      if [ -n "${RUN_KVER:-}" ] && [ -n "${LAST_KVER:-}" ] && [ "$RUN_KVER" != "$LAST_KVER" ]; then
        return 0
      fi
    fi
  fi
  return 1
}

install_resume_unit() {
  mkdir -p "$GALLEY_STATE_DIR"
  printf "%s\n" "$*" > "$GALLEY_RESUME_ARGS_FILE"

  cat > "$GALLEY_RESUME_WRAPPER" <<'WRAP'
#!/bin/sh
set -eu
GALLEY_URL="${GALLEY_URL:-https://galley.run}"
ARGS_FILE="/var/lib/galley/resume.args"
if [ ! -f "$ARGS_FILE" ]; then
  exit 0
fi
ARGS="$(cat "$ARGS_FILE")"
curl -sSf "$GALLEY_URL" | sudo sh -s -- --resume $ARGS
WRAP
  chmod 0755 "$GALLEY_RESUME_WRAPPER"

  cat > "/etc/systemd/system/$GALLEY_RESUME_UNIT" <<UNIT
[Unit]
Description=Galley installer resume
After=network-online.target
Wants=network-online.target
ConditionPathExists=$GALLEY_RESUME_ARGS_FILE

[Service]
Type=oneshot
ExecStart=$GALLEY_RESUME_WRAPPER
RemainAfterExit=no

[Install]
WantedBy=multi-user.target
UNIT

  systemctl daemon-reload
  systemctl enable "$GALLEY_RESUME_UNIT" >/dev/null
}

cleanup_resume_unit() {
  systemctl disable "$GALLEY_RESUME_UNIT" >/dev/null 2>&1 || true
  rm -f "/etc/systemd/system/$GALLEY_RESUME_UNIT" || true
  rm -f "$GALLEY_RESUME_WRAPPER" || true
  rm -f "$GALLEY_RESUME_ARGS_FILE" || true
  systemctl daemon-reload || true
}

force_reboot_now() {
  echo "Reboot required. Rebooting now to complete updates..."
  sync
  reboot
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
  detect_distro

  RESUME=0
  if [ "${1-}" = "--resume" ]; then
    RESUME=1
    shift
  fi

  if [ $RESUME -eq 0 ]; then
    install_resume_unit "$*"
  fi

  echo "Synchronizing system time..."
  sync_time

  echo "Applying security updates..."
  update_security_no_reboot

  if needs_reboot; then
    if [ $RESUME -eq 0 ]; then
      printf "A reboot is required to complete security updates. The installation will automatically resume after reboot.\nReboot now? [y/N] "
      read -r answer
      case "$answer" in
        [Yy]*)
          force_reboot_now
          ;;
        *)
          echo "Reboot cancelled. Please reboot manually and re-run the installer to complete setup."
          exit 1
          ;;
      esac
    else
      force_reboot_now
    fi
  fi

  cleanup_resume_unit

  # Ensure CLI is installed
  if ! command -v "$GALLEY_BIN" >/dev/null 2>&1; then
    download_cli
  fi

  echo "System is updated. Galley CLI installed at $GALLEY_BIN."
  echo "Next step: run 'galley --help' or follow your chosen flow."
}

main "$@"
