package condition

import (
	"context"
	"fmt"

	vopt "uniquery/common/value_opt"
	dtype "uniquery/interfaces/data_type"
)

type NotPrefixCond struct {
	mCfg             *CondCfg
	mValue           string
	mFilterFieldName string
}

func NewNotPrefixCond(ctx context.Context, cfg *CondCfg, fieldsMap map[string]*ViewField) (Condition, error) {
	if !dtype.DataType_IsString(cfg.NameField.Type) &&
		dtype.SimpleTypeMapping[cfg.NameField.Type] != dtype.DataType_String {
		return nil, fmt.Errorf("condition [not_prefix] left field is not a string field: %s:%s", cfg.NameField.Name, cfg.NameField.Type)
	}

	if cfg.ValueOptCfg.ValueFrom != vopt.ValueFrom_Const {
		return nil, fmt.Errorf("condition [not_prefix] does not support value_from type '%s'", cfg.ValueFrom)
	}

	val, ok := cfg.ValueOptCfg.Value.(string)
	if !ok {
		return nil, fmt.Errorf("condition [not_prefix] right value is not a string value: %v", cfg.Value)
	}

	return &NotPrefixCond{
		mCfg:             cfg,
		mValue:           val,
		mFilterFieldName: getFilterFieldName(ctx, cfg.Name, fieldsMap, false),
	}, nil
}

func (cond *NotPrefixCond) Convert(ctx context.Context) (string, error) {
	return "", nil
}

func (cond *NotPrefixCond) Convert2SQL(ctx context.Context) (string, error) {
	v := cond.mCfg.Value
	vStr, ok := v.(string)
	if ok {
		v = Special.Replace(fmt.Sprintf("%v", vStr))
	}

	vStr = fmt.Sprintf("%v", v)
	sqlStr := fmt.Sprintf(`"%s" NOT LIKE '%s'`, cond.mFilterFieldName, vStr+"%")

	return sqlStr, nil
}
