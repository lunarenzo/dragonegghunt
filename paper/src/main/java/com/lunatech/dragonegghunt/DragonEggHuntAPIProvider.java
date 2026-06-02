package com.lunatech.dragonegghunt;

import com.lunatech.dragonegghunt.api.DragonEggHuntAPI;

class DragonEggHuntAPIProvider extends DragonEggHuntAPI implements Reloadable {
    private final DragonEggHunt plugin;

    DragonEggHuntAPIProvider(DragonEggHunt plugin) {
        super();
        this.plugin = plugin;
        setInstance(this);
    }
}
