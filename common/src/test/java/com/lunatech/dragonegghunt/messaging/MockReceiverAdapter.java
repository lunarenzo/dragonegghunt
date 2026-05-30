package com.lunatech.dragonegghunt.messaging;

import com.lunatech.dragonegghunt.event.MockEventSystem;
import com.lunatech.dragonegghunt.messaging.adapter.receiver.ReceiverAdapter;
import com.lunatech.dragonegghunt.messaging.message.Message;

public class MockReceiverAdapter extends ReceiverAdapter {
    @Override
    public void accept(Message<?> message) {
        MockEventSystem.fireEvent(new MockSyncMessageEvent(message));
    }
}
