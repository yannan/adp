package interfaces

import (
	"context"
)

//go:generate mockgen -source ../interfaces/event_service.go -destination ../interfaces/mock/mock_event_service.go
type EventService interface {
	Query(ctx context.Context, queryReq EventQueryReq) (int, any, []Records, error)
	QuerySingleEventByEventId(ctx context.Context, query EventDetailsQueryReq) (IEvent, error)
}

type EventEngine interface {
	Apply(ctx context.Context, query EventQuery, em EventModel) (IEvents, Records, int)
	Assemble(Events IEvents, sortKey string, direction string, limit int64, offsert int64) (IEvents, error)
	QuerySingleEventByEventId(ctx context.Context, em EventModel, query EventDetailsQueryReq) (IEvent, error)
	Judge(ctx context.Context, sr SourceRecords, em EventModel) (IEvents, int, error)
}

type DataModelQuery interface {
	FetchSourceRecordsFrom(ctx context.Context, format string) (SourceRecords, Record, error)
}
