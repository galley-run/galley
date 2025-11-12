package main

import (
  "crypto/tls"
  "crypto/x509"
  "encoding/base64"
  "errors"
  "flag"
  "fmt"
  "log"
  "net/http"
  "os"
  "os/exec"
  "path/filepath"
  "strings"
  "time"

  "golang.org/x/net/websocket"
)

// Minimal dependency CLI (no Cobra) to keep the binary tiny.
// Subcommands:
//   controller install <node-id>
//   controller invite
//   controller join <vessel-engine-id> <join-token>
//   worker invite
//   worker join <vessel-engine-id> <token>
//   node provision [--node <uuid>]
//   config set <key> <value>

const (
  defaultConfigPath = ".galley/config.yaml"
  defaultCADir      = ".galley"
)

func main() {
  if len(os.Args) < 2 {
    usage()
    return
  }
  switch os.Args[1] {
  case "controller":
    controllerCmd(os.Args[2:])
  case "worker":
    workerCmd(os.Args[2:])
  case "node":
    nodeCmd(os.Args[2:])
  case "config":
    configCmd(os.Args[2:])
  case "--help", "help", "-h":
    usage()
  default:
    fmt.Fprintf(os.Stderr, "Unknown command: %s\n", os.Args[1])
    usage()
    os.Exit(2)
  }
}

func usage() {
  fmt.Println(`galley - Galley Node Agent CLI

Usage:
  galley controller install <node-id> [--platform-url URL] [--platform-ws-url URL] [--dry-run]
  galley controller invite [--expiry 10m]
  galley controller join <vessel-engine-id> <join-token> [--platform-url URL]
  galley worker invite [--expiry 10m]
  galley worker join <vessel-engine-id> <token> [--platform-url URL]
  galley node provision [--node UUID] [--platform-ws-url URL]
  galley config set <key> <value>
  galley --help
`)
}

// ---- Controller ----

func controllerCmd(args []string) {
  if len(args) == 0 {
    fmt.Fprintln(os.Stderr, "controller subcommand required: install|invite|join")
    os.Exit(2)
  }
  switch args[0] {
  case "install":
    controllerInstall(args[1:])
  case "invite":
    controllerInvite(args[1:])
  case "join":
    controllerJoin(args[1:])
  default:
    fmt.Fprintf(os.Stderr, "unknown controller subcommand: %s\n", args[0])
    os.Exit(2)
  }
}

func controllerInstall(args []string) {
  fs := flag.NewFlagSet("controller install", flag.ExitOnError)
  platformURL := fs.String("platform-url", "", "Platform API base URL")
  wsURL := fs.String("platform-ws-url", "", "Platform WebSocket URL (optional)")
  dryRun := fs.Bool("dry-run", false, "Do not mutate the system, print steps only")
  spkiPin := fs.String("tls-spki-pin", "", "Optional base64-encoded SPKI pin for TLS pinning")
  _ = fs.Parse(args)

  if fs.NArg() < 1 {
    fmt.Fprintln(os.Stderr, "missing <node-id>")
    os.Exit(2)
  }
  nodeID := fs.Arg(0)

  checkCommands("k0s")
  if *dryRun {
    fmt.Printf("[dry-run] Would install controller for node %s, platform=%s ws=%s\n", nodeID, *platformURL, *wsURL)
    return
  }

  role, err := platformDesiredRole(*platformURL, nodeID, *spkiPin)
  if err != nil {
    log.Fatalf("platformDesiredRole failed: %v", err)
  }

  argsList := []string{"install", "controller"}
  if role == "controller+worker" {
    argsList = append(argsList, "--enable-worker")
  }
  runSudo("k0s", argsList...)
  runSudo("systemctl", "enable", "--now", "k0scontroller")

  token := runSudoCapture("k0s", "token", "create", "--role=worker", "--expiry", "10m")
  fmt.Println("\n# To finish your cluster setup add worker nodes via SSH and run:")
  fmt.Println("curl -sSf https://galley.run/install.sh | sudo sh")
  fmt.Printf("galley worker join %s %s\n", nodeID, strings.TrimSpace(token))
}

func controllerInvite(args []string) {
  fs := flag.NewFlagSet("controller invite", flag.ExitOnError)
  expiry := fs.String("expiry", "10m", "Token expiry")
  _ = fs.Parse(args)

  ensureK0sRunning()
  token := runSudoCapture("k0s", "token", "create", "--role=controller", "--expiry", *expiry)
  fmt.Println("curl -sSf https://galley.run/install.sh | sudo sh")
  fmt.Printf("galley controller join <vessel-engine-id> %s\n", strings.TrimSpace(token))
}

