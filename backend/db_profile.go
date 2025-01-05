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
	Name() string
	Picture() string
	ShortName() string
}

type profile struct{ sdk.ProfileMetadata }

func (pm profile) Name() string    { return pm.ProfileMetadata.Name }
func (pm profile) Picture() string { return pm.ProfileMetadata.Picture }

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
