package driveradapters

import (
	"context"
	"fmt"
	"net/http"
	"strings"
	"unicode/utf8"

	"github.com/dlclark/regexp2"
	libCommon "github.com/kweaver-ai/kweaver-go-lib/common"
	"github.com/kweaver-ai/kweaver-go-lib/rest"
	"github.com/mitchellh/mapstructure"

	"data-model/common"
	derrors "data-model/errors"
	"data-model/interfaces"
)

// 校验 builtin 参数, 兼容 0, 1 和 bool 类型
func validateBuiltin(ctx context.Context, builtin any) (bool, error) {
	// 不传 builtin 参数时, 默认为false
	if builtin == nil {
		return false, nil
	}

	switch v := builtin.(type) {
	case bool:
		return v, nil
	case float64: // JSON 数字会被解析为 float64
		return v > 0, nil
	default:
		return false, rest.NewHTTPError(ctx, http.StatusBadRequest, derrors.DataModel_DataView_InvalidParameter_Builtin).
			WithErrorDetails(fmt.Sprintf("'builtin' must be boolean or number 0 or 1, got %v", builtin))
	}
}

// 校验过滤条件的类型，兼容旧过滤条件
func validateViewFiltersType(ctx context.Context, cfg any) (*interfaces.CondCfg, error) {
	switch cfg := cfg.(type) {
	case nil:
		return nil, nil
	case *interfaces.CondCfg:
		return cfg, nil
	case []interfaces.Filter:
		return common.ConvertFiltersToCondition(cfg), nil
	case map[string]any:
		if len(cfg) == 0 {
			return nil, nil
		}

		var viewCond *interfaces.CondCfg
		err := mapstructure.Decode(cfg, &viewCond)
		if err != nil {
			return nil, rest.NewHTTPError(ctx, http.StatusBadRequest, derrors.DataModel_InvalidParameter_Filters).
				WithErrorDetails(fmt.Sprintf("mapstructure decode condition failed: %s", err.Error()))
		}
		return viewCond, nil
	case []any:
		var filters []interfaces.Filter
		err := mapstructure.Decode(cfg, &filters)
		if err != nil {
			return nil, rest.NewHTTPError(ctx, http.StatusBadRequest, derrors.DataModel_InvalidParameter_Filters).
				WithErrorDetails(fmt.Sprintf("mapstructure decode filters failed: %s", err.Error()))
		}

		// 将filters转成condition
		return common.ConvertFiltersToCondition(filters), nil
	default:
		return nil, rest.NewHTTPError(ctx, http.StatusBadRequest, derrors.DataModel_InvalidParameter_Filters).
			WithErrorDetails(fmt.Sprintf("unsupported filters type: %T", cfg))
	}
}

// 分组名称校验
func validateGroupName(ctx context.Context, groupName string) error {
	httpErr := validateObjectName(ctx, groupName, interfaces.DATA_VIEW_GROUP)
	if httpErr != nil {
		return httpErr
	}

	// 校验不能包含这些特殊字符，*"\/<>:|?#
	if strings.ContainsAny(groupName, "*\"\\/<>:|?#") {
		return rest.NewHTTPError(ctx, http.StatusBadRequest, derrors.DataModel_DataViewGroup_InvalidParameter_GroupName).
			WithErrorDetails("Group name cannot contain special characters: *\"\\/<>:|?#")
	}

	return nil
}

