package com.yesman.epicfight.client.compat;

import com.yesman.epicfight.compat.IModPlugin;

/// Each module for mod compatibility code is managed in independent packages
/// to avoid code crash and keep coherence
///
/// For common side code (especially logical server side) should implement [IModPlugin]
public interface IClientModPlugin {
    void onInitializeClient();
}
