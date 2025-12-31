package interfaces

import "time"

const (
	EXPIRATION_TIME = 2 * time.Minute
	DELETE_TIME     = 5 * time.Minute
)

type EventSubService interface {
	Subscribe(exitCh chan bool) error
}

type DataSource struct {
	DataSourceId   string
	DataSourceType string
}
