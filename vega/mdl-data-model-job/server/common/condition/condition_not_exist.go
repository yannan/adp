package condition

import (
	"context"
)

type NotExistCond struct {
	mCfg       *CondCfg
	mfieldName string
}

func NewNotExistCond(ctx context.Context, cfg *CondCfg) (Condition, error) {
	return &NotExistCond{
		mCfg:       cfg,
		mfieldName: cfg.Name,
	}, nil
}

func (cond *NotExistCond) Pass(ctx context.Context, data *OriginalData) (bool, error) {
	vData, err := data.GetData(ctx, cond.mCfg.NameField)
	if err != nil {
		return false, err
	}

	return (len(vData) == 0), nil
}
