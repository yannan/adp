package interfaces

import (
	"context"
)

//go:generate mockgen -source ../interfaces/data_connection_service.go -destination ../interfaces/mock/mock_data_connection_service.go
type DataConnectionService interface {
	GetDataConnectionByID(ctx context.Context, connID string) (*DataConnection, bool, error)
}
