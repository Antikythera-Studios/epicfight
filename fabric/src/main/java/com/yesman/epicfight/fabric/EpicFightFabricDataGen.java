package com.yesman.epicfight.fabric;

import com.yesman.akythera.api.data.provider.AnimationSequenceProvider;
import com.yesman.akythera.api.data.provider.BlendSpace2DProvider;
import com.yesman.akythera.core.registry.AkytheraRegistries;
//import com.yesman.epicfight.akythera.providers.EpicfightAnimationMontages;
import com.yesman.epicfight.akythera.providers.EpicfightAnimationSequences;
import com.yesman.epicfight.akythera.providers.EpicfightBlendSpace2Ds;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.minecraft.core.RegistrySetBuilder;
import org.jspecify.annotations.NonNull;

public final class EpicFightFabricDataGen implements DataGeneratorEntrypoint {
    @Override
    public void buildRegistry(RegistrySetBuilder registryBuilder) {
        registryBuilder.add(AkytheraRegistries.DataPack.ANIM_SEQUENCE_KEY, EpicfightAnimationSequences::bootstrap);
    }

    @Override
    public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
        FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();
        pack.addProvider((FabricDataGenerator.Pack.RegistryDependentFactory<@NonNull AnimationSequenceProvider>) EpicfightAnimationSequences::new);
        pack.addProvider((FabricDataGenerator.Pack.RegistryDependentFactory<@NonNull BlendSpace2DProvider>) EpicfightBlendSpace2Ds::new);
        //pack.addProvider((DataProvider.Factory<@NonNull AnimationMontageProvider>) EpicfightAnimationMontages::new);
    }
}
