package backend

import (
	"context"
	"database/sql"
	"path/filepath"

	"github.com/fiatjaf/eventstore/lmdb"
	_ "github.com/mattn/go-sqlite3"
	"github.com/nbd-wtf/go-nostr"
	sdk "github.com/nbd-wtf/go-nostr/sdk"
	"github.com/nbd-wtf/go-nostr/sdk/hints/sqlh"
)

var (
	sys            *sdk.System
	ctx            = context.Background()
	store          *lmdb.LMDBBackend
	hints          sqlh.SQLHints
	internal       *InternalDB
	currentAccount string
)

var DBI DBInterface

func Start(datadir string) {
	var err error

	sqlhintsdb, err := sql.Open("sqlite3", filepath.Join(datadir, "hints.sqlite3"))
	if err != nil {
		panic(err)
	}
	hints, err = sqlh.NewSQLHints(sqlhintsdb, "sqlite3")
	if err != nil {
		panic(err)
	}

	sys = sdk.NewSystem(sdk.WithHintsDB(hints))
	sys.Pool = nostr.NewSimplePool(context.Background(),
		nostr.WithAuthorKindQueryMiddleware(sys.TrackQueryAttempts),
		nostr.WithEventMiddleware(sys.TrackEventHintsAndRelays),
		nostr.WithDuplicateMiddleware(sys.TrackEventRelaysD),
		nostr.WithPenaltyBox(),
	)

	internal, err = NewInternalDB(filepath.Join(datadir, "internaldb"))
	if err != nil {
		panic(err)
	}

	store = &lmdb.LMDBBackend{
		Path:    filepath.Join(datadir, "eventstore"),
		MapSize: 1 << 28,
	}
	if err := store.Init(); err != nil {
		panic(err)
	}
}
