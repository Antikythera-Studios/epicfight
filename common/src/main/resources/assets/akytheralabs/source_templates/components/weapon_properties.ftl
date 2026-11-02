<#--
  Constructor expression for an "epicfight:weapon_properties" ComponentBlueprint
  (EpicFightWeaponProperties).
  DTO (EpicFightWeaponPropertiesHolder#getDTO):
    { categories, combo, dash, air_slash, swing_sound, hit_sound, hit_particle, offhand, enabled }
  A key is absent for a property the block leaves to whatever it overrides.
  Emits a single inline Java expression (no surrounding whitespace).
--><#t>
new EpicFightWeaponProperties(<#t>
<#if categories??>Optional.of(Set.of(<#list categories as cat>WeaponCategory.ENUM_MANAGER.getOrThrow(Identifier.parse("${cat}"))<#sep>, </#sep></#list>))<#else>Optional.empty()</#if><#t>
, <#if combo??>Optional.of(List.of(<#list combo as m>ResourceKey.create(AkytheraRegistries.DataPack.ANIM_MONTAGE_KEY, Identifier.parse("${m}"))<#sep>, </#sep></#list>))<#else>Optional.empty()</#if><#t>
, <#if dash??>Optional.of(ResourceKey.create(AkytheraRegistries.DataPack.ANIM_MONTAGE_KEY, Identifier.parse("${dash}")))<#else>Optional.empty()</#if><#t>
, <#if air_slash??>Optional.of(ResourceKey.create(AkytheraRegistries.DataPack.ANIM_MONTAGE_KEY, Identifier.parse("${air_slash}")))<#else>Optional.empty()</#if><#t>
, <#if swing_sound??>Optional.of(BuiltInRegistries.SOUND_EVENT.getOrThrow(ResourceKey.create(Registries.SOUND_EVENT, Identifier.parse("${swing_sound}"))))<#else>Optional.empty()</#if><#t>
, <#if hit_sound??>Optional.of(BuiltInRegistries.SOUND_EVENT.getOrThrow(ResourceKey.create(Registries.SOUND_EVENT, Identifier.parse("${hit_sound}"))))<#else>Optional.empty()</#if><#t>
, <#if hit_particle??>Optional.of(BuiltInRegistries.PARTICLE_TYPE.getValue(Identifier.parse("${hit_particle}")))<#else>Optional.empty()</#if><#t>
, <#if offhand??>Optional.of(${offhand?c})<#else>Optional.empty()</#if><#t>
, ${enabled?c})<#t>
