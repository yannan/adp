package value_opt

const (
	ValueFrom_Const = "const"
	ValueFrom_Field = "field"
	ValueFrom_User  = "user"
)

type ValueOptCfg struct {
	ValueFrom string `json:"value_from,omitempty" mapstructure:"value_from"`
	Value     any    `json:"value,omitempty" mapstructure:"value"`
	RealValue any    `json:"real_value,omitempty" mapstructure:"real_value"`
}
