package com.yesman.epicfight.compat;

import com.yesman.akythera.client.compat.IClientModPlugin;

/// Each module for mod compatibility code is managed in independent packages
/// to avoid code crash and keep coherence
///
/// Should avoid using client code. They need to be remained in [IClientModPlugin]
public interface IModPlugin {
    /// Called both in client and server side. You need to check the physical side if
    /// you want *server only* compatibility module.
    void onInitialize();
}