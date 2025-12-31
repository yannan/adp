package version

import (
	"runtime"
)

var (
	ServerName    string = "mdl-data-model-job"
	ServerVersion string = "6.0.0"
	LanguageGo    string = "go"
	GoVersion     string = runtime.Version()
	GoArch        string = runtime.GOARCH
)
