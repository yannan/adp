package condition

import (
	"context"
	"fmt"

	dtype "uniquery/interfaces/data_type"
)

type FalseCond struct {
	mCfg             *CondCfg
	mFilterFieldName string
}

func NewFalseCond(ctx context.Context, cfg *CondCfg, fieldsMap map[string]*ViewField) (Condition, error) {
	if cfg.NameField.Type != dtype.DataType_Boolean {
		return nil, fmt.Errorf("condition [false] left field is not a boolean field: %s:%s", cfg.NameField.Name, cfg.NameField.Type)
	}

	return &FalseCond{
		mCfg:             cfg,
		mFilterFieldName: getFilterFieldName(ctx, cfg.Name, fieldsMap, false),
	}, nil
}

func (cond *FalseCond) Convert(ctx context.Context) (string, error) {
	return "", nil
}

func (cond *FalseCond) Convert2SQL(ctx context.Context) (string, error) {
	sqlStr := fmt.Sprintf(`"%s" = false`, cond.mFilterFieldName)
	return sqlStr, nil
}
