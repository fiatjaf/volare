package backend

import (
	"context"
	"time"

	"github.com/nbd-wtf/go-nostr/nip46"
)

type BunkerSession interface {
	GetPublicKey() (string, error)
	SignEvent(json string) (string, error)
}

type bunkerSession struct{ *nip46.BunkerClient }

func (bs bunkerSession) GetPublicKey() (string, error) {
	ctx, cancel := context.WithTimeout(ctx, time.Second*5)
	defer cancel()

	return bs.BunkerClient.GetPublicKey(ctx)
}

func (bs bunkerSession) SignEvent(json string) (string, error) {
	ctx, cancel := context.WithTimeout(ctx, time.Second*7)
	defer cancel()
	return bs.BunkerClient.RPC(ctx, "sign_event", []string{json})
}

type AuthURLHandler interface {
	Handle(string)
}

func StartBunkerSession(clientKey string, bunkerUri string, auh AuthURLHandler) (BunkerSession, error) {
	bunker, err := nip46.ConnectBunker(ctx, clientKey, bunkerUri, sys.Pool, auh.Handle)
	return bunkerSession{bunker}, err
}
