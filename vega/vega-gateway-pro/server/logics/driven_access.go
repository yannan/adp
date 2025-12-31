package logics

import (
	"vega-gateway-pro/interfaces"
)

var (
	DataConnectionAccess interfaces.DataConnectionAccess
	VegaCalculateAccess  interfaces.VegaCalculateAccess
)

func SetDataConnectionAccess(dca interfaces.DataConnectionAccess) {
	DataConnectionAccess = dca
}

func SetVegaViewAccess(vva interfaces.VegaCalculateAccess) {
	VegaCalculateAccess = vva
}
