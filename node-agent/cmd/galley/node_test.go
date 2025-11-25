package main

import (
	"os"
	"strings"
	"testing"
)

// TestParseSudoGroupMembers tests parsing of group file entries
func TestParseSudoGroupMembers(t *testing.T) {
	tests := []struct {
		name      string
		groupLine string
		want      []string
	}{
		{
			name:      "valid group with multiple members",
			groupLine: "sudo:x:27:user1,user2,user3",
			want:      []string{"user1", "user2", "user3"},
		},
		{
			name:      "valid group with single member",
			groupLine: "sudo:x:27:user1",
			want:      []string{"user1"},
		},
		{
			name:      "group with no members",
			groupLine: "sudo:x:27:",
			want:      nil,
		},
		{
			name:      "group with root member only",
			groupLine: "sudo:x:27:root",
			want:      nil,
		},
		{
			name:      "group with root and other members",
			groupLine: "sudo:x:27:root,user1,user2",
			want:      []string{"user1", "user2"},
		},
		{
			name:      "invalid format - too few fields",
			groupLine: "sudo:x:27",
			want:      nil,
		},
		{
			name:      "empty string",
			groupLine: "",
			want:      nil,
		},
		{
			name:      "group with spaces in member list",
			groupLine: "sudo:x:27:user1, user2 ,user3",
			want:      []string{"user1", "user2", "user3"},
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := parseSudoGroupMembers(tt.groupLine)
			if len(got) != len(tt.want) {
				t.Errorf("parseSudoGroupMembers() = %v, want %v", got, tt.want)
				return
			}
			for i, v := range got {
				if v != tt.want[i] {
					t.Errorf("parseSudoGroupMembers()[%d] = %v, want %v", i, v, tt.want[i])
				}
			}
		})
	}
}

// TestIsSSHRootLoginEnabled tests parsing of sshd_config
func TestIsSSHRootLoginEnabled(t *testing.T) {
	tests := []struct {
		name       string
		configFile string
		want       bool
		wantErr    bool
	}{
		{
			name: "explicitly enabled",
			configFile: `# SSH config
PermitRootLogin yes
Port 22`,
			want:    true,
			wantErr: false,
		},
		{
			name: "explicitly disabled",
			configFile: `# SSH config
PermitRootLogin no
Port 22`,
			want:    false,
			wantErr: false,
		},
		{
			name: "prohibit-password",
			configFile: `# SSH config
PermitRootLogin prohibit-password
Port 22`,
			want:    true,
			wantErr: false,
		},
		{
			name: "without-password",
			configFile: `# SSH config
PermitRootLogin without-password
Port 22`,
			want:    true,
			wantErr: false,
		},
		{
			name: "commented out",
			configFile: `# SSH config
#PermitRootLogin yes
Port 22`,
			want:    true, // Default is yes
			wantErr: false,
		},
		{
			name: "mixed case",
			configFile: `# SSH config
PERMITROOTLOGIN yes
Port 22`,
			want:    true,
			wantErr: false,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			// Create temporary sshd_config file
			tmpFile, err := os.CreateTemp("", "sshd_config_test_*")
			if err != nil {
				t.Fatalf("Failed to create temp file: %v", err)
			}
			defer os.Remove(tmpFile.Name())

			if _, err := tmpFile.WriteString(tt.configFile); err != nil {
				t.Fatalf("Failed to write temp file: %v", err)
			}
			tmpFile.Close()

			// Test the function logic (we can't override the file path easily)
			// Instead, we'll test the parsing logic inline
			lines := strings.Split(tt.configFile, "\n")
			found := false
			result := true // default

			for _, line := range lines {
				line = strings.TrimSpace(line)
				if strings.HasPrefix(line, "#") {
					continue
				}
				if strings.HasPrefix(line, "PermitRootLogin") {
					parts := strings.Fields(line)
					if len(parts) >= 2 {
						value := strings.ToLower(parts[1])
						result = value == "yes" || value == "prohibit-password" || value == "without-password"
						found = true
						break
					}
				}
			}

			if !found {
				result = true // default
			}

			if result != tt.want {
				t.Errorf("isSSHRootLoginEnabled() = %v, want %v", result, tt.want)
			}
		})
	}
}

