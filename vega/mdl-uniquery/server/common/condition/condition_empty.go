package condition

import (
	"context"
	"fmt"

	dtype "uniquery/interfaces/data_type"
)

type EmptyCond struct {
	mCfg             *CondCfg
	mFilterFieldName string
}

func NewEmptyCond(ctx context.Context, cfg *CondCfg, fieldsMap map[string]*ViewField) (Condition, error) {
	// 只允许字符串类型
	if !dtype.DataType_IsString(cfg.NameField.Type) {
		return nil, fmt.Errorf("condition [empty] left field %s is not of string type, but %s", cfg.Name, cfg.NameField.Type)
	}

	return &EmptyCond{
		mCfg:             cfg,
		mFilterFieldName: getFilterFieldName(ctx, cfg.Name, fieldsMap, false),
	}, nil

}

// 检查字段值是否为空字符串
func (cond *EmptyCond) Convert(ctx context.Context) (string, error) {
	dslStr := fmt.Sprintf(`
		{
			"term": {
				"%s": ""
			}
		}`, cond.mFilterFieldName)

	return dslStr, nil
}

func (cond *EmptyCond) Convert2SQL(ctx context.Context) (string, error) {
	sqlStr := fmt.Sprintf(`"%s" IS NULL OR "%s" = ''`, cond.mFilterFieldName, cond.mFilterFieldName)
	return sqlStr, nil
}
