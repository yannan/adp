package interfaces

import (
	"context"
)

//go:generate mockgen -source ../interfaces/event_model_access.go -destination ../interfaces/mock/mock_event_model_access.go
type EventModelAccess interface {
	UpdateEventTaskAttributesById(ctx context.Context, task EventTask) error
	GetEventModel(ctx context.Context, modelID string) (EventModel, error)
}
