package backend

import "github.com/nbd-wtf/go-nostr"

type DBInterface struct {
	emitters struct {
		profile       []*PairedProfileEmitter
		homefeed      []*PairedFeedEmitter
		bookmarksFeed []*PairedFeedEmitter
	}
}

type Canceller interface {
	Cancel()
}

type canceller func()

func (c canceller) Cancel() { c() }

func (dbi DBInterface) activateEmitters(ie nostr.RelayEvent) {
	switch ie.Kind {
	case 0:
		for _, pe := range dbi.emitters.profile {
			if pe.pubkey == ie.PubKey {
				emitProfile(pe)
			}
		}
	case nostr.KindBookmarkList:
		for _, pe := range dbi.emitters.bookmarksFeed {
			if pe.pubkey == ie.PubKey {
				emitBookmarksFeed(pe)
			}
		}
	}
}

// this should be called at view time, not note downloaded time
func (dbi DBInterface) augmentNote(ie nostr.RelayEvent) {
	// TODO
	// fetch profiles
	// fetch reaction counts
	// fetch replies
	// fetch poll responses -- ensure we don't write a new poll response from the same user if we already have one
}

func trackRelays(ie nostr.RelayEvent) {
	// TODO
}
