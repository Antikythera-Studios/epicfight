package dev.isxander.controlify.api;
public class ControlifyApi {
    public static ControlifyApi INSTANCE = new ControlifyApi();
    public static ControlifyApi get() { return INSTANCE; }
    public java.util.Optional<dev.isxander.controlify.controller.ControllerEntity> getCurrentController() { return null; }
    public Object currentInputMode() { return null; }
    public dev.isxander.controlify.api.bind.ControlifyBindApi bindApi() { return new dev.isxander.controlify.api.bind.ControlifyBindApi(); }
}
