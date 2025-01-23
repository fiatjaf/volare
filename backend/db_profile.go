package backend

import (
	"context"
	"time"

	"github.com/nbd-wtf/go-nostr/sdk"
)

type PairedProfileEmitter struct {
	pubkey  string
	emitter ProfileEmitter
}

type ProfileEmitter interface {
	Emit(Profile)
}

type Profile interface {
	Pubkey() string
	Npub() string
	Name() string
	Picture() string
	About() string
	ShortName() string
	Lightning() string
	CreatedAt() int64

	IsFollowedBy(pubkey string) bool
	IsInNetworkOf(pubkey string) bool
	IsMutedBy(pubkey string) bool
	IsInFollowSetOf(pubkey string) bool
	IsTrustedBy(pubkey string) bool
}

type profile struct{ sdk.ProfileMetadata }

func (pm profile) Pubkey() string    { return pm.ProfileMetadata.PubKey }
func (pm profile) Name() string      { return pm.ProfileMetadata.Name }
func (pm profile) About() string     { return pm.ProfileMetadata.About }
func (pm profile) Picture() string   { return pm.ProfileMetadata.Picture }
func (pm profile) Lightning() string { return pm.ProfileMetadata.LUD16 }
func (pm profile) CreatedAt() int64 {
	if pm.ProfileMetadata.Event != nil {
		return int64(pm.ProfileMetadata.Event.CreatedAt)
	}
	return 0
}

func (pm profile) IsFollowedBy(pubkey string) bool {
	ctx, cancel := context.WithTimeout(ctx, time.Second*2)
	defer cancel()

	list := sys.FetchFollowList(ctx, pubkey)
	for _, item := range list.Items {
		if item.Pubkey == pm.ProfileMetadata.PubKey {
			return true
		}
	}
	return false
}

func (pm profile) IsInNetworkOf(pubkey string) bool {
	return false // TODO
}

func (pm profile) IsMutedBy(pubkey string) bool {
	ctx, cancel := context.WithTimeout(ctx, time.Second*2)
	defer cancel()

	list := sys.FetchMuteList(ctx, pubkey)
	for _, item := range list.Items {
		if item.Pubkey == pm.ProfileMetadata.PubKey {
			return true
		}
	}
	return false
}

func (pm profile) IsInFollowSetOf(pubkey string) bool {
	ctx, cancel := context.WithTimeout(ctx, time.Second*2)
	defer cancel()

	list := sys.FetchFollowSets(ctx, pubkey)
	for _, set := range list.Sets {
		for _, item := range set {
			if item.Pubkey == pm.ProfileMetadata.PubKey {
				return true
			}
		}
	}
	return false
}

func (pm profile) IsTrustedBy(pubkey string) bool {
	return pm.IsFollowedBy(pubkey) || pm.IsInNetworkOf(pubkey)
}

func (dbi DBInterface) WatchProfile(pubkey string, emitter ProfileEmitter) Canceller {
	tpe := &PairedProfileEmitter{pubkey, emitter}
	emitProfile(tpe)

	dbi.emitters.profile = append(dbi.emitters.profile, tpe)
	return canceller(
		func() {
			for i, pe := range dbi.emitters.profile {
				if pe == tpe {
					dbi.emitters.profile = swapDelete(dbi.emitters.profile, i)
					return
				}
			}
		},
	)
}

func emitProfile(pe *PairedProfileEmitter) {
	ctx, cancel := context.WithTimeout(ctx, time.Second*2)
	defer cancel()

	pm := sys.FetchProfileMetadata(ctx, pe.pubkey)
	pe.emitter.Emit(profile{pm})
}

func FetchProfile(pubkey string) Profile {
	ctx, cancel := context.WithTimeout(ctx, time.Second*2)
	defer cancel()

	pm := sys.FetchProfileMetadata(ctx, pubkey)
	return profile{pm}
}