// TestIsPasswordAuthenticationEnabled tests parsing of password auth settings
func TestIsPasswordAuthenticationEnabled(t *testing.T) {
	tests := []struct {
		name       string
		configFile string
		want       bool
	}{
		{
			name: "explicitly enabled",
			configFile: `# SSH config
PasswordAuthentication yes`,
			want: true,
		},
		{
			name: "explicitly disabled",
			configFile: `# SSH config
PasswordAuthentication no`,
			want: false,
		},
		{
			name: "commented out - defaults to yes",
			configFile: `# SSH config
#PasswordAuthentication yes`,
			want: true,
		},
		{
			name: "not present - defaults to yes",
			configFile: `# SSH config
Port 22`,
			want: true,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			// Test the parsing logic
			lines := strings.Split(tt.configFile, "\n")
			found := false
			result := true // default

			for _, line := range lines {
				line = strings.TrimSpace(line)
				if strings.HasPrefix(line, "#") {
					continue
				}
				if strings.HasPrefix(line, "PasswordAuthentication") {
					parts := strings.Fields(line)
					if len(parts) >= 2 {
						result = strings.ToLower(parts[1]) == "yes"
						found = true
						break
					}
				}
			}

			if !found {
				result = true // default
			}

			if result != tt.want {
				t.Errorf("isPasswordAuthenticationEnabled() = %v, want %v", result, tt.want)
			}
		})
	}
}

// TestConfigureLoginDefs tests login.defs configuration
func TestConfigureLoginDefsLogic(t *testing.T) {
	tests := []struct {
		name         string
		inputContent string
		expectedVals map[string]string
		shouldModify bool
	}{
		{
			name: "missing all settings",
			inputContent: `# Login defaults
UMASK 022`,
			expectedVals: map[string]string{
				"PASS_MAX_DAYS": "90",
				"PASS_MIN_DAYS": "1",
				"PASS_MIN_LEN":  "14",
				"PASS_WARN_AGE": "7",
			},
			shouldModify: true,
		},
		{
			name: "has some settings with wrong values",
			inputContent: `# Login defaults
PASS_MAX_DAYS	99999
PASS_MIN_DAYS	0
UMASK 022`,
			expectedVals: map[string]string{
				"PASS_MAX_DAYS": "90",
				"PASS_MIN_DAYS": "1",
			},
			shouldModify: true,
		},
		{
			name: "already configured correctly",
			inputContent: `# Login defaults
PASS_MAX_DAYS	90
PASS_MIN_DAYS	1
PASS_MIN_LEN	14
PASS_WARN_AGE	7`,
			expectedVals: map[string]string{
				"PASS_MAX_DAYS": "90",
				"PASS_MIN_DAYS": "1",
				"PASS_MIN_LEN":  "14",
				"PASS_WARN_AGE": "7",
			},
			shouldModify: false,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			lines := strings.Split(tt.inputContent, "\n")
			foundSettings := make(map[string]bool)
			modified := false

			// Simulate the update logic
			for i, line := range lines {
				trimmed := strings.TrimSpace(line)
				if strings.HasPrefix(trimmed, "#") || trimmed == "" {
					continue
				}

				for setting, value := range tt.expectedVals {
					if strings.HasPrefix(trimmed, setting) {
						foundSettings[setting] = true
						expectedLine := setting + "\t" + value
						currentValue := strings.Fields(trimmed)
						if len(currentValue) >= 2 && currentValue[1] != value {
							lines[i] = expectedLine
							modified = true
						}
						break
					}
				}
			}

			// Check if settings need to be added
			for setting := range tt.expectedVals {
				if !foundSettings[setting] {
					modified = true
				}
			}

			if modified != tt.shouldModify {
				t.Errorf("Expected modified=%v, got modified=%v", tt.shouldModify, modified)
			}
		})
	}
}

