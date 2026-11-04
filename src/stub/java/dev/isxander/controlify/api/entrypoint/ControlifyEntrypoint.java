package dev.isxander.controlify.api.entrypoint;

import dev.isxander.controlify.api.ControlifyApi;

/// Stub interface for Controlify API.
public interface ControlifyEntrypoint {
    default void onControllersDiscovered(ControlifyApi controlify) {}
}