func validateViewID(ctx context.Context, viewID string, builtin bool) error {
	if viewID != "" {
		if builtin {
			// 内置视图 id 校验，只包含小写英文字母和数字和下划线(_)和连字符(-)，允许以下划线开头，不能超过40个字符
			re := regexp2.MustCompile(interfaces.RegexPattern_Builtin_ViewID, regexp2.RE2)
			match, err := re.MatchString(viewID)
			if err != nil || !match {
				errDetails := `The view id can contain only lowercase letters, digits and underscores(_),
			it cannot exceed 40 characters`
				return rest.NewHTTPError(ctx, http.StatusBadRequest, derrors.DataModel_DataView_InvalidParameter_ViewID).
					WithErrorDetails(errDetails)
			}
		} else {
			// 非内置视图校验数据视图 id，只包含小写英文字母和数字和下划线(_)和连字符(-)，且不能以下划线开头，不能超过40个字符
			re := regexp2.MustCompile(interfaces.RegexPattern_NonBuiltin_ViewID, regexp2.RE2)
			match, err := re.MatchString(viewID)
			if err != nil || !match {
				errDetails := `The view id can contain only lowercase letters, digits and underscores(_),
			it cannot start with underscores and cannot exceed 40 characters`
				return rest.NewHTTPError(ctx, http.StatusBadRequest, derrors.DataModel_DataView_InvalidParameter_ViewID).
					WithErrorDetails(errDetails)
			}
		}
	}

	return nil
}

// // 校验技术名称
// func validateTechnicalName(ctx context.Context, viewType string, technicalName string) error {
// 	// 原子视图不需要校验技术名称
// 	if viewType == interfaces.ViewType_Atomic {
// 		return nil
// 	}
// 	// 自定义视图需要校验技术名称
// 	if technicalName == "" {
// 		return rest.NewHTTPError(ctx, http.StatusBadRequest, derrors.DataModel_DataView_InvalidParameter_TechnicalName)
// 	}

// 	if utf8.RuneCountInString(technicalName) > interfaces.OBJECT_NAME_MAX_LENGTH {
// 		return rest.NewHTTPError(ctx, http.StatusBadRequest, derrors.DataModel_DataView_InvalidParameter_TechnicalName).
// 			WithErrorDetails(fmt.Sprintf("The length of the technical name named %v exceeds %v", technicalName, interfaces.OBJECT_NAME_MAX_LENGTH))
// 	}

// 	// 仅支持小写字母、数字及下划线，且不能以数字开头
// 	re := regexp2.MustCompile(interfaces.RegexPattern_TechnicalName, regexp2.RE2)
// 	match, err := re.MatchString(technicalName)
// 	if err != nil || !match {
// 		return rest.NewHTTPError(ctx, http.StatusBadRequest, derrors.DataModel_DataView_InvalidParameter_TechnicalName).
// 			WithErrorDetails("The technical name can contain only lowercase letters, digits and underscores(_), and cannot start with a digit")
// 	}

// 	return nil
// }

