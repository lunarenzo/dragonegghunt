package com.lunatech.dragonegghunt;

import com.lunatech.dragonegghunt.api.ExampleAPI;

class DragonEggHuntAPIProvider extends ExampleAPI implements Reloadable {
    private final DragonEggHunt plugin;

    DragonEggHuntAPIProvider(DragonEggHunt plugin) {
        super();
        this.plugin = plugin;
        setInstance(this);
    }
}
