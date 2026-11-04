package yesman.epicfight.api.animation.types;

import org.junit.jupiter.api.Test;
import yesman.epicfight.api.animation.property.AnimationProperty.StaticAnimationProperty;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A metadata-less animation has no accessor; addEvents() must not dereference
 * it for a diagnostic log when debug is disabled.
 */
class StaticAnimationDiagnosticsTest {

    @Test
    void addEventsOnAccessorlessAnimationDoesNotThrow() {
        StaticAnimation animation = new StaticAnimation();

        assertNull(animation.getAccessor(), "fresh animation has no accessor");
        assertDoesNotThrow(() -> {
            animation.addEvents();
        }, "addEvents must not touch the null accessor for diagnostics");
        assertTrue(animation.getProperty(StaticAnimationProperty.TICK_EVENTS).isPresent(),
                "TICK_EVENTS property is created");
        assertTrue(animation.getProperty(StaticAnimationProperty.TICK_EVENTS).map(java.util.List.class::cast)
                        .orElseThrow().isEmpty(),
                "empty addEvents leaves TICK_EVENTS empty");
    }
}