// 数据视图参数校验
func ValidateDataView(ctx context.Context, view *interfaces.DataView) error {
	// 校验数据视图 id
	err := validateViewID(ctx, view.ViewID, view.Builtin)
	if err != nil {
		return err
	}

	// 校验对象名称
	err = validateObjectName(ctx, view.ViewName, interfaces.MODULE_TYPE_DATA_VIEW)
	if err != nil {
		return err
	}

	// 校验技术名称
	// err = validateTechnicalName(ctx, view.Type, view.TechnicalName)
	// if err != nil {
	// 	return err
	// }

	// 校验分组名称
	if view.GroupName != "" {
		err = validateGroupName(ctx, view.GroupName)
		if err != nil {
			return err
		}
	}

	// 校验标签
	err = validateObjectTags(ctx, view.Tags)
	if err != nil {
		return err
	}

	// 去掉tag前后空格以及数组去重
	view.Tags = libCommon.TagSliceTransform(view.Tags)

	// 校验备注
	err = validateObjectComment(ctx, view.Comment)
	if err != nil {
		return err
	}

	// 校验 dataScope
	err = validateDataScope(ctx, view.DataScope)
	if err != nil {
		return err
	}

	// var dataSourceType string
	// 视图数据源类型非空和类型校验
	// if value, ok := view.DataSource["type"]; !ok {
	// 	return rest.NewHTTPError(ctx, http.StatusBadRequest, derrors.DataModel_DataView_InvalidParameter_DataSource).
	// 		WithErrorDetails("The dataSource type is null")
	// } else {
	// 	if dataSourceType, ok = value.(string); !ok {
	// 		return rest.NewHTTPError(ctx, http.StatusBadRequest, derrors.DataModel_DataView_InvalidParameter_DataSource).
	// 			WithErrorDetails("The dataSource type is not string")
	// 	}
	// }

	// 数据源类型校验、索引库校验
	// switch dataSourceType {
	// case interfaces.INDEX_BASE:
	// 	if value, ok := view.DataSource[interfaces.INDEX_BASE]; !ok {
	// 		return rest.NewHTTPError(ctx, http.StatusBadRequest, derrors.DataModel_DataView_InvalidParameter_DataSource).
	// 			WithErrorDetails("There is no 'index_base' parameter in the dataSource")
	// 	} else {

	// 		if bases, ok := value.([]any); ok {
	// 			if len(bases) <= 0 {
	// 				return rest.NewHTTPError(ctx, http.StatusBadRequest, derrors.DataModel_DataView_InvalidParameter_DataSource).
	// 					WithErrorDetails("The number of index base must be at least one")
	// 			}

	// 			// 如果选择全部字段，来源索引库只能选一个，以减少字段类型冲突
	// 			if view.FieldScope == interfaces.FieldScope_All && len(bases) > 1 {
	// 				return rest.NewHTTPError(ctx, http.StatusBadRequest, derrors.DataModel_DataView_InvalidParameter_DataSource).
	// 					WithErrorDetails("When the field scope is 'all fields', there can be only one index base")
	// 			}
	// 		} else {
	// 			return rest.NewHTTPError(ctx, http.StatusBadRequest, derrors.DataModel_DataView_InvalidParameter_DataSource).
	// 				WithErrorDetails("The index base names are not a list")
	// 		}
	// 	}
	// default:
	// 	return rest.NewHTTPError(ctx, http.StatusBadRequest, derrors.DataModel_DataView_UnsupportDataSourceType).
	// 		WithErrorDetails("Only 'index_base' is supported currently")
	// }

	// 字段范围校验，只能是0 或 1
	// if view.FieldScope != interfaces.FieldScope_All && view.FieldScope != interfaces.FieldScope_Custom {
	// 	return rest.NewHTTPError(ctx, http.StatusBadRequest, derrors.DataModel_DataView_InvalidParameter_FieldScope).
	// 		WithErrorDetails("The field scope can only be 0 or 1")
	// }

	// 字段范围为部分字段时校验
	// if view.FieldScope == interfaces.FieldScope_Custom {
	// 字段列表非空校验
	// if len(view.Fields) == 0 {
	// 	return rest.NewHTTPError(ctx, http.StatusBadRequest, derrors.DataModel_DataView_NullParameter_Fields).
	// 		WithErrorDetails("If the field scope is partial, the fields list is not empty")
	// }

	// 校验字段名称、显示名是否重复
	nameMap := make(map[string]any)
	displayNameMap := make(map[string]any)
	for _, field := range view.Fields {
		if field.Name == "" {
			return rest.NewHTTPError(ctx, http.StatusBadRequest, derrors.DataModel_DataView_InvalidParameter_FieldName).
				WithErrorDetails("The field name is null")
		}

		// 校验名称长度
		if utf8.RuneCountInString(field.Name) > interfaces.FIELD_NAME_MAX_LENGTH {
			errDetails := fmt.Sprintf("The length of the field name %s exceeds %d", field.Name, interfaces.FIELD_NAME_MAX_LENGTH)
			return rest.NewHTTPError(ctx, http.StatusBadRequest, derrors.DataModel_DataView_LengthExceeded_FieldName).
				WithErrorDetails(errDetails)
		}

		// 如果display_name为 "", 将display_name的值等于field的值
		if field.DisplayName == "" {
			field.DisplayName = field.Name
		}

		// 校验显示名长度
		if utf8.RuneCountInString(field.DisplayName) > interfaces.FIELD_DISPLAY_NAME_MAX_LENGTH {
			errDetails := fmt.Sprintf("The length of the field display name %s exceeds %d", field.DisplayName, interfaces.FIELD_DISPLAY_NAME_MAX_LENGTH)
			return rest.NewHTTPError(ctx, http.StatusBadRequest, derrors.DataModel_DataView_LengthExceeded_FieldDisplayName).
				WithErrorDetails(errDetails)
		}

		// 校验字段名称是否重复
		if _, ok := nameMap[field.Name]; !ok {
			nameMap[field.Name] = nil
		} else {
			errDetails := fmt.Sprintf("Data view field '%s' name '%s' already exists", field.Name, field.Name)
			return rest.NewHTTPError(ctx, http.StatusBadRequest, derrors.DataModel_DataView_Duplicated_FieldName).
				WithDescription(map[string]any{"FieldName": field.Name}).
				WithErrorDetails(errDetails)
		}

		if _, ok := displayNameMap[field.DisplayName]; !ok {
			displayNameMap[field.DisplayName] = nil
		} else {
			errDetails := fmt.Sprintf("Data view field '%s' display name '%s' already exists", field.Name, field.DisplayName)
			return rest.NewHTTPError(ctx, http.StatusBadRequest, derrors.DataModel_DataView_Duplicated_FieldDisplayName).
				WithDescription(map[string]any{"FieldName": field.Name, "DisplayName": field.DisplayName}).
				WithErrorDetails(errDetails)
		}
	}
	// }

	return nil
}

