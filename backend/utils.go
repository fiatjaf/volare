package backend

import (
	"github.com/nbd-wtf/go-nostr"
	"github.com/nbd-wtf/go-nostr/nip19"
	"github.com/nbd-wtf/go-nostr/nip46"
	_ "golang.org/x/mobile/bind"
)

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
