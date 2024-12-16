package backend

import (
	"context"
	"time"

	"github.com/nbd-wtf/go-nostr"
	"github.com/nbd-wtf/go-nostr/nip19"
	"github.com/nbd-wtf/go-nostr/nip46"
	"github.com/nbd-wtf/go-nostr/sdk"
	_ "golang.org/x/mobile/bind"
)

var (
	sys *sdk.System
	ctx = context.Background()
)

func Start() {
	sys = sdk.NewSystem()
}

func GenerateKey() string {
	return nostr.GeneratePrivateKey()
}

func GetPublicKey(sk string) string {
	pk, _ := nostr.GetPublicKey(sk)
	return pk
}

func IsValidKeyOrBunker(input string) bool {
	prefix, _, err := nip19.Decode(input)
	if prefix == "nsec" && err == nil {
		return true
	}
	return nostr.IsValid32ByteHex(input) || nip46.IsValidBunkerURL(input)
}

func IsValidBunker(input string) bool { return nip46.IsValidBunkerURL(input) }

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
