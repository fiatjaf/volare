package backend

import (
	"errors"

	"github.com/nbd-wtf/go-nostr"
	"github.com/nbd-wtf/go-nostr/nip19"
)

func EventPointerFromID(id string) EventPointer {
	return eventPointer{EventPointer: nostr.EventPointer{ID: id}}
}

type EventPointer interface {
	MatchesNote(Note) bool
	Id() string
}

type eventPointer struct {
	nostr.EventPointer
}

func (p eventPointer) MatchesNote(n Note) bool { return p.EventPointer.MatchesEvent(*n.(note).Event) }
func (p eventPointer) Id() string              { return p.ID }

func NeventParse(bech32 string) (EventPointer, error) {
	prefix, value, err := nip19.Decode(bech32)
	if err != nil {
		return nil, err
	}
	switch prefix {
	case "nevent":
		return eventPointer{value.(nostr.EventPointer)}, nil
	case "note":
		return eventPointer{nostr.EventPointer{ID: value.(string)}}, nil
	}

	return nil, errors.New("failed to parse nevent")
}

type ProfilePointer interface {
	MatchesProfile(Profile) bool
	Pubkey() string
}

type profilePointer struct {
	nostr.ProfilePointer
}

func (p profilePointer) MatchesProfile(n Profile) bool { return p.PublicKey == n.Pubkey() }
func (p profilePointer) Pubkey() string                { return p.ProfilePointer.PublicKey }

func NprofileParse(bech32 string) (ProfilePointer, error) {
	prefix, value, err := nip19.Decode(bech32)
	if err != nil {
		return nil, err
	}
	switch prefix {
	case "npub":
		return profilePointer{nostr.ProfilePointer{PublicKey: value.(string)}}, nil
	case "nprofile":
		return profilePointer{value.(nostr.ProfilePointer)}, nil
	}

	return nil, errors.New("failed to parse nprofile")
}