// 校验自定义视图配置
func validateDataScope(ctx context.Context, nodes []*interfaces.DataScopeNode) error {
	if nodes == nil {
		return nil
	}

	if len(nodes) > 20 {
		return rest.NewHTTPError(ctx, http.StatusBadRequest, derrors.DataModel_DataView_InvalidParameter_DataScope).
			WithErrorDetails("The data scope nodes cannot be more than 20")
	}

	for _, node := range nodes {
		// 检测 nodeType
		if _, ok := interfaces.DataScopeNodeTypeMap[node.Type]; !ok {
			return rest.NewHTTPError(ctx, http.StatusBadRequest, derrors.DataModel_DataView_InvalidParameter_DataScope).
				WithErrorDetails("The data scope node type is invalid")
		}

	}

	return nil
}

// 校验数据视图行列权限对象
func validateDataViewRowColumnRule(ctx context.Context, rule *interfaces.DataViewRowColumnRule) error {
	if rule.RuleName == "" {
		return rest.NewHTTPError(ctx, http.StatusBadRequest, derrors.DataModel_DataViewRowColumnRule_NullParameter_RuleName).
			WithErrorDetails("The data view row column rule name is null")
	}

	if rule.ViewID == "" {
		return rest.NewHTTPError(ctx, http.StatusBadRequest, derrors.DataModel_DataViewRowColumnRule_NullParameter_ViewID).
			WithErrorDetails("The data view row column rule view id is null")
	}

	// 校验对象名称
	err := validateObjectName(ctx, rule.RuleName, interfaces.MODULE_TYPE_DATA_VIEW_ROW_COLUMN_RULE)
	if err != nil {
		return err
	}

	// 校验标签
	err = validateObjectTags(ctx, rule.Tags)
	if err != nil {
		return err
	}

	// 去掉tag前后空格以及数组去重
	rule.Tags = libCommon.TagSliceTransform(rule.Tags)

	// 校验备注
	err = validateObjectComment(ctx, rule.Comment)
	if err != nil {
		return err
	}

	// 字段不能为空
	if len(rule.Fields) == 0 {
		return rest.NewHTTPError(ctx, http.StatusBadRequest, rest.PublicError_BadRequest).
			WithErrorDetails("The data view row column rule fields is null")
	}

	// 校验行过滤条件
	err = validateCond(ctx, rule.RowFilters)
	if err != nil {
		return err
	}

	return nil
}