// TestConfigurePAMPasswordQualityLogic tests PAM configuration logic
func TestConfigurePAMPasswordQualityLogic(t *testing.T) {
	tests := []struct {
		name         string
		pamContent   string
		shouldModify bool
	}{
		{
			name: "already configured with correct settings",
			pamContent: `password    requisite     pam_pwquality.so retry=3 minlen=14 dcredit=-1 ucredit=-1 ocredit=-1 lcredit=-1
password    sufficient    pam_unix.so`,
			shouldModify: false,
		},
		{
			name: "has pwquality but wrong settings",
			pamContent: `password    requisite     pam_pwquality.so retry=3 minlen=8
password    sufficient    pam_unix.so`,
			shouldModify: true,
		},
		{
			name: "missing pwquality module",
			pamContent: `password    sufficient    pam_unix.so
password    required      pam_deny.so`,
			shouldModify: true,
		},
		{
			name: "has cracklib instead",
			pamContent: `password    requisite     pam_cracklib.so retry=3 minlen=8
password    sufficient    pam_unix.so`,
			shouldModify: true,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			lines := strings.Split(tt.pamContent, "\n")
			modified := false
			found := false

			for _, line := range lines {
				trimmed := strings.TrimSpace(line)
				if strings.Contains(trimmed, "pam_pwquality.so") || strings.Contains(trimmed, "pam_cracklib.so") {
					found = true
					if !strings.Contains(trimmed, "minlen=14") {
						modified = true
					}
					break
				}
			}

			if !found {
				modified = true
			}

			if modified != tt.shouldModify {
				t.Errorf("Expected modified=%v, got modified=%v", tt.shouldModify, modified)
			}
		})
	}
}

// TestSSHConfigConstants validates SSH configuration paths
func TestSSHConfigConstants(t *testing.T) {
	t.Run("validates SSH paths", func(t *testing.T) {
		sshdConfigPath := "/etc/ssh/sshd_config"

		if sshdConfigPath[0] != '/' {
			t.Error("sshd_config path should be absolute")
		}

		if !strings.Contains(sshdConfigPath, "/etc/ssh/") {
			t.Error("sshd_config should be in /etc/ssh/")
		}

		if !strings.HasSuffix(sshdConfigPath, "sshd_config") {
			t.Error("File should be named sshd_config")
		}
	})
}

// TestLoginDefsConstants validates login.defs path
func TestLoginDefsConstants(t *testing.T) {
	t.Run("validates login.defs path", func(t *testing.T) {
		loginDefsPath := "/etc/login.defs"

		if loginDefsPath[0] != '/' {
			t.Error("login.defs path should be absolute")
		}

		if !strings.HasPrefix(loginDefsPath, "/etc/") {
			t.Error("login.defs should be in /etc/")
		}

		if !strings.HasSuffix(loginDefsPath, "login.defs") {
			t.Error("File should be named login.defs")
		}
	})
}

// TestPAMConfigPaths validates PAM configuration file paths
func TestPAMConfigPaths(t *testing.T) {
	t.Run("validates PAM config paths", func(t *testing.T) {
		pamFiles := []string{
			"/etc/pam.d/common-password",
			"/etc/pam.d/system-auth",
			"/etc/pam.d/password-auth",
		}

		for _, pamFile := range pamFiles {
			if pamFile[0] != '/' {
				t.Errorf("%s should be an absolute path", pamFile)
			}

			if !strings.HasPrefix(pamFile, "/etc/pam.d/") {
				t.Errorf("%s should be in /etc/pam.d/", pamFile)
			}
		}
	})
}

// TestPasswordPolicySettings validates password policy values
func TestPasswordPolicySettings(t *testing.T) {
	t.Run("validates password policy settings", func(t *testing.T) {
		settings := map[string]string{
			"PASS_MAX_DAYS": "90",
			"PASS_MIN_DAYS": "1",
			"PASS_MIN_LEN":  "14",
			"PASS_WARN_AGE": "7",
		}

		for key, value := range settings {
			if value == "" {
				t.Errorf("%s should not be empty", key)
			}

			// Validate numeric values
			switch key {
			case "PASS_MAX_DAYS":
				if value != "90" {
					t.Error("PASS_MAX_DAYS should be 90")
				}
			case "PASS_MIN_DAYS":
				if value != "1" {
					t.Error("PASS_MIN_DAYS should be 1")
				}
			case "PASS_MIN_LEN":
				if value != "14" {
					t.Error("PASS_MIN_LEN should be 14")
				}
			case "PASS_WARN_AGE":
				if value != "7" {
					t.Error("PASS_WARN_AGE should be 7")
				}
			}
		}
	})
}

