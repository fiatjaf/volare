package backend

import (
	"fmt"

	"fiatjaf.com/leafdb"
)

type InternalDB struct {
	*leafdb.DB[any]
}

func NewInternalDB(path string) (*InternalDB, error) {
	ldb, err := leafdb.New(path, leafdb.Options[any]{
		MapSize: 1 << 26,
		Encode: func(t leafdb.DataType, msg any) ([]byte, error) {
			switch t {
			default:
				return nil, fmt.Errorf("what is this? %v", t)
			}
		},
		Decode: func(t leafdb.DataType, buf []byte) (any, error) {
			switch t {
			default:
				return nil, fmt.Errorf("what is this? %v", t)
			}
		},
		Indexes: map[string]leafdb.IndexDefinition[any]{},
		Views:   map[string]leafdb.ViewDefinition[any]{},
	})
	if err != nil {
		return nil, err
	}

	return &InternalDB{ldb}, err
}
