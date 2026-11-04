package yesman.epicfight.api.utils.side;

import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.skill.SkillContainer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Marked to fields and methods that have danger to crash if they're called
/// in a dedicated server, to avoid a indirect referring to the types that only
/// exist on the client side.
///
/// For example, Fabric marks client-side classes with the
/// [net.fabricmc.api.Environment] annotation, which leads to a crash
/// in a dedicated server when class loader tries to reference client only fields
/// or methods.
///
/// This annotation doesn't have any direct effect in the class load phase. But more
/// to mark the danger fields or methods to ease debugging when invalid dist crash
/// happens.
///
/// This shouldn't necessarily be marked to the fields and methods that belong to
/// classes under `*.client.*` packages since they already ensure the class is only
/// referenced on the client side.
///
/// @see SkillContainer#runOnLocalClient SkillContainer#runOnLocalClient, A common
/// side class but the linked method should be called only in client side since
/// it references [LocalPlayerPatch] in parameters which is a client-side only class.
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.CLASS)
public @interface ClientOnly {
}
