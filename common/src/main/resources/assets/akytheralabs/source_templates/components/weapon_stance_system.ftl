<#--
  Constructor expression for an "epicfight:weapon_stance_system" ComponentBlueprint (WeaponStanceSystem).
  DTO (WeaponStanceSystemHolder#getDTO):
    { default_stance, conditions: [ { stance, source } ], properties: [ { stance, categories, combo,
      dash, air_slash, swing_sound, hit_sound, hit_particle, offhand } ] }
  Guava ImmutableMap keeps insertion order — conditional stance order is runtime priority order.
  Spliced as a List.of(...) element at indent 5; continuation lines bake absolute indents.
--><#t>
new WeaponStanceSystem(
                        ImmutableMap.<WeaponStance, PureNode>builder()
<#list conditions as c>
                            .put(WeaponStance.ENUM_MANAGER.getOrThrow(Identifier.parse("${c.stance}")),
${c.source})
</#list>
                            .build(),
                        WeaponStance.ENUM_MANAGER.getOrThrow(Identifier.parse("${default_stance}")),
                        ImmutableMap.<WeaponStance, EpicFightWeaponProperties>builder()
<#list properties as p>
                            .put(WeaponStance.ENUM_MANAGER.getOrThrow(Identifier.parse("${p.stance}")),
                                new EpicFightWeaponProperties(
                                    Set.of(<#list p.categories as cat>WeaponCategory.ENUM_MANAGER.getOrThrow(Identifier.parse("${cat}"))<#sep>, </#sep></#list>),
                                    new WeaponAttackMotions(
                                        List.of(<#list p.combo as m>ResourceKey.create(AkytheraRegistries.DataPack.ANIM_MONTAGE_KEY, Identifier.parse("${m}"))<#sep>, </#sep></#list>),
                                        ResourceKey.create(AkytheraRegistries.DataPack.ANIM_MONTAGE_KEY, Identifier.parse("${p.dash}")),
                                        ResourceKey.create(AkytheraRegistries.DataPack.ANIM_MONTAGE_KEY, Identifier.parse("${p.air_slash}"))
                                    ),
                                    BuiltInRegistries.SOUND_EVENT.getOrThrow(ResourceKey.create(Registries.SOUND_EVENT, Identifier.parse("${p.swing_sound}"))),
                                    BuiltInRegistries.SOUND_EVENT.getOrThrow(ResourceKey.create(Registries.SOUND_EVENT, Identifier.parse("${p.hit_sound}"))),
                                    BuiltInRegistries.PARTICLE_TYPE.getValue(Identifier.parse("${p.hit_particle}")),
                                    ${p.offhand?c}
                                ))
</#list>
                            .build()
                    )<#t>
