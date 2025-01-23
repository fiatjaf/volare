package backend

import (
	"errors"

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

func CreateNeventFromID(id string) string {
	v, _ := nip19.EncodeEvent(id, nil, "")
	return v
}

type Pointer interface {
	AsTagReference() string
}

func ParseBech32(bech32 string) (Pointer, error) {
	prefix, value, err := nip19.Decode(bech32)
	if err != nil {
		return nil, err
	}
	switch prefix {
	case "npub":
		return nostr.ProfilePointer{PublicKey: value.(string)}, nil
	case "nprofile":
		return value.(nostr.ProfilePointer), nil
	case "nevent":
		return value.(nostr.EventPointer), nil
	case "naddr":
		return value.(nostr.EntityPointer), nil
	}

	return nil, errors.New("nothing could be parsed")
}
