package guivnf.epicfight.fabric;

import com.yesman.akythera.fabric.initializer.EnumerableDataBlockInitializer;
import guivnf.epicfight.EpicFight;

public final class EpicFightDataBlockInitializer implements EnumerableDataBlockInitializer {
    @Override
    public void registerEnums() {
        EpicFight.registerDataBlockEnumClasses();
    }
}
