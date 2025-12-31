package condition

import (
	"database/sql"
	"reflect"
	"strings"

	vopt "uniquery/common/value_opt"
)

type contextKey string // 自定义专属的key类型

const (
	vType_Atomic = "atomic"
	vType_Custom = "custom"

	QueryType_DSL       = "DSL"
	QueryType_SQL       = "SQL"
	QueryType_IndexBase = "IndexBase"
)

const (
	CtxKey_QueryType contextKey = "query-type" // 避免直接使用string

	DESENSITIZE_FIELD_SUFFIX = "_desensitize"

	AllField = "*"

	MetaField_ID = "__id"

	OS_MetaField_ID = "_id"
)

const (
	OperationAnd = "and"
	OperationOr  = "or"

	OperationEq          = "=="
	OperationNotEq       = "!="
	OperationGt          = ">"
	OperationGte         = ">="
	OperationLt          = "<"
	OperationLte         = "<="
	OperationIn          = "in"
	OperationNotIn       = "not_in"
	OperationLike        = "like"
	OperationNotLike     = "not_like"
	OperationContain     = "contain"
	OperationNotContain  = "not_contain"
	OperationRange       = "range"
	OperationOutRange    = "out_range"
	OperationExist       = "exist"
	OperationNotExist    = "not_exist"
	OperationEmpty       = "empty"
	OperationNotEmpty    = "not_empty"
	OperationRegex       = "regex"
	OperationMatch       = "match"
	OperationMatchPhrase = "match_phrase"
	OperationPrefix      = "prefix"
	OperationNotPrefix   = "not_prefix"
	OperationNull        = "null"
	OperationNotNull     = "not_null"
	OperationTrue        = "true"
	OperationFalse       = "false"
	OperationBefore      = "before"
	OperationCurrent     = "current"
	OperationBetween     = "between"
	OperationKnnVector   = "knn_vector"
	OperationMultiMatch  = "multi_match"
)

var (
	OperationMap = map[string]struct{}{
		"=":                  {}, // 兼容filter中定义的等于是 =
		OperationAnd:         {},
		OperationOr:          {},
		OperationEq:          {},
		OperationNotEq:       {},
		OperationGt:          {},
		OperationGte:         {},
		OperationLt:          {},
		OperationLte:         {},
		OperationIn:          {},
		OperationNotIn:       {},
		OperationLike:        {},
		OperationNotLike:     {},
		OperationContain:     {},
		OperationNotContain:  {},
		OperationRange:       {},
		OperationOutRange:    {},
		OperationExist:       {},
		OperationNotExist:    {},
		OperationEmpty:       {},
		OperationNotEmpty:    {},
		OperationRegex:       {},
		OperationMatch:       {},
		OperationMatchPhrase: {},
		OperationPrefix:      {},
		OperationNotPrefix:   {},
		OperationNull:        {},
		OperationNotNull:     {},
		OperationTrue:        {},
		OperationFalse:       {},
		OperationBefore:      {},
		OperationCurrent:     {},
		OperationBetween:     {},
		OperationKnnVector:   {},
		OperationMultiMatch:  {},
	}

	NotRequiredValueOperationMap = map[string]struct{}{
		OperationExist:    {},
		OperationNotExist: {},
		OperationEmpty:    {},
		OperationNotEmpty: {},
		OperationNull:     {},
		OperationNotNull:  {},
		OperationTrue:     {},
		OperationFalse:    {},
	}

	HavingOperationMap = map[string]struct{}{
		OperationEq:       {},
		OperationNotEq:    {},
		OperationGt:       {},
		OperationGte:      {},
		OperationLt:       {},
		OperationLte:      {},
		OperationIn:       {},
		OperationNotIn:    {},
		OperationRange:    {},
		OperationOutRange: {},
	}

	// match_type
	MatchTypeMap = map[string]bool{
		"best_fields":   true,
		"most_fields":   true,
		"cross_fields":  true,
		"phrase":        true,
		"phrase_prefix": true,
		"bool_prefix":   true,
	}
)

// type VectorResp struct {
// 	Object string    `json:"object"`
// 	Vector []float32 `json:"embedding"`
// 	Index  int       `json:"index"`
// }

type Filter struct {
	Name      string `json:"name"`
	Operation string `json:"operation"`
	Value     any    `json:"value"`
}

type CondCfg struct {
	Name             string     `json:"field,omitempty" mapstructure:"field"` // 传递name
	Operation        string     `json:"operation,omitempty" mapstructure:"operation"`
	SubConds         []*CondCfg `json:"sub_conditions,omitempty" mapstructure:"sub_conditions"`
	vopt.ValueOptCfg `mapstructure:",squash"`

	RemainCfg map[string]any `mapstructure:",remain"`

	NameField *ViewField `json:"-" mapstructure:"-"`
}

// 数据视图字段
type ViewField struct {
	Name              string       `json:"name" mapstructure:"name"`
	Type              string       `json:"type" mapstructure:"type"`
	Comment           string       `json:"comment" mapstructure:"comment"`
	DisplayName       string       `json:"display_name" mapstructure:"display_name"`
	OriginalName      string       `json:"original_name" mapstructure:"original_name"`
	DataLength        int32        `json:"data_length,omitempty" mapstructure:"data_length"`
	DataAccuracy      int32        `json:"data_accuracy,omitempty" mapstructure:"data_accuracy"`
	Status            string       `json:"status,omitempty" mapstructure:"status"`
	IsNullable        string       `json:"is_nullable,omitempty" mapstructure:"is_nullable"`
	BusinessTimestamp bool         `json:"business_timestamp,omitempty" mapstructure:"business_timestamp"`
	SrcNodeID         string       `json:"src_node_id,omitempty"  mapstructure:"src_node_id"`
	SrcNodeName       string       `json:"src_node_name,omitempty" mapstructure:"src_node_name"`
	PrimaryKey        sql.NullBool `json:"-" mapstructure:"-"`

	Path []string `json:"-" mapstructure:"-"`
}

func (field *ViewField) InitFieldPath() {
	if len(field.Path) == 0 {
		field.Path = strings.Split(field.Name, ".")
	}
}

func IsSlice(i any) bool {
	kind := reflect.ValueOf(i).Kind()
	return kind == reflect.Slice || kind == reflect.Array
}

func IsSameType(arr []any) bool {
	if len(arr) == 0 {
		return true
	}

	firstType := reflect.TypeOf(arr[0])
	for _, v := range arr {
		if reflect.TypeOf(v) != firstType {
			return false
		}
	}

	return true
}
