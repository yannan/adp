package version

import (
	"runtime"
)

var (
	ServerName    string = "vega-gateway-pro"
	ServerVersion string = "1.0.0"
	LanguageGo    string = "go"
	GoVersion     string = runtime.Version()
	GoArch        string = runtime.GOARCH
)
