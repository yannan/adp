package interfaces

import (
	"context"
)

//go:generate mockgen -source ../interfaces/vega_access.go -destination ../interfaces/mock/mock_vega_access.go
type VegaAccess interface {
	GetVegaViewFieldsByID(ctx context.Context, viewID string) (VegaViewWithFields, error)
	FetchDatasFromVega(ctx context.Context, nextUri string, sql string) (VegaFetchData, error)
}
