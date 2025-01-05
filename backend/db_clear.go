package backend

import (
	"context"
	"slices"
	"time"

	"github.com/nbd-wtf/go-nostr"
)

func (dbi DBInterface) DeleteOldEvents(userPubKey string) {
	ctx, cancel := context.WithTimeout(ctx, time.Second*10)
	defer cancel()

	bookmarks := sys.FetchBookmarkList(ctx, userPubKey)
	bookmarkIds := make([]string, 0, len(bookmarks.Items))
	bookmarkAddrs := make([]nostr.EntityPointer, 0, len(bookmarks.Items))
	for _, ref := range bookmarks.Items {
		switch ref := ref.Pointer.(type) {
		case nostr.EventPointer:
			bookmarkIds = append(bookmarkIds, ref.ID)
		case nostr.EntityPointer:
			bookmarkAddrs = append(bookmarkAddrs, ref)
		}
	}

	sixMonthsAgo := nostr.Now() - 60*60*24*180
	res, _ := store.QueryEvents(ctx, nostr.Filter{Until: &sixMonthsAgo})
	for evt := range res {
		// don't delete our own stuff
		if evt.PubKey == userPubKey {
			continue
		}

		switch evt.Kind {
		case
			0, 3,
			10000, 10001, 10002, 10003, 10004, 10005, 10006, 10007, 10015, 10030,
			30002, 30015, 30030:
			// never delete these kinds from anyone
			continue
		default:
		}

		// don't delete anything we bookmarked
		if slices.Contains(bookmarkIds, evt.ID) || slices.ContainsFunc(bookmarkAddrs, func(ep nostr.EntityPointer) bool {
			return ep.PublicKey == evt.PubKey && ep.Kind == evt.Kind && ep.Identifier == evt.Tags.GetD()
		}) {
			continue
		}

		// everything else we can delete
		store.DeleteEvent(ctx, evt)
	}
}
