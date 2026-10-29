package dev.isxander.controlify.api.bind;

import dev.isxander.controlify.controller.ControllerEntity;
import java.util.function.Supplier;

/// Stub for Controlify's InputBindingSupplier.
public class InputBindingSupplier implements Supplier<InputBinding> {
    private final InputBinding binding;

    public InputBindingSupplier(InputBinding binding) {
        this.binding = binding;
    }

    @Override
    public InputBinding get() {
        return binding;
    }

    public InputBinding on(ControllerEntity controller) {
        return binding;
    }

    public InputBinding onOrNull(ControllerEntity controller) {
        return binding;
    }
}
