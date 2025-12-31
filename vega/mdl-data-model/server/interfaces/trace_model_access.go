package interfaces

import "context"

//go:generate mockgen -source ../interfaces/trace_model_access.go -destination ../interfaces/mock/mock_trace_model_access.go
type TraceModelAccess interface {
	CreateTraceModels(ctx context.Context, models []TraceModel) error
	DeleteTraceModels(ctx context.Context, modelIDs []string) error
	UpdateTraceModel(ctx context.Context, model TraceModel) error
	GetDetailedTraceModelMapByIDs(ctx context.Context, modelIDs []string) (map[string]TraceModel, error)
	GetSimpleTraceModelMapByIDs(ctx context.Context, modelIDs []string) (map[string]TraceModel, error)
	GetSimpleTraceModelMapByNames(ctx context.Context, modelNames []string) (map[string]TraceModel, error)
	ListTraceModels(ctx context.Context, queryParams TraceModelListQueryParams) ([]TraceModelListEntry, error)
	GetTraceModelTotal(ctx context.Context, queryParams TraceModelListQueryParams) (int64, error)
}
