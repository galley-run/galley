package main

import (
	"testing"
)

func TestExtractJWTSubject(t *testing.T) {
	tests := []struct {
		name    string
		token   string
		want    string
		wantErr bool
	}{
		{
			name:    "valid JWT token",
			token:   "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ2ZXNzZWwtZW5naW5lLTEyMyIsIm5hbWUiOiJUZXN0IFVzZXIiLCJpYXQiOjE1MTYyMzkwMjJ9.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c",
			want:    "vessel-engine-123",
			wantErr: false,
		},
		{
			name:    "invalid format - too few parts",
			token:   "invalid.token",
			want:    "",
			wantErr: true,
		},
		{
			name:    "invalid format - too many parts",
			token:   "too.many.parts.here",
			want:    "",
			wantErr: true,
		},
		{
			name:    "invalid base64 encoding",
			token:   "header.!!!invalid!!!.signature",
			want:    "",
			wantErr: true,
		},
		{
			name:    "invalid JSON in payload",
			token:   "header.aW52YWxpZGpzb24.signature",
			want:    "",
			wantErr: true,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, err := extractJWTSubject(tt.token)
			if (err != nil) != tt.wantErr {
				t.Errorf("extractJWTSubject() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if got != tt.want {
				t.Errorf("extractJWTSubject() = %v, want %v", got, tt.want)
			}
		})
	}
}
