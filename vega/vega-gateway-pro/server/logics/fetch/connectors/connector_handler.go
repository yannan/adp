package connectors

import (
	"fmt"
	_ "github.com/go-sql-driver/mysql" // MySQL驱动
	"github.com/kweaver-ai/kweaver-go-lib/logger"
	"vega-gateway-pro/interfaces"
)

// ConnectorHandler 定义连接器接口
type ConnectorHandler interface {
	GetResultSet(sql string) (any, error)
	GetColumns(resultSet any) ([]*interfaces.Column, error)
	GetData(resultSet any, columnSize int, queryType int, batchSize int) (any, []*[]any, error)
	Close() error
}

// NewConnectorHandler 根据数据源类型创建对应的处理器
func NewConnectorHandler(dataSource *interfaces.DataSource) (ConnectorHandler, error) {
	switch dataSource.Type {
	case "mysql":
		return NewMySQLConnector(dataSource)
	case "maria":
		return NewMySQLConnector(dataSource)
	default:
		logger.Errorf("unsupported data source type: %s", dataSource.Type)
		return nil, fmt.Errorf("unsupported data source type: %s", dataSource.Type)
	}
}
