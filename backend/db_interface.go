package backend

import (
	"context"
	"fmt"
	"time"

	"github.com/nbd-wtf/go-nostr"
	"github.com/nbd-wtf/go-nostr/nip14"
	"github.com/nbd-wtf/go-nostr/nip92"
	"github.com/nbd-wtf/go-nostr/sdk"
)

type DBInterface struct {
	emitters struct {
		profile []PairedStringParamEmitter
	}
}

type PairedStringParamEmitter struct {
	param   string
	emitter ProfileEmitter
}

type ProfileEmitter interface {
	Emit(Profile)
}

type Profile interface {
	Name() string
	Picture() string
	ShortName() string
}

type profile struct{ sdk.ProfileMetadata }

func (pm profile) Name() string    { return pm.ProfileMetadata.Name }
func (pm profile) Picture() string { return pm.ProfileMetadata.Picture }

func (dbi DBInterface) WatchProfile(pubkey string, emitter ProfileEmitter) {
	dbi.emitters.profile = append(dbi.emitters.profile, PairedStringParamEmitter{pubkey, emitter})

	ctx, cancel := context.WithTimeout(ctx, time.Second*2)
	defer cancel()

	pm := sys.FetchProfileMetadata(ctx, pubkey)
	emitter.Emit(profile{pm})
}

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
	AuthorIsInPubKeySetOf(string) bool
	IsBookmarkedBy(string) bool
	LikeCount() int
	ReplyCount() int
}

type note struct {
	*nostr.Event

	imeta nip92.IMeta
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
	if n.imeta == nil {
		n.imeta = nip92.ParseTags(n.Event.Tags)
	}
	entry, ok := n.imeta.Get(url)
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

func (n note) AuthorIsFollowedBy(string) bool {
	return false // TODO
}

func (n note) AuthorIsInNetworkOf(string) bool {
	return false // TODO
}

func (n note) AuthorIsMutedBy(string) bool {
	return false // TODO
}

func (n note) AuthorIsInPubKeySetOf(string) bool {
	return false // TODO
}

func (n note) IsBookmarkedBy(string) bool {
	return false // TODO
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

	event, relays, err := sys.FetchSpecificEvent(ctx, idOrCode, false /* withRelays? */)
	if err != nil {
		return nil, fmt.Errorf("note %s not found", idOrCode)
	}

	// TODO: store these relays somewhere?
	_ = relays

	return note{Event: event}, nil
}

func (dbi DBInterface) GetWriteRelays(pubkey string) []string {
	ctx, cancel := context.WithTimeout(ctx, time.Second*2)
	defer cancel()

	rl := sys.FetchRelayList(ctx, pubkey)
	urls := make([]string, 0, len(rl.Items))
	for _, item := range rl.Items {
		if item.Outbox {
			urls = append(urls, item.URL)
		}
	}

	return urls
}

func (dbi DBInterface) GetReadRelays(pubkey string) []string {
	ctx, cancel := context.WithTimeout(ctx, time.Second*2)
	defer cancel()

	rl := sys.FetchRelayList(ctx, pubkey)
	urls := make([]string, 0, len(rl.Items))
	for _, item := range rl.Items {
		if item.Inbox {
			urls = append(urls, item.URL)
		}
	}

	return urls
}
