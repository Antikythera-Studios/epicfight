package dev.isxander.controlify.api.event;
import dev.isxander.controlify.controller.ControllerEntity;
import java.util.function.Consumer;

/// Stub for Controlify's event API.
/// The real Controlify dependency is currently disabled (malformed access widener).
/// These stubs allow the code to compile; the register methods are no-ops.
/// When Controlify is re-enabled, these stubs will be replaced by the real API classes.
public class ControlifyEvents {
    public static final LookInputModifierRegistry LOOK_INPUT_MODIFIER = new LookInputModifierRegistry();
    public static Object on(Consumer<ControllerEntity> consumer) { return new Object(); }
    public static Object onOrNull(Consumer<ControllerEntity> consumer) { return new Object(); }

    /// Stub registry for LOOK_INPUT_MODIFIER events.
    /// The register method is a no-op until Controlify is re-enabled as a dependency.
    public static final class LookInputModifierRegistry {
        public void register(Consumer<LookInputModifierEvent> consumer) {}
    }
}
