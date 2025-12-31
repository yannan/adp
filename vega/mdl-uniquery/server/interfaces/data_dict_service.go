package interfaces

import "context"

//go:generate mockgen -source ../interfaces/data_dict_service.go -destination ../interfaces/mock/mock_data_dict_service.go
type DataDictService interface {
	LoadDict(ctx context.Context, dictName string) error
	UpdateCache(ctx context.Context, dict DataDict)
}
