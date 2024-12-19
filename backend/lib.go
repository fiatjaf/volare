package backend

import (
	"context"
	"path/filepath"

	sdk "github.com/nbd-wtf/go-nostr/sdk"
)

var (
	sys      *sdk.System
	ctx      = context.Background()
	internal *InternalDB
)

func Start(datadir string) {
	var err error
	sys = sdk.NewSystem()
	internal, err = NewInternalDB(filepath.Join(datadir, "internaldb"))
	if err != nil {
		panic(err)
	}
}
