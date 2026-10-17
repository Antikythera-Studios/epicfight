package dev.isxander.controlify.api.entrypoint;
public class InitContext {
    public static final Object KEYBOARD_MOUSE = new Object();
    public static final Object CONTROLLER = new Object();
    public static final Object MIXED = new Object();
    public dev.isxander.controlify.api.InputMode currentInputMode() { return dev.isxander.controlify.api.InputMode.KEYBOARD_MOUSE; }
}
