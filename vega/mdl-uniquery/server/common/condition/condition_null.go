package condition

import (
	"context"
	"fmt"
)

type NullCond struct {
	mCfg             *CondCfg
	mFilterFieldName string
}

func NewNullCond(ctx context.Context, cfg *CondCfg, fieldsMap map[string]*ViewField) (Condition, error) {
	return &NullCond{
		mCfg:             cfg,
		mFilterFieldName: getFilterFieldName(ctx, cfg.Name, fieldsMap, false),
	}, nil

}

// 检查字段值是否为空字符串
func (cond *NullCond) Convert(ctx context.Context) (string, error) {

	return "", nil
}

func (cond *NullCond) Convert2SQL(ctx context.Context) (string, error) {
	sqlStr := fmt.Sprintf(`"%s" IS NULL`, cond.mFilterFieldName)
	return sqlStr, nil
}
