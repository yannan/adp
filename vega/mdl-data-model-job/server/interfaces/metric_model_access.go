package interfaces

import (
	"context"
)

//go:generate mockgen -source ../interfaces/metric_model_access.go -destination ../interfaces/mock/mock_metric_model_access.go
type MetricModelAccess interface {
	GetTaskPlanTimeById(ctx context.Context, taskId string) (int64, error)
	UpdateTaskAttributesById(ctx context.Context, taskId string, task MetricTask) error
}
