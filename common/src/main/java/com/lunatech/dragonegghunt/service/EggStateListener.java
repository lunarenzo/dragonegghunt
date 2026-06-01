package com.lunatech.dragonegghunt.service;

import com.lunatech.dragonegghunt.state.EggState;

/**
 * Listener interface to observe transitions of the Dragon Egg state.
 */
public interface EggStateListener {
    /**
     * Called whenever the Dragon Egg state transitions from one state to another.
     *
     * @param oldState the previous state of the egg
     * @param newState the new state of the egg
     */
    void onStateTransition(EggState oldState, EggState newState);
}
