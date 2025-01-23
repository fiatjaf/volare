module backend

go 1.23.4

require (
	fiatjaf.com/leafdb v0.0.7
	github.com/fiatjaf/eventstore v0.15.0
	github.com/mailru/easyjson v0.7.7
	github.com/mattn/go-sqlite3 v1.14.24
	github.com/nbd-wtf/go-nostr v0.46.0
	golang.org/x/mobile v0.0.0-20241213221354-a87c1cf6cf46
)

require (
	fiatjaf.com/lib v0.2.0 // indirect
	github.com/PowerDNS/lmdb-go v1.9.2 // indirect
	github.com/btcsuite/btcd/btcec/v2 v2.3.4 // indirect
	github.com/btcsuite/btcd/btcutil v1.1.3 // indirect
	github.com/btcsuite/btcd/chaincfg/chainhash v1.1.0 // indirect
	github.com/cespare/xxhash/v2 v2.3.0 // indirect
	github.com/coder/websocket v1.8.12 // indirect
	github.com/decred/dcrd/crypto/blake256 v1.1.0 // indirect
	github.com/decred/dcrd/dcrec/secp256k1/v4 v4.3.0 // indirect
	github.com/dgraph-io/ristretto v1.0.0 // indirect
	github.com/dustin/go-humanize v1.0.1 // indirect
	github.com/graph-gophers/dataloader/v7 v7.1.0 // indirect
	github.com/jmoiron/sqlx v1.4.0 // indirect
	github.com/josharian/intern v1.0.0 // indirect
	github.com/json-iterator/go v1.1.12 // indirect
	github.com/modern-go/concurrent v0.0.0-20180228061459-e0a39a4cb421 // indirect
	github.com/modern-go/reflect2 v1.0.2 // indirect
	github.com/pkg/errors v0.9.1 // indirect
	github.com/puzpuzpuz/xsync/v3 v3.4.0 // indirect
	github.com/tidwall/gjson v1.18.0 // indirect
	github.com/tidwall/match v1.1.1 // indirect
	github.com/tidwall/pretty v1.2.1 // indirect
	golang.org/x/crypto v0.32.0 // indirect
	golang.org/x/exp v0.0.0-20241204233417-43b7b7cde48d // indirect
	golang.org/x/mod v0.22.0 // indirect
	golang.org/x/sync v0.10.0 // indirect
	golang.org/x/sys v0.29.0 // indirect
	golang.org/x/tools v0.28.0 // indirect
)

replace github.com/PowerDNS/lmdb-go => github.com/fiatjaf/lmdb-go v0.0.0-20241216175215-ce7e8c333ddb

replace fiatjaf.com/leafdb => fiatjaf.com/leafdb v0.0.0-20241219032121-5f027f319f97

replace github.com/fiatjaf/eventstore => ../../eventstore

replace github.com/nbd-wtf/go-nostr => ../../go-nostr
