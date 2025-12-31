package interfaces

import (
	"context"
)

//go:generate mockgen -source ../interfaces/objective_model_service.go -destination ../interfaces/mock/mock_objective_model_service.go
type ObjectiveModelService interface {
	Simulate(ctx context.Context, query ObjectiveModelQuery) (ObjectiveModelUniResponse, error)
	Exec(ctx context.Context, query ObjectiveModelQuery) (ObjectiveModelUniResponse, error)
}
