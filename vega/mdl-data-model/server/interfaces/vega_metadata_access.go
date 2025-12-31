package interfaces

import (
	"context"
	"fmt"
)

type ListMetadataTablesParams struct {
	DataSourceId string
	Keyword      string // 模糊搜索表名称
	UpdateTime   string // 更新时间
	PaginationQueryParameters
}

type SimpleMetadataTable struct {
	ID         string `json:"id"`
	Name       string `json:"name"`
	UpdateTime string `json:"update_time"`
}

type MetadataTable struct {
	TableID    string         `json:"table_id"`
	Table      *TableInfo     `json:"table"`
	DataSource DataSourceInfo `json:"datasource"`
	FieldList  []*MetaField   `json:"field_list"`
}

type TableInfo struct {
	ID             string              `json:"table_id"`
	Name           string              `json:"table_name"`
	AdvancedParams AdvancedParamStruct `json:"table_advanced_params"`
	// AdvancedParamsStruct AdvancedParamStruct `json:"-"`
	Description string `json:"table_description"`
	Rows        int64  `json:"table_rows"`
}

type DataSourceInfo struct {
	DataSourceID   string `json:"ds_id"`
	DataSourceName string `json:"ds_name"`
	Type           string `json:"ds_type"`
	Catalog        string `json:"ds_catalog"`
	Database       string `json:"ds_database"`
	Schema         string `json:"ds_schema"`
	// DataSourceConnectProtocol       string `json:"ds_connect_protocol"`
	// DataSourceHost                  string `json:"ds_host"`
	// DataSourcePort                  string `json:"ds_port"`
	// DataSourceAccount               string `json:"ds_account"`
	// DataSourcePassword              string `json:"ds_password"`
	// DataSourceStorageProtocol       string `json:"ds_storage_protocol"`
	// DataSourceStorageBase           string `json:"ds_storage_base"`
	// DataSourceToken                 string `json:"ds_token"`
	// DataSourceReplicaSet            string `json:"ds_replicaSet"`
	// DataSourceConnectionSource      string `json:"ds_connection_source"`
	// DataSourceOrganizationStructure string `json:"ds_organization_structure"`
	// DataSourceIsBuiltIn             int    `json:"ds_is_built_in"`
	// DataSourceComment               string `json:"ds_comment"`
}

type MetaField struct {
	FieldName      string              `json:"f_field_name"`
	TableID        string              `json:"f_table_id"`
	TableName      string              `json:"f_table_name"`
	FieldType      string              `json:"f_field_type"`
	FieldLength    int32               `json:"f_field_length"`
	FieldPrecision int32               `json:"f_field_precision"`
	FieldComment   string              `json:"f_field_comment"`
	AdvancedParams AdvancedParamStruct `json:"f_advanced_params"`
	// FieldOrderNo   string              `json:"f_field_order_no"`
	// AdvancedParamsStruct AdvancedParamStruct `json:"-"`
}

func (fb *MetaField) String() string {
	return fmt.Sprintf("MetaField{name: %s, type: %s, comment: %s}", fb.FieldName, fb.FieldType, fb.FieldComment)
}

type AdvancedParamStruct []*AdvancedParams

//go:generate mockgen -source ../interfaces/vega_metadata_access.go -destination ../interfaces/mock/mock_vega_metadata_access.go
type VegaMetadataAccess interface {
	ListMetadataTablesBySourceID(ctx context.Context, params *ListMetadataTablesParams) ([]SimpleMetadataTable, error)
	GetMetadataTablesByIDs(ctx context.Context, tableIDs []string) ([]MetadataTable, error)
}

//region GetDataTableDetailBatch

// type GetDataTableDetailBatchReq struct {
// 	Limit        int    `json:"limit"`
// 	Offset       int    `json:"offset"`
// 	DataSourceId string `json:"data_source_id"`
// 	SchemaId     string `json:"schema_id"`
// 	//TableIds     []string `json:"table_ids"`
// }

// type GetDataTableDetailBatchRes struct {
// 	Code        string                            `json:"code"`
// 	Description string                            `json:"description"`
// 	TotalCount  int                               `json:"total_count"`
// 	Solution    string                            `json:"solution"`
// 	Data        []*GetDataTableDetailDataBatchRes `json:"data"`
// }

// type GetDataTableDetailDataBatchRes struct {
// 	SchemaId                  string              `json:"schema_id"`
// 	SchemaName                string              `json:"schema_name"`
// 	Id                        string              `json:"id"`
// 	Name                      string              `json:"name"`
// 	OrgName                   string              `json:"org_name"`
// 	Description               string              `json:"description"`
// 	AdvancedParams            AdvancedParamStruct `json:"_"`
// 	AdvancedParamMap          map[string]string   `json:"-"`
// 	AdvancedParamMapAvailable bool                `json:"-"`
// 	AdvancedParamsO           string              `json:"advanced_params"`
// 	HaveField                 bool                `json:"have_field"`
// 	Fields                    []*FieldsBatch      `json:"fields"`
// }

// type FieldsBatch struct {
// 	ID                        string              `json:"id"`
// 	FieldName                 string              `json:"field_name"`
// 	OrgFieldName              string              `json:"org_field_name"`
// 	FieldLength               int32               `json:"field_length"`
// 	FieldPrecision            int32               `json:"field_precision"`
// 	FieldComment              string              `json:"field_comment"`
// 	AdvancedParams            AdvancedParamStruct `json:"_"`
// 	AdvancedParamMap          map[string]string   `json:"-"`
// 	AdvancedParamMapAvailable bool                `json:"-"`
// 	AdvancedParamsO           string              `json:"advanced_params"`
// 	FieldTypeName             string              `json:"field_type_name"`
// }

func (s AdvancedParamStruct) GetValue(key string) any {
	for _, params := range s {
		if params.Key == key {
			val := params.Value
			if val == nil {
				switch key {
				case VirtualDataType:
					return ""
				case OriginFieldType:
					return ""
				case IsNullable:
					return ""
				case ColumnDef:
					return ""
				case CheckPrimaryKey:
					return ""
				case ExcelSheet:
					return ""
				case ExcelStartCell:
					return ""
				case ExcelEndCell:
					return ""
				case ExcelHasHeaders:
					return false
				case ExcelSheetAsNewColumn:
					return false
				case ExcelFileName:
					return ""
				default:
					return ""
				}
			}

			return val
		}
	}
	// TODO
	return ""
}

func (s AdvancedParamStruct) IsPrimaryKey() bool {
	for _, params := range s {
		if params.Key == CheckPrimaryKey && params.Value == "YES" {
			return true
		}
		if params.Key == CheckPrimaryKey && params.Value == "NO" {
			return false
		}
	}
	return false
}
