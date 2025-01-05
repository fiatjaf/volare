package backend

import (
	"context"
	"log"
	"slices"
	"time"

	"github.com/nbd-wtf/go-nostr"
	"github.com/nbd-wtf/go-nostr/sdk"
)

type PairedFeedEmitter struct {
	pubkey  string
	limit   int
	since   nostr.Timestamp
	emitter FeedEmitter

	// extra stuff
	topic      string // for topic feeds
	identifier string // for list feeds
}

type FeedEmitter interface {
	Emit(NoteFeed)
}

type NoteFeed interface {
	Len() int
	Get(int) Note
}

type noteFeed []note

func (nf noteFeed) Len() int       { return len(nf) }
func (nf noteFeed) Get(i int) Note { return nf[i] }

func (dbi DBInterface) WatchHomeFeed(pubkey string, until int64, limit int, emitter FeedEmitter) Canceller {
	tpe := &PairedFeedEmitter{pubkey, limit, nostr.Timestamp(until), emitter, "", ""}
	emitHomeFeed(tpe)

	// TODO: trigger subscription to this -- only once

	dbi.emitters.homefeed = append(dbi.emitters.homefeed, tpe)
	return canceller(func() {
		for i, pe := range dbi.emitters.homefeed {
			if pe == tpe {
				dbi.emitters.homefeed = swapDelete(dbi.emitters.homefeed, i)
				return
			}
		}
	})
}

func emitHomeFeed(pe *PairedFeedEmitter) {
	ctx, cancel := context.WithTimeout(ctx, time.Second*7)
	defer cancel()

	fl := sys.FetchFollowList(ctx, pe.pubkey)
	perQueryLimit := sdk.PerQueryLimitInBatch(pe.limit, len(fl.Items))
	notes := make(noteFeed, 0, perQueryLimit*len(fl.Items))
	authors := flattenProfileList(fl.Items)

	ch, err := store.QueryEvents(ctx, nostr.Filter{Limit: pe.limit, Authors: authors, Kinds: []int{1, 6, 1068}})
	if err != nil {
		panic(err)
	}
	for evt := range ch {
		notes = append(notes, note{Event: evt})
	}

	pe.emitter.Emit(notes)
}

func (dbi DBInterface) WatchInboxFeed(pubkey string, until int64, limit int, emitter FeedEmitter) Canceller {
	tpe := &PairedFeedEmitter{pubkey, limit, nostr.Timestamp(until), emitter, "", ""}
	emitInboxFeed(tpe)

	// TODO: trigger subscription to this -- only once

	dbi.emitters.homefeed = append(dbi.emitters.homefeed, tpe)
	return canceller(func() {
		for i, pe := range dbi.emitters.homefeed {
			if pe == tpe {
				dbi.emitters.homefeed = swapDelete(dbi.emitters.homefeed, i)
				return
			}
		}
	})
}

func emitInboxFeed(pe *PairedFeedEmitter) {
	ctx, cancel := context.WithTimeout(ctx, time.Second*7)
	defer cancel()

	notes := make(noteFeed, 0, pe.limit)
	muted := flattenProfileList(sys.FetchMuteList(ctx, pe.pubkey).Items)

	ch, err := store.QueryEvents(ctx, nostr.Filter{
		Limit: pe.limit,
		Tags:  nostr.TagMap{"p": []string{pe.pubkey}},
		Kinds: []int{1, 6, 1068, 1111},
	})
	if err != nil {
		panic(err)
	}
	for evt := range ch {
		if slices.Contains(muted, evt.PubKey) {
			notes = append(notes, note{Event: evt})
		}
	}

	pe.emitter.Emit(notes)
}

func (dbi DBInterface) WatchBookmarksFeed(pubkey string, until int64, limit int, emitter FeedEmitter) Canceller {
	tpe := &PairedFeedEmitter{pubkey, limit, nostr.Timestamp(until), emitter, "", ""}
	emitBookmarksFeed(tpe)

	// TODO: trigger subscription to this -- only once

	dbi.emitters.bookmarksFeed = append(dbi.emitters.bookmarksFeed, tpe)
	return canceller(func() {
		for i, pe := range dbi.emitters.bookmarksFeed {
			if pe == tpe {
				dbi.emitters.bookmarksFeed = swapDelete(dbi.emitters.bookmarksFeed, i)
				return
			}
		}
	})
}

func emitBookmarksFeed(pe *PairedFeedEmitter) {
	ctx, cancel := context.WithTimeout(ctx, time.Second*5)
	defer cancel()

	li := sys.FetchBookmarkList(ctx, pe.pubkey)
	notes := make(noteFeed, 0, pe.limit)
	for _, item := range li.Items {
		evt, _, err := sys.FetchSpecificEvent(ctx, item.Pointer, false)
		if err == nil && evt.CreatedAt >= pe.since {
			notes = append(notes, note{Event: evt})
		}
	}

	slices.SortFunc(notes, compareNotesMostRecentFirst)
	if len(notes) > pe.limit {
		notes = notes[0:pe.limit]
	}

	pe.emitter.Emit(notes)
}

