package main

import (
	"fmt"

	"github.com/spf13/cobra"
)

var (
	flagLogsLines int
	flagLogsPath  bool
)

var logsCmd = &cobra.Command{
	Use:   "logs",
	Short: "View Galley CLI action logs",
	Long:  "Display logs of all actions performed by the Galley CLI on this system",
	RunE:  runLogs,
}

func init() {
	logsCmd.Flags().IntVarP(&flagLogsLines, "lines", "n", 50, "Number of lines to show")
	logsCmd.Flags().BoolVar(&flagLogsPath, "path", false, "Show the log file path")
}

func runLogs(cmd *cobra.Command, args []string) error {
	if flagLogsPath {
		fmt.Println(getLogPath())
		return nil
	}

	if err := showRecentLogs(flagLogsLines); err != nil {
		return fmt.Errorf("failed to show logs: %w", err)
	}

	return nil
}
