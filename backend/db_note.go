package backend

import (
	"context"
	"fmt"
	"time"

	"github.com/mailru/easyjson"
	"github.com/nbd-wtf/go-nostr"
	"github.com/nbd-wtf/go-nostr/nip10"
	"github.com/nbd-wtf/go-nostr/nip14"
	"github.com/nbd-wtf/go-nostr/nip19"
	"github.com/nbd-wtf/go-nostr/nip92"
	"github.com/nbd-wtf/go-nostr/sdk"
)

const (
	IsRoot = iota
	IsReply
	IsRepost
	IsPoll
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
	Is() int
	ID() string
	Pubkey() string
	CreatedAt() int64
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
	Repost() Note
	RelevantID() string
	Parent() string
	Nevent() string
}

type note struct {
	*nostr.Event

	_imeta nip92.IMeta
}

func (n note) ID() string       { return n.Event.ID }
func (n note) Pubkey() string   { return n.Event.PubKey }
func (n note) CreatedAt() int64 { return int64(n.Event.CreatedAt) }
func (n note) Content() string  { return n.Event.Content }
func (n note) Subject() string  { return nip14.GetSubject(n.Event.Tags) }

func (n note) Is() int {
	switch n.Event.Kind {
	case 1068:
		return IsPoll
	case 6:
		return IsRepost
	case 22:
		return IsReply
	case 1:
		for _, tag := range n.Event.Tags {
			if len(tag) >= 2 && tag[0] == "e" {
				return IsReply
			}
		}
		return IsRoot
	default:
		return IsRoot
	}
}

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

func (n note) Repost() Note {
	if n.Kind != 6 {
		return nil
	}
	tag := n.Event.Tags.GetFirst([]string{"e", ""})
	if tag == nil {
		return nil
	}

	evt := &nostr.Event{}
	if n.Event.Content == "" {
		// reposted event is not embedded, try to fetch it, we should probably do this beforehand in the background?
		ctx, cancel := context.WithTimeout(ctx, time.Second*2)
		defer cancel()

		pointer := nostr.EventPointer{
			ID: (*tag)[1],
		}
		if len(*tag) >= 3 {
			if (*tag)[2] != "" {
				pointer.Relays = []string{(*tag)[2]}
			}
			if len(*tag) >= 4 {
				pointer.Author = (*tag)[3]
			}
		}

		res, _, err := sys.FetchSpecificEvent(ctx, pointer, false)
		if err != nil {
			return nil
		}

		evt = res
	}

	easyjson.Unmarshal([]byte(n.Event.Content), evt)
	if evt.ID != (*tag)[1] {
		// invalid -- this should never happen, we should catch this before
		return nil
	}
	if ok, _ := evt.CheckSignature(); !ok || !evt.CheckID() {
		// idem
		return nil
	}

	return note{Event: evt}
}

func (n note) RelevantID() string {
	if n.Is() == IsRepost {
		return n.Repost().ID()
	}
	return n.Event.ID
}

func (n note) Parent() string {
	tag := nip10.GetImmediateParent(n.Event.Tags)
	if tag == nil {
		return ""
	}
	return (*tag)[1]
}

func (n note) Nevent() string {
	relays, _ := sys.GetEventRelays(n.Event.ID)
	nevent, _ := nip19.EncodeEvent(n.Event.ID, relays, n.Event.PubKey)
	return nevent
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
