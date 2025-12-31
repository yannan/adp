package condition

import (
	"context"
	"fmt"
)

type NotNullCond struct {
	mCfg             *CondCfg
	mFilterFieldName string
}

func NewNotNullCond(ctx context.Context, cfg *CondCfg, fieldsMap map[string]*ViewField) (Condition, error) {
	return &NotNullCond{
		mCfg:             cfg,
		mFilterFieldName: getFilterFieldName(ctx, cfg.Name, fieldsMap, false),
	}, nil

}

// 检查字段值是否为空字符串
func (cond *NotNullCond) Convert(ctx context.Context) (string, error) {

	return "", nil
}

func (cond *NotNullCond) Convert2SQL(ctx context.Context) (string, error) {
	sqlStr := fmt.Sprintf(`"%s" IS NOT NULL`, cond.mFilterFieldName)
	return sqlStr, nil
}
