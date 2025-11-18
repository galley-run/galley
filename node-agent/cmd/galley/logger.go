package main

import (
	"fmt"
	"os"
	"time"
)

const (
	galleyLogDir  = "/var/log/galley"
	galleyLogFile = "/var/log/galley/galley.log"
)

// initLogger ensures the log directory exists
func initLogger() error {
	if err := os.MkdirAll(galleyLogDir, 0755); err != nil {
		return fmt.Errorf("failed to create log directory: %w", err)
	}
	return nil
}

// logAction logs an action to the galley log file
func logAction(action string, details map[string]string) {
	// Try to initialize logger, but don't fail if we can't
	if err := initLogger(); err != nil {
		// Silently fail - we don't want to break functionality if logging fails
		return
	}

	// Open log file in append mode
	f, err := os.OpenFile(galleyLogFile, os.O_APPEND|os.O_CREATE|os.O_WRONLY, 0644)
	if err != nil {
		// Silently fail
		return
	}
	defer f.Close()

	// Build log entry
	timestamp := time.Now().Format(time.RFC3339)
	logEntry := fmt.Sprintf("[%s] ACTION: %s\n", timestamp, action)

	// Add details if provided
	if len(details) > 0 {
		for key, value := range details {
			logEntry += fmt.Sprintf("  %s: %s\n", key, value)
		}
	}

	// Write to log file
	f.WriteString(logEntry)
}

// logCommand logs a command execution
func logCommand(command string, args []string) {
	details := map[string]string{
		"command": command,
		"args":    fmt.Sprintf("%v", args),
	}
	logAction("COMMAND_EXECUTED", details)
}

// logFileWrite logs a file write operation
func logFileWrite(path string, description string) {
	details := map[string]string{
		"file":        path,
		"description": description,
	}
	logAction("FILE_WRITTEN", details)
}

// logServiceChange logs a systemd service change
func logServiceChange(service string, operation string) {
	details := map[string]string{
		"service":   service,
		"operation": operation,
	}
	logAction("SERVICE_CHANGED", details)
}

// logError logs an error
func logError(operation string, err error) {
	details := map[string]string{
		"operation": operation,
		"error":     err.Error(),
	}
	logAction("ERROR", details)
}

// getLogPath returns the path to the log file (for displaying to users)
func getLogPath() string {
	return galleyLogFile
}

// showRecentLogs displays the last N lines of the log file
func showRecentLogs(lines int) error {
	content, err := os.ReadFile(galleyLogFile)
	if err != nil {
		if os.IsNotExist(err) {
			fmt.Println("No log file found yet")
			return nil
		}
		return fmt.Errorf("failed to read log file: %w", err)
	}

	// Simple approach: split by newlines and show last N
	logLines := []byte{}
	lineCount := 0
	for i := len(content) - 1; i >= 0; i-- {
		if content[i] == '\n' {
			lineCount++
			if lineCount > lines {
				logLines = content[i+1:]
				break
			}
		}
	}
	if lineCount <= lines {
		logLines = content
	}

	fmt.Print(string(logLines))
	return nil
}