func (dbi DBInterface) WatchSetFeed(pubkey string, identifier string, until int64, limit int, emitter FeedEmitter) Canceller {
	tpe := &PairedFeedEmitter{pubkey, limit, nostr.Timestamp(until), emitter, "", identifier}
	emitSetFeed(tpe)

	// TODO: trigger subscription to this -- only once

	dbi.emitters.bookmarksFeed = append(dbi.emitters.bookmarksFeed, tpe)
	return canceller(func() {
		for i, pe := range dbi.emitters.bookmarksFeed {
			if pe == tpe {
				dbi.emitters.bookmarksFeed = swapDelete(dbi.emitters.bookmarksFeed, i)
				return
			}
		}
	})
}

func emitSetFeed(pe *PairedFeedEmitter) {
	ctx, cancel := context.WithTimeout(ctx, time.Second*5)
	defer cancel()

	ss := sys.FetchFollowSets(ctx, pe.pubkey)
	fl, ok := ss.Sets[pe.identifier]
	if !ok {
		pe.emitter.Emit(noteFeed{})
		return
	}

	perQueryLimit := sdk.PerQueryLimitInBatch(pe.limit, len(fl))
	notes := make(noteFeed, 0, perQueryLimit*len(fl))
	authors := flattenProfileList(fl)

	ch, err := store.QueryEvents(ctx, nostr.Filter{Limit: pe.limit, Authors: authors, Kinds: []int{1, 6, 1068}})
	if err != nil {
		panic(err)
	}
	for evt := range ch {
		notes = append(notes, note{Event: evt})
	}

	pe.emitter.Emit(notes)
}

func (dbi DBInterface) WatchProfileFeed(pubkeyOrCode string, until int64, limit int, emitter FeedEmitter) Canceller {
	ctx, cancel := context.WithTimeout(ctx, time.Second*5)
	defer cancel()

	pm, err := sys.FetchProfileFromInput(ctx, pubkeyOrCode)
	if err != nil {
		log.Println("invalid pubkeyOrCode: ", pubkeyOrCode)
		return canceller(func() {})
	}

	tpe := &PairedFeedEmitter{pm.PubKey, limit, nostr.Timestamp(until), emitter, "", ""}
	emitProfileFeed(tpe)

	// TODO: trigger subscription to this -- only once

	dbi.emitters.homefeed = append(dbi.emitters.homefeed, tpe)
	return canceller(func() {
		for i, pe := range dbi.emitters.homefeed {
			if pe == tpe {
				dbi.emitters.homefeed = swapDelete(dbi.emitters.homefeed, i)
				return
			}
		}
	})
}

func emitProfileFeed(pe *PairedFeedEmitter) {
	ctx, cancel := context.WithTimeout(ctx, time.Second*7)
	defer cancel()

	notes := make(noteFeed, 0, pe.limit)
	ch, err := store.QueryEvents(ctx, nostr.Filter{Limit: pe.limit, Authors: []string{pe.pubkey}, Kinds: []int{1, 6, 1068}})
	if err != nil {
		panic(err)
	}
	for evt := range ch {
		notes = append(notes, note{Event: evt})
	}

	pe.emitter.Emit(notes)
}

func (dbi DBInterface) WatchTopicFeed(topic string, until int64, limit int, emitter FeedEmitter) Canceller {
	tpe := &PairedFeedEmitter{"", limit, nostr.Timestamp(until), emitter, topic, ""}
	emitTopicFeed(tpe)

	// do not trigger a subscription to this for now because we don't know what relays to use
	// and we'll probably remove this functionality later

	dbi.emitters.homefeed = append(dbi.emitters.homefeed, tpe)
	return canceller(func() {
		for i, pe := range dbi.emitters.homefeed {
			if pe == tpe {
				dbi.emitters.homefeed = swapDelete(dbi.emitters.homefeed, i)
				return
			}
		}
	})
}

func emitTopicFeed(pe *PairedFeedEmitter) {
	ctx, cancel := context.WithTimeout(ctx, time.Second*7)
	defer cancel()

	muted := flattenProfileList(sys.FetchMuteList(ctx, pe.pubkey).Items)

	notes := make(noteFeed, 0, pe.limit)
	ch, err := store.QueryEvents(ctx, nostr.Filter{Limit: pe.limit, Tags: nostr.TagMap{"t": []string{pe.topic}}, Kinds: []int{1, 6, 1068}})
	if err != nil {
		panic(err)
	}
	for evt := range ch {
		if slices.Contains(muted, evt.PubKey) {
			notes = append(notes, note{Event: evt})
		}
	}

	pe.emitter.Emit(notes)
}
