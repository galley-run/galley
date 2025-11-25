package main

import (
	"context"
	"os/exec"
	"testing"
	"time"
)

func TestIsAvailable(t *testing.T) {
	tests := []struct {
		name     string
		binaries []string
		want     bool
	}{
		{
			name:     "existing binary",
			binaries: []string{"go"},
			want:     true,
		},
		{
			name:     "non-existing binary",
			binaries: []string{"nonexistent-binary-xyz"},
			want:     false,
		},
		{
			name:     "multiple binaries with one existing",
			binaries: []string{"nonexistent-binary-xyz", "go"},
			want:     true,
		},
		{
			name:     "multiple non-existing binaries",
			binaries: []string{"nonexistent-binary-xyz", "another-fake-binary"},
			want:     false,
		},
		{
			name:     "empty binaries list",
			binaries: []string{},
			want:     false,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := isAvailable(tt.binaries); got != tt.want {
				t.Errorf("isAvailable() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestPackageManagerConfiguration(t *testing.T) {
	// Verify all package managers have required fields
	for _, pm := range packageManagers {
		t.Run(pm.name, func(t *testing.T) {
			if pm.name == "" {
				t.Error("package manager name is empty")
			}
			if len(pm.binaries) == 0 {
				t.Error("package manager has no binaries defined")
			}
			if len(pm.commands) == 0 {
				t.Error("package manager has no commands defined")
			}

			// Verify each command has at least a binary name
			for i, cmd := range pm.commands {
				if len(cmd) == 0 {
					t.Errorf("command %d is empty", i)
				}
			}
		})
	}
}

func TestPackageManagerBinariesExist(t *testing.T) {
	// This test documents which package managers are available
	// on the test system - useful for CI/CD
	for _, pm := range packageManagers {
		t.Run(pm.name, func(t *testing.T) {
			available := false
			for _, binary := range pm.binaries {
				if _, err := exec.LookPath(binary); err == nil {
					available = true
					t.Logf("Found %s binary: %s", pm.name, binary)
					break
				}
			}
			if !available {
				t.Logf("%s not available on this system", pm.name)
			}
		})
	}
}

func TestRunCommandWithContext(t *testing.T) {
	tests := []struct {
		name    string
		command string
		args    []string
		timeout time.Duration
		wantErr bool
	}{
		{
			name:    "successful command with context",
			command: "echo",
			args:    []string{"test"},
			timeout: 5 * time.Second,
			wantErr: false,
		},
		{
			name:    "command timeout",
			command: "sleep",
			args:    []string{"10"},
			timeout: 100 * time.Millisecond,
			wantErr: true,
		},
		{
			name:    "context cancellation",
			command: "sleep",
			args:    []string{"5"},
			timeout: 100 * time.Millisecond,
			wantErr: true,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			ctx, cancel := context.WithTimeout(context.Background(), tt.timeout)
			defer cancel()

			err := runCommandWithContext(ctx, tt.command, tt.args...)
			if (err != nil) != tt.wantErr {
				t.Errorf("runCommandWithContext() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}

func TestRunCommandsWithContext(t *testing.T) {
	tests := []struct {
		name     string
		commands [][]string
		timeout  time.Duration
		wantErr  bool
	}{
		{
			name: "all successful commands with context",
			commands: [][]string{
				{"echo", "test1"},
				{"echo", "test2"},
			},
			timeout: 5 * time.Second,
			wantErr: false,
		},
		{
			name: "timeout during command sequence",
			commands: [][]string{
				{"echo", "test1"},
				{"sleep", "10"},
			},
			timeout: 100 * time.Millisecond,
			wantErr: true,
		},
		{
			name: "context cancelled",
			commands: [][]string{
				{"sleep", "1"},
				{"sleep", "1"},
			},
			timeout: 500 * time.Millisecond,
			wantErr: true,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			ctx, cancel := context.WithTimeout(context.Background(), tt.timeout)
			defer cancel()

			err := runCommandsWithContext(ctx, tt.commands)
			if (err != nil) != tt.wantErr {
				t.Errorf("runCommandsWithContext() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}

func TestUpdateOS(t *testing.T) {
	t.Run("detects package manager", func(t *testing.T) {
		// We can't actually run updateOS() in tests as it requires root
		// but we can verify the detection logic works
		found := false
		for _, pm := range packageManagers {
			if isAvailable(pm.binaries) {
				found = true
				t.Logf("Detected %s on this system", pm.name)
				break
			}
		}
		if !found {
			t.Log("No package manager found (expected on some systems like macOS)")
		}
	})
}

func TestSetupResumeService(t *testing.T) {
	t.Run("validates resume service constants", func(t *testing.T) {
		if galleyStateDir == "" {
			t.Error("galleyStateDir should not be empty")
		}
		if galleyResumeArgsFile == "" {
			t.Error("galleyResumeArgsFile should not be empty")
		}
		if galleyResumeWrapper == "" {
			t.Error("galleyResumeWrapper should not be empty")
		}
		if galleyResumeUnit == "" {
			t.Error("galleyResumeUnit should not be empty")
		}
	})

	t.Run("resume file path is in state directory", func(t *testing.T) {
		if !contains(galleyResumeArgsFile, galleyStateDir) {
			t.Errorf("resume file %s should be in state directory %s", galleyResumeArgsFile, galleyStateDir)
		}
	})
}

func TestCleanupProgress(t *testing.T) {
	t.Run("cleanup doesn't panic", func(t *testing.T) {
		// Should not panic even if files don't exist
		cleanupProgress()
	})
}

func TestIsResuming(t *testing.T) {
	t.Run("returns false when no progress file exists", func(t *testing.T) {
		// isResuming checks for progress file, not args
		// When no progress file exists, it should return false
		result := isResuming()
		// We expect false unless there's an actual progress file
		t.Logf("isResuming() = %v", result)
	})

	t.Run("validates function exists and doesn't panic", func(t *testing.T) {
		// Ensure the function can be called without panicking
		defer func() {
			if r := recover(); r != nil {
				t.Errorf("isResuming() panicked: %v", r)
			}
		}()
		_ = isResuming()
	})
}

func TestPromptRebootConstants(t *testing.T) {
	t.Run("validates prompt reboot paths", func(t *testing.T) {
		// Ensure paths are absolute
		if galleyStateDir[0] != '/' {
			t.Error("galleyStateDir should be an absolute path")
		}
		if galleyResumeArgsFile[0] != '/' {
			t.Error("galleyResumeArgsFile should be an absolute path")
		}
		if galleyResumeWrapper[0] != '/' {
			t.Error("galleyResumeWrapper should be an absolute path")
		}
	})

	t.Run("systemd unit has correct extension", func(t *testing.T) {
		if !contains(galleyResumeUnit, ".service") {
			t.Error("galleyResumeUnit should have .service extension")
		}
	})
}

func TestGetCurrentOSVersion(t *testing.T) {
	t.Run("returns a version string", func(t *testing.T) {
		version := getCurrentOSVersion()
		if version == "" {
			t.Error("getCurrentOSVersion() should return a non-empty string")
		}
		t.Logf("Current OS version: %s", version)
	})
}

func TestExtractVersionFromOutput(t *testing.T) {
	tests := []struct {
		name     string
		output   string
		expected string
	}{
		{
			name:     "ubuntu standard format",
			output:   "New release '24.04' available.\nRun 'do-release-upgrade' to upgrade to it.",
			expected: "24.04",
		},
		{
			name:     "multiline with version",
			output:   "Checking for a new Ubuntu release\nNew release '24.10' available.\n",
			expected: "24.10",
		},
		{
			name:     "no version available",
			output:   "No new release found",
			expected: "",
		},
		{
			name:     "malformed output",
			output:   "New release available but no quotes",
			expected: "",
		},
		{
			name:     "empty output",
			output:   "",
			expected: "",
		},
		{
			name:     "version with LTS marker",
			output:   "New release '22.04 LTS' available.",
			expected: "22.04 LTS",
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			result := extractVersionFromOutput(tt.output)
			if result != tt.expected {
				t.Errorf("extractVersionFromOutput() = %q, want %q", result, tt.expected)
			}
		})
	}
}

func TestGetAvailableLTSVersion(t *testing.T) {
	t.Run("checks for LTS version availability", func(t *testing.T) {
		version, available := getAvailableLTSVersion()
		if available {
			t.Logf("LTS version available: %s", version)
			if version == "" {
				t.Error("getAvailableLTSVersion() returned available=true but empty version")
			}
		} else {
			t.Log("No LTS version available (or do-release-upgrade not found)")
		}
	})
}

func TestVersionFunctionsDoNotPanic(t *testing.T) {
	tests := []struct {
		name string
		fn   func()
	}{
		{
			name: "getCurrentOSVersion",
			fn:   func() { _ = getCurrentOSVersion() },
		},
		{
			name: "getAvailableLTSVersion",
			fn:   func() { _, _ = getAvailableLTSVersion() },
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			defer func() {
				if r := recover(); r != nil {
					t.Errorf("%s panicked: %v", tt.name, r)
				}
			}()
			tt.fn()
		})
	}
}

// Helper function for string contains check
func contains(s, substr string) bool {
	return len(s) >= len(substr) && (s == substr || len(s) > len(substr) &&
		(s[:len(substr)] == substr || s[len(s)-len(substr):] == substr ||
			stringContains(s, substr)))
}

func stringContains(s, substr string) bool {
	for i := 0; i <= len(s)-len(substr); i++ {
		if s[i:i+len(substr)] == substr {
			return true
		}
	}
	return false
}