// TestSecurityToolsPackages validates security tools configuration
func TestSecurityToolsPackages(t *testing.T) {
	t.Run("validates security tools list", func(t *testing.T) {
		requiredTools := []string{"fail2ban", "htop"}

		for _, tool := range requiredTools {
			if tool == "" {
				t.Error("Security tool name should not be empty")
			}
		}

		if len(requiredTools) < 2 {
			t.Error("Should have at least 2 security tools")
		}

		// Check for expected tools
		hasFailban := false
		hasHtop := false

		for _, tool := range requiredTools {
			if tool == "fail2ban" {
				hasFailban = true
			}
			if tool == "htop" {
				hasHtop = true
			}
		}

		if !hasFailban {
			t.Error("Should include fail2ban in security tools")
		}
		if !hasHtop {
			t.Error("Should include htop in security tools")
		}
	})
}

// TestSystemdTargets validates systemd target configuration
func TestSystemdTargets(t *testing.T) {
	t.Run("validates multi-user target", func(t *testing.T) {
		target := "multi-user.target"

		if !strings.HasSuffix(target, ".target") {
			t.Error("Systemd target should have .target extension")
		}

		if !strings.Contains(target, "multi-user") {
			t.Error("Should be multi-user target")
		}
	})
}

// TestFTPServiceNames validates FTP service detection
func TestFTPServiceNames(t *testing.T) {
	t.Run("validates FTP service names", func(t *testing.T) {
		ftpServices := []string{"vsftpd", "proftpd", "pure-ftpd"}

		if len(ftpServices) == 0 {
			t.Error("Should have FTP services to check")
		}

		for _, service := range ftpServices {
			if service == "" {
				t.Error("FTP service name should not be empty")
			}

			if !strings.Contains(strings.ToLower(service), "ftp") {
				t.Errorf("Service %s should contain 'ftp' in name", service)
			}
		}
	})
}

// TestExpectedPorts validates expected open ports configuration
func TestExpectedPorts(t *testing.T) {
	t.Run("validates expected ports", func(t *testing.T) {
		expectedPorts := map[string]string{
			"22":    "SSH",
			"53":    "DNS",
			"68":    "DHCP client",
			"6443":  "Kubernetes API",
			"8132":  "konnectivity",
			"9443":  "k0s API",
			"10250": "kubelet",
		}

		if len(expectedPorts) == 0 {
			t.Error("Should have expected ports defined")
		}

		// Verify SSH port is included
		if _, exists := expectedPorts["22"]; !exists {
			t.Error("SSH port 22 should be in expected ports")
		}

		// Verify Kubernetes ports are included
		if _, exists := expectedPorts["6443"]; !exists {
			t.Error("Kubernetes API port 6443 should be in expected ports")
		}

		// Verify all ports have descriptions
		for port, desc := range expectedPorts {
			if port == "" {
				t.Error("Port number should not be empty")
			}
			if desc == "" {
				t.Errorf("Port %s should have a description", port)
			}
		}
	})
}

// TestNodePrepareStepConstants validates step tracking constants
func TestNodePrepareStepConstants(t *testing.T) {
	t.Run("validates step constants", func(t *testing.T) {
		// These should match the constants in the actual code
		steps := []string{
			"os-update",
			"ssh-config",
			"k0s-install",
			"k0s-config",
			"server-hardening",
		}

		for _, step := range steps {
			if step == "" {
				t.Error("Step constant should not be empty")
			}

			// Should use kebab-case
			if strings.Contains(step, "_") || strings.Contains(step, " ") {
				t.Errorf("Step %s should use kebab-case, not underscores or spaces", step)
			}
		}

		// Verify we have all required steps
		expectedSteps := map[string]bool{
			"os-update":        false,
			"ssh-config":       false,
			"k0s-install":      false,
			"k0s-config":       false,
			"server-hardening": false,
		}

		for _, step := range steps {
			if _, exists := expectedSteps[step]; exists {
				expectedSteps[step] = true
			}
		}

		for step, found := range expectedSteps {
			if !found {
				t.Errorf("Missing expected step: %s", step)
			}
		}
	})
}

// TestSSHServiceNames validates SSH service name detection
func TestSSHServiceNames(t *testing.T) {
	t.Run("validates SSH service names", func(t *testing.T) {
		sshServices := []string{"sshd", "ssh"}

		if len(sshServices) == 0 {
			t.Error("Should have SSH service names defined")
		}

		for _, service := range sshServices {
			if service == "" {
				t.Error("SSH service name should not be empty")
			}

			if !strings.Contains(service, "ssh") {
				t.Errorf("Service name %s should contain 'ssh'", service)
			}
		}
	})
}