func controllerJoin(args []string) {
  fs := flag.NewFlagSet("controller join", flag.ExitOnError)
  platformURL := fs.String("platform-url", "", "Platform API base URL")
  configPath := fs.String("config", defaultConfigPath, "Config path")
  _ = fs.Parse(args)
  if fs.NArg() < 2 {
    fmt.Fprintln(os.Stderr, "usage: galley controller join <vessel-engine-id> <join-token>")
    os.Exit(2)
  }
  vesselEngineID := fs.Arg(0)
  joinToken := fs.Arg(1)

  runSudo("k0s", "install", "controller", "--token-file", writeTmp(joinToken))
  runSudo("systemctl", "enable", "--now", "k0scontroller")

  if *platformURL != "" {
    _ = platformControllerJoined(*platformURL, vesselEngineID)
  }
  _ = configWrite(*configPath, map[string]string{
    "vessel_engine_id": vesselEngineID,
  })
}

// ---- Worker ----

func workerCmd(args []string) {
  if len(args) == 0 {
    fmt.Fprintln(os.Stderr, "worker subcommand required: invite|join")
    os.Exit(2)
  }
  switch args[0] {
  case "invite":
    workerInvite(args[1:])
  case "join":
    workerJoin(args[1:])
  default:
    fmt.Fprintf(os.Stderr, "unknown worker subcommand: %s\n", args[0])
    os.Exit(2)
  }
}

func workerInvite(args []string) {
  fs := flag.NewFlagSet("worker invite", flag.ExitOnError)
  expiry := fs.String("expiry", "10m", "Token expiry")
  _ = fs.Parse(args)

  ensureK0sRunning()
  token := runSudoCapture("k0s", "token", "create", "--role=worker", "--expiry", *expiry)
  fmt.Println("curl -sSf https://galley.run/install.sh | sudo sh")
  fmt.Printf("galley worker join <vessel-engine-id> %s\n", strings.TrimSpace(token))
}

func workerJoin(args []string) {
  fs := flag.NewFlagSet("worker join", flag.ExitOnError)
  platformURL := fs.String("platform-url", "", "Platform API base URL")
  wsURL := fs.String("platform-ws-url", "", "Platform WebSocket URL (optional)")
  configPath := fs.String("config", defaultConfigPath, "Config path")
  _ = fs.Parse(args)

  if fs.NArg() < 2 {
    fmt.Fprintln(os.Stderr, "usage: galley worker join <vessel-engine-id> <token>")
    os.Exit(2)
  }
  vesselEngineID := fs.Arg(0)
  token := fs.Arg(1)

  tmp := writeTmp(token)
  runSudo("k0s", "install", "worker", "--token-file", tmp)
  runSudo("systemctl", "enable", "--now", "k0sworker")

  _ = configWrite(*configPath, map[string]string{
    "vessel_engine_id": vesselEngineID,
  })
  _ = wsPing(*wsURL)

  if *platformURL != "" {
    _ = platformWorkerJoined(*platformURL, vesselEngineID)
  }
}

// ---- Node ----

func nodeCmd(args []string) {
  if len(args) == 0 {
    fmt.Fprintln(os.Stderr, "node subcommand required: provision")
    os.Exit(2)
  }
  switch args[0] {
  case "provision":
    nodeProvision(args[1:])
  default:
    fmt.Fprintf(os.Stderr, "unknown node subcommand: %s\n", args[0])
    os.Exit(2)
  }
}

func nodeProvision(args []string) {
  fs := flag.NewFlagSet("node provision", flag.ExitOnError)
  nodeID := fs.String("node", "", "Node UUID (optional)")
  wsURL := fs.String("platform-ws-url", "", "Platform WebSocket URL")
  _ = fs.Parse(args)

  if err := wsRunAgent(*wsURL, *nodeID); err != nil {
    log.Fatalf("agent exit: %v", err)
  }
}

// ---- Config ----

func configCmd(args []string) {
  if len(args) < 1 {
    fmt.Fprintln(os.Stderr, "config subcommand required: set")
    os.Exit(2)
  }
  switch args[0] {
  case "set":
    if len(args) != 3 {
      fmt.Fprintln(os.Stderr, "usage: galley config set <key> <value>")
      os.Exit(2)
    }
    key, value := args[1], args[2]
    path := defaultConfigPath
    if err := configWrite(path, map[string]string{key: value}); err != nil {
      log.Fatal(err)
    }
  default:
    fmt.Fprintf(os.Stderr, "unknown config subcommand: %s\n", args[0])
    os.Exit(2)
  }
}

// ---- Helpers ----

func checkCommands(names ...string) {
  for _, n := range names {
    if _, err := exec.LookPath(n); err != nil {
      fmt.Fprintf(os.Stderr, "warning: command not found in PATH: %s\n", n)
    }
  }
}

