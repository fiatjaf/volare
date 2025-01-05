package backend

import "github.com/nbd-wtf/go-nostr/sdk"

func flattenProfileList(pl []sdk.ProfileRef) []string {
	pubkeys := make([]string, len(pl))
	for i, ref := range pl {
		pubkeys[i] = ref.Pubkey
	}
	return pubkeys
}

func swapDelete[A any](slice []A, idx int) []A {
	slice[idx] = slice[len(slice)-1] // move last element to target position
	return slice[0 : len(slice)-1]   // forget last element
}
