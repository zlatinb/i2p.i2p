package net.i2p.router.peermanager;

/**
 * Possible outcomes from an attempt to build a tunnel
 * with a given peer.  Ordered from best to worst.
 */
public enum PeerAttempt {
    SUCCESS, REJECT, FAILED
}
