package com.lunatech.dragonegghunt.messaging;

import com.lunatech.dragonegghunt.event.MockEvent;
import com.lunatech.dragonegghunt.messaging.message.Message;

public class MockSyncMessageEvent extends MockEvent {
    private final Message<?> message;

    public MockSyncMessageEvent(Message<?> message) {
        this.message = message;
    }

    public Message<?> getMessage() {
        return message;
    }
}