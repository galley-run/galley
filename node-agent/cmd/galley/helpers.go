package main

import (
	"fmt"
	"os/exec"
)

func platformDesiredRole(baseURL, nodeID, spkiPin string) (string, error) {
	if baseURL == "" {
		return "controller", nil
	}

	_ = nodeID
	_ = spkiPin

	return "controller", nil
}

func checkCommands(names ...string) {
	for _, name := range names {
		if _, err := exec.LookPath(name); err != nil {
			_ = fmt.Errorf("warning: command not found in PATH: %s\n", name)
		}
	}
}
