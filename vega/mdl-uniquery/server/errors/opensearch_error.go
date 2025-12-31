package errors

import "github.com/bytedance/sonic"

type OpenSearchError struct {
	StatusCode int      `json:"status"`
	ErrBody    *errBody `json:"error"`
}

type errBody struct {
	ErrType string `json:"type"`
	Reason  string `json:"reason"`
}

func NewOpenSearchError(errType string) *OpenSearchError {
	err := openSearchErrors[errType]
	return &OpenSearchError{
		StatusCode: err.StatusCode,
		ErrBody: &errBody{
			ErrType: err.ErrBody.ErrType,
			Reason:  err.ErrBody.Reason,
		},
	}
}

func (err *OpenSearchError) WithReason(reason string) *OpenSearchError {
	err.ErrBody.Reason = reason
	return err
}

func (err *OpenSearchError) Error() string {
	b, _ := sonic.Marshal(err)
	return string(b)
}