func runSudo(cmd string, args ...string) {
  all := append([]string{cmd}, args...)
  c := exec.Command("sudo", all...)
  c.Stdout = os.Stdout
  c.Stderr = os.Stderr
  if err := c.Run(); err != nil {
    log.Fatalf("sudo %s failed: %v", strings.Join(all, " "), err)
  }
}

func runSudoCapture(cmd string, args ...string) string {
  all := append([]string{cmd}, args...)
  c := exec.Command("sudo", all...)
  out, err := c.Output()
  if err != nil {
    log.Fatalf("sudo %s failed: %v", strings.Join(all, " "), err)
  }
  return string(out)
}

func writeTmp(content string) string {
  dir := os.TempDir()
  p := filepath.Join(dir, fmt.Sprintf("galley-%d.token", time.Now().UnixNano()))
  if err := os.WriteFile(p, []byte(content), 0600); err != nil {
    log.Fatal(err)
  }
  return p
}

func configWrite(relPath string, kv map[string]string) error {
  home, _ := os.UserHomeDir()
  path := relPath
  if !filepath.IsAbs(path) {
    path = filepath.Join(home, relPath)
  }
  _ = os.MkdirAll(filepath.Dir(path), 0700)
  var b strings.Builder
  for k, v := range kv {
    b.WriteString(fmt.Sprintf("%s: %q\n", k, v))
  }
  return os.WriteFile(path, []byte(b.String()), 0600)
}

// ---- Platform stubs ----

func platformDesiredRole(baseURL, nodeID, spkiPin string) (string, error) {
  if baseURL == "" {
    return "controller", nil
  }
  _ = spkiPin
  return "controller", nil
}

func platformControllerJoined(baseURL, vesselEngineID string) error {
  _ = baseURL
  _ = vesselEngineID
  return nil
}

func platformWorkerJoined(baseURL, vesselEngineID string) error {
  _ = baseURL
  _ = vesselEngineID
  return nil
}

// ---- WebSocket agent ----

func wsPing(wsURL string) error {
  if wsURL == "" {
    return nil
  }
  origin := "http://localhost/"
  ws, err := websocket.Dial(wsURL, "", origin)
  if err != nil {
    return err
  }
  defer ws.Close()
  _, _ = ws.Write([]byte("ping"))
  return nil
}

func wsRunAgent(wsURL, nodeID string) error {
  if wsURL == "" {
    return errors.New("missing --platform-ws-url")
  }
  origin := "http://localhost/"
  backoff := time.Second
  for {
    ws, err := websocket.Dial(wsURL, "", origin)
    if err != nil {
      time.Sleep(backoff)
      if backoff < 60*time.Second {
        backoff *= 2
      }
      continue
    }
    _ = websocket.Message.Send(ws, fmt.Sprintf(`{"type":"hello","node":"%s"}`, nodeID))
    ws.SetDeadline(time.Now().Add(60 * time.Second))
    var msg string
    if err := websocket.Message.Receive(ws, &msg); err != nil {
      ws.Close()
      time.Sleep(time.Second)
      continue
    }
    _ = ws.Close()
    backoff = time.Second
  }
}

// ---- Service checks ----

func ensureK0sRunning() {
  if _, err := exec.LookPath("k0s"); err != nil {
    log.Fatal("k0s not found in PATH; install k0s first")
  }
  if serviceActive("k0scontroller") || serviceActive("k0sworker") {
    return
  }
  log.Fatal("k0s installed but neither controller nor worker service is active")
}

func serviceActive(name string) bool {
  c := exec.Command("systemctl", "is-active", "--quiet", name)
  if err := c.Run(); err != nil {
    return false
  }
  return true
}

// ---- TLS pin stub ----

func buildHTTPClientWithPin(spkiBase64 string) *http.Client {
  tr := &http.Transport{}
  if spkiBase64 != "" {
    tr.TLSClientConfig = &tls.Config{
      VerifyConnection: func(cs tls.ConnectionState) error {
        if len(cs.PeerCertificates) == 0 {
          return errors.New("no peer cert")
        }
        pub := cs.PeerCertificates[0].PublicKey
        der, err := x509.MarshalPKIXPublicKey(pub)
        if err != nil {
          return err
        }
        sum := sha256Sum(der)
        want, _ := base64.StdEncoding.DecodeString(spkiBase64)
        if !hmacEqual(sum, want) {
          return errors.New("SPKI pin mismatch")
        }
        return nil
      },
    }
  }
  return &http.Client{Transport: tr, Timeout: 15 * time.Second}
}

func sha256Sum(b []byte) []byte {
  // Placeholder: import crypto/sha256 later if you want real hashing.
  return []byte("not-implemented")
}

func hmacEqual(a, b []byte) bool {
  if len(a) != len(b) {
    return false
  }
  var v byte
  for i := range a {
    v |= a[i] ^ b[i]
  }
  return v == 0
}
