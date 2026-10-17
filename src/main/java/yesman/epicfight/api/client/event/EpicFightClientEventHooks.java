package yesman.epicfight.api.client.event;

import yesman.epicfight.api.client.event.types.camera.*;
import yesman.epicfight.api.client.event.types.control.MappedMovementInputUpdateEvent;
import yesman.epicfight.api.client.event.types.entity.ModifyPlayerLivingMotionEvent;
import yesman.epicfight.api.client.event.types.entity.ProcessEntityPairingPacketEvent;
import yesman.epicfight.api.client.event.types.hud.TickTargetIndicatorEvent;
import yesman.epicfight.api.client.event.types.registry.RegisterAttributeIconEvent;
import yesman.epicfight.api.client.event.types.registry.RegisterPatchedRenderersEvent;
import yesman.epicfight.api.client.event.types.registry.RegisterWeaponCategoryIconEvent;
import yesman.epicfight.api.client.event.types.render.AnimatedArmorTextureEvent;
import yesman.epicfight.api.client.event.types.render.PrepareModelEvent;
import yesman.epicfight.api.client.event.types.render.RenderEnderDragonEvent;
import yesman.epicfight.api.client.event.types.render.RenderHandEvent;
import yesman.epicfight.api.client.event.types.render.RenderLivingPreEvent;
import yesman.epicfight.api.client.event.types.render.RenderNameTagEvent;
import yesman.epicfight.api.client.event.types.render.ValidatePlayerModelEvent;
import yesman.epicfight.api.event.*;
import yesman.epicfight.api.utils.side.LogicalSide;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

/// These are pre-defined hooks for all existing event types
///
/// To register an event listener that subscribes a specific event, call one of [EventHook#registerEvent],
/// [CancelableEventHook#registerCancelableEvent], or [CancelableEventHook#registerContextAwareEvent].
///
/// If you want to listen an event not globally, but per entity, call the exact same registering methods in
/// [EntityEventListener], which you can access by [LivingEntityPatch#getEventListener] Be aware that you
/// only can register events that inherit [LivingEntityPatchEvent] for per entity events.
///
/// For common event hooks for both client and server side, refer to [EpicFightEventHooks]
public final class EpicFightClientEventHooks {
    public static final class Camera {
        public static final CancelableEventHook<BuildCameraTransform.Pre> BUILD_TRANSFORM_PRE = CancelableEventHook.createCancelableEventHook();
        public static final EventHook<BuildCameraTransform.Post> BUILD_TRANSFORM_POST = EventHook.createEventHook();
        public static final EventHook<ItemUsedInDecoupledCamera> ITEM_USED_WHEN_DECOUPLED = EventHook.createEventHook();
        public static final EventHook<CoupleTPSCamera> COUPLE_CAMERA = EventHook.createEventHook();
        public static final EventHook<LockOnEvent.Start> LOCK_ON_START = CancelableEventHook.createCancelableEventHook();
        public static final EventHook<LockOnEvent.Tick> LOCK_ON_TICK = EventHook.createEventHook();
        public static final EventHook<LockOnEvent.Release> LOCK_ON_RELEASED = CancelableEventHook.createCancelableEventHook();
        public static final EventHook<ActivateTPSCamera> ACTIVATE_TPS_CAMERA = CancelableEventHook.createCancelableEventHook();

        private Camera() {}
    }

    public static final class Control {
        public static final EventHook<MappedMovementInputUpdateEvent> MAPPED_MOVEMENT_INPUT_UPDATE = EventHook.createSidedEventHook(LogicalSide.CLIENT);

        private Control() {}
    }

    public static final class Entity {
        public static final EventHook<ProcessEntityPairingPacketEvent> HANDLE_ENTITY_PAIRING_PACKET = CancelableEventHook.createSidedCancelableEventHook(LogicalSide.CLIENT);
        public static final EventHook<ModifyPlayerLivingMotionEvent.BaseLayer> MODIFY_PLAYER_LIVING_MOTION_BASE = EventHook.createSidedEventHook(LogicalSide.CLIENT);
        public static final EventHook<ModifyPlayerLivingMotionEvent.CompositeLayer> MODIFY_PLAYER_LIVING_MOTION_COMPOSITE = EventHook.createSidedEventHook(LogicalSide.CLIENT);

        private Entity() {}
    }

    public static final class HUD {
        public static final EventHook<TickTargetIndicatorEvent> TARGET_INDICATOR_TICK = EventHook.createSidedEventHook(LogicalSide.CLIENT);

        private HUD() {}
    }

    public static final class Registry {
        public static final EventHook<RegisterAttributeIconEvent> ATTRIBUTE_ICON = EventHook.createSidedEventHook(LogicalSide.CLIENT);
        public static final EventHook<RegisterPatchedRenderersEvent.ModifyEntity> MODIFY_PATCHED_ENTITY = EventHook.createSidedEventHook(LogicalSide.CLIENT);
        public static final EventHook<RegisterPatchedRenderersEvent.AddEntity> ADD_PATCHED_ENTITY = EventHook.createSidedEventHook(LogicalSide.CLIENT);
        public static final EventHook<RegisterPatchedRenderersEvent.Item> PATCHED_ITEM = EventHook.createSidedEventHook(LogicalSide.CLIENT);
        public static final EventHook<RegisterWeaponCategoryIconEvent> WEAPON_CATEGORY_ICON = EventHook.createSidedEventHook(LogicalSide.CLIENT);

        private Registry() {}
    }

    public static final class Render {
        public static final EventHook<AnimatedArmorTextureEvent> ANIMATED_ARMOR_TEXTURE = EventHook.createSidedEventHook(LogicalSide.CLIENT);
        public static final EventHook<PrepareModelEvent> PREPARE_MODEL_TO_RENDER = EventHook.createSidedEventHook(LogicalSide.CLIENT);
        public static final EventHook<RenderEnderDragonEvent> RENDER_ENDER_DRAGON = EventHook.createSidedEventHook(LogicalSide.CLIENT);
        public static final EventHook<RenderLivingPreEvent> RENDER_LIVING_PRE = EventHook.createSidedEventHook(LogicalSide.CLIENT);
        public static final EventHook<RenderNameTagEvent> RENDER_NAME_TAG = EventHook.createSidedEventHook(LogicalSide.CLIENT);
        public static final EventHook<RenderHandEvent> RENDER_HAND = EventHook.createSidedEventHook(LogicalSide.CLIENT);
        public static final EventHook<ValidatePlayerModelEvent> VALIDATE_PLAYER_MODEL_TO_RENDER = EventHook.createSidedEventHook(LogicalSide.CLIENT);

        private Render() {}
    }

    private EpicFightClientEventHooks() {}
}