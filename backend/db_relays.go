package backend

import (
	"context"
	"time"
)

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
