package condition

import (
	"context"
	"fmt"

	dtype "uniquery/interfaces/data_type"
)

type TrueCond struct {
	mCfg             *CondCfg
	mFilterFieldName string
}

func NewTrueCond(ctx context.Context, cfg *CondCfg, fieldsMap map[string]*ViewField) (Condition, error) {
	if cfg.NameField.Type != dtype.DataType_Boolean {
		return nil, fmt.Errorf("condition [true] left field is not a boolean field: %s:%s", cfg.NameField.Name, cfg.NameField.Type)
	}

	return &TrueCond{
		mCfg:             cfg,
		mFilterFieldName: getFilterFieldName(ctx, cfg.Name, fieldsMap, false),
	}, nil
}

func (cond *TrueCond) Convert(ctx context.Context) (string, error) {
	return "", nil
}

func (cond *TrueCond) Convert2SQL(ctx context.Context) (string, error) {
	sqlStr := fmt.Sprintf(`"%s" = true`, cond.mFilterFieldName)
	return sqlStr, nil
}
