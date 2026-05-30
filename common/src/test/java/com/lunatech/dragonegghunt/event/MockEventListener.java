package com.lunatech.dragonegghunt.event;

@FunctionalInterface
public interface MockEventListener {
    void onEvent(MockEvent event);
}