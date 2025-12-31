package errors

import (
	"data-model-job/locale"

	"github.com/kweaver-ai/kweaver-go-lib/rest"
)

const (
	// 400
	DataModelJob_InvalidParameter_Job   = "DataModelJob.InvalidParameter.Job"
	DataModelJob_InvalidParameter_JobId = "DataModelJob.InvalidParameter.JobId"

	// 500
	DataModelJob_InternalError_StartJobFailed  = "DataModelJob.InternalError.StartJobFailed"
	DataModelJob_InternalError_UpdateJobFailed = "DataModelJob.InternalError.UpdateJobFailed"
	DataModelJob_InternalError_StopJobFailed   = "DataModelJob.InternalError.StopJobFailed"
)

var errorCodeList = []string{
	// 400
	DataModelJob_InvalidParameter_Job,
	DataModelJob_InvalidParameter_JobId,

	// 500
	DataModelJob_InternalError_StartJobFailed,
	DataModelJob_InternalError_UpdateJobFailed,
	DataModelJob_InternalError_StopJobFailed,
}

func init() {
	locale.Register()
	rest.Register(errorCodeList)
}
