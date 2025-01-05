package backend

import (
	"cmp"
	"context"
	"fmt"
	"time"

	"github.com/nbd-wtf/go-nostr"
	"github.com/nbd-wtf/go-nostr/nip14"
	"github.com/nbd-wtf/go-nostr/nip92"
	"github.com/nbd-wtf/go-nostr/sdk"
)

type BlurhashDef interface {
	Width() int
	Height() int
	Blurhash() string
}

type blurhashDef struct{ nip92.IMetaEntry }

func (b blurhashDef) Width() int       { return b.IMetaEntry.Width }
func (b blurhashDef) Height() int      { return b.IMetaEntry.Height }
func (b blurhashDef) Blurhash() string { return b.IMetaEntry.Blurhash }

type Note interface {
	ID() string
	PubKey() string
	Subject() string
	Content() string
	Mentions(string) bool
	BlurhashFor(string) BlurhashDef
	Author() Profile
	AuthorIsFollowedBy(string) bool
	AuthorIsInNetworkOf(string) bool
	AuthorIsMutedBy(string) bool
	AuthorIsInFollowSetOf(string) bool
	IsBookmarkedBy(string) bool
	LikeCount() int
	ReplyCount() int
}

type note struct {
	*nostr.Event

	_imeta nip92.IMeta
}

func (n note) ID() string       { return n.Event.ID }
func (n note) PubKey() string   { return n.Event.PubKey }
func (n note) Content() string  { return n.Event.Content }
func (n note) CreatedAt() int64 { return int64(n.Event.CreatedAt) }
func (n note) Subject() string  { return nip14.GetSubject(n.Event.Tags) }

func (n note) RelaySource() []string {
	// TODO: use internal db
	return []string{}
}

func (n note) Mentions(pubkey string) bool {
	for ref := range sdk.ParseReferences(*n.Event) {
		if ref.Profile != nil && ref.Profile.PublicKey == pubkey {
			return true
		}
	}
	return false
}

func (n note) BlurhashFor(url string) BlurhashDef {
	if n._imeta == nil {
		n._imeta = nip92.ParseTags(n.Event.Tags)
	}
	entry, ok := n._imeta.Get(url)
	if ok {
		return blurhashDef{entry}
	}
	return nil
}

func (n note) Author() Profile {
	ctx, cancel := context.WithTimeout(ctx, time.Second*2)
	defer cancel()
	pm := sys.FetchProfileMetadata(ctx, n.Event.PubKey)
	return profile{pm}
}

func (n note) AuthorIsFollowedBy(pubkey string) bool {
	ctx, cancel := context.WithTimeout(ctx, time.Second*2)
	defer cancel()

	list := sys.FetchFollowList(ctx, pubkey)
	for _, item := range list.Items {
		if item.Pubkey == n.Event.PubKey {
			return true
		}
	}
	return false
}

func (n note) AuthorIsInNetworkOf(pubkey string) bool {
	return false // TODO
}

func (n note) AuthorIsMutedBy(pubkey string) bool {
	ctx, cancel := context.WithTimeout(ctx, time.Second*2)
	defer cancel()

	list := sys.FetchMuteList(ctx, pubkey)
	for _, item := range list.Items {
		if item.Pubkey == n.Event.PubKey {
			return true
		}
	}
	return false
}

func (n note) AuthorIsInFollowSetOf(pubkey string) bool {
	ctx, cancel := context.WithTimeout(ctx, time.Second*2)
	defer cancel()

	list := sys.FetchFollowSets(ctx, pubkey)
	for _, set := range list.Sets {
		for _, item := range set {
			if item.Pubkey == n.Event.PubKey {
				return true
			}
		}
	}
	return false
}

func (n note) IsBookmarkedBy(pubkey string) bool {
	ctx, cancel := context.WithTimeout(ctx, time.Second*2)
	defer cancel()

	list := sys.FetchBookmarkList(ctx, pubkey)
	for _, item := range list.Items {
		if item.Value() == n.Event.ID {
			// this matched only against the exact id, ignores "a" references
			return true
		}
	}
	return false
}

func (n note) LikeCount() int {
	return 12 // TODO
}

func (n note) ReplyCount() int {
	return 12 // TODO
}

func (dbi DBInterface) GetNote(idOrCode string) (Note, error) {
	ctx, cancel := context.WithTimeout(ctx, time.Second*5)
	defer cancel()

	event, relays, err := sys.FetchSpecificEventFromInput(ctx, idOrCode, false /* withRelays? */)
	if err != nil {
		return nil, fmt.Errorf("note %s not found", idOrCode)
	}

	// TODO: store these relays somewhere?
	_ = relays

	return note{Event: event}, nil
}

func compareNotesMostRecentFirst(a, b note) int {
	return -cmp.Compare(a.Event.CreatedAt, b.Event.CreatedAt)
}
