package com.yesman.epicfight.fabric;

import com.yesman.akythera.fabric.initializer.EnumerableDataBlockInitializer;
import com.yesman.epicfight.EpicFight;

public final class EpicFightDataBlockInitializer implements EnumerableDataBlockInitializer {
    @Override
    public void registerEnums() {
        EpicFight.registerDataBlockEnumClasses();
    }
}
