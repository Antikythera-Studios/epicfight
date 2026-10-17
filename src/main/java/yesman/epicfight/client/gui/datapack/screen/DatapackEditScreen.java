package yesman.epicfight.client.gui.datapack.screen;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.netty.util.internal.StringUtil;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.tabs.GridLayoutTab;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.components.tabs.TabNavigationBar;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.FolderRepositorySource;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.validation.DirectoryValidator;
import org.jetbrains.annotations.Nullable;
import yesman.epicfight.EpicFight;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.LivingMotion;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.api.animation.types.HitAnimation;
import yesman.epicfight.api.animation.types.LongHitAnimation;
import yesman.epicfight.api.animation.types.MainFrameAnimation;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.animation.types.datapack.DatapackAnimation;
import yesman.epicfight.api.animation.types.datapack.EditorAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.asset.JsonAssetLoader;
import yesman.epicfight.api.asset.SelfAccessor;
import yesman.epicfight.api.client.animation.AnimationSubFileReader;
import yesman.epicfight.api.client.animation.property.TrailInfo;
import yesman.epicfight.api.client.model.Meshes;
import yesman.epicfight.api.client.model.SkinnedMesh;
import yesman.epicfight.api.collider.Collider;
import yesman.epicfight.api.collider.MultiOBBCollider;
import yesman.epicfight.api.collider.OBBCollider;
import yesman.epicfight.api.data.reloader.ItemCapabilityReloadListener;
import yesman.epicfight.api.data.reloader.MobPatchReloadListener;
import yesman.epicfight.api.data.reloader.SkillReloadListener;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.InstantiateInvoker;
import yesman.epicfight.api.utils.ParseUtil;
import yesman.epicfight.client.gui.datapack.widgets.*;
import yesman.epicfight.client.gui.datapack.widgets.Grid.GridBuilder.RowEditButton;
import yesman.epicfight.client.gui.datapack.widgets.ResizableComponent.HorizontalSizing;
import yesman.epicfight.client.gui.screen.SideBarScreen;
import yesman.epicfight.client.gui.widgets.SideNavigationBarOpener;
import yesman.epicfight.data.conditions.Condition.ParameterEditor;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.gameasset.ColliderPreset;
import yesman.epicfight.generated.LangKeys;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.registry.EpicFightRegistries;
import yesman.epicfight.registry.entries.EpicFightParticles;
import yesman.epicfight.skill.SkillCategories;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.Faction;
import yesman.epicfight.world.capabilities.item.*;
import yesman.epicfight.world.capabilities.provider.ExtraEntryProvider;
import yesman.epicfight.world.damagesource.StunType;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class DatapackEditScreen extends Screen implements ExtraEntryProvider {
	private static DatapackEditScreen workingPackScreen;
	
	public static DatapackEditScreen getCurrentScreen() {
		return workingPackScreen;
	}
	
	public static boolean hasEditingScreen() {
		return workingPackScreen != null;
	}

    public static AssetAccessor<? extends StaticAnimation> animationByKey(String path) {
        if (workingPackScreen != null) {
            return workingPackScreen.getExtraOrBuiltInAnimation(path);
        }

        return AnimationManager.byKey(ResourceLocation.parse(path));
    }

    public static AssetAccessor<? extends SkinnedMesh> getMesh(String path) {
        if (workingPackScreen != null) {
            return workingPackScreen.getExtraOrBuiltInMesh(path);
        }

        return Meshes.get(ResourceLocation.parse(path));
    }

    public static AssetAccessor<? extends Armature> getArmature(String path) {
        if (workingPackScreen != null) {
            return workingPackScreen.getExtraOrBuiltInArmature(path);
        }

        return Armatures.get(ResourceLocation.parse(path));
    }

    public static Function<Item, ? extends CapabilityItem.Builder<?>> getWeaponType(String typeName) {
        if (workingPackScreen != null) {
            return workingPackScreen.getExtraOrBuiltInWeaponType(typeName);
        }

        return WeaponTypeReloadListener.get(typeName);
    }

    @SuppressWarnings("unchecked")
    public <T extends StaticAnimation> AnimationAccessor<T> getExtraOrBuiltInAnimation(String path) throws ClassCastException {
        ResourceLocation rl = ResourceLocation.parse(path);

        if (userAnimations.containsKey(rl)) {
            return (AnimationAccessor<T>)userAnimations.get(rl).getValue();
        }

        return AnimationManager.byKey(rl);
    }

    public AssetAccessor<? extends SkinnedMesh> getExtraOrBuiltInMesh(String path) {
        ResourceLocation rl = ResourceLocation.parse(path);

        if (workingPackScreen != null && workingPackScreen.userMeshes.containsKey(rl)) {
            return workingPackScreen.userMeshes.get(rl);
        }

        return Meshes.get(rl);
    }

    public AssetAccessor<? extends Armature> getExtraOrBuiltInArmature(String path) {
        ResourceLocation rl = ResourceLocation.parse(path);

        if (workingPackScreen != null && workingPackScreen.userArmatures.containsKey(rl)) {
            return workingPackScreen.userArmatures.get(rl);
        }

        return Armatures.get(rl);
    }

    public Function<Item, ? extends CapabilityItem.Builder<?>> getExtraOrBuiltInWeaponType(String typeName) {
        ResourceLocation typeId = ResourceLocation.parse(typeName);

        if (userWeaponTypes.containsKey(typeId)) {
            return userWeaponTypes.get(typeId);
        }

        return WeaponTypeReloadListener.get(typeName);
    }
	
	public static Set<Map.Entry<ResourceLocation, Function<Item, ? extends CapabilityItem.Builder<?>>>> getSerializableWeaponTypes() {
		return workingPackScreen.weaponTypeTab.packList.stream().reduce(Sets.<Map.Entry<ResourceLocation, Function<Item, ? extends CapabilityItem.Builder<?>>>>newHashSet(), (set, entry) -> {
			try {
				WeaponCapability.Builder builder = WeaponTypeReloadListener.deserializeWeaponCapabilityBuilder(entry.getKey(), entry.getValue(), workingPackScreen);
				Function<Item, ? extends CapabilityItem.Builder<?>> provider = (itemstack) -> builder;
				
				if (!workingPackScreen.userWeaponTypes.containsKey(entry.getKey())) {
					workingPackScreen.userWeaponTypes.put(entry.getKey(), provider);
				}
				
				set.add(PackEntry.ofValue(entry.getKey(), provider));
			} catch (Exception e) {
				EpicFight.LOGGER.error("Can not deserialize weapon type " + entry.getKey());
				return set;
			}
			
			return set;
		}, (set1, set2) -> {
			set1.addAll(set2);
			return set1;
		});
	}

	private GridLayout bottomButtons;
	private TabNavigationBar tabNavigationBar;

    @Nullable
    private final Screen parentScreen;
	private final DatapackEditScreen.WeaponTypeTab weaponTypeTab;
	private final DatapackEditScreen.ItemCapabilityTab itemCapabilityTab;
	private final DatapackEditScreen.MobCapabilityTab mobCapabilityTab;
    private final SideNavigationBarOpener sideNavigationBarOpener;

	private final Map<ResourceLocation, PackEntry<EditorAnimation, DatapackAnimation<? extends StaticAnimation>>> userAnimations = Maps.newLinkedHashMap();
	private final Map<ResourceLocation, AssetAccessor<? extends SkinnedMesh>> userMeshes = Maps.newLinkedHashMap();
	private final Map<ResourceLocation, AssetAccessor<? extends Armature>> userArmatures = Maps.newLinkedHashMap();
	private final Map<ResourceLocation, Function<Item, ? extends CapabilityItem.Builder<?>>> userWeaponTypes = Maps.newLinkedHashMap();
	
	private final TabManager tabManager = new TabManager(this::addRenderableWidget, (p_267853_) -> {
		this.removeWidget(p_267853_);
	}) {
		@Override
		public void setCurrentTab(Tab tab, boolean playSound) {
			if (this.getCurrentTab() instanceof DatapackTab<?> datapackTab) {
				DatapackEditScreen.this.removeWidget(datapackTab.packListGrid);
				DatapackEditScreen.this.removeWidget(datapackTab.inputComponentsList);
			}
			
			super.setCurrentTab(tab, playSound);
			
			if (tab instanceof DatapackTab<?> datapackTab) {
				DatapackEditScreen.this.addRenderableWidget(datapackTab.packListGrid);
				DatapackEditScreen.this.addRenderableWidget(datapackTab.inputComponentsList);
			}
		}
	};
	
	public DatapackEditScreen(@Nullable Screen parentScreen) {
		super(Component.translatable(LangKeys.GUI_TITLE_DATAPACK_EDITOR));

		this.parentScreen = parentScreen;
		this.minecraft = parentScreen == null ? Minecraft.getInstance() : Minecraft.getInstance();
		this.font = this.minecraft.font;
		
		this.weaponTypeTab = new DatapackEditScreen.WeaponTypeTab();
		this.itemCapabilityTab = new DatapackEditScreen.ItemCapabilityTab();
		this.mobCapabilityTab = new DatapackEditScreen.MobCapabilityTab();
		
		if (workingPackScreen == null) {
			workingPackScreen = this;
		}

        this.sideNavigationBarOpener = new SideNavigationBarOpener(this, this.parentScreen, SideBarScreen::createConfigScreenSideNavBar);
	}

	public boolean importDataPack(Path path) {
		FolderRepositorySource.FolderPackDetector packDetector = new FolderRepositorySource.FolderPackDetector(new DirectoryValidator(path$2 -> true));
		Pack.ResourcesSupplier pack$resourcessupplier = null;
		try {
			pack$resourcessupplier = packDetector.detectPackResources(path, new ArrayList<> ());
		} catch (IOException e) {
			this.minecraft.setScreen(new MessageScreen<>("Invalid datapack", e.getMessage(), this, (button2) -> this.minecraft.setScreen(this), 160, 60));
		}
		
		if (pack$resourcessupplier != null) {
			String s = path.getFileName().toString();
			PackLocationInfo packlocationinfo = new PackLocationInfo("file/" + s, Component.literal(s), PackSource.WORLD, Optional.empty());
            Pack pack = Pack.readMetaAndCreate(packlocationinfo, pack$resourcessupplier, PackType.SERVER_DATA, null);
            
			PackResources packResources = pack.open();
			
			this.importUserData(packResources);
			this.weaponTypeTab.importEntries(packResources);
			this.itemCapabilityTab.importEntries(packResources);
			this.mobCapabilityTab.importEntries(packResources);
			
			packResources.close();
			
			return true;
		} else {
			this.minecraft.setScreen(new MessageScreen<>("Invalid datapack", "", this, (button2) -> this.minecraft.setScreen(this), 160, 60));
			return false;
		}
	}
	
	public boolean exportDataPack(String packName) {
		ZipOutputStream out = null;
		File zipFile = null;
		
		try {
			File resourcePackDirectory = Minecraft.getInstance().getResourcePackDirectory().toFile();
			zipFile = new File(resourcePackDirectory, packName + ".zip");
			
			int duplicateCount = 1;
			
			while (zipFile.exists()) {
				zipFile = new File(resourcePackDirectory, packName + String.format(" (%d).zip", duplicateCount));
				duplicateCount++;
			}
			
			out = new ZipOutputStream(new FileOutputStream(zipFile));
			
			this.weaponTypeTab.exportEntries(out);
			this.itemCapabilityTab.exportEntries(out);
			this.mobCapabilityTab.exportEntries(out);
			this.exportUserData(out);
			
			ZipEntry zipEntry = new ZipEntry("pack.mcmeta");
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			JsonObject root = new JsonObject();
			JsonObject pack = new JsonObject();
			int datapackVersion = SharedConstants.getCurrentVersion().getPackVersion(PackType.SERVER_DATA);
			int resourcepackVersion = SharedConstants.getCurrentVersion().getPackVersion(PackType.CLIENT_RESOURCES);
			
			if (datapackVersion != resourcepackVersion) {
				EpicFight.LOGGER.warn(new StringBuilder("Pack version is not matching in ").append(SharedConstants.getCurrentVersion().getId()).toString());
			}
			
			pack.addProperty("description", packName);
			pack.addProperty("pack_format", datapackVersion);
			root.add("pack", pack);
			out.putNextEntry(zipEntry);
			out.write(gson.toJson(root).getBytes());
			out.closeEntry();
			out.close();
			
			Util.getPlatform().openFile(resourcePackDirectory);
			
			return true;
		} catch (Exception e) {
			this.minecraft.setScreen(new MessageScreen<>("Failed to export datapack", e.getMessage(), this, (button2) -> this.minecraft.setScreen(this), 400, 110).autoCalculateHeight());
			e.printStackTrace();
			
			try {
				if (out != null) {
					out.close();
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			
			zipFile.delete();
			
			return false;
		}
	}
	
	@Override
	public void onFilesDrop(List<Path> filePath) {
		boolean isJsonFiles = true;
		
		for (Path path : filePath) {
			isJsonFiles &= path.getFileName().toString().indexOf(".json") > -1;
		}
		
		if (isJsonFiles) {
			this.minecraft.setScreen(new MessageScreen<>("", "Enter the mod id", this,
					(modid) -> {
						StringBuilder sb = new StringBuilder();
						boolean hasException = false;
						
						for (Path path : filePath) {
							try {
								InputStream stream = Files.newInputStream(path);
								ResourceLocation registryName = ResourceLocation.fromNamespaceAndPath(modid, path.getFileName().toString().replaceAll(".json", ""));
								
								if (this.tabManager.getCurrentTab() == this.weaponTypeTab) {
									this.weaponTypeTab.importJson(registryName, stream);
								} else if (this.tabManager.getCurrentTab() == this.itemCapabilityTab) {
									this.itemCapabilityTab.importJson(registryName, null, stream);
								} else if (this.tabManager.getCurrentTab() == this.mobCapabilityTab) {
									this.mobCapabilityTab.importJson(registryName, stream);
								}
							} catch (Exception e) {
								hasException = true;
								sb.append(String.format("%s : %s\n", path.getFileName().toString(), e.getMessage()));
								e.printStackTrace();
							}
						}
						
						if (hasException) {
							this.minecraft.setScreen(new MessageScreen<>("Failed to import these items", sb.toString(), this, (button2) -> this.minecraft.setScreen(this), 300, 70).autoCalculateHeight());
						} else {
							this.minecraft.setScreen(this);
						}
					},
					(button) -> this.minecraft.setScreen(this),
					new ResizableEditBox(this.minecraft.font, 0, 0, 0, 16, Component.literal("datapack_edit.input"), null, null), 120, 80));
		} else {
			if (filePath.size() > 1) {
				this.minecraft.setScreen(new MessageScreen<>("", "Please drop only one file", this, (button) -> this.minecraft.setScreen(this), 160, 50));
			}
			
			Path path = filePath.get(0);
			
			if (this.weaponTypeTab.packList.size() > 0 || this.itemCapabilityTab.packList.size() > 0 || this.mobCapabilityTab.packList.size() > 0) {
				this.minecraft.setScreen(new MessageScreen<>("", "The current entries will be removed if you import the new data pack. Do you want to proceed?", this,
															(button) -> {
																DatapackEditScreen newScreen = new DatapackEditScreen(this.parentScreen);
																workingPackScreen = newScreen;
																
																if (newScreen.importDataPack(path)) {
																	this.minecraft.setScreen(newScreen);
																}
															}, (button) -> this.minecraft.setScreen(this), 200, 90));
			} else {
				workingPackScreen = this;
				this.importDataPack(path);
			}
		}
	}
	
	@Override
	protected void init() {
		// Enable stencil buffer to render a grid inside the area
		// Note: RenderTarget.enableStencil() is a NeoForge-only method and not available in vanilla 1.21.1.
		// The stencil buffer may not be explicitly enabled on Fabric, but grid rendering still works
		// because the default framebuffer may already have stencil bits allocated.
		Minecraft.getInstance().getMainRenderTarget();

        if (this.tabNavigationBar != null) this.removeWidget(this.tabNavigationBar);
        if (this.bottomButtons != null) this.bottomButtons.visitWidgets(this::removeWidget);
        if (this.sideNavigationBarOpener != null) this.removeWidget(this.sideNavigationBarOpener);

		this.tabNavigationBar = TabNavigationBar.builder(this.tabManager, this.width).addTabs(this.weaponTypeTab, this.itemCapabilityTab, this.mobCapabilityTab).build();
		this.tabNavigationBar.selectTab(0, false);
		
	    this.addRenderableWidget(this.tabNavigationBar);
	    this.bottomButtons = (new GridLayout()).columnSpacing(10);
	    
		GridLayout.RowHelper gridlayout$rowhelper = this.bottomButtons.createRowHelper(2);
		gridlayout$rowhelper.addChild(Button.builder(Component.translatable(LangKeys.DATAPACK_EDIT_EXPORT), (button) -> {
			try {
				this.weaponTypeTab.validateBeforeExport();
				this.itemCapabilityTab.validateBeforeExport();
				this.mobCapabilityTab.validateBeforeExport();
			} catch (Exception e) {
				e.printStackTrace();
				this.minecraft.setScreen(new MessageScreen<>("", e.getMessage(), this, (button2) -> this.minecraft.setScreen(this.createExportScreen()),
															(button3) -> this.minecraft.setScreen(this), 400, 110).autoCalculateHeight().withOkTitle(Component.translatable("datapack_edit.export_with_exceptions")));
				return;
			}
			
			this.minecraft.setScreen(this.createExportScreen());
		}).build());
		
		gridlayout$rowhelper.addChild(Button.builder(CommonComponents.GUI_CANCEL, (button) -> {
			this.onClose();
		}).build());
		
		this.bottomButtons.visitWidgets((button) -> {
			button.setTabOrderGroup(1);
			this.addRenderableWidget(button);
		});
		
		this.repositionElements();

		if (!this.initialized && workingPackScreen != null && workingPackScreen != this && this.minecraft.screen == this) {
			this.initialized = true;
			
			this.minecraft.setScreen(new MessageScreen<>("", "Would you like to load the previous pack?", this, (button) -> {
				this.minecraft.setScreen(workingPackScreen);
			}, (button) -> {
				if (workingPackScreen != null) {
					workingPackScreen.destroyUserData();
				}
				
				this.minecraft.setScreen(this);
				workingPackScreen = this;
			}, 180, 70));
		}

        this.addRenderableWidget(this.sideNavigationBarOpener);
	}
	
	@Override
	public void repositionElements() {
		if (this.tabNavigationBar != null && this.bottomButtons != null) {
			this.tabNavigationBar.setWidth(this.width);
			this.tabNavigationBar.arrangeElements();
			this.bottomButtons.arrangeElements();
			FrameLayout.centerInRectangle(this.bottomButtons, 0, this.height - 36, this.width, 36);
			int i = this.tabNavigationBar.getRectangle().bottom();
			ScreenRectangle screenrectangle = new ScreenRectangle(0, i, this.width, this.bottomButtons.getY() - i);
			this.tabManager.setTabArea(screenrectangle);
		}

        this.sideNavigationBarOpener.relocate(this.getRectangle());
	}
	
	@Override
	public void tick() {
		if (this.tabManager.getCurrentTab() instanceof TickableComponent tickableComponent) {
			tickableComponent._tick();
		}
	}
	
	@Override
	public void onClose() {
		if (this.weaponTypeTab.packList.size() > 0 || this.itemCapabilityTab.packList.size() > 0 || this.mobCapabilityTab.packList.size() > 0 || !this.userMeshes.isEmpty() || !this.userArmatures.isEmpty() || !this.userAnimations.isEmpty()) {
			workingPackScreen = this;
		} else {
			workingPackScreen = null;
		}
		
		this.weaponTypeTab.modelPreviewer.onDestroy();
		this.itemCapabilityTab.modelPreviewer.onDestroy();
		this.minecraft.setScreen(this.parentScreen);
	}
	
	@Override
	public boolean mouseClicked(double x, double y, int button) {
		if (this.getFocused() != null && this.getFocused().mouseClicked(x, y, button)) {
			return true;
		}
		
		return super.mouseClicked(x, y, button);
	}
	
	@Override
	public boolean keyPressed(int keycode, int p_100876_, int p_100877_) {
		if (this.tabNavigationBar.keyPressed(keycode)) {
			return true;
		} else if (super.keyPressed(keycode, p_100876_, p_100877_)) {
			return true;
		} else if (keycode != 257 && keycode != 335) {
			return false;
		} else {
			return true;
		}
	}
	
	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
		return this.getChildAt(mouseX, mouseY).filter((listener) -> {
			return listener.mouseDragged(mouseX, mouseY, button, dx, dy);
		}).isPresent();
	}
	
	@Override
	public void setFocused(@Nullable GuiEventListener target) {
		if (this.getFocused() == target) {
			return;
		}
		
		super.setFocused(target);
	}
	
	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
		guiGraphics.blit(Screen.FOOTER_SEPARATOR, 0, Mth.roundToward(this.height - 36 - 2, 2), 0.0F, 0.0F, this.width, 2, 32, 2);
		super.render(guiGraphics, mouseX, mouseY, partialTick);
	}
	
	private <T> MessageScreen<T> createExportScreen() {
		return new MessageScreen<>("", "Enter the pack title", this, null, 180, 70) {
			@Override
			protected void init() {
				this.parentScreen.init(this.minecraft, this.width, this.height);
				int height = this.messageBoxHeight / 2;
				
				final EditBox titleEditBox = new EditBox(this.font, this.width / 2 - 72, this.height / 2 - 6, 144, 16, Component.literal("pack_title_input_box"));
				
				this.addRenderableWidget(titleEditBox);
				this.addRenderableWidget(Button.builder(CommonComponents.GUI_OK, (button$2) -> {
					if (DatapackEditScreen.this.exportDataPack(titleEditBox.getValue())) {
						DatapackEditScreen.this.minecraft.setScreen(DatapackEditScreen.this);
					}
				}).bounds(this.width / 2 - 56, this.height / 2 + height - 20, 55, 16).build());
				this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, (button$2) -> this.minecraft.setScreen(DatapackEditScreen.this)).bounds(this.width / 2 + 1, this.height / 2 + height - 20, 55, 16).build());
			}
		};
	}
	
	private void importUserData(PackResources packResources) {
		packResources.getNamespaces(PackType.CLIENT_RESOURCES).stream().distinct().forEach((namespace) -> {
			packResources.listResources(PackType.CLIENT_RESOURCES, namespace, "animmodels/entity", (resourceLocation, stream) -> {
				try {
					JsonReader jsonReader = new JsonReader(new InputStreamReader(stream.get(), StandardCharsets.UTF_8));
					jsonReader.setLenient(true);
					JsonObject jsonObject = Streams.parse(jsonReader).getAsJsonObject();
					ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(resourceLocation.getNamespace(), resourceLocation.getPath().replaceAll("animmodels/", "").replaceAll(".json", ""));
					JsonAssetLoader modelLoader = new JsonAssetLoader(jsonObject, resourceLocation);
					SkinnedMesh mesh = null;
					Armature armature = null;
					
					try {
						mesh = modelLoader.loadSkinnedMesh(SkinnedMesh::new);
						armature = modelLoader.loadArmature(Armature::new);
					} catch(Exception e) {
						e.printStackTrace();
					} finally {}
					
					if (mesh != null) {
						this.userMeshes.put(rl, SelfAccessor.create(rl, mesh));
					}
					
					if (armature != null) {
						this.userArmatures.put(rl, SelfAccessor.create(rl, armature));
					}
				} catch (Exception e) {
					EpicFight.LOGGER.error("Failed to read model " + resourceLocation);
					e.printStackTrace();
				}
			});
			
			packResources.listResources(PackType.CLIENT_RESOURCES, namespace, "animmodels/animations", (resourceLocation, stream) -> {
				if (resourceLocation.getPath().contains("/data/")) {
					return;
				}
				
				try {
					JsonReader jsonReader = new JsonReader(new InputStreamReader(stream.get(), StandardCharsets.UTF_8));
					jsonReader.setLenient(true);
					
					JsonObject jsonObject = Streams.parse(jsonReader).getAsJsonObject();
					ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(resourceLocation.getNamespace(), resourceLocation.getPath().replaceAll("animmodels/animations/", "").replaceAll(".json", ""));
					ResourceLocation datapath = AnimationManager.getSubAnimationFileLocation(resourceLocation, AnimationSubFileReader.SUBFILE_CLIENT_PROPERTY);
					IoSupplier<InputStream> streamSupplier = packResources.getResource(PackType.CLIENT_RESOURCES, datapath);
					JsonElement constructorElement = jsonObject.getAsJsonObject().get("constructor");
					
					if (constructorElement == null) {
						throw new IllegalStateException(String.format("No constructor information has provided in User animation %s", rl));
					}
					
					JsonObject constructorObject = constructorElement.getAsJsonObject();
					String invocationCommand = constructorObject.get("invocation_command").getAsString();
					
					if (invocationCommand.lastIndexOf('#') == -1) {
						throw new IllegalStateException(String.format("Invocation command exception: Missing separator %s in animation %s", invocationCommand, rl));
					}
					
					String className = invocationCommand.substring(invocationCommand.lastIndexOf('#') + 1);

                    if ("yesman.epicfight.api.animation.types.BasicAttackAnimation".equals(className)) {
                        className = "yesman.epicfight.api.animation.types.ComboAttackAnimation"; // Class name changed: BasicAttackAnimation(1.20.1) -> "ComboAttackAnimation"
                    }

					@SuppressWarnings({ "unchecked" })
					Class<? extends DatapackAnimation<? extends StaticAnimation>> animationClass = EditorAnimation.switchType((Class<? extends StaticAnimation>)Class.forName(className));
					DatapackAnimation<? extends StaticAnimation> animation = InstantiateInvoker.invoke(invocationCommand, animationClass).getResult();
					
					if (!animation.registryName().equals(rl)) {
						throw new IllegalArgumentException("Initializer registry identifier " + animation.registryName() + " is not matching with file path " + rl);
					}
					
					if (streamSupplier != null) {
						AnimationSubFileReader.SUBFILE_CLIENT_PROPERTY.apply(streamSupplier.get(), animation.get());
					}
					
					JsonAssetLoader modelLoader = new JsonAssetLoader(jsonObject, resourceLocation);
					animation.setAnimationClip(modelLoader.loadAnimationClip(animation.get().getArmature().get()));
					this.userAnimations.put(
                        animation.registryName(),
                        PackEntry.ofValue(
                            animation.readAnimationFromJson(
                                JsonAssetLoader.getAsTransformFormatOrDefault(modelLoader.getRootJson(), "format"),
                                modelLoader.getRootJson().get("animation").getAsJsonArray()
                            ),
                            animation
                        )
                    );
				} catch (Exception e) {
					EpicFight.LOGGER.error("Failed to read animation " + resourceLocation);
					e.printStackTrace();
				}
			});
		});
	}
	
	private void exportUserData(ZipOutputStream out) throws Exception {
		Map<ResourceLocation, JsonObject> models = Maps.newHashMap();
		
		for (Map.Entry<ResourceLocation, AssetAccessor<? extends SkinnedMesh>> entry : this.userMeshes.entrySet()) {
			models.put(entry.getKey(), entry.getValue().get().toJsonObject());
		}
		
		for (Map.Entry<ResourceLocation, AssetAccessor<? extends Armature>> entry : this.userArmatures.entrySet()) {
			String exportPath = String.format("data/%s/animmodels/%s.json", entry.getKey().getNamespace(), entry.getKey().getPath());
			ZipEntry zipEntry = new ZipEntry(exportPath);
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			JsonObject armatureJson = entry.getValue().get().toJsonObject();
			
			out.putNextEntry(zipEntry);
			out.write(gson.toJson(armatureJson).getBytes());
			out.closeEntry();
			
			models.computeIfAbsent(entry.getKey(), (k) -> armatureJson);
			models.computeIfPresent(entry.getKey(), (k, oldVal) -> {
				oldVal.add("armature", armatureJson.get("armature"));
				return oldVal;
			});
		}
		
		for (Map.Entry<ResourceLocation, JsonObject> entry : models.entrySet()) {
			String exportPath = String.format("assets/%s/animmodels/%s.json", entry.getKey().getNamespace(), entry.getKey().getPath());
			ZipEntry zipEntry = new ZipEntry(exportPath);
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			
			out.putNextEntry(zipEntry);
			out.write(gson.toJson(entry.getValue()).getBytes());
			out.closeEntry();
		}
		
		for (Map.Entry<ResourceLocation, PackEntry<EditorAnimation, DatapackAnimation<? extends StaticAnimation>>> entry : this.userAnimations.entrySet()) {
			String exportPath = String.format("assets/%s/animmodels/animations/%s.json", entry.getKey().getNamespace(), entry.getKey().getPath());
			ResourceLocation clientData = AnimationManager.getSubAnimationFileLocation(ResourceLocation.fromNamespaceAndPath(entry.getKey().getNamespace(), exportPath), AnimationSubFileReader.SUBFILE_CLIENT_PROPERTY);
			ZipEntry asResource = new ZipEntry(exportPath);
			ZipEntry asResourceClientData = new ZipEntry(clientData.getPath());
			ZipEntry asData = new ZipEntry(String.format("data/%s/animmodels/animations/%s.json", entry.getKey().getNamespace(), entry.getKey().getPath()));
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			EditorAnimation animation = entry.getValue().getValue().getCreator();
			JsonObject root = new JsonObject();
			JsonObject constructorInfo = new JsonObject();
			constructorInfo.addProperty("invocation_command", animation.getInvocationCommand());
			
			root.add("constructor", constructorInfo);
            root.addProperty("format", ParseUtil.toLowerCase(animation.getTransformFormat().toString()));
			root.add("animation", animation.getRawAnimationJson());
			
			out.putNextEntry(asResource);
			out.write(gson.toJson(root).getBytes());
			out.closeEntry();
			
			if (animation.getPropertiesJson().size() > 0) {
				out.putNextEntry(asResourceClientData);
				out.write(gson.toJson(animation.getPropertiesJson()).getBytes());
				out.closeEntry();
			}
			
			out.putNextEntry(asData);
			out.write(gson.toJson(root).getBytes());
			out.closeEntry();
		}
	}
	
	public void destroyUserData() {
		this.userMeshes.values().forEach((accessor) -> {
			accessor.get().destroy();
		});
	}
	
	public Map<ResourceLocation, AssetAccessor<? extends SkinnedMesh>> getUserMeshes() {
		return this.userMeshes;
	}
	
	public Map<ResourceLocation, AssetAccessor<? extends Armature>> getUserArmatures() {
		return this.userArmatures;
	}
	
	public Map<ResourceLocation, PackEntry<EditorAnimation, DatapackAnimation<? extends StaticAnimation>>> getUserAniamtions() {
		return this.userAnimations;
	}
	
	abstract class DatapackTab<T> extends GridLayoutTab implements TickableComponent {
		protected Grid packListGrid;
		protected InputComponentList<CompoundTag> inputComponentsList;
		protected final Registry<T> registry;
		protected final Set<ResourceLocation> namesSet = Sets.newHashSet();
		protected final List<PackEntry<ResourceLocation, CompoundTag>> packList = Lists.newLinkedList();
		protected final String directory;
		
		public DatapackTab(Component title, String directory, @Nullable Registry<T> registry) {
			this(title, directory, registry, (item) -> true);
		}
		
		public DatapackTab(Component title, String directory, @Nullable Registry<T> registry, Predicate<T> filter) {
			super(title);
			
			this.directory = directory;
			
			ScreenRectangle screenRect = DatapackEditScreen.this.getRectangle();
			
			this.packListGrid = Grid.builder(DatapackEditScreen.this)
									.xy1(8, screenRect.top() + 14)
									.xy2(150, screenRect.height() - screenRect.top() - 7)
									.rowHeight(26)
									.rowEditable(RowEditButton.ADD_REMOVE)
									.rowpositionChanged(this::packGridRowpositionChanged)
									.addColumn(Grid.editbox("pack_item")
													.editWidgetCreated((editbox) -> editbox.setFilter(ParseUtil::canParseToResourceLocation))
													.valueChanged((event) -> this.packList.get(event.rowposition).setPackKey(ResourceLocation.parse(event.postValue)))
									.defaultVal(EpicFightMod.prefix("")).editable(registry == null ? true : false).width(180))
									.pressAdd((grid, button) -> {
										if (registry != null) {
											DatapackEditScreen.this.minecraft.setScreen(new SelectFromRegistryScreen<>(DatapackEditScreen.this, registry, (registryName, selItem) -> {
												grid.setValueChangeEnabled(false);
												int rowposition = grid.addRowWithDefaultValues("pack_item", registryName);
												this.packList.add(rowposition, PackEntry.of(ResourceLocation.parse(registryName), CompoundTag::new));
												grid.setGridFocus(rowposition, "pack_item");
												grid.setValueChangeEnabled(true);
											}, (registryName, selItem) -> {}, filter));
										} else {
											grid.setValueChangeEnabled(false);
											int rowposition = grid.addRowWithDefaultValues("pack_item", EpicFightMod.prefix(""));
											this.packList.add(rowposition, PackEntry.of(EpicFight.identifier(""), CompoundTag::new));
											grid.setGridFocus(rowposition, "pack_item");
											grid.setValueChangeEnabled(true);
										}
										
										DatapackEditScreen.this.setFocused(grid);
									})
									.pressRemove((grid, button) -> {
										grid.removeRow((removedRow) -> this.packList.remove(removedRow));
										
										if (this.packList.size() == 0) {
											this.inputComponentsList.setComponentsActive(false);
										}
									})
									.build();
			
			this.registry = registry;
		}
		
		@Override
		public void doLayout(ScreenRectangle screenRectangle) {
			this.layout.arrangeElements();
			this.layout.setY(screenRectangle.top());
			
			this.packListGrid.updateSizeAndPosition(150, screenRectangle.height() - screenRectangle.top() - 7, screenRectangle.top() + 14);
			this.inputComponentsList.updateSizeAndPosition(screenRectangle.width() - 172, screenRectangle.height() - screenRectangle.top() - 7, screenRectangle.top() + 14);
			
			this.packListGrid.setX(8);
			this.inputComponentsList.setX(164);
		}
		
		@Override
		public void _tick() {
			this.inputComponentsList.tick();
		}
		
		public void clear() {
			this.packList.clear();
			this.packListGrid.reset();
			this.inputComponentsList.resetComponents();
			this.inputComponentsList.setComponentsActive(false);
		}
		
		public void packGridRowpositionChanged(int rowposition, Map<String, Object> values) {
			this.inputComponentsList.importTag(this.packList.get(rowposition).getValue());
		}
		
		public abstract void validateBeforeExport();
		public abstract void importEntries(PackResources packResources) throws Exception;
		public abstract void exportEntries(ZipOutputStream out) throws Exception;
	}
	
	class WeaponTypeTab extends DatapackTab<ResourceLocation> {
		private final ModelPreviewer modelPreviewer;
		
		public WeaponTypeTab() {
			super(Component.translatable(LangKeys.GUI_TAB_DATAPACK_WEAPON_TYPE), WeaponTypeReloadListener.DIRECTORY, null);

			Screen parentScreen = DatapackEditScreen.this;
			Font font = DatapackEditScreen.this.font;
			ScreenRectangle rect = DatapackEditScreen.this.getRectangle();
			
			this.modelPreviewer = new ModelPreviewer(9, 15, 0, 140, HorizontalSizing.LEFT_RIGHT, null, Armatures.BIPED, Meshes.BIPED);
			this.modelPreviewer.setColliderJoint(Armatures.BIPED.get().searchJointByName("Tool_R"));
			
			this.inputComponentsList = new InputComponentList<>(DatapackEditScreen.this, 0, 0, 0, 30) {
				@Override
				public void importTag(CompoundTag tag) {
					CompoundTag colliderTag = ParseUtil.getOrSupply(tag, "collider", CompoundTag::new);
					boolean centerInit = colliderTag.contains("center");
					boolean sizeInit = colliderTag.contains("size");
					
					if (!centerInit) {
						ListTag list = new ListTag();
						list.add(DoubleTag.valueOf(0.0D));
						list.add(DoubleTag.valueOf(0.0D));
						list.add(DoubleTag.valueOf(0.0D));
						colliderTag.put("center", list);
					}
					
					if (!sizeInit) {
						ListTag list = new ListTag();
						list.add(DoubleTag.valueOf(0.0D));
						list.add(DoubleTag.valueOf(0.0D));
						list.add(DoubleTag.valueOf(0.0D));
						colliderTag.put("size", list);
					}
					
					this.setComponentsActive(true);
					
					Grid.PackImporter packImporter = new Grid.PackImporter();
					
					for (String key : tag.getCompound("innate_skills").getAllKeys()) {
						packImporter.newRow();
						packImporter.newValue("style", Style.ENUM_MANAGER.get(key));
						packImporter.newValue("skill", SkillReloadListener.getSkill(tag.getCompound("innate_skills").getString(key)));
					}
					
					this.setDataBindingComponenets(new Object[] {
						WeaponCategory.ENUM_MANAGER.get(tag.getString("category")),
						BuiltInRegistries.PARTICLE_TYPE.get(ResourceLocation.parse(tag.getString("hit_particle"))),
						BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse(tag.getString("hit_sound"))),
						BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse(tag.getString("swing_sound"))),
						tag.contains("usable_in_offhand") ? tag.getBoolean("usable_in_offhand") : true,
						null,
						ParseUtil.nullOrToString(colliderTag.get("number"), Tag::getAsString),
						centerInit ? ParseUtil.valueOfOmittingType(ParseUtil.nullOrToString(colliderTag.getList("center", Tag.TAG_DOUBLE).get(0), Tag::getAsString)) : "",
						centerInit ? ParseUtil.valueOfOmittingType(ParseUtil.nullOrToString(colliderTag.getList("center", Tag.TAG_DOUBLE).get(1), Tag::getAsString)) : "",
						centerInit ? ParseUtil.valueOfOmittingType(ParseUtil.nullOrToString(colliderTag.getList("center", Tag.TAG_DOUBLE).get(2), Tag::getAsString)) : "",
						sizeInit ? ParseUtil.valueOfOmittingType(ParseUtil.nullOrToString(colliderTag.getList("size", Tag.TAG_DOUBLE).get(0), Tag::getAsString)) : "",
						sizeInit ? ParseUtil.valueOfOmittingType(ParseUtil.nullOrToString(colliderTag.getList("size", Tag.TAG_DOUBLE).get(1), Tag::getAsString)) : "",
						sizeInit ? ParseUtil.valueOfOmittingType(ParseUtil.nullOrToString(colliderTag.getList("size", Tag.TAG_DOUBLE).get(2), Tag::getAsString)) : "",
						packImporter
					});
				}
			};
			
			this.inputComponentsList.setX(164);
			
			this.inputComponentsList.newRow();
			this.inputComponentsList.addComponentCurrentRow(new Static(parentScreen, this.inputComponentsList.nextStart(4), 100, 60, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.weapon_type.category"));
			this.inputComponentsList.addComponentCurrentRow(new ComboBox<>(parentScreen, Minecraft.getInstance().font, this.inputComponentsList.nextStart(5), 124, 100, 15, HorizontalSizing.LEFT_WIDTH, null, 8,
																			Component.translatable("datapack_edit.weapon_type.category"), new ArrayList<>(WeaponCategory.ENUM_MANAGER.universalValues()), ParseUtil::snakeToSpacedCamel,
																			(weaponCategory) -> this.packList.get(this.packListGrid.getRowposition()).getValue().putString("category", ParseUtil.nullParam(weaponCategory).toLowerCase(Locale.ROOT))));
			
			this.inputComponentsList.newRow();
			this.inputComponentsList.addComponentCurrentRow(new Static(parentScreen, this.inputComponentsList.nextStart(4), 100, 60, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.weapon_type.hit_particle"));
			this.inputComponentsList.addComponentCurrentRow(new PopupBox.RegistryPopupBox<>(parentScreen, font, this.inputComponentsList.nextStart(5), 15, 130, 15, HorizontalSizing.LEFT_RIGHT, null,
																			Component.translatable("datapack_edit.weapon_type.hit_particle"), BuiltInRegistries.PARTICLE_TYPE,
																			(pair) -> this.packList.get(this.packListGrid.getRowposition()).getValue().putString("hit_particle", ParseUtil.getRegistryName(pair.getSecond(), BuiltInRegistries.PARTICLE_TYPE))));
			
			this.inputComponentsList.newRow();
			this.inputComponentsList.addComponentCurrentRow(new Static(parentScreen, this.inputComponentsList.nextStart(4), 100, 60, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.weapon_type.hit_sound"));
			this.inputComponentsList.addComponentCurrentRow(new PopupBox.SoundPopupBox(parentScreen, font, this.inputComponentsList.nextStart(5), 15, 130, 15, HorizontalSizing.LEFT_RIGHT, null,
																			Component.translatable("datapack_edit.weapon_type.hit_sound"),
																			(pair) -> this.packList.get(this.packListGrid.getRowposition()).getValue().putString("hit_sound", ParseUtil.getRegistryName(pair.getSecond(), BuiltInRegistries.SOUND_EVENT))));
			
			this.inputComponentsList.newRow();
			this.inputComponentsList.addComponentCurrentRow(new Static(parentScreen, this.inputComponentsList.nextStart(4), 100, 60, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.weapon_type.swing_sound"));
			this.inputComponentsList.addComponentCurrentRow(new PopupBox.SoundPopupBox(parentScreen, font, this.inputComponentsList.nextStart(5), 15, 130, 15, HorizontalSizing.LEFT_RIGHT, null,
																			Component.translatable("datapack_edit.weapon_type.swing_sound"),
																			(pair) -> this.packList.get(this.packListGrid.getRowposition()).getValue().putString("swing_sound", ParseUtil.getRegistryName(pair.getSecond(), BuiltInRegistries.SOUND_EVENT))));
			
			this.inputComponentsList.newRow();
			this.inputComponentsList.addComponentCurrentRow(new Static(parentScreen, this.inputComponentsList.nextStart(4), 100, 60, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.weapon_type.styles"));
			this.inputComponentsList.addComponentCurrentRow(SubScreenOpenButton.builder().subScreen(() -> {
				return new StylesScreen(DatapackEditScreen.this, this.packList.get(this.packListGrid.getRowposition()).getValue());
			}).bounds(this.inputComponentsList.nextStart(4), 0, 15, 15).build());
			
			this.inputComponentsList.newRow();
			this.inputComponentsList.addComponentCurrentRow(new Static(parentScreen, this.inputComponentsList.nextStart(4), 100, 60, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.weapon_type.offhand_usability"));
			this.inputComponentsList.addComponentCurrentRow(new CheckBox(font, this.inputComponentsList.nextStart(4), 60, 0, 10, HorizontalSizing.LEFT_WIDTH, null, null, Component.literal(""), (val) -> {
				this.packList.get(this.packListGrid.getRowposition()).getValue().putBoolean("usable_in_offhand", val);
			}));
			
			this.inputComponentsList.newRow();
			this.inputComponentsList.addComponentCurrentRow(new Static(parentScreen, this.inputComponentsList.nextStart(4), 100, 100, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.weapon_type.offhand_visibility"));
			this.inputComponentsList.addComponentCurrentRow(SubScreenOpenButton.builder().subScreen(() -> {
				return new OffhandValidatorScreen(DatapackEditScreen.this, this.packList.get(this.packListGrid.getRowposition()).getValue());
			}).bounds(this.inputComponentsList.nextStart(4), 0, 15, 15).build());
			
			final ResizableEditBox colliderCount = new ResizableEditBox(font, 0, 40, 0, 15, Component.translatable("datapack_edit.collider.count"), HorizontalSizing.LEFT_WIDTH, null);
			final ResizableEditBox colliderCenterX = new ResizableEditBox(font, 0, 35, 0, 15, Component.translatable("datapack_edit.collider.center.x"), HorizontalSizing.LEFT_WIDTH, null);
			final ResizableEditBox colliderCenterY = new ResizableEditBox(font, 0, 35, 0, 15, Component.translatable("datapack_edit.collider.center.y"), HorizontalSizing.LEFT_WIDTH, null);
			final ResizableEditBox colliderCenterZ = new ResizableEditBox(font, 0, 35, 0, 15, Component.translatable("datapack_edit.collider.center.z"), HorizontalSizing.LEFT_WIDTH, null);
			final ResizableEditBox colliderSizeX = new ResizableEditBox(font, 0, 35, 0, 15, Component.translatable("datapack_edit.collider.size.x"), HorizontalSizing.LEFT_WIDTH, null);
			final ResizableEditBox colliderSizeY = new ResizableEditBox(font, 0, 35, 0, 15, Component.translatable("datapack_edit.collider.size.y"), HorizontalSizing.LEFT_WIDTH, null);
			final ResizableEditBox colliderSizeZ = new ResizableEditBox(font, 0, 35, 0, 15, Component.translatable("datapack_edit.collider.size.z"), HorizontalSizing.LEFT_WIDTH, null);
			
			final Runnable setColliderToPreviewer = () -> {
				CompoundTag comp = this.packList.get(this.packListGrid.getRowposition()).getValue();
				
				Collider collider = null;
				
				if (comp.contains("collider")) {
					try {
						collider = ColliderPreset.deserializeSimpleCollider(comp.getCompound("collider"));
					} catch (IllegalArgumentException e) {
					}
				}
				
				this.modelPreviewer.setCollider(collider);
			};
			
			colliderCount.setResponder((input) -> {
				CompoundTag collider = ParseUtil.getOrDefaultTag(this.packList.get(this.packListGrid.getRowposition()).getValue(), "collider", new CompoundTag());
				
				if (StringUtil.isNullOrEmpty(input)) {
					collider.remove("number");
				} else {
					collider.put("number", IntTag.valueOf(Integer.valueOf(input)));
				}
				
				setColliderToPreviewer.run();
			});
			
			colliderCenterX.setResponder((input) -> {
				CompoundTag collider = ParseUtil.getOrDefaultTag(this.packList.get(this.packListGrid.getRowposition()).getValue(), "collider", new CompoundTag());
				ListTag centerVec = collider.getList("center", Tag.TAG_DOUBLE);
				
				double i = StringUtil.isNullOrEmpty(input) ? 0 : ParseUtil.parseOrGet(input, Double::valueOf, 0.0D);
				centerVec.remove(0);
				centerVec.add(0, DoubleTag.valueOf(i));
				
				setColliderToPreviewer.run();
			});
			
			colliderCenterY.setResponder((input) -> {
				CompoundTag collider = ParseUtil.getOrDefaultTag(this.packList.get(this.packListGrid.getRowposition()).getValue(), "collider", new CompoundTag());
				ListTag centerVec = collider.getList("center", Tag.TAG_DOUBLE);
				
				double i = StringUtil.isNullOrEmpty(input) ? 0 : ParseUtil.parseOrGet(input, Double::valueOf, 0.0D);
				centerVec.remove(1);
				centerVec.add(1, DoubleTag.valueOf(i));
				
				setColliderToPreviewer.run();
			});
			
			colliderCenterZ.setResponder((input) -> {
				CompoundTag collider = ParseUtil.getOrDefaultTag(this.packList.get(this.packListGrid.getRowposition()).getValue(), "collider", new CompoundTag());
				ListTag centerVec = collider.getList("center", Tag.TAG_DOUBLE);
				
				double i = StringUtil.isNullOrEmpty(input) ? 0 : ParseUtil.parseOrGet(input, Double::valueOf, 0.0D);
				centerVec.remove(2);
				centerVec.add(2, DoubleTag.valueOf(i));
				
				setColliderToPreviewer.run();
			});
			
			colliderSizeX.setResponder((input) -> {
				CompoundTag collider = ParseUtil.getOrDefaultTag(this.packList.get(this.packListGrid.getRowposition()).getValue(), "collider", new CompoundTag());
				ListTag centerVec = collider.getList("size", Tag.TAG_DOUBLE);
				
				double i = StringUtil.isNullOrEmpty(input) ? 0 : Double.valueOf(input);
				centerVec.remove(0);
				centerVec.add(0, DoubleTag.valueOf(i));
				
				setColliderToPreviewer.run();
			});
			
			colliderSizeY.setResponder((input) -> {
				CompoundTag collider = ParseUtil.getOrDefaultTag(this.packList.get(this.packListGrid.getRowposition()).getValue(), "collider", new CompoundTag());
				ListTag centerVec = collider.getList("size", Tag.TAG_DOUBLE);
				
				double i = StringUtil.isNullOrEmpty(input) ? 0 : Double.valueOf(input);
				centerVec.remove(1);
				centerVec.add(1, DoubleTag.valueOf(i));
				
				setColliderToPreviewer.run();
			});
			
			colliderSizeZ.setResponder((input) -> {
				CompoundTag collider = ParseUtil.getOrDefaultTag(this.packList.get(this.packListGrid.getRowposition()).getValue(), "collider", new CompoundTag());
				ListTag centerVec = collider.getList("size", Tag.TAG_DOUBLE);
				
				double i = StringUtil.isNullOrEmpty(input) ? 0 : Double.valueOf(input);
				centerVec.remove(2);
				centerVec.add(2, DoubleTag.valueOf(i));
				
				setColliderToPreviewer.run();
			});
			
			colliderCount.setFilter((context) -> StringUtil.isNullOrEmpty(context) || ParseUtil.isParsable(context, Integer::parseInt));
			colliderCenterX.setFilter((context) -> StringUtil.isNullOrEmpty(context) || ParseUtil.isParsableAllowingMinus(context, Double::parseDouble));
			colliderCenterY.setFilter((context) -> StringUtil.isNullOrEmpty(context) || ParseUtil.isParsableAllowingMinus(context, Double::parseDouble));
			colliderCenterZ.setFilter((context) -> StringUtil.isNullOrEmpty(context) || ParseUtil.isParsableAllowingMinus(context, Double::parseDouble));
			colliderSizeX.setFilter((context) -> StringUtil.isNullOrEmpty(context) || ParseUtil.isParsable(context, Double::parseDouble));
			colliderSizeY.setFilter((context) -> StringUtil.isNullOrEmpty(context) || ParseUtil.isParsable(context, Double::parseDouble));
			colliderSizeZ.setFilter((context) -> StringUtil.isNullOrEmpty(context) || ParseUtil.isParsable(context, Double::parseDouble));
			
			this.inputComponentsList.newRow();
			this.inputComponentsList.addComponentCurrentRow(new Static(parentScreen, this.inputComponentsList.nextStart(4), 100, 60, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.collider"));
			this.inputComponentsList.addComponentCurrentRow(new PopupBox.ColliderPopupBox(parentScreen, font, this.inputComponentsList.nextStart(5), 15, 130, 15, HorizontalSizing.LEFT_RIGHT, null, Component.translatable("datapack_edit.collider"),
																							(pair) -> {
																								if (pair.getSecond() != null) {
																									CompoundTag colliderTag = ParseUtil.getOrDefaultTag(this.packList.get(this.packListGrid.getRowposition()).getValue(), "collider", new CompoundTag());
																									pair.getSecond().serialize(colliderTag);
																									
																									colliderCount.setValue(String.valueOf(colliderTag.getInt("number")));
																									
																									ListTag centerVec = colliderTag.getList("center", Tag.TAG_DOUBLE);
																									colliderCenterX.setValue(String.valueOf(centerVec.getDouble(0)));
																									colliderCenterY.setValue(String.valueOf(centerVec.getDouble(1)));
																									colliderCenterZ.setValue(String.valueOf(centerVec.getDouble(2)));
																									
																									ListTag sizeVec = colliderTag.getList("size", Tag.TAG_DOUBLE);
																									colliderSizeX.setValue(String.valueOf(sizeVec.getDouble(0)));
																									colliderSizeY.setValue(String.valueOf(sizeVec.getDouble(1)));
																									colliderSizeZ.setValue(String.valueOf(sizeVec.getDouble(2)));
																								} else {
																									if (this.packList.get(this.packListGrid.getRowposition()).getValue().contains("collider")) {
																										colliderCount.setValue("");
																										colliderCenterX.setValue("");
																										colliderCenterY.setValue("");
																										colliderCenterZ.setValue("");
																										colliderSizeX.setValue("");
																										colliderSizeY.setValue("");
																										colliderSizeZ.setValue("");
																									}
																								}
																								
																								this.modelPreviewer.setCollider(pair.getSecond());
																							}).applyFilter((collider) -> collider instanceof OBBCollider || collider instanceof MultiOBBCollider));
			
			this.inputComponentsList.newRow();
			this.inputComponentsList.addComponentCurrentRow(new Static(parentScreen, this.inputComponentsList.nextStart(20), 40, 60, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.collider.count"));
			this.inputComponentsList.addComponentCurrentRow(colliderCount.relocateX(rect, this.inputComponentsList.nextStart(5)));
			
			this.inputComponentsList.newRow();
			this.inputComponentsList.addComponentCurrentRow(new Static(parentScreen, this.inputComponentsList.nextStart(20), 40, 60, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.collider.center"));
			this.inputComponentsList.addComponentCurrentRow(new Static(parentScreen, this.inputComponentsList.nextStart(5), 8, 60, 15, HorizontalSizing.LEFT_WIDTH, null, Component.literal("X: ")));
			this.inputComponentsList.addComponentCurrentRow(colliderCenterX.relocateX(rect, this.inputComponentsList.nextStart(5)));
			this.inputComponentsList.addComponentCurrentRow(new Static(parentScreen, this.inputComponentsList.nextStart(8), 8, 60, 15, HorizontalSizing.LEFT_WIDTH, null, Component.literal("Y: ")));
			this.inputComponentsList.addComponentCurrentRow(colliderCenterY.relocateX(rect, this.inputComponentsList.nextStart(5)));
			this.inputComponentsList.addComponentCurrentRow(new Static(parentScreen, this.inputComponentsList.nextStart(8), 8, 60, 15, HorizontalSizing.LEFT_WIDTH, null, Component.literal("Z: ")));
			this.inputComponentsList.addComponentCurrentRow(colliderCenterZ.relocateX(rect, this.inputComponentsList.nextStart(5)));
			
			this.inputComponentsList.newRow();
			this.inputComponentsList.addComponentCurrentRow(new Static(parentScreen, this.inputComponentsList.nextStart(20), 40, 60, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.collider.size"));
			this.inputComponentsList.addComponentCurrentRow(new Static(parentScreen, this.inputComponentsList.nextStart(5), 8, 60, 15, HorizontalSizing.LEFT_WIDTH, null, Component.literal("X: ")));
			this.inputComponentsList.addComponentCurrentRow(colliderSizeX.relocateX(rect, this.inputComponentsList.nextStart(5)));
			this.inputComponentsList.addComponentCurrentRow(new Static(parentScreen, this.inputComponentsList.nextStart(8), 8, 60, 15, HorizontalSizing.LEFT_WIDTH, null, Component.literal("Y: ")));
			this.inputComponentsList.addComponentCurrentRow(colliderSizeY.relocateX(rect, this.inputComponentsList.nextStart(5)));
			this.inputComponentsList.addComponentCurrentRow(new Static(parentScreen, this.inputComponentsList.nextStart(8), 8, 60, 15, HorizontalSizing.LEFT_WIDTH, null, Component.literal("Z: ")));
			this.inputComponentsList.addComponentCurrentRow(colliderSizeZ.relocateX(rect, this.inputComponentsList.nextStart(5)));
			
			this.inputComponentsList.newRow();
			this.inputComponentsList.newRow();
			this.inputComponentsList.newRow();
			this.inputComponentsList.addComponentCurrentRow(this.modelPreviewer);
			this.inputComponentsList.newRow();
			this.inputComponentsList.newRow();
			
			this.inputComponentsList.newRow();
			this.inputComponentsList.addComponentCurrentRow(new Static(parentScreen, this.inputComponentsList.nextStart(4), 100, 60, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.weapon_type.combos"));
			this.inputComponentsList.addComponentCurrentRow(SubScreenOpenButton.builder().subScreen(() -> {
				return new WeaponComboScreen(DatapackEditScreen.this, this.packList.get(this.packListGrid.getRowposition()).getValue());
			}).bounds(this.inputComponentsList.nextStart(4), 0, 15, 15).build());
			
			this.inputComponentsList.newRow();
			this.inputComponentsList.addComponentCurrentRow(new Static(parentScreen, this.inputComponentsList.nextStart(4), 100, 60, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.weapon_type.innate_skill"));
			
			this.inputComponentsList.newRow();
			this.inputComponentsList.newRow();
			this.inputComponentsList.addComponentCurrentRow(Grid.builder(DatapackEditScreen.this)
																.xy1(this.inputComponentsList.nextStart(5), 0)
																.xy2(15, 90)
																.horizontalSizing(HorizontalSizing.LEFT_RIGHT)
																.rowHeight(26)
																.rowEditable(RowEditButton.ADD_REMOVE)
																.addColumn(Grid.combo("style", Style.ENUM_MANAGER.universalValues()).valueChanged((event) -> {
																				CompoundTag innateSkillsTag = ParseUtil.getOrDefaultTag(this.packList.get(this.packListGrid.getRowposition()).getValue(), "innate_skills", new CompoundTag());
																				innateSkillsTag.remove(ParseUtil.nullParam(event.prevValue).toLowerCase(Locale.ROOT));
																				innateSkillsTag.putString(ParseUtil.nullParam(event.postValue).toLowerCase(Locale.ROOT), ParseUtil.nullParam(event.grid.getValue(event.rowposition, "skill")));
																			}).editable(true).width(100))
																.addColumn(Grid.registryPopup("skill", EpicFightRegistries.SKILL).filter((skill) -> skill.getCategory() == SkillCategories.WEAPON_INNATE).valueChanged((event) -> {
																				CompoundTag innateSkillsTag = ParseUtil.getOrDefaultTag(this.packList.get(this.packListGrid.getRowposition()).getValue(), "innate_skills", new CompoundTag());
																				innateSkillsTag.putString(ParseUtil.nullParam(event.grid.getValue(event.rowposition, "style")).toLowerCase(Locale.ROOT), ParseUtil.nullParam(event.postValue));
																			}).toDisplayText((item) -> item == null ? "" : item.getRegistryName().toString()).width(150))
																.pressAdd((grid, button) -> {
																	grid.setValueChangeEnabled(false);
																	int rowposition = grid.addRow();
																	grid.setGridFocus(rowposition, "style");
																	grid.setValueChangeEnabled(true);
																})
																.pressRemove((grid, button) -> {
																	int rowposition = grid.getRowposition();
																	
																	if (rowposition > -1) {
																		CompoundTag innateSkillsTag = ParseUtil.getOrDefaultTag(this.packList.get(this.packListGrid.getRowposition()).getValue(), "innate_skills", new CompoundTag());
																		innateSkillsTag.remove(ParseUtil.nullParam(grid.getValue(rowposition, "style")).toLowerCase(Locale.ROOT));
																		grid.removeRow(rowposition);
																	}
																})
																.build());
			
			this.inputComponentsList.newRow();
			this.inputComponentsList.newRow();
			this.inputComponentsList.addComponentCurrentRow(new Static(parentScreen, this.inputComponentsList.nextStart(4), 100, 80, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.weapon_type.living_animations"));
			
			this.inputComponentsList.addComponentCurrentRow(SubScreenOpenButton.builder().subScreen(() -> {
				return new LivingAnimationsScreen(DatapackEditScreen.this, ParseUtil.getOrDefaultTag(this.packList.get(this.packListGrid.getRowposition()).getValue(), "livingmotion_modifier", new CompoundTag()));
			}).bounds(this.inputComponentsList.nextStart(4), 0, 15, 15).build());
			
			this.inputComponentsList.setComponentsActive(false);
		}
		
		@Override
		public void packGridRowpositionChanged(int rowposition, Map<String, Object> values) {
			super.packGridRowpositionChanged(rowposition, values);
			
			try {
				CompoundTag tag = this.packList.get(rowposition).getValue();
				Collider collider = ColliderPreset.deserializeSimpleCollider(tag.getCompound("collider"));
				this.modelPreviewer.setCollider(collider);
			} catch (IllegalArgumentException e) {
				this.modelPreviewer.setCollider(null);
			}
		}
		
		@Override
		public void validateBeforeExport() {
			for (PackEntry<ResourceLocation, CompoundTag> packEntry : this.packList) {
				try {
					WeaponTypeReloadListener.deserializeWeaponCapabilityBuilder(packEntry.getKey(), packEntry.getValue(), DatapackEditScreen.this);
				} catch (Exception e) {
					e.printStackTrace();
					throw new IllegalStateException("Failed to export weapon type " + packEntry.getKey() + " :\n" + e.getMessage());
				}
			}
		}
		
		@Override
		public void importEntries(PackResources packResources) {
			packResources.getNamespaces(PackType.SERVER_DATA).stream().distinct().forEach((namespace) -> {
				packResources.listResources(PackType.SERVER_DATA, namespace, this.directory, (resourceLocation, streamSupplier) -> {
					try {
						ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(resourceLocation.getNamespace(), resourceLocation.getPath().replaceAll(this.directory + "/", "").replaceAll(".json", ""));
						this.importJson(rl, streamSupplier.get());
					} catch (Exception e) {
						EpicFight.LOGGER.info("Failed to import " + resourceLocation + ": " + e.getMessage());
						e.printStackTrace();
					}
				});
			});
		}
		
		public void importJson(ResourceLocation registryName, InputStream inputStream) throws Exception {
			JsonReader jsonReader = new JsonReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
			
			try {
				jsonReader.setLenient(true);
				JsonObject jsonObject = Streams.parse(jsonReader).getAsJsonObject();
				CompoundTag compTag = TagParser.parseTag(jsonObject.toString());
				
				try {
					WeaponCapability.Builder builder = WeaponTypeReloadListener.deserializeWeaponCapabilityBuilder(registryName, compTag, DatapackEditScreen.this);
					DatapackEditScreen.this.userWeaponTypes.put(registryName, (item) -> builder);
				} catch (Exception e) {
					EpicFight.LOGGER.warn("Failed to deserialize weapon type from datapack." + registryName + ": " + e.getMessage());
					e.printStackTrace();
				}
				
				this.packList.add(PackEntry.ofValue(registryName, compTag));
				this.packListGrid.addRowWithDefaultValues("pack_item", registryName.toString());
			} catch (Exception e) {
				EpicFight.LOGGER.info("Failed to import " + registryName + ": " + e.getMessage());
				throw e;
			} finally {
				try {
					jsonReader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		@Override
		public void exportEntries(ZipOutputStream out) throws Exception {
			for (PackEntry<ResourceLocation, CompoundTag> packEntry : this.packList) {
				try {
					ZipEntry zipEntry = new ZipEntry(String.format("data/%s/" + this.directory + "/%s.json", packEntry.getKey().getNamespace(), packEntry.getKey().getPath()));
					Gson gson = new GsonBuilder().setPrettyPrinting().create();
					out.putNextEntry(zipEntry);
					out.write(gson.toJson(ParseUtil.convertToJsonObject(packEntry.getValue())).getBytes());
					out.closeEntry();
				} catch (Exception e) {
					e.printStackTrace();
					throw new IllegalStateException("Failded to export " + packEntry.getKey() +". "+ e.getMessage());
				}
			}
		}
	}
	
	class ItemCapabilityTab extends DatapackTab<Item> {
		private ModelPreviewer modelPreviewer;
		private ComboBox<ItemType> itemTypeCombo;
		private Consumer<ItemType> responder;
		
		enum ItemType {
			ARMOR("armors"), WEAPON("weapons");
			
			String directoryName;
			
			ItemType(String directoryName) {
				this.directoryName = directoryName;
			}
		}
		
		public ItemCapabilityTab() {
			super(Component.translatable(LangKeys.GUI_TAB_DATAPACK_ITEM_CAPABILITY), ItemCapabilityReloadListener.DIRECTORY, BuiltInRegistries.ITEM);

			Screen parentScreen = DatapackEditScreen.this;
			
			this.modelPreviewer = new ModelPreviewer(20, 15, 0, 150, HorizontalSizing.LEFT_RIGHT, null, Armatures.BIPED, Meshes.BIPED);
			this.modelPreviewer.setColliderJoint(Armatures.BIPED.get().searchJointByName("Tool_R"));
			
			this.responder = (itemType) -> {
				CompoundTag tag = this.packList.get(this.packListGrid.getRowposition()).getValue();
				tag.tags.clear();
				tag.putString("item_type", ParseUtil.nullParam(itemType));
				this.rearrangeElements(itemType, tag);
				
				if (itemType == ItemType.WEAPON) {
					CompoundTag trailTag = ParseUtil.getOrSupply(tag, "trail", CompoundTag::new);
					CompoundTag colliderTag = ParseUtil.getOrSupply(tag, "collider", CompoundTag::new);
					
					boolean centerInit = colliderTag.contains("center");
					boolean sizeInit = colliderTag.contains("size");
					boolean colorInit = trailTag.contains("color");
					boolean beginInit = trailTag.contains("begin_pos");
					boolean endInit = trailTag.contains("end_pos");
					
					this.itemTypeCombo._setResponder(null);
					
					this.inputComponentsList.setDataBindingComponenets(new Object[] {
						itemType,
						DatapackEditScreen.getWeaponType(tag.getString("type")),
						null,
						ParseUtil.nullParam(colliderTag.get("number")),
						centerInit ? ParseUtil.valueOfOmittingType(ParseUtil.nullOrToString(colliderTag.getList("center", Tag.TAG_DOUBLE).get(0), Tag::getAsString)) : "",
						centerInit ? ParseUtil.valueOfOmittingType(ParseUtil.nullOrToString(colliderTag.getList("center", Tag.TAG_DOUBLE).get(1), Tag::getAsString)) : "",
						centerInit ? ParseUtil.valueOfOmittingType(ParseUtil.nullOrToString(colliderTag.getList("center", Tag.TAG_DOUBLE).get(2), Tag::getAsString)) : "",
						sizeInit ? ParseUtil.valueOfOmittingType(ParseUtil.nullOrToString(colliderTag.getList("size", Tag.TAG_DOUBLE).get(0), Tag::getAsString)) : "",
						sizeInit ? ParseUtil.valueOfOmittingType(ParseUtil.nullOrToString(colliderTag.getList("size", Tag.TAG_DOUBLE).get(1), Tag::getAsString)) : "",
						sizeInit ? ParseUtil.valueOfOmittingType(ParseUtil.nullOrToString(colliderTag.getList("size", Tag.TAG_DOUBLE).get(2), Tag::getAsString)) : "",
						colorInit ? ParseUtil.nullParam(ParseUtil.nullOrToString(trailTag.getList("color", Tag.TAG_INT).get(0), Tag::getAsString)) : "",
						colorInit ? ParseUtil.nullParam(ParseUtil.nullOrToString(trailTag.getList("color", Tag.TAG_INT).get(1), Tag::getAsString)) : "",
						colorInit ? ParseUtil.nullParam(ParseUtil.nullOrToString(trailTag.getList("color", Tag.TAG_INT).get(2), Tag::getAsString)) : "",
						beginInit ? ParseUtil.valueOfOmittingType(ParseUtil.nullOrToString(trailTag.getList("begin_pos", Tag.TAG_DOUBLE).get(0), Tag::getAsString)) : "",
						beginInit ? ParseUtil.valueOfOmittingType(ParseUtil.nullOrToString(trailTag.getList("begin_pos", Tag.TAG_DOUBLE).get(1), Tag::getAsString)) : "",
						beginInit ? ParseUtil.valueOfOmittingType(ParseUtil.nullOrToString(trailTag.getList("begin_pos", Tag.TAG_DOUBLE).get(2), Tag::getAsString)) : "",
						endInit ? ParseUtil.valueOfOmittingType(ParseUtil.nullOrToString(trailTag.getList("end_pos", Tag.TAG_DOUBLE).get(0), Tag::getAsString)) : "",
						endInit ? ParseUtil.valueOfOmittingType(ParseUtil.nullOrToString(trailTag.getList("end_pos", Tag.TAG_DOUBLE).get(1), Tag::getAsString)) : "",
						endInit ? ParseUtil.valueOfOmittingType(ParseUtil.nullOrToString(trailTag.getList("end_pos", Tag.TAG_DOUBLE).get(2), Tag::getAsString)) : "",
						ParseUtil.nullParam(trailTag.get("lifetime")),
						ParseUtil.nullParam(trailTag.get("interpolations")),
						ParseUtil.nullParam(trailTag.getString("texture_path")),
						BuiltInRegistries.PARTICLE_TYPE.get(ResourceLocation.parse(trailTag.getString("particle_type")))
					});
					this.itemTypeCombo._setResponder(this.responder);
				} else {
					this.itemTypeCombo._setResponder(null);
					this.inputComponentsList.setDataBindingComponenets(new Object[] {itemType});
					this.itemTypeCombo._setResponder(this.responder);
				}
			};
			
			this.itemTypeCombo = new ComboBox<> (parentScreen, Minecraft.getInstance().font, 0, 124, 100, 15, HorizontalSizing.LEFT_WIDTH, null, 8, Component.translatable("datapack_edit.item_capability.item_type"),
													List.of(ItemType.values()), ParseUtil::snakeToSpacedCamel, this.responder);
			
			this.inputComponentsList = new InputComponentList<>(DatapackEditScreen.this, 0, 0, 0, 30) {
				@Override
				public void importTag(CompoundTag tag) {
					ItemType itemType = null;
					
					try {
						itemType = ItemType.valueOf(tag.getString("item_type"));
					} catch (IllegalArgumentException e) {
					}
					
					ItemCapabilityTab.this.rearrangeElements(itemType, tag);
					this.setComponentsActive(true);
					
					if (itemType == ItemType.WEAPON) {
						CompoundTag trailTag = ParseUtil.getOrSupply(tag, "trail", CompoundTag::new);
						CompoundTag colliderTag = ParseUtil.getOrSupply(tag, "collider", CompoundTag::new);
						
						boolean centerInit = colliderTag.contains("center");
						boolean sizeInit = colliderTag.contains("size");
						boolean colorInit = trailTag.contains("color");
						boolean beginInit = trailTag.contains("begin_pos");
						boolean endInit = trailTag.contains("end_pos");
						
						ItemCapabilityTab.this.itemTypeCombo._setResponder(null);
						
						this.setDataBindingComponenets(new Object[] {
							itemType,
							DatapackEditScreen.getWeaponType(tag.getString("type")),
							null,
							ParseUtil.nullParam(colliderTag.get("number")),
							centerInit ? ParseUtil.valueOfOmittingType(ParseUtil.nullOrToString(colliderTag.getList("center", Tag.TAG_DOUBLE).get(0), Tag::getAsString)) : "",
							centerInit ? ParseUtil.valueOfOmittingType(ParseUtil.nullOrToString(colliderTag.getList("center", Tag.TAG_DOUBLE).get(1), Tag::getAsString)) : "",
							centerInit ? ParseUtil.valueOfOmittingType(ParseUtil.nullOrToString(colliderTag.getList("center", Tag.TAG_DOUBLE).get(2), Tag::getAsString)) : "",
							sizeInit ? ParseUtil.valueOfOmittingType(ParseUtil.nullOrToString(colliderTag.getList("size", Tag.TAG_DOUBLE).get(0), Tag::getAsString)) : "",
							sizeInit ? ParseUtil.valueOfOmittingType(ParseUtil.nullOrToString(colliderTag.getList("size", Tag.TAG_DOUBLE).get(1), Tag::getAsString)) : "",
							sizeInit ? ParseUtil.valueOfOmittingType(ParseUtil.nullOrToString(colliderTag.getList("size", Tag.TAG_DOUBLE).get(2), Tag::getAsString)) : "",
							colorInit ? ParseUtil.nullParam(ParseUtil.nullOrToString(trailTag.getList("color", Tag.TAG_INT).get(0), Tag::getAsString)) : "",
							colorInit ? ParseUtil.nullParam(ParseUtil.nullOrToString(trailTag.getList("color", Tag.TAG_INT).get(1), Tag::getAsString)) : "",
							colorInit ? ParseUtil.nullParam(ParseUtil.nullOrToString(trailTag.getList("color", Tag.TAG_INT).get(2), Tag::getAsString)) : "",
							beginInit ? ParseUtil.valueOfOmittingType(ParseUtil.nullOrToString(trailTag.getList("begin_pos", Tag.TAG_DOUBLE).get(0), Tag::getAsString)) : "",
							beginInit ? ParseUtil.valueOfOmittingType(ParseUtil.nullOrToString(trailTag.getList("begin_pos", Tag.TAG_DOUBLE).get(1), Tag::getAsString)) : "",
							beginInit ? ParseUtil.valueOfOmittingType(ParseUtil.nullOrToString(trailTag.getList("begin_pos", Tag.TAG_DOUBLE).get(2), Tag::getAsString)) : "",
							endInit ? ParseUtil.valueOfOmittingType(ParseUtil.nullOrToString(trailTag.getList("end_pos", Tag.TAG_DOUBLE).get(0), Tag::getAsString)) : "",
							endInit ? ParseUtil.valueOfOmittingType(ParseUtil.nullOrToString(trailTag.getList("end_pos", Tag.TAG_DOUBLE).get(1), Tag::getAsString)) : "",
							endInit ? ParseUtil.valueOfOmittingType(ParseUtil.nullOrToString(trailTag.getList("end_pos", Tag.TAG_DOUBLE).get(2), Tag::getAsString)) : "",
							ParseUtil.nullParam(trailTag.get("lifetime")),
							ParseUtil.nullParam(trailTag.get("interpolations")),
							ParseUtil.nullParam(trailTag.getString("texture_path")),
							BuiltInRegistries.PARTICLE_TYPE.get(ResourceLocation.parse(trailTag.getString("particle_type")))
						});
						ItemCapabilityTab.this.itemTypeCombo._setResponder(ItemCapabilityTab.this.responder);
					} else {
						ItemCapabilityTab.this.itemTypeCombo._setResponder(null);
						this.setDataBindingComponenets(new Object[] {itemType});
						ItemCapabilityTab.this.itemTypeCombo._setResponder(ItemCapabilityTab.this.responder);
					}
				}
			};
			
			this.inputComponentsList.newRow();
			this.inputComponentsList.addComponentCurrentRow(new Static(parentScreen, this.inputComponentsList.nextStart(4), 100, 60, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.item_capability.item_type"));
			this.inputComponentsList.addComponentCurrentRow(this.itemTypeCombo.relocateX(DatapackEditScreen.this.getRectangle(), this.inputComponentsList.nextStart(5)));
			this.inputComponentsList.setComponentsActive(false);
		}
		
		private void rearrangeElements(ItemType itemType, CompoundTag tag) {
			this.inputComponentsList.clearComponents();
			
			Font font = DatapackEditScreen.this.font;
			ScreenRectangle rect = DatapackEditScreen.this.getRectangle();
			
			this.inputComponentsList.newRow();
			this.inputComponentsList.addComponentCurrentRow(new Static(DatapackEditScreen.this, this.inputComponentsList.nextStart(4), 100, 60, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.item_capability.item_type"));
			this.inputComponentsList.addComponentCurrentRow(this.itemTypeCombo.relocateX(rect, this.inputComponentsList.nextStart(5)));
			
			if (itemType == ItemType.WEAPON) {
				this.inputComponentsList.newRow();
				this.inputComponentsList.addComponentCurrentRow(new Static(DatapackEditScreen.this, this.inputComponentsList.nextStart(4), 100, 60, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.item_capability.attributes"));
				this.inputComponentsList.addComponentCurrentRow(SubScreenOpenButton.builder().subScreen(() -> {
					return new WeaponAttributeScreen(DatapackEditScreen.this, ParseUtil.getOrDefaultTag(this.packList.get(this.packListGrid.getRowposition()).getValue(), "attributes", new CompoundTag()), itemType);
				}).bounds(this.inputComponentsList.nextStart(4), 0, 15, 15).build());
				
				this.inputComponentsList.newRow();
				this.inputComponentsList.addComponentCurrentRow(new Static(DatapackEditScreen.this, this.inputComponentsList.nextStart(4), 100, 60, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.item_capability.type"));
				this.inputComponentsList.addComponentCurrentRow(new PopupBox.WeaponTypePopupBox(DatapackEditScreen.this, font, this.inputComponentsList.nextStart(5), 15, 15, 15, HorizontalSizing.LEFT_RIGHT, null, Component.translatable("datapack_edit.item_capability.type"),
																				(pair) -> {
																					CompoundTag currentTag = this.packList.get(this.packListGrid.getRowposition()).getValue();
																					currentTag.putString("type", pair.getFirst());
																					
																					if (pair.getSecond() != null) {
																						CapabilityItem.Builder<?> builder = pair.getSecond().apply(this.registry.get(this.packList.get(this.packListGrid.getRowposition()).getKey()));
																						
																						if (builder instanceof WeaponCapability.Builder weaponBuilder) {
																							this.modelPreviewer.clearAnimations();
																							
																							List<AssetAccessor<? extends StaticAnimation>> allAnimations = weaponBuilder.getComboAnimations().entrySet().stream().reduce(Lists.newArrayList(), (list, entry) -> {
																								list.addAll(entry.getValue());
																								return list;
																							}, (list1, list2) -> {
																								list1.addAll(list2);
																								return list1;
																							});
																							
																							allAnimations.stream().forEach(this.modelPreviewer::addAnimationToPlay);
																							boolean hasCustomCollider = false;
																							
																							if (currentTag.contains("collider")) {
																								try {
																									ColliderPreset.deserializeSimpleCollider(currentTag.getCompound("collider"));
																									hasCustomCollider = true;
																								} catch (IllegalArgumentException e) {
																								}
																							}
																							
																							if (!hasCustomCollider) {
																								this.modelPreviewer.setCollider(weaponBuilder.getCollider());
																							}
																						}
																					}
																				}));
				
				final ResizableEditBox colliderCount = new ResizableEditBox(font, 0, 40, 0, 15, Component.translatable("datapack_edit.collider.count"), HorizontalSizing.LEFT_WIDTH, null);
				final ResizableEditBox colliderCenterX = new ResizableEditBox(font, 0, 35, 0, 15, Component.translatable("datapack_edit.collider.center.x"), HorizontalSizing.LEFT_WIDTH, null);
				final ResizableEditBox colliderCenterY = new ResizableEditBox(font, 0, 35, 0, 15, Component.translatable("datapack_edit.collider.center.y"), HorizontalSizing.LEFT_WIDTH, null);
				final ResizableEditBox colliderCenterZ = new ResizableEditBox(font, 0, 35, 0, 15, Component.translatable("datapack_edit.collider.center.z"), HorizontalSizing.LEFT_WIDTH, null);
				final ResizableEditBox colliderSizeX = new ResizableEditBox(font, 0, 35, 0, 15, Component.translatable("datapack_edit.collider.size.x"), HorizontalSizing.LEFT_WIDTH, null);
				final ResizableEditBox colliderSizeY = new ResizableEditBox(font, 0, 35, 0, 15, Component.translatable("datapack_edit.collider.size.y"), HorizontalSizing.LEFT_WIDTH, null);
				final ResizableEditBox colliderSizeZ = new ResizableEditBox(font, 0, 35, 0, 15, Component.translatable("datapack_edit.collider.size.z"), HorizontalSizing.LEFT_WIDTH, null);
				
				colliderCount.setResponder((input) -> {
					CompoundTag collider = ParseUtil.getOrDefaultTag(this.packList.get(this.packListGrid.getRowposition()).getValue(), "collider", new CompoundTag());
					
					if (StringUtil.isNullOrEmpty(input)) {
						collider.remove("number");
					} else {
						collider.put("number", IntTag.valueOf(Integer.valueOf(input)));
					}
					
					try {
						this.modelPreviewer.setCollider(ColliderPreset.deserializeSimpleCollider(collider));
					} catch (IllegalArgumentException e) {
						this.modelPreviewer.setCollider(null);
					}
				});
				
				colliderCenterX.setResponder((input) -> {
					CompoundTag collider = ParseUtil.getOrDefaultTag(this.packList.get(this.packListGrid.getRowposition()).getValue(), "collider", new CompoundTag());
					ListTag centerVec = ParseUtil.getOrDefaultTag(collider, "center", new ListTag());
					
					if (centerVec.isEmpty()) {
						centerVec.add(DoubleTag.valueOf(0.0D));
						centerVec.add(DoubleTag.valueOf(0.0D));
						centerVec.add(DoubleTag.valueOf(0.0D));
					}
					
					double i = StringUtil.isNullOrEmpty(input) ? 0 : ParseUtil.parseOrGet(input, Double::valueOf, 0.0D);
					centerVec.set(0, DoubleTag.valueOf(i));
					
					try {
						this.modelPreviewer.setCollider(ColliderPreset.deserializeSimpleCollider(collider));
					} catch (IllegalArgumentException e) {
						this.modelPreviewer.setCollider(null);
					}
				});
				
				colliderCenterY.setResponder((input) -> {
					CompoundTag collider = ParseUtil.getOrDefaultTag(this.packList.get(this.packListGrid.getRowposition()).getValue(), "collider", new CompoundTag());
					ListTag centerVec = ParseUtil.getOrDefaultTag(collider, "center", new ListTag());
					
					if (centerVec.isEmpty()) {
						centerVec.add(DoubleTag.valueOf(0.0D));
						centerVec.add(DoubleTag.valueOf(0.0D));
						centerVec.add(DoubleTag.valueOf(0.0D));
					}
					
					double i = StringUtil.isNullOrEmpty(input) ? 0 : ParseUtil.parseOrGet(input, Double::valueOf, 0.0D);
					centerVec.set(1, DoubleTag.valueOf(i));
					
					try {
						this.modelPreviewer.setCollider(ColliderPreset.deserializeSimpleCollider(collider));
					} catch (IllegalArgumentException e) {
						this.modelPreviewer.setCollider(null);
					}
				});
				
				colliderCenterZ.setResponder((input) -> {
					CompoundTag collider = ParseUtil.getOrDefaultTag(this.packList.get(this.packListGrid.getRowposition()).getValue(), "collider", new CompoundTag());
					ListTag centerVec = ParseUtil.getOrDefaultTag(collider, "center", new ListTag());
					
					if (centerVec.isEmpty()) {
						centerVec.add(DoubleTag.valueOf(0.0D));
						centerVec.add(DoubleTag.valueOf(0.0D));
						centerVec.add(DoubleTag.valueOf(0.0D));
					}
					
					double i = StringUtil.isNullOrEmpty(input) ? 0 : ParseUtil.parseOrGet(input, Double::valueOf, 0.0D);
					centerVec.set(2, DoubleTag.valueOf(i));
					
					try {
						this.modelPreviewer.setCollider(ColliderPreset.deserializeSimpleCollider(collider));
					} catch (IllegalArgumentException e) {
					this.modelPreviewer.setCollider(null);
					}
				});
				
				colliderSizeX.setResponder((input) -> {
					CompoundTag collider = ParseUtil.getOrDefaultTag(this.packList.get(this.packListGrid.getRowposition()).getValue(), "collider", new CompoundTag());
					ListTag sizeVec = ParseUtil.getOrDefaultTag(collider, "size", new ListTag());
					
					if (sizeVec.isEmpty()) {
						sizeVec.add(DoubleTag.valueOf(0.0D));
						sizeVec.add(DoubleTag.valueOf(0.0D));
						sizeVec.add(DoubleTag.valueOf(0.0D));
					}
					
					double i = StringUtil.isNullOrEmpty(input) ? 0 : Double.valueOf(input);
					sizeVec.set(0, DoubleTag.valueOf(i));
					
					try {
						this.modelPreviewer.setCollider(ColliderPreset.deserializeSimpleCollider(collider));
					} catch (IllegalArgumentException e) {
						this.modelPreviewer.setCollider(null);
					}
				});
				
				colliderSizeY.setResponder((input) -> {
					CompoundTag collider = ParseUtil.getOrDefaultTag(this.packList.get(this.packListGrid.getRowposition()).getValue(), "collider", new CompoundTag());
					ListTag sizeVec = ParseUtil.getOrDefaultTag(collider, "size", new ListTag());
					
					if (sizeVec.isEmpty()) {
						sizeVec.add(DoubleTag.valueOf(0.0D));
						sizeVec.add(DoubleTag.valueOf(0.0D));
						sizeVec.add(DoubleTag.valueOf(0.0D));
					}
					
					double i = StringUtil.isNullOrEmpty(input) ? 0 : Double.valueOf(input);
					sizeVec.set(1, DoubleTag.valueOf(i));
					
					try {
						this.modelPreviewer.setCollider(ColliderPreset.deserializeSimpleCollider(collider));
					} catch (IllegalArgumentException e) {
						this.modelPreviewer.setCollider(null);
					}
				});
				
				colliderSizeZ.setResponder((input) -> {
					CompoundTag collider = ParseUtil.getOrDefaultTag(this.packList.get(this.packListGrid.getRowposition()).getValue(), "collider", new CompoundTag());
					ListTag sizeVec = ParseUtil.getOrDefaultTag(collider, "size", new ListTag());
					
					if (sizeVec.isEmpty()) {
						sizeVec.add(DoubleTag.valueOf(0.0D));
						sizeVec.add(DoubleTag.valueOf(0.0D));
						sizeVec.add(DoubleTag.valueOf(0.0D));
					}
					
					double i = StringUtil.isNullOrEmpty(input) ? 0 : Double.valueOf(input);
					sizeVec.set(2, DoubleTag.valueOf(i));
					
					try {
						this.modelPreviewer.setCollider(ColliderPreset.deserializeSimpleCollider(collider));
					} catch (IllegalArgumentException e) {
						this.modelPreviewer.setCollider(null);
					}
				});
				
				colliderCount.setFilter((context) -> StringUtil.isNullOrEmpty(context) || ParseUtil.isParsable(context, Integer::parseInt));
				colliderCenterX.setFilter((context) -> StringUtil.isNullOrEmpty(context) || ParseUtil.isParsableAllowingMinus(context, Double::parseDouble));
				colliderCenterY.setFilter((context) -> StringUtil.isNullOrEmpty(context) || ParseUtil.isParsableAllowingMinus(context, Double::parseDouble));
				colliderCenterZ.setFilter((context) -> StringUtil.isNullOrEmpty(context) || ParseUtil.isParsableAllowingMinus(context, Double::parseDouble));
				colliderSizeX.setFilter((context) -> StringUtil.isNullOrEmpty(context) || ParseUtil.isParsable(context, Double::parseDouble));
				colliderSizeY.setFilter((context) -> StringUtil.isNullOrEmpty(context) || ParseUtil.isParsable(context, Double::parseDouble));
				colliderSizeZ.setFilter((context) -> StringUtil.isNullOrEmpty(context) || ParseUtil.isParsable(context, Double::parseDouble));
				
				this.inputComponentsList.newRow();
				this.inputComponentsList.addComponentCurrentRow(new Static(DatapackEditScreen.this, this.inputComponentsList.nextStart(4), 100, 60, 15, HorizontalSizing.LEFT_WIDTH, null, Component.translatable("datapack_edit.collider"), Component.translatable("datapack_edit.item_capability.collider.tooltip")));
				this.inputComponentsList.addComponentCurrentRow(new PopupBox.ColliderPopupBox(DatapackEditScreen.this, font, this.inputComponentsList.nextStart(5), 15, 130, 15, HorizontalSizing.LEFT_RIGHT, null, Component.translatable("datapack_edit.collider"),
																								(pair) -> {
																									if (pair.getSecond() != null) {
																										CompoundTag colliderTag = ParseUtil.getOrDefaultTag(this.packList.get(this.packListGrid.getRowposition()).getValue(), "collider", new CompoundTag());
																										pair.getSecond().serialize(colliderTag);
																										
																										colliderCount.setValue(String.valueOf(colliderTag.getInt("number")));
																										
																										ListTag centerVec = colliderTag.getList("center", Tag.TAG_DOUBLE);
																										colliderCenterX.setValue(String.valueOf(centerVec.getDouble(0)));
																										colliderCenterY.setValue(String.valueOf(centerVec.getDouble(1)));
																										colliderCenterZ.setValue(String.valueOf(centerVec.getDouble(2)));
																										
																										ListTag sizeVec = colliderTag.getList("size", Tag.TAG_DOUBLE);
																										colliderSizeX.setValue(String.valueOf(sizeVec.getDouble(0)));
																										colliderSizeY.setValue(String.valueOf(sizeVec.getDouble(1)));
																										colliderSizeZ.setValue(String.valueOf(sizeVec.getDouble(2)));
																									} else {
																										if (this.packList.get(this.packListGrid.getRowposition()).getValue().contains("collider")) {
																											colliderCount.setValue("");
																											colliderCenterX.setValue("");
																											colliderCenterY.setValue("");
																											colliderCenterZ.setValue("");
																											colliderSizeX.setValue("");
																											colliderSizeY.setValue("");
																											colliderSizeZ.setValue("");
																										}
																									}
																									
																									this.modelPreviewer.setCollider(pair.getSecond());
																								}).applyFilter((collider) -> collider instanceof OBBCollider || collider instanceof MultiOBBCollider));
				
				this.inputComponentsList.newRow();
				this.inputComponentsList.addComponentCurrentRow(new Static(DatapackEditScreen.this, this.inputComponentsList.nextStart(20), 40, 60, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.collider.count"));
				this.inputComponentsList.addComponentCurrentRow(colliderCount.relocateX(rect, this.inputComponentsList.nextStart(5)));
				
				this.inputComponentsList.newRow();
				this.inputComponentsList.addComponentCurrentRow(new Static(DatapackEditScreen.this, this.inputComponentsList.nextStart(20), 40, 60, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.collider.center"));
				this.inputComponentsList.addComponentCurrentRow(new Static(DatapackEditScreen.this, this.inputComponentsList.nextStart(5), 8, 60, 15, HorizontalSizing.LEFT_WIDTH, null, Component.literal("X: ")));
				this.inputComponentsList.addComponentCurrentRow(colliderCenterX.relocateX(rect, this.inputComponentsList.nextStart(5)));
				this.inputComponentsList.addComponentCurrentRow(new Static(DatapackEditScreen.this, this.inputComponentsList.nextStart(8), 8, 60, 15, HorizontalSizing.LEFT_WIDTH, null, Component.literal("Y: ")));
				this.inputComponentsList.addComponentCurrentRow(colliderCenterY.relocateX(rect, this.inputComponentsList.nextStart(5)));
				this.inputComponentsList.addComponentCurrentRow(new Static(DatapackEditScreen.this, this.inputComponentsList.nextStart(8), 8, 60, 15, HorizontalSizing.LEFT_WIDTH, null, Component.literal("Z: ")));
				this.inputComponentsList.addComponentCurrentRow(colliderCenterZ.relocateX(rect, this.inputComponentsList.nextStart(5)));
				
				this.inputComponentsList.newRow();
				this.inputComponentsList.addComponentCurrentRow(new Static(DatapackEditScreen.this, this.inputComponentsList.nextStart(20), 40, 60, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.collider.size"));
				this.inputComponentsList.addComponentCurrentRow(new Static(DatapackEditScreen.this, this.inputComponentsList.nextStart(5), 8, 60, 15, HorizontalSizing.LEFT_WIDTH, null, Component.literal("X: ")));
				this.inputComponentsList.addComponentCurrentRow(colliderSizeX.relocateX(rect, this.inputComponentsList.nextStart(5)));
				this.inputComponentsList.addComponentCurrentRow(new Static(DatapackEditScreen.this, this.inputComponentsList.nextStart(8), 8, 60, 15, HorizontalSizing.LEFT_WIDTH, null, Component.literal("Y: ")));
				this.inputComponentsList.addComponentCurrentRow(colliderSizeY.relocateX(rect, this.inputComponentsList.nextStart(5)));
				this.inputComponentsList.addComponentCurrentRow(new Static(DatapackEditScreen.this, this.inputComponentsList.nextStart(8), 8, 60, 15, HorizontalSizing.LEFT_WIDTH, null, Component.literal("Z: ")));
				this.inputComponentsList.addComponentCurrentRow(colliderSizeZ.relocateX(rect, this.inputComponentsList.nextStart(5)));
				
				this.inputComponentsList.newRow();
				this.inputComponentsList.addComponentCurrentRow(new Static(DatapackEditScreen.this, this.inputComponentsList.nextStart(4), 90, 60, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.item_capability.trail"));
				
				final ResizableEditBox colorR = new ResizableEditBox(font, 0, 30, 0, 15, Component.translatable("datapack_edit.item_capability.trail.color.r"), HorizontalSizing.LEFT_WIDTH, null);
				final ResizableEditBox colorG = new ResizableEditBox(font, 0, 30, 0, 15, Component.translatable("datapack_edit.item_capability.trail.color.g"), HorizontalSizing.LEFT_WIDTH, null);
				final ResizableEditBox colorB = new ResizableEditBox(font, 0, 30, 0, 15, Component.translatable("datapack_edit.item_capability.trail.color.b"), HorizontalSizing.LEFT_WIDTH, null);
				final ColorPreviewWidget colorWidget = new ColorPreviewWidget(0, 12, 0, 12, HorizontalSizing.LEFT_WIDTH, null, Component.translatable("datapack_edit.item_capability.color"));
				
				colorR.setResponder((input) -> {
					CompoundTag trailTag = this.packList.get(this.packListGrid.getRowposition()).getValue().getCompound("trail");
					ListTag list = ParseUtil.getOrDefaultTag(trailTag, "color", new ListTag());
					
					if (list.isEmpty()) {
						list.add(IntTag.valueOf(0));
						list.add(IntTag.valueOf(0));
						list.add(IntTag.valueOf(0));
					}
					
					int i = StringUtil.isNullOrEmpty(input) ? 0 : Integer.valueOf(input);
					list.set(0, IntTag.valueOf(i));
					colorWidget.setColor(ParseUtil.parseOrGet(input, Integer::valueOf, 0), ParseUtil.parseOrGet(colorG.getValue(), Integer::valueOf, 0), ParseUtil.parseOrGet(colorB.getValue(), Integer::valueOf, 0));
					
					TrailInfo trailInfo = TrailInfo.deserialize(trailTag);
					this.modelPreviewer.setTrailInfo(trailInfo);
				});
				colorG.setResponder((input) -> {
					CompoundTag trailTag = this.packList.get(this.packListGrid.getRowposition()).getValue().getCompound("trail");
					ListTag list = ParseUtil.getOrDefaultTag(trailTag, "color", new ListTag());
					
					if (list.isEmpty()) {
						list.add(IntTag.valueOf(0));
						list.add(IntTag.valueOf(0));
						list.add(IntTag.valueOf(0));
					}
					
					int i = StringUtil.isNullOrEmpty(input) ? 0 : Integer.valueOf(input);
					list.set(1, IntTag.valueOf(i));
					colorWidget.setColor(ParseUtil.parseOrGet(colorR.getValue(), Integer::valueOf, 0), ParseUtil.parseOrGet(input, Integer::valueOf, 0), ParseUtil.parseOrGet(colorB.getValue(), Integer::valueOf, 0));
					
					TrailInfo trailInfo = TrailInfo.deserialize(trailTag);
					this.modelPreviewer.setTrailInfo(trailInfo);
				});
				colorB.setResponder((input) -> {
					CompoundTag trailTag = this.packList.get(this.packListGrid.getRowposition()).getValue().getCompound("trail");
					ListTag list = ParseUtil.getOrDefaultTag(trailTag, "color", new ListTag());
					
					if (list.isEmpty()) {
						list.add(IntTag.valueOf(0));
						list.add(IntTag.valueOf(0));
						list.add(IntTag.valueOf(0));
					}
					
					int i = StringUtil.isNullOrEmpty(input) ? 0 : Integer.valueOf(input);
					list.set(2, IntTag.valueOf(i));
					colorWidget.setColor(ParseUtil.parseOrGet(colorR.getValue(), Integer::valueOf, 0), ParseUtil.parseOrGet(colorG.getValue(), Integer::valueOf, 0), ParseUtil.parseOrGet(input, Integer::valueOf, 0));
					
					TrailInfo trailInfo = TrailInfo.deserialize(trailTag);
					this.modelPreviewer.setTrailInfo(trailInfo);
				});
				
				colorR.setFilter((context) -> StringUtil.isNullOrEmpty(context) || (ParseUtil.isParsable(context, Integer::parseInt) && ParseUtil.parseOrGet(context, Integer::parseInt, 0) < 256));
				colorG.setFilter((context) -> StringUtil.isNullOrEmpty(context) || (ParseUtil.isParsable(context, Integer::parseInt) && ParseUtil.parseOrGet(context, Integer::parseInt, 0) < 256));
				colorB.setFilter((context) -> StringUtil.isNullOrEmpty(context) || (ParseUtil.isParsable(context, Integer::parseInt) && ParseUtil.parseOrGet(context, Integer::parseInt, 0) < 256));
				
				this.inputComponentsList.newRow();
				this.inputComponentsList.addComponentCurrentRow(new Static(DatapackEditScreen.this, this.inputComponentsList.nextStart(20), 28, 0, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.item_capability.color"));
				this.inputComponentsList.addComponentCurrentRow(colorWidget.relocateX(rect, this.inputComponentsList.nextStart(4)));
				this.inputComponentsList.addComponentCurrentRow(new Static(DatapackEditScreen.this, this.inputComponentsList.nextStart(41), 8, 0, 15, HorizontalSizing.LEFT_WIDTH, null, Component.literal("R: ")));
				this.inputComponentsList.addComponentCurrentRow(colorR.relocateX(rect, this.inputComponentsList.nextStart(4)));
				this.inputComponentsList.addComponentCurrentRow(new Static(DatapackEditScreen.this, this.inputComponentsList.nextStart(8), 8, 0, 15, HorizontalSizing.LEFT_WIDTH, null, Component.literal("G: ")));
				this.inputComponentsList.addComponentCurrentRow(colorG.relocateX(rect, this.inputComponentsList.nextStart(4)));
				this.inputComponentsList.addComponentCurrentRow(new Static(DatapackEditScreen.this, this.inputComponentsList.nextStart(8), 8, 0, 15, HorizontalSizing.LEFT_WIDTH, null, Component.literal("B: ")));
				this.inputComponentsList.addComponentCurrentRow(colorB.relocateX(rect, this.inputComponentsList.nextStart(4)));
				
				final ResizableEditBox beginX = new ResizableEditBox(font, 0, 35, 0, 15, Component.translatable("datapack_edit.item_capability.trail.begin_pos.x"), HorizontalSizing.LEFT_WIDTH, null);
				final ResizableEditBox beginY = new ResizableEditBox(font, 0, 35, 0, 15, Component.translatable("datapack_edit.item_capability.trail.begin_pos.y"), HorizontalSizing.LEFT_WIDTH, null);
				final ResizableEditBox beginZ = new ResizableEditBox(font, 0, 35, 0, 15, Component.translatable("datapack_edit.item_capability.trail.begin_pos.z"), HorizontalSizing.LEFT_WIDTH, null);
				
				beginX.setResponder((input) -> {
					CompoundTag trailTag = this.packList.get(this.packListGrid.getRowposition()).getValue().getCompound("trail");
					ListTag list = ParseUtil.getOrDefaultTag(trailTag, "begin_pos", new ListTag());
					
					if (list.isEmpty()) {
						list.add(DoubleTag.valueOf(0));
						list.add(DoubleTag.valueOf(0));
						list.add(DoubleTag.valueOf(0));
					}
					
					double d = StringUtil.isNullOrEmpty(input) ? 0 : ParseUtil.parseOrGet(input, Double::valueOf, 0.0D);
					list.set(0, DoubleTag.valueOf(d));
					
					TrailInfo trailInfo = TrailInfo.deserialize(trailTag);
					this.modelPreviewer.setTrailInfo(trailInfo);
				});
				beginY.setResponder((input) -> {
					CompoundTag trailTag = this.packList.get(this.packListGrid.getRowposition()).getValue().getCompound("trail");
					ListTag list = ParseUtil.getOrDefaultTag(trailTag, "begin_pos", new ListTag());
					
					if (list.isEmpty()) {
						list.add(DoubleTag.valueOf(0));
						list.add(DoubleTag.valueOf(0));
						list.add(DoubleTag.valueOf(0));
					}
					
					double d = StringUtil.isNullOrEmpty(input) ? 0 : ParseUtil.parseOrGet(input, Double::valueOf, 0.0D);
					list.set(1, DoubleTag.valueOf(d));
					
					TrailInfo trailInfo = TrailInfo.deserialize(trailTag);
					this.modelPreviewer.setTrailInfo(trailInfo);
				});
				beginZ.setResponder((input) -> {
					CompoundTag trailTag = this.packList.get(this.packListGrid.getRowposition()).getValue().getCompound("trail");
					ListTag list = ParseUtil.getOrDefaultTag(trailTag, "begin_pos", new ListTag());
					
					if (list.isEmpty()) {
						list.add(DoubleTag.valueOf(0));
						list.add(DoubleTag.valueOf(0));
						list.add(DoubleTag.valueOf(0));
					}
					
					double d = StringUtil.isNullOrEmpty(input) ? 0 : ParseUtil.parseOrGet(input, Double::valueOf, 0.0D);
					list.set(2, DoubleTag.valueOf(d));
					
					TrailInfo trailInfo = TrailInfo.deserialize(trailTag);
					this.modelPreviewer.setTrailInfo(trailInfo);
				});
				
				beginX.setFilter((context) -> StringUtil.isNullOrEmpty(context) || ParseUtil.isParsableAllowingMinus(context, Double::parseDouble));
				beginY.setFilter((context) -> StringUtil.isNullOrEmpty(context) || ParseUtil.isParsableAllowingMinus(context, Double::parseDouble));
				beginZ.setFilter((context) -> StringUtil.isNullOrEmpty(context) || ParseUtil.isParsableAllowingMinus(context, Double::parseDouble));
				
				this.inputComponentsList.newRow();
				this.inputComponentsList.addComponentCurrentRow(new Static(DatapackEditScreen.this, this.inputComponentsList.nextStart(20), 80, 0, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.item_capability.begin_pos"));
				this.inputComponentsList.addComponentCurrentRow(new Static(DatapackEditScreen.this, this.inputComponentsList.nextStart(5), 8, 0, 15, HorizontalSizing.LEFT_WIDTH, null, Component.literal("X: ")));
				this.inputComponentsList.addComponentCurrentRow(beginX.relocateX(rect, this.inputComponentsList.nextStart(4)));
				this.inputComponentsList.addComponentCurrentRow(new Static(DatapackEditScreen.this, this.inputComponentsList.nextStart(8), 8, 0, 15, HorizontalSizing.LEFT_WIDTH, null, Component.literal("Y: ")));
				this.inputComponentsList.addComponentCurrentRow(beginY.relocateX(rect, this.inputComponentsList.nextStart(4)));
				this.inputComponentsList.addComponentCurrentRow(new Static(DatapackEditScreen.this, this.inputComponentsList.nextStart(8), 8, 0, 15, HorizontalSizing.LEFT_WIDTH, null, Component.literal("Z: ")));
				this.inputComponentsList.addComponentCurrentRow(beginZ.relocateX(rect, this.inputComponentsList.nextStart(4)));
				
				final ResizableEditBox endX = new ResizableEditBox(font, 0, 35, 0, 15, Component.translatable("datapack_edit.item_capability.trail.end_pos.x"), HorizontalSizing.LEFT_WIDTH, null);
				final ResizableEditBox endY = new ResizableEditBox(font, 0, 35, 0, 15, Component.translatable("datapack_edit.item_capability.trail.end_pos.y"), HorizontalSizing.LEFT_WIDTH, null);
				final ResizableEditBox endZ = new ResizableEditBox(font, 0, 35, 0, 15, Component.translatable("datapack_edit.item_capability.trail.end_pos.z"), HorizontalSizing.LEFT_WIDTH, null);
				
				endX.setResponder((input) -> {
					CompoundTag trailTag = this.packList.get(this.packListGrid.getRowposition()).getValue().getCompound("trail");
					ListTag list = ParseUtil.getOrDefaultTag(trailTag, "end_pos", new ListTag());
					
					if (list.isEmpty()) {
						list.add(DoubleTag.valueOf(0));
						list.add(DoubleTag.valueOf(0));
						list.add(DoubleTag.valueOf(0));
					}
					
					double d = StringUtil.isNullOrEmpty(input) ? 0 : ParseUtil.parseOrGet(input, Double::valueOf, 0.0D);
					list.remove(0);
					list.add(0, DoubleTag.valueOf(d));
					
					TrailInfo trailInfo = TrailInfo.deserialize(trailTag);
					this.modelPreviewer.setTrailInfo(trailInfo);
				});
				endY.setResponder((input) -> {
					CompoundTag trailTag = this.packList.get(this.packListGrid.getRowposition()).getValue().getCompound("trail");
					ListTag list = ParseUtil.getOrDefaultTag(trailTag, "end_pos", new ListTag());
					
					if (list.isEmpty()) {
						list.add(DoubleTag.valueOf(0));
						list.add(DoubleTag.valueOf(0));
						list.add(DoubleTag.valueOf(0));
					}
					
					double d = StringUtil.isNullOrEmpty(input) ? 0 : ParseUtil.parseOrGet(input, Double::valueOf, 0.0D);
					list.remove(1);
					list.add(1, DoubleTag.valueOf(d));
					
					TrailInfo trailInfo = TrailInfo.deserialize(trailTag);
					this.modelPreviewer.setTrailInfo(trailInfo);
				});
				endZ.setResponder((input) -> {
					CompoundTag trailTag = this.packList.get(this.packListGrid.getRowposition()).getValue().getCompound("trail");
					ListTag list = ParseUtil.getOrDefaultTag(trailTag, "end_pos", new ListTag());
					
					if (list.isEmpty()) {
						list.add(DoubleTag.valueOf(0));
						list.add(DoubleTag.valueOf(0));
						list.add(DoubleTag.valueOf(0));
					}
					
					double d = StringUtil.isNullOrEmpty(input) ? 0 : ParseUtil.parseOrGet(input, Double::valueOf, 0.0D);
					list.remove(2);
					list.add(2, DoubleTag.valueOf(d));
					
					TrailInfo trailInfo = TrailInfo.deserialize(trailTag);
					this.modelPreviewer.setTrailInfo(trailInfo);
				});
				
				endX.setFilter((context) -> StringUtil.isNullOrEmpty(context) || ParseUtil.isParsableAllowingMinus(context, Double::parseDouble));
				endY.setFilter((context) -> StringUtil.isNullOrEmpty(context) || ParseUtil.isParsableAllowingMinus(context, Double::parseDouble));
				endZ.setFilter((context) -> StringUtil.isNullOrEmpty(context) || ParseUtil.isParsableAllowingMinus(context, Double::parseDouble));
				
				this.inputComponentsList.newRow();
				this.inputComponentsList.addComponentCurrentRow(new Static(DatapackEditScreen.this, this.inputComponentsList.nextStart(20), 80, 0, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.item_capability.end_pos"));
				this.inputComponentsList.addComponentCurrentRow(new Static(DatapackEditScreen.this, this.inputComponentsList.nextStart(5), 8, 0, 15, HorizontalSizing.LEFT_WIDTH, null, Component.literal("X: ")));
				this.inputComponentsList.addComponentCurrentRow(endX.relocateX(rect, this.inputComponentsList.nextStart(4)));
				this.inputComponentsList.addComponentCurrentRow(new Static(DatapackEditScreen.this, this.inputComponentsList.nextStart(8), 8, 0, 15, HorizontalSizing.LEFT_WIDTH, null, Component.literal("Y: ")));
				this.inputComponentsList.addComponentCurrentRow(endY.relocateX(rect, this.inputComponentsList.nextStart(4)));
				this.inputComponentsList.addComponentCurrentRow(new Static(DatapackEditScreen.this, this.inputComponentsList.nextStart(8), 8, 0, 15, HorizontalSizing.LEFT_WIDTH, null, Component.literal("Z: ")));
				this.inputComponentsList.addComponentCurrentRow(endZ.relocateX(rect, this.inputComponentsList.nextStart(4)));
				
				final ResizableEditBox lifetime = new ResizableEditBox(font, 0, 30, 0, 15, Component.translatable("datapack_edit.item_capability.trail.lifetime"), HorizontalSizing.LEFT_WIDTH, null);
				final ResizableEditBox interpolation = new ResizableEditBox(font, 0, 30, 0, 15, Component.translatable("datapack_edit.item_capability.trail.interpolation"), HorizontalSizing.LEFT_WIDTH, null);
				
				lifetime.setResponder((input) -> {
					int i = StringUtil.isNullOrEmpty(input) ? 0 : Integer.valueOf(input);
					CompoundTag trailTag = this.packList.get(this.packListGrid.getRowposition()).getValue().getCompound("trail");
					trailTag.put("lifetime", IntTag.valueOf(i));
					
					TrailInfo trailInfo = TrailInfo.deserialize(trailTag);
					this.modelPreviewer.setTrailInfo(trailInfo);
				});
				lifetime.setFilter((context) -> StringUtil.isNullOrEmpty(context) || ParseUtil.isParsable(context, Integer::parseInt));
				
				interpolation.setResponder((input) -> {
					int i = StringUtil.isNullOrEmpty(input) ? 0 : Integer.valueOf(input);
					
					CompoundTag trailTag = this.packList.get(this.packListGrid.getRowposition()).getValue().getCompound("trail");
					trailTag.put("interpolations", IntTag.valueOf(i));
					
					TrailInfo trailInfo = TrailInfo.deserialize(trailTag);
					this.modelPreviewer.setTrailInfo(trailInfo);
				});
				interpolation.setFilter((context) -> StringUtil.isNullOrEmpty(context) || ParseUtil.isParsable(context, Integer::parseInt));
				
				this.inputComponentsList.newRow();
				this.inputComponentsList.addComponentCurrentRow(new Static(DatapackEditScreen.this, this.inputComponentsList.nextStart(20), 80, 0, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.item_capability.lifetime"));
				this.inputComponentsList.addComponentCurrentRow(lifetime.relocateX(rect, this.inputComponentsList.nextStart(8)));
				
				this.inputComponentsList.newRow();
				this.inputComponentsList.addComponentCurrentRow(new Static(DatapackEditScreen.this, this.inputComponentsList.nextStart(20), 80, 0, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.item_capability.interpolations"));
				this.inputComponentsList.addComponentCurrentRow(interpolation.relocateX(rect, this.inputComponentsList.nextStart(8)));
				
				final ResizableEditBox texturePath = new ResizableEditBox(font, 0, 15, 0, 15, Component.translatable("datapack_edit.item_capability.trail.end_pos.z"), HorizontalSizing.LEFT_RIGHT, null);
				texturePath.setResponder((input) -> {
					CompoundTag trailTag = ParseUtil.getOrDefaultTag(this.packList.get(this.packListGrid.getRowposition()).getValue(), "trail", new CompoundTag()); 
					trailTag.putString("texture_path", ResourceLocation.parse(input).toString());
					
					TrailInfo trailInfo = TrailInfo.deserialize(trailTag);
					this.modelPreviewer.setTrailInfo(trailInfo);
				});
				texturePath.setFilter((context) -> StringUtil.isNullOrEmpty(context) || (ParseUtil.canParseToResourceLocation(context)));
				texturePath.setMaxLength(100);
				texturePath.setValue("epicfight:textures/particle/swing_trail.png");
				texturePath.moveCursorToStart(true);
				
				final PopupBox<ParticleType<?>> particlePopup = new PopupBox.RegistryPopupBox<>(DatapackEditScreen.this, font, 0, 15, 130, 15, HorizontalSizing.LEFT_RIGHT, null,
																								Component.translatable("datapack_edit.weapon_type.hit_particle"), BuiltInRegistries.PARTICLE_TYPE,
																								(pair) -> {
																									CompoundTag trailTag = ParseUtil.getOrDefaultTag(this.packList.get(this.packListGrid.getRowposition()).getValue(), "trail", new CompoundTag());
																									trailTag.putString("particle_type", ParseUtil.getRegistryName(pair.getSecond(), BuiltInRegistries.PARTICLE_TYPE));
																									
																									TrailInfo trailInfo = TrailInfo.deserialize(trailTag);
																									this.modelPreviewer.setTrailInfo(trailInfo);
																								});
				
				particlePopup._setValue(EpicFightParticles.SWING_TRAIL.get());
				
				this.inputComponentsList.newRow();
				this.inputComponentsList.addComponentCurrentRow(new Static(DatapackEditScreen.this, this.inputComponentsList.nextStart(20), 80, 0, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.item_capability.texture_path"));
				this.inputComponentsList.addComponentCurrentRow(texturePath.relocateX(rect, this.inputComponentsList.nextStart(8)));
				
				this.inputComponentsList.newRow();
				this.inputComponentsList.addComponentCurrentRow(new Static(DatapackEditScreen.this, this.inputComponentsList.nextStart(20), 80, 0, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.item_capability.particle_type"));
				this.inputComponentsList.addComponentCurrentRow(particlePopup.relocateX(rect, this.inputComponentsList.nextStart(8)));
				
				this.inputComponentsList.newRow();
				this.inputComponentsList.newRow();
				this.inputComponentsList.newRow();
				this.inputComponentsList.addComponentCurrentRow(this.modelPreviewer.relocateX(rect, this.inputComponentsList.nextStart(8)));
				this.inputComponentsList.newRow();
				this.inputComponentsList.newRow();
			} else if (itemType == ItemType.ARMOR) {
				this.inputComponentsList.newRow();
				this.inputComponentsList.addComponentCurrentRow(new Static(DatapackEditScreen.this, this.inputComponentsList.nextStart(4), 100, 60, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.item_capability.attributes"));
				this.inputComponentsList.addComponentCurrentRow(SubScreenOpenButton.builder().subScreen(() -> {
					return new WeaponAttributeScreen(DatapackEditScreen.this, ParseUtil.getOrDefaultTag(this.packList.get(this.packListGrid.getRowposition()).getValue(), "attributes", new CompoundTag()), itemType);
				}).bounds(this.inputComponentsList.nextStart(4), 0, 15, 15).build());
			}
			
			this.inputComponentsList.setX(164);
		}
		
		@Override
		public void packGridRowpositionChanged(int rowposition, Map<String, Object> values) {
			CompoundTag tag = this.packList.get(rowposition).getValue();
			this.inputComponentsList.importTag(tag);
			ResourceLocation rl = ResourceLocation.parse(ParseUtil.nullParam(values.get("pack_item")));
			
			if (this.registry.containsKey(rl)) {
				this.modelPreviewer.setItemToRender(this.registry.get(rl));
			}
			
			Function<Item, ? extends CapabilityItem.Builder<?>> builderProvider = DatapackEditScreen.getWeaponType(tag.getString("type"));
			
			if (builderProvider != null) {
				CapabilityItem.Builder<?> builder = builderProvider.apply(this.registry.get(this.packList.get(rowposition).getKey()));
				
				if (builder instanceof WeaponCapability.Builder weaponBuilder) {
					this.modelPreviewer.clearAnimations();
					
					List<AssetAccessor<? extends StaticAnimation>> allAnimations = weaponBuilder.getComboAnimations().entrySet().stream().reduce(Lists.newArrayList(), (list, entry) -> {
						list.addAll(entry.getValue());
						return list;
					}, (list1, list2) -> {
						list1.addAll(list2);
						return list1;
					});
					
					allAnimations.stream().forEach(this.modelPreviewer::addAnimationToPlay);
					boolean hasCustomCollider = false;
					
					if (tag.contains("collider")) {
						try {
							ColliderPreset.deserializeSimpleCollider(tag.getCompound("collider"));
							hasCustomCollider = true;
						} catch (IllegalArgumentException e) {
						}
					}
					
					if (!hasCustomCollider) {
						this.modelPreviewer.setCollider(weaponBuilder.getCollider());
					}
				}
			}
		}
		
		@Override
		public void validateBeforeExport() {
			for (PackEntry<ResourceLocation, CompoundTag> packEntry : this.packList) {
                try {
                    if (!packEntry.getValue().contains("item_type")) {
                        throw new NoSuchElementException("Item type undefined!");
                    }

                    Item item = BuiltInRegistries.ITEM.get(packEntry.getKey());
                    ItemType itemType = ItemType.valueOf(packEntry.getValue().getString("item_type"));

                    if (itemType == ItemType.WEAPON && !packEntry.getValue().contains("type")) {
                        throw new IllegalStateException("Weapon type undefined!");
                    }

                    if (itemType == ItemType.WEAPON) {
                        ItemCapabilityReloadListener.deserializeWeapon(item, packEntry.getValue(), DatapackEditScreen.this);
                    } else {
                        ItemCapabilityReloadListener.deserializeArmor(item, packEntry.getValue());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new IllegalStateException("Failed to export item capability " + packEntry.getKey() + " :\n" + e.getMessage());
                }
			}
		}
		
		@Override
		public void importEntries(PackResources packResources) {
			packResources.getNamespaces(PackType.SERVER_DATA).stream().distinct().forEach((namespace) -> {
				packResources.listResources(PackType.SERVER_DATA, namespace, this.directory, (resourceLocation, streamSupplier) -> {
					if (resourceLocation.toString().contains("/types/")) {
						return;
					}
					
					try {
						ResourceLocation registryName = ResourceLocation.fromNamespaceAndPath(resourceLocation.getNamespace(), resourceLocation.getPath().replaceAll(String.format("%s/%s/", this.directory, ItemType.WEAPON.directoryName), "")
																													.replaceAll(String.format("%s/%s/", this.directory, ItemType.ARMOR.directoryName), "").replaceAll(".json", ""));
						
						ItemType itemType = resourceLocation.getPath().contains(ItemType.WEAPON.directoryName) ? ItemType.WEAPON : ItemType.ARMOR;
						this.importJson(registryName, itemType, streamSupplier.get());
						
						ResourceLocation itemSkin = ResourceLocation.fromNamespaceAndPath(registryName.getNamespace(), "item_skins/" + registryName.getPath() + ".json");
						IoSupplier<InputStream> itemSkinStreamSupplier = packResources.getResource(PackType.CLIENT_RESOURCES, itemSkin);
						
						if (itemSkinStreamSupplier != null) {
							InputStream itemSkinInputStream = itemSkinStreamSupplier.get();
							
							if (itemSkinInputStream != null) {
								JsonReader jsonReader = new JsonReader(new InputStreamReader(itemSkinInputStream, StandardCharsets.UTF_8));
								jsonReader.setLenient(true);
								CompoundTag comp = null;
								
								try {
									comp = TagParser.parseTag(Streams.parse(jsonReader).getAsJsonObject().toString());
								} finally {
									jsonReader.close();
								}
								
								this.packList.get(this.packList.size() - 1).getValue().put("trail", comp.get("trail"));
							}
						}
					} catch (Exception e) {
						EpicFight.LOGGER.info("Failed to import " + resourceLocation + ": " + e.getMessage());
						e.printStackTrace();
					}
				});
			});
		}
		
		public void importJson(ResourceLocation registryName, ItemType itemType, InputStream inputStream) throws Exception {
			JsonReader jsonReader = new JsonReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
			
			try {
				jsonReader.setLenient(true);
				JsonObject jsonObject = Streams.parse(jsonReader).getAsJsonObject();
				CompoundTag compTag = TagParser.parseTag(jsonObject.toString());
				
				if (itemType != null) {
					compTag.putString("item_type", itemType.toString());
				}
				
				//Removal expected after 20.8.1.6
				if (itemType == ItemType.WEAPON) {
					if (compTag.contains("attributes", Tag.TAG_COMPOUND)) {
						final Map<String, ParameterEditor> weaponAttributeEditors = Maps.newLinkedHashMap();
						weaponAttributeEditors.put("armor_negation", ParameterEditor.of((value) -> DoubleTag.valueOf(Double.parseDouble(value.toString())), (tag) -> ParseUtil.valueOfOmittingType(tag.getAsString()), null));
						weaponAttributeEditors.put("impact", ParameterEditor.of((value) -> DoubleTag.valueOf(Double.parseDouble(value.toString())), (tag) -> ParseUtil.valueOfOmittingType(tag.getAsString()), null));
						weaponAttributeEditors.put("max_strikes", ParameterEditor.of((value) -> IntTag.valueOf(Integer.parseInt(value.toString())), (tag) -> ParseUtil.valueOfOmittingType(tag.getAsString()), null));
						weaponAttributeEditors.put("damage_bonus", ParameterEditor.of((value) -> DoubleTag.valueOf(Double.parseDouble(value.toString())), (tag) -> ParseUtil.valueOfOmittingType(tag.getAsString()), null));
						weaponAttributeEditors.put("speed_bonus", ParameterEditor.of((value) -> DoubleTag.valueOf(Double.parseDouble(value.toString())), (tag) -> ParseUtil.valueOfOmittingType(tag.getAsString()), null));
						
						CompoundTag attrComp = compTag.getCompound("attributes");
						
						for (Map.Entry<String, Tag> e : attrComp.tags.entrySet()) {
							CompoundTag comp = (CompoundTag)e.getValue();
							
							for (Map.Entry<String, Tag> attrEntry : comp.tags.entrySet()) {
								if (attrEntry.getValue().getId() == Tag.TAG_STRING) {
									ParameterEditor paramEditor = weaponAttributeEditors.get(attrEntry.getKey());
									
									if (ParseUtil.isParsable(attrEntry.getValue().getAsString(), Double::parseDouble)) {
										comp.put(attrEntry.getKey(), paramEditor.toTag.apply(attrEntry.getValue().getAsString()));
									} else {
										comp.remove(attrEntry.getKey());
									}
								}
							}
						}
					}
				} else if (itemType == ItemType.ARMOR) {
					if (compTag.contains("attributes", Tag.TAG_COMPOUND)) {
						final Map<String, ParameterEditor> armorAttributeEditors = Maps.newLinkedHashMap();
						armorAttributeEditors.put("stun_armor", ParameterEditor.of((value) -> DoubleTag.valueOf(Double.parseDouble(value.toString())), (tag) -> ParseUtil.valueOfOmittingType(tag.getAsString()), null));
						armorAttributeEditors.put("weight", ParameterEditor.of((value) -> DoubleTag.valueOf(Double.parseDouble(value.toString())), (tag) -> ParseUtil.valueOfOmittingType(tag.getAsString()), null));
						
						CompoundTag comp = compTag.getCompound("attributes");
						
						for (Map.Entry<String, Tag> attrEntry : comp.tags.entrySet()) {
							if (attrEntry.getValue().getId() == Tag.TAG_STRING) {
								ParameterEditor paramEditor = armorAttributeEditors.get(attrEntry.getKey());
								
								if (ParseUtil.isParsable(attrEntry.getValue().getAsString(), Double::parseDouble)) {
									comp.put(attrEntry.getKey(), paramEditor.toTag.apply(attrEntry.getValue().getAsString()));
								} else {
									comp.remove(attrEntry.getKey());
								}
							}
						}
					}
				}
				//Removal expected after 20.8.1.6
				
				this.packList.add(PackEntry.ofValue(registryName, compTag));
				this.packListGrid.addRowWithDefaultValues("pack_item", registryName.toString());
			} catch (Exception e) {
				EpicFight.LOGGER.info("Failed to import " + registryName + ": " + e.getMessage());
				throw e;
			} finally {
				try {
					jsonReader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		@Override
		public void exportEntries(ZipOutputStream out) throws Exception {
			for (PackEntry<ResourceLocation, CompoundTag> packEntry : this.packList) {
				String sItemType = packEntry.getValue().getString("item_type");
				
				try {
					ItemType itemType = ItemType.valueOf(sItemType);
					packEntry.getValue().remove("item_type");
					
					ZipEntry zipEntry = new ZipEntry(String.format("data/%s/" + this.directory + "/%s/%s.json", packEntry.getKey().getNamespace(), itemType.directoryName, packEntry.getKey().getPath()));
					Gson gson = new GsonBuilder().setPrettyPrinting().create();
					CompoundTag tag = packEntry.getValue();
					
					if (tag.contains("trail")) {
						TrailInfo result = TrailInfo.ANIMATION_DEFAULT_TRAIL.overwrite(TrailInfo.deserialize(tag.getCompound("trail")));
						
						if (result.playable()) {
							ZipEntry asItemSkin = new ZipEntry(String.format("assets/%s/item_skins/%s.json", packEntry.getKey().getNamespace(), packEntry.getKey().getPath()));
							CompoundTag itemSkinsTag = new CompoundTag();
							itemSkinsTag.put("trail", tag.getCompound("trail"));
							
							out.putNextEntry(asItemSkin);
							out.write(gson.toJson(ParseUtil.convertToJsonObject(itemSkinsTag)).getBytes());
							out.closeEntry();
						}
						
						tag.remove("trail");
					}
					
					if (tag.contains("collider")) {
						try {
							ColliderPreset.deserializeSimpleCollider(tag.getCompound("collider"));
						} catch (Exception e) {
							tag.remove("collider");
						}
					}
					
					out.putNextEntry(zipEntry);
					out.write(gson.toJson(ParseUtil.convertToJsonObject(tag)).getBytes());
					out.closeEntry();
				} catch (Exception e) {
					e.printStackTrace();
					throw new IllegalStateException("Failded to export " + packEntry.getKey() +". "+ e.getMessage());
				} finally {
					packEntry.getValue().putString("item_type", sItemType);
				}
			}
		}
	}
	
	class MobCapabilityTab extends DatapackTab<EntityType<?>> {
		private final ModelPreviewer modelPreviewer;
		private final ComboBox<EntityType<?>> presetCombo = new ComboBox<>(DatapackEditScreen.this, DatapackEditScreen.this.font, 0, 124, 100, 15, HorizontalSizing.LEFT_WIDTH, null, 8,
				Component.translatable("datapack_edit.mob_patch.preset"), EpicFightCapabilities.ENTITY_PATCH_PROVIDER.getPatchedEntities(), (entityType) -> entityType == null ? "none" : EntityType.getKey(entityType).toString(), null);
		
		private final Consumer<EntityType<?>> presetResponder = (entityType) -> {
			CompoundTag tag = this.packList.get(this.packListGrid.getRowposition()).getValue();
			
			if (entityType == null) {
				tag.remove("preset");
			} else {
				tag.putString("preset", EntityType.getKey(entityType).toString());
			}
			
			this.rearrangeComponents(false, entityType == null ? false : true, tag.getBoolean("isHumanoid"));
			
			if (entityType == null) {
				this.bindTag(tag);
			}
		};
		
		private final CheckBox disableCheckBox = new CheckBox(font, 0, 60, 0, 10, HorizontalSizing.LEFT_WIDTH, null, false, Component.literal(""), null);
		private final Consumer<Boolean> disableResponder = (value) -> {
			CompoundTag tag = this.packList.get(this.packListGrid.getRowposition()).getValue();
			tag.putBoolean("disabled", value);
			boolean preset = tag.contains("preset", Tag.TAG_STRING) && !StringUtil.isNullOrEmpty(tag.getString("preset"));
			boolean isHumanoid = tag.getBoolean("isHumanoid");
			
			this.rearrangeComponents(value, preset, isHumanoid);
			
			if (!value) {
				this.bindTag(tag);
			}
		};
		
		private final CheckBox isHumanoidCheckbox = new CheckBox(font, 0, 60, 0, 10, HorizontalSizing.LEFT_WIDTH, null, false, Component.literal(""), null);
		
		private final Consumer<Boolean> isHumanoidResponder = (value) -> {
			CompoundTag tag = this.packList.get(this.packListGrid.getRowposition()).getValue();
			tag.putBoolean("isHumanoid", value);
			
			this.rearrangeComponents(false, false, value);
			this.bindTag(tag);
		};
		
		private final Map<String, ParameterEditor> attributeEditors = Maps.newLinkedHashMap();
		
		private PopupBox.MeshPopupBox meshPopupBox;
		private PopupBox.ArmaturePopupBox armaturePopupBox;
		
		public MobCapabilityTab() {
			super(Component.translatable(LangKeys.GUI_TAB_DATAPACK_MOB_PATCH), MobPatchReloadListener.DIRECTORY, BuiltInRegistries.ENTITY_TYPE, (entityType) -> {
				return entityType.getCategory() != MobCategory.MISC && entityType != EntityType.ENDER_DRAGON;
			});

			this.inputComponentsList = new InputComponentList<>(DatapackEditScreen.this, 0, 0, 0, 30) {
				@Override
				public void importTag(CompoundTag tag) {
					boolean disabled = tag.getBoolean("disabled");
					boolean preset = tag.contains("preset", Tag.TAG_STRING) && !StringUtil.isNullOrEmpty(tag.getString("preset"));
					
					if (!tag.contains("isHumanoid")) {
						tag.putBoolean("isHumanoid", false);
					}
					
					boolean isHumanoid = tag.getBoolean("isHumanoid");
					
					MobCapabilityTab.this.rearrangeComponents(disabled, preset, isHumanoid);
					this.setComponentsActive(true);
					
					if (preset) {
						MobCapabilityTab.this.presetCombo._setResponder(null);
						
						this.setDataBindingComponenets(new Object[] {
							EntityType.byString(tag.getString("preset")).orElse(null)
						});
						
						MobCapabilityTab.this.presetCombo._setResponder(MobCapabilityTab.this.presetResponder);
					} else if (!disabled) {
						MobCapabilityTab.this.bindTag(tag);
					}
				}
			};
			
			this.modelPreviewer = new ModelPreviewer(9, 15, 0, 140, HorizontalSizing.LEFT_RIGHT, null, Armatures.BIPED, Meshes.BIPED);
			this.modelPreviewer.setColliderJoint(Armatures.BIPED.get().toolR);
			
			this.meshPopupBox = new PopupBox.MeshPopupBox(DatapackEditScreen.this, DatapackEditScreen.this.font, 0, 15, 130, 15, HorizontalSizing.LEFT_RIGHT, null, Component.translatable("datapack_edit.weapon_type.model"), (pair) -> {
				if (this.armaturePopupBox._getValue() != null && this.armaturePopupBox._getValue().isPresent() && pair.getSecond() != null && pair.getSecond().isPresent() && pair.getSecond().get().getMaxJointCount() > this.armaturePopupBox._getValue().get().getJointNumber()) {
					this.meshPopupBox._setValue(null);
					
					StringBuilder sb = new StringBuilder();
					sb.append("[");
					
					this.armaturePopupBox._getValue().get().rootJoint.iterSubJoints((joint) -> {
						sb.append(joint.getName() + ", ");
					});
					
					sb.replace(sb.length() - 2, sb.length(), "");
					sb.append("]");
					
					throw new IllegalArgumentException(MessageFormat.format("The model requires {0} joints, but the armature only has {1}. {2}", this.meshPopupBox._getValue().get().getMaxJointCount(), this.armaturePopupBox._getValue().get().getJointNumber(), sb.toString()));
				}
				
				this.packList.get(this.packListGrid.getRowposition()).getValue().putString("model", pair.getFirst());
			});
			
			this.armaturePopupBox = new PopupBox.ArmaturePopupBox(DatapackEditScreen.this, DatapackEditScreen.this.font, 0, 15, 130, 15, HorizontalSizing.LEFT_RIGHT, null, Component.translatable("datapack_edit.weapon_type.armature"), (pair) -> {
				if (this.meshPopupBox._getValue() != null && this.meshPopupBox._getValue().isPresent() && pair.getSecond() != null && pair.getSecond().isPresent() && this.meshPopupBox._getValue().get().getMaxJointCount() > pair.getSecond().get().getJointNumber()) {
					this.armaturePopupBox._setValue(null);
					
					StringBuilder sb = new StringBuilder();
					sb.append("[");
					
					pair.getSecond().get().rootJoint.iterSubJoints((joint) -> {
						sb.append(joint.getName() + ", ");
					});
					
					sb.replace(sb.length() - 2, sb.length(), "");
					sb.append("]");
					
					throw new IllegalArgumentException(MessageFormat.format("The model requires {0} joints, but the armature only has {1}. {2}", this.meshPopupBox._getValue().get().getMaxJointCount(), pair.getSecond().get().getJointNumber(), sb.toString()));
				}
				
				this.packList.get(this.packListGrid.getRowposition()).getValue().putString("armature", pair.getFirst());
			});
			
			final ResizableEditBox impactEditBox = new ResizableEditBox(Minecraft.getInstance().font, 0, 0, 0, 0, Component.literal("impact"), null, null);
			final ResizableEditBox armorNegationEditBox = new ResizableEditBox(Minecraft.getInstance().font, 0, 0, 0, 0, Component.literal("armor_negation"), null, null);
			final ResizableEditBox maxStrikesEditBox = new ResizableEditBox(Minecraft.getInstance().font, 0, 0, 0, 0, Component.literal("max_strikes"), null, null);
			final ResizableEditBox chasingSpeedEditBox = new ResizableEditBox(Minecraft.getInstance().font, 0, 0, 0, 0, Component.literal("chasing_speed"), null, null);
			final ResizableEditBox scaleEditBox = new ResizableEditBox(Minecraft.getInstance().font, 0, 0, 0, 0, Component.literal("scale"), null, null);
			final ResizableEditBox stunArmorBox = new ResizableEditBox(Minecraft.getInstance().font, 0, 0, 0, 0, Component.literal("stun_armor"), null, null);
			
			impactEditBox.setFilter((context) -> StringUtil.isNullOrEmpty(context) || ParseUtil.isParsableAllowingMinus(context, Double::parseDouble));
			armorNegationEditBox.setFilter((context) -> StringUtil.isNullOrEmpty(context) || ParseUtil.isParsableAllowingMinus(context, Double::parseDouble));
			maxStrikesEditBox.setFilter((context) -> StringUtil.isNullOrEmpty(context) || ParseUtil.isParsableAllowingMinus(context, Integer::parseInt));
			chasingSpeedEditBox.setFilter((context) -> StringUtil.isNullOrEmpty(context) || ParseUtil.isParsableAllowingMinus(context, Double::parseDouble));
			scaleEditBox.setFilter((context) -> StringUtil.isNullOrEmpty(context) || ParseUtil.isParsable(context, Double::parseDouble));
			stunArmorBox.setFilter((context) -> StringUtil.isNullOrEmpty(context) || ParseUtil.isParsableAllowingMinus(context, Double::parseDouble));
			
			this.attributeEditors.put("impact", ParameterEditor.of((value) -> DoubleTag.valueOf(Double.parseDouble(value.toString())), (tag) -> ParseUtil.valueOfOmittingType(tag.getAsString()), impactEditBox));
			this.attributeEditors.put("armor_negation", ParameterEditor.of((value) -> DoubleTag.valueOf(Double.parseDouble(value.toString())), (tag) -> ParseUtil.valueOfOmittingType(tag.getAsString()), armorNegationEditBox));
			this.attributeEditors.put("max_strikes", ParameterEditor.of((value) -> IntTag.valueOf(Integer.parseInt(value.toString())), (tag) -> ParseUtil.valueOfOmittingType(tag.getAsString()), maxStrikesEditBox));
			this.attributeEditors.put("chasing_speed", ParameterEditor.of((value) -> DoubleTag.valueOf(Double.parseDouble(value.toString())), (tag) -> ParseUtil.valueOfOmittingType(tag.getAsString()), chasingSpeedEditBox));
			this.attributeEditors.put("scale", ParameterEditor.of((value) -> DoubleTag.valueOf(Double.parseDouble(value.toString())), (tag) -> ParseUtil.valueOfOmittingType(tag.getAsString()), scaleEditBox));
			this.attributeEditors.put("stun_armor", ParameterEditor.of((value) -> DoubleTag.valueOf(Double.parseDouble(value.toString())), (tag) -> ParseUtil.valueOfOmittingType(tag.getAsString()), stunArmorBox));
		}
		
		private void rearrangeComponents(boolean disable, boolean usePreset, boolean isHumanoid) {
			Screen parentScreen = DatapackEditScreen.this;
			final ScreenRectangle screen = getRectangle();
			
			this.inputComponentsList.clearComponents();
			
			if (disable) {
				this.disableCheckBox._setResponder(null);
				this.disableCheckBox._setValue(true);
				this.disableCheckBox._setResponder(MobCapabilityTab.this.disableResponder);
				
				this.inputComponentsList.newRow();
				this.inputComponentsList.addComponentCurrentRow(new Static(parentScreen, this.inputComponentsList.nextStart(4), 100, 60, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.mob_patch.disabled"));
				this.inputComponentsList.addComponentCurrentRow(this.disableCheckBox.relocateX(screen, this.inputComponentsList.nextStart(5)));
			} else if (usePreset) {
				this.inputComponentsList.newRow();
				this.inputComponentsList.addComponentCurrentRow(new Static(parentScreen, this.inputComponentsList.nextStart(4), 100, 60, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.mob_patch.preset"));
				this.inputComponentsList.addComponentCurrentRow(this.presetCombo.relocateX(screen, this.inputComponentsList.nextStart(5)));
				this.presetCombo._setResponder(this.presetResponder);
			} else {
				this.disableCheckBox._setResponder(null);
				this.disableCheckBox._setValue(false);
				this.disableCheckBox._setResponder(MobCapabilityTab.this.disableResponder);
				
				this.inputComponentsList.newRow();
				this.inputComponentsList.addComponentCurrentRow(new Static(parentScreen, this.inputComponentsList.nextStart(4), 100, 60, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.mob_patch.disabled"));
				this.inputComponentsList.addComponentCurrentRow(this.disableCheckBox.relocateX(screen, this.inputComponentsList.nextStart(5)));
				
				this.inputComponentsList.newRow();
				this.inputComponentsList.addComponentCurrentRow(new Static(parentScreen, this.inputComponentsList.nextStart(4), 100, 60, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.mob_patch.preset"));
				this.inputComponentsList.addComponentCurrentRow(this.presetCombo.relocateX(screen, this.inputComponentsList.nextStart(5)));
				this.presetCombo._setResponder(this.presetResponder);
				
				this.inputComponentsList.newRow();
				this.inputComponentsList.addComponentCurrentRow(new Static(parentScreen, this.inputComponentsList.nextStart(4), 100, 60, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.mob_patch.model"));
				this.inputComponentsList.addComponentCurrentRow(this.meshPopupBox.relocateX(screen, this.inputComponentsList.nextStart(5)));
				
				this.inputComponentsList.newRow();
				this.inputComponentsList.addComponentCurrentRow(new Static(parentScreen, this.inputComponentsList.nextStart(4), 100, 60, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.mob_patch.armature"));
				this.inputComponentsList.addComponentCurrentRow(this.armaturePopupBox.relocateX(screen, this.inputComponentsList.nextStart(5)));
				
				this.inputComponentsList.newRow();
				this.inputComponentsList.addComponentCurrentRow(new Static(parentScreen, this.inputComponentsList.nextStart(4), 100, 60, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.mob_patch.renderer"));
				this.inputComponentsList.addComponentCurrentRow(new PopupBox.RendererPopupBox(parentScreen, font, this.inputComponentsList.nextStart(5), 15, 130, 15, HorizontalSizing.LEFT_RIGHT, null,
					Component.translatable("datapack_edit.weapon_type.renderer"), (pair) -> {
						this.packList.get(this.packListGrid.getRowposition()).getValue().putString("renderer", pair.getFirst());
					}));
				
				this.inputComponentsList.newRow();
				this.inputComponentsList.addComponentCurrentRow(new Static(parentScreen, this.inputComponentsList.nextStart(4), 100, 60, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.mob_patch.humanoid"));
				this.inputComponentsList.addComponentCurrentRow(this.isHumanoidCheckbox.relocateX(screen, this.inputComponentsList.nextStart(5)));
				
				this.inputComponentsList.newRow();
				this.inputComponentsList.addComponentCurrentRow(new Static(parentScreen, this.inputComponentsList.nextStart(4), 100, 60, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.mob_patch.faction"));
				this.inputComponentsList.addComponentCurrentRow(new ComboBox<>(parentScreen, Minecraft.getInstance().font, this.inputComponentsList.nextStart(5), 124, 100, 15, HorizontalSizing.LEFT_WIDTH, null, 8,
					Component.translatable("datapack_edit.mob_patch.faction"), Faction.ENUM_MANAGER.universalValues(), (faction) -> ParseUtil.snakeToSpacedCamel(faction), (faction) -> {
						this.packList.get(this.packListGrid.getRowposition()).getValue().putString("faction", ParseUtil.nullOrToString(faction, (value) -> value.toString().toLowerCase(Locale.ROOT)));
					}));
				
				this.inputComponentsList.newRow();
				this.inputComponentsList.addComponentCurrentRow(new Static(parentScreen, this.inputComponentsList.nextStart(4), 100, 60, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.mob_patch.swing_sound"));
				this.inputComponentsList.addComponentCurrentRow(new PopupBox.SoundPopupBox(parentScreen, Minecraft.getInstance().font, this.inputComponentsList.nextStart(5), 15, 0, 15, HorizontalSizing.LEFT_RIGHT, null,
					Component.translatable("datapack_edit.mob_patch.swing_sound"), (soundevent) -> {
						this.packList.get(this.packListGrid.getRowposition()).getValue().putString("swing_sound", ParseUtil.getRegistryName(soundevent.getSecond(), BuiltInRegistries.SOUND_EVENT));
					}));
				
				this.inputComponentsList.newRow();
				this.inputComponentsList.addComponentCurrentRow(new Static(parentScreen, this.inputComponentsList.nextStart(4), 100, 60, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.mob_patch.hit_sound"));
				this.inputComponentsList.addComponentCurrentRow(new PopupBox.SoundPopupBox(parentScreen, Minecraft.getInstance().font, this.inputComponentsList.nextStart(5), 15, 0, 15, HorizontalSizing.LEFT_RIGHT, null,
					Component.translatable("datapack_edit.mob_patch.hit_sound"), (soundevent) -> {
						this.packList.get(this.packListGrid.getRowposition()).getValue().putString("hit_sound", ParseUtil.getRegistryName(soundevent.getSecond(), BuiltInRegistries.SOUND_EVENT));
					}));
				
				this.inputComponentsList.newRow();
				this.inputComponentsList.addComponentCurrentRow(new Static(parentScreen, this.inputComponentsList.nextStart(4), 100, 60, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.mob_patch.hit_particle"));
				this.inputComponentsList.addComponentCurrentRow(new PopupBox.RegistryPopupBox<>(parentScreen, font, this.inputComponentsList.nextStart(5), 15, 0, 15, HorizontalSizing.LEFT_RIGHT, null,
					Component.translatable("datapack_edit.weapon_type.hit_particle"), BuiltInRegistries.PARTICLE_TYPE, (pair) -> {
						this.packList.get(this.packListGrid.getRowposition()).getValue().putString("hit_particle", ParseUtil.getRegistryName(pair.getSecond(), BuiltInRegistries.PARTICLE_TYPE));
					}));
				
				this.inputComponentsList.newRow();
				this.inputComponentsList.addComponentCurrentRow(new Static(parentScreen, this.inputComponentsList.nextStart(4), 100, 60, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.mob_patch.attributes"));
				this.inputComponentsList.newRow();
				this.inputComponentsList.newRow();
				this.inputComponentsList.addComponentCurrentRow(Grid.builder(parentScreen, Minecraft.getInstance())
																	.xy1(this.inputComponentsList.nextStart(5), 0)
																	.xy2(15, 90)
																	.horizontalSizing(HorizontalSizing.LEFT_RIGHT)
																	.rowHeight(21)
																	.rowEditable(RowEditButton.ADD_REMOVE)
																	.addColumn(Grid.combo("attribute", List.copyOf(this.attributeEditors.values()))
																					.toDisplayText((editor) -> ParseUtil.nullOrToString(editor, (editor$1) -> ParseUtil.snakeToSpacedCamel(editor.editWidget.getMessage().getString())))
																					.valueChanged((event) -> {
																						CompoundTag attributesTag = ParseUtil.getOrSupply(this.packList.get(this.packListGrid.getRowposition()).getValue(), "attributes", CompoundTag::new);
																						
																						if (event.prevValue != null) {
																							attributesTag.remove(event.prevValue.editWidget.getMessage().getString());
																						} else {
																							attributesTag.remove("");
																						}
																						
																						attributesTag.putString(ParseUtil.nullParam(event.postValue.editWidget.getMessage().getString()), "");
																					})
																					.width(100))
																	.addColumn(Grid.wildcard("amount")
																					.editWidgetProvider((row) -> {
																						ParameterEditor editor = row.getValue("attribute");
																						return editor == null ? null : editor.editWidget;
																					})
																					.valueChanged((event) -> {
																						CompoundTag attributesTag = ParseUtil.getOrSupply(this.packList.get(this.packListGrid.getRowposition()).getValue(), "attributes", CompoundTag::new);
																						ParameterEditor editor = event.grid.getValue(event.rowposition, "attribute");
																						
																						if (!StringUtil.isNullOrEmpty(ParseUtil.nullParam(event.postValue))) {
																							attributesTag.put(editor.editWidget.getMessage().getString(), editor.toTag.apply(event.postValue));
																						} else {
																							attributesTag.remove(editor.editWidget.getMessage().getString());
																						}
																					})
																					.width(150))
																	.pressAdd((grid, button) -> {
																		CompoundTag attributeTag = ParseUtil.getOrDefaultTag(this.packList.get(this.packListGrid.getRowposition()).getValue(), "attributes", new CompoundTag());
																		attributeTag.putString("", "");
																		int rowposition = grid.addRow();
																		grid.setGridFocus(rowposition, "attribute");
																	})
																	.pressRemove((grid, button) -> {
																		int rowposition = grid.getRowposition();
																		
																		if (rowposition > -1) {
																			CompoundTag attributeTag = ParseUtil.getOrDefaultTag(this.packList.get(this.packListGrid.getRowposition()).getValue(), "attributes", new CompoundTag());
																			attributeTag.remove(ParseUtil.nullParam(grid.getValue(rowposition, "attribute")));
																			grid.removeRow(rowposition);
																		}
																	})
																	.build());
				this.inputComponentsList.newRow();
				
				this.inputComponentsList.newRow();
				this.inputComponentsList.addComponentCurrentRow(new Static(parentScreen, this.inputComponentsList.nextStart(4), 100, 60, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.mob_patch.default_livingmotions"));
				this.inputComponentsList.newRow();
				this.inputComponentsList.newRow();
				this.inputComponentsList.addComponentCurrentRow(Grid.builder(DatapackEditScreen.this)
																	.xy1(this.inputComponentsList.nextStart(5), 0)
																	.xy2(15, 90)
																	.horizontalSizing(HorizontalSizing.LEFT_RIGHT)
																	.rowHeight(21)
																	.rowEditable(RowEditButton.ADD_REMOVE)
																	.addColumn(Grid.combo("living_motion", List.of(LivingMotions.IDLE, LivingMotions.WALK, LivingMotions.CHASE, LivingMotions.MOUNT, LivingMotions.FALL, LivingMotions.FLOAT, LivingMotions.DEATH, LivingMotions.RELOAD, LivingMotions.AIM))
																					.valueChanged((event) -> {
																						CompoundTag livingMotionTag = ParseUtil.getOrSupply(this.packList.get(this.packListGrid.getRowposition()).getValue(), "default_livingmotions", CompoundTag::new);
																						livingMotionTag.remove(ParseUtil.nullParam(event.prevValue));
																						livingMotionTag.putString(ParseUtil.nullOrToString(event.postValue, (livingmotion) -> livingmotion.name().toLowerCase(Locale.ROOT)), "");
																					}).editable(true).width(100))
																	.addColumn(Grid.popup("animation", PopupBox.AnimationPopupBox::new).filter((accessor) -> !(accessor.checkType(MainFrameAnimation.class)) || accessor.checkType(LongHitAnimation.class))
																					.editWidgetCreated((popupBox) -> popupBox.setModel(this.armaturePopupBox._getValue(), this.meshPopupBox._getValue()))
																					.valueChanged((event) -> {
																						CompoundTag livingMotionTag = ParseUtil.getOrSupply(this.packList.get(this.packListGrid.getRowposition()).getValue(), "default_livingmotions", CompoundTag::new);
																						livingMotionTag.putString(ParseUtil.nullOrToString((LivingMotions)event.grid.getValue(event.rowposition, "living_motion"), (livingmotion) -> livingmotion.name().toLowerCase(Locale.ROOT)),
																													ParseUtil.nullOrToString(event.postValue, (animation) -> animation.registryName().toString()));
																					}).toDisplayText((item) -> item == null ? "" : item.registryName().toString()).width(150))
																	.pressAdd((grid, button) -> {
																		CompoundTag attributeTag = ParseUtil.getOrDefaultTag(this.packList.get(this.packListGrid.getRowposition()).getValue(), "default_livingmotions", new CompoundTag());
																		attributeTag.putString("", "");
																		
																		int rowposition = grid.addRow();
																		grid.setGridFocus(rowposition, "living_motion");
																	})
																	.pressRemove((grid, button) -> {
																		int rowposition = grid.getRowposition();
																		
																		if (rowposition > -1) {
																			CompoundTag livingMotionTag = ParseUtil.getOrDefaultTag(this.packList.get(this.packListGrid.getRowposition()).getValue(), "default_livingmotions", new CompoundTag());
																			livingMotionTag.remove(ParseUtil.nullParam(grid.getValue(rowposition, "living_motion")).toLowerCase(Locale.ROOT));
																			grid.removeRow(rowposition);
																		}
																	})
																	.build());
				this.inputComponentsList.newRow();
				
				this.inputComponentsList.newRow();
				this.inputComponentsList.addComponentCurrentRow(new Static(parentScreen, this.inputComponentsList.nextStart(4), 100, 60, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.mob_patch.stun_animations"));
				this.inputComponentsList.newRow();
				this.inputComponentsList.newRow();
				this.inputComponentsList.addComponentCurrentRow(Grid.builder(DatapackEditScreen.this)
																	.xy1(this.inputComponentsList.nextStart(5), 0)
																	.xy2(15, 90)
																	.horizontalSizing(HorizontalSizing.LEFT_RIGHT)
																	.rowHeight(21)
																	.rowEditable(RowEditButton.ADD_REMOVE)
																	.addColumn(Grid.combo("stun_type", List.of(StunType.values()))
																					.toDisplayText((stunType) -> ParseUtil.nullOrToString(stunType, (type) -> ParseUtil.snakeToSpacedCamel(type.name())))
																					.valueChanged((event) -> {
																						CompoundTag attributesTag = ParseUtil.getOrSupply(this.packList.get(this.packListGrid.getRowposition()).getValue(), "stun_animations", CompoundTag::new);
																						attributesTag.remove(ParseUtil.nullParam(event.prevValue));
																						attributesTag.putString(ParseUtil.nullOrToString(event.postValue, (stunType) -> stunType.name().toLowerCase(Locale.ROOT)), "");
																					}).editable(true).width(100))
																	.addColumn(Grid.popup("animation", PopupBox.AnimationPopupBox::new)
																					.filter(animation -> animation.checkType(HitAnimation.class) || animation.checkType(LongHitAnimation.class))
																					.editWidgetCreated((popupBox) -> popupBox.setModel(this.armaturePopupBox._getValue(), this.meshPopupBox._getValue()))
																					.valueChanged((event) -> {
																						CompoundTag stunTypeTag = ParseUtil.getOrSupply(this.packList.get(this.packListGrid.getRowposition()).getValue(), "stun_animations", CompoundTag::new);
																						stunTypeTag.putString(ParseUtil.nullOrToString((StunType)event.grid.getValue(event.rowposition, "stun_type"), (stunType) -> stunType.name().toLowerCase(Locale.ROOT)),
																												ParseUtil.nullOrToString(event.postValue, (animation) -> animation.registryName().toString()));
																					}).toDisplayText((item) -> item == null ? "" : item.registryName().toString()).width(150))
																	.pressAdd((grid, button) -> {
																		CompoundTag attributeTag = ParseUtil.getOrDefaultTag(this.packList.get(this.packListGrid.getRowposition()).getValue(), "stun_animations", new CompoundTag());
																		attributeTag.putString("", "");
																		
																		int rowposition = grid.addRow();
																		grid.setGridFocus(rowposition, "stun_type");
																	})
																	.pressRemove((grid, button) -> {
																		int rowposition = grid.getRowposition();
																		
																		if (rowposition > -1) {
																			CompoundTag stunTypeTag = ParseUtil.getOrDefaultTag(this.packList.get(this.packListGrid.getRowposition()).getValue(), "stun_animations", new CompoundTag());
																			stunTypeTag.remove(ParseUtil.nullOrToString((StunType)grid.getValue(rowposition, "stun_type"), (stunType) -> stunType.name().toLowerCase(Locale.ROOT)));
																			grid.removeRow(rowposition);
																		}
																	})
																	.build());
				this.inputComponentsList.newRow();
				
				if (isHumanoid) {
					this.inputComponentsList.newRow();
					this.inputComponentsList.addComponentCurrentRow(new Static(parentScreen, this.inputComponentsList.nextStart(4), 140, 60, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.mob_patch.humanoid_weapon_motions"));
					this.inputComponentsList.addComponentCurrentRow(SubScreenOpenButton.builder().subScreen(() -> {
						if (this.armaturePopupBox._getValue() == null || this.meshPopupBox._getValue() == null) {
							return new MessageScreen<>("", "Define model and armature first.", DatapackEditScreen.this, (button2) -> Minecraft.getInstance().setScreen(DatapackEditScreen.this), 180, 60);
						} else {
							return new HumanoidWeaponMotionScreen(DatapackEditScreen.this, this.packList.get(this.packListGrid.getRowposition()).getValue(), this.armaturePopupBox._getValue(), this.meshPopupBox._getValue());
						}
					}).bounds(this.inputComponentsList.nextStart(4), 0, 15, 15).build());
				}
				
				this.inputComponentsList.newRow();
				this.inputComponentsList.addComponentCurrentRow(new Static(parentScreen, this.inputComponentsList.nextStart(4), 140, 60, 15, HorizontalSizing.LEFT_WIDTH, null, "datapack_edit.mob_patch.combat_behavior"));
				this.inputComponentsList.addComponentCurrentRow(SubScreenOpenButton.builder().subScreen(() -> {
					if (this.armaturePopupBox._getValue() == null || this.meshPopupBox._getValue() == null) {
						return new MessageScreen<>("", "Define model and armature first.", DatapackEditScreen.this, (button2) -> Minecraft.getInstance().setScreen(DatapackEditScreen.this), 180, 60);
					} else if (isHumanoid) {
						return new HumanoidCombatBehaviorScreen(DatapackEditScreen.this, this.packList.get(this.packListGrid.getRowposition()).getValue(), this.armaturePopupBox._getValue(), this.meshPopupBox._getValue());
					} else {
						return new CombatBehaviorScreen(DatapackEditScreen.this, this.packList.get(this.packListGrid.getRowposition()).getValue(), this.armaturePopupBox._getValue(), this.meshPopupBox._getValue(), false);
					}
				}).bounds(this.inputComponentsList.nextStart(4), 0, 15, 15).build());
			}
			
			this.inputComponentsList.setX(164);
		}
		
		private void bindTag(CompoundTag tag) {
			MobCapabilityTab.this.presetCombo._setResponder(null);
			MobCapabilityTab.this.disableCheckBox._setResponder(null);
			MobCapabilityTab.this.isHumanoidCheckbox._setResponder(null);
			
			Grid.PackImporter attributePackImporter = new Grid.PackImporter();
			Grid.PackImporter livingmotionPackImporter = new Grid.PackImporter();
			Grid.PackImporter stunPackImporter = new Grid.PackImporter();
			
			for (Map.Entry<String, Tag> attributesTag : tag.getCompound("attributes").tags.entrySet()) {
				ParameterEditor editor = this.attributeEditors.get(attributesTag.getKey());
				
				attributePackImporter.newRow();
				attributePackImporter.newValue("attribute", editor);
				attributePackImporter.newValue("amount", ParseUtil.valueOfOmittingType(attributesTag.getValue().getAsString()));
			}
			
			for (Map.Entry<String, Tag> livingmotionTag : tag.getCompound("default_livingmotions").tags.entrySet()) {
				try {
					LivingMotion livingMotion = LivingMotion.ENUM_MANAGER.get(livingmotionTag.getKey().toUpperCase(Locale.ROOT));
					livingmotionPackImporter.newRow();
					livingmotionPackImporter.newValue("living_motion", livingMotion);
					livingmotionPackImporter.newValue("animation", DatapackEditScreen.animationByKey(livingmotionTag.getValue().getAsString()));
				} finally {}
			}
			
			for (Map.Entry<String, Tag> stunTag : tag.getCompound("stun_animations").tags.entrySet()) {
				try {
					StunType stunType = ParseUtil.enumValueOfOrNull(StunType.class, stunTag.getKey().toUpperCase(Locale.ROOT));
					stunPackImporter.newRow();
					stunPackImporter.newValue("stun_type", stunType);
					stunPackImporter.newValue("animation", DatapackEditScreen.animationByKey(stunTag.getValue().getAsString()));
				} finally {}
			}
			
			this.inputComponentsList.setDataBindingComponenets(new Object[] {
				false,
				EntityType.byString(tag.getString("preset")).orElse(null),
				DatapackEditScreen.getMesh(tag.getString("model")),
				DatapackEditScreen.getArmature(tag.getString("armature")),
				StringUtil.isNullOrEmpty(tag.getString("renderer")) ? null : ResourceLocation.parse(tag.getString("renderer")),
				tag.getBoolean("isHumanoid"),
				ParseUtil.nullOrApply(tag.get("faction"), (jsonElement) -> Faction.ENUM_MANAGER.getOrThrow(jsonElement.getAsString())),
				BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse(tag.getString("swing_sound"))),
				BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse(tag.getString("hit_sound"))),
				BuiltInRegistries.PARTICLE_TYPE.get(ResourceLocation.parse(tag.getString("hit_particle"))),
				attributePackImporter,
				livingmotionPackImporter,
				stunPackImporter
			});
			
			MobCapabilityTab.this.presetCombo._setResponder(MobCapabilityTab.this.presetResponder);
			MobCapabilityTab.this.disableCheckBox._setResponder(MobCapabilityTab.this.disableResponder);
			MobCapabilityTab.this.isHumanoidCheckbox._setResponder(MobCapabilityTab.this.isHumanoidResponder);
		}
		
		@Override
		public void packGridRowpositionChanged(int rowposition, Map<String, Object> values) {
			this.inputComponentsList.importTag(this.packList.get(rowposition).getValue());
		}
		
		@Override
		public void validateBeforeExport() {
			for (PackEntry<ResourceLocation, CompoundTag> packEntry : this.packList) {
				try {
					Optional<EntityType<?>> type = EntityType.byString(packEntry.getKey().toString());
					
					if (type.isEmpty()) {
						throw new IllegalStateException("Invalid entity type");
					}
					
					MobPatchReloadListener.deserializeMobPatchProvider(type.get(), packEntry.getValue(), true, Minecraft.getInstance().getResourceManager(), DatapackEditScreen.this);
				} catch (Exception e) {
					e.printStackTrace();
					throw new IllegalStateException("Failed to export mobpatch " + packEntry.getKey() + " :\n" + e.getMessage());
				}
			}
		}
		
		@Override
		public void importEntries(PackResources packResources) {
			packResources.getNamespaces(PackType.SERVER_DATA).stream().distinct().forEach((namespace) -> {
				packResources.listResources(PackType.SERVER_DATA, namespace, this.directory, (resourceLocation, streamSupplier) -> {
					ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(resourceLocation.getNamespace(), resourceLocation.getPath().replaceAll(String.format("%s/", this.directory), "").replaceAll(".json", ""));
					
					try {
						this.importJson(rl, streamSupplier.get());
					} catch (Exception e) {
						EpicFight.LOGGER.info("Failed to import " + resourceLocation + ": " + e.getMessage());
						e.printStackTrace();
					}
				});
			});
		}
		
		public void importJson(ResourceLocation registryName, InputStream inputStream) throws Exception {
			InputStreamReader reader = new InputStreamReader(inputStream);
		    StringBuilder sb = new StringBuilder();
		    Exception exception = null;
		    
		    try {
		    	for (int data = reader.read(); data != -1; data = reader.read()) {
					sb.append((char) data);
				}
				
				CompoundTag compTag = TagParser.parseTag(sb.toString());
				
				if (compTag.getBoolean("isHumanoid")) {
					ListTag combatBehaviorList = compTag.getList("combat_behavior", Tag.TAG_COMPOUND);
					
					if (!combatBehaviorList.isEmpty()) {
						compTag.remove("combat_behavior");
						compTag.put("combat_behavior_humanoid", combatBehaviorList);
					}
				}
				
				this.packList.add(PackEntry.of(registryName, () -> compTag));
				this.packListGrid.addRowWithDefaultValues("pack_item", registryName.toString());
		    } catch (IOException | CommandSyntaxException e) {
		    	exception = e;
		    } finally {
		    	reader.close();
		    }
		    
		    if (exception != null) {
		    	throw exception;
		    }
		}
		
		@Override
		public void exportEntries(ZipOutputStream out) throws Exception {
			for (PackEntry<ResourceLocation, CompoundTag> packEntry : this.packList) {
				try {
					CompoundTag packCompound = packEntry.getValue();
					
					if (packCompound.getBoolean("disabled")) {
						packCompound.tags.clear();
						packCompound.putBoolean("disabled", true);
					} else if (packCompound.contains("preset")) {
						String preset = packCompound.getString("preset");
						packCompound.tags.clear();
						packCompound.putString("preset", preset);
					} else {
						packCompound.remove("disabled");
					}
					
					if (packCompound.getBoolean("isHumanoid")) {
						ListTag humanoidCombatBehavior = packCompound.getList("combat_behavior_humanoid", Tag.TAG_COMPOUND);
						packCompound.remove("combat_behavior_humanoid");
						packCompound.put("combat_behavior", humanoidCombatBehavior);
					}
					
					ZipEntry zipEntry = new ZipEntry(String.format("data/%s/" + this.directory + "/%s.json", packEntry.getKey().getNamespace(), packEntry.getKey().getPath()));
					Gson gson = new GsonBuilder().setPrettyPrinting().create();
					out.putNextEntry(zipEntry);
					out.write(gson.toJson(ParseUtil.convertToJsonObject(packCompound)).getBytes());
					out.closeEntry();
				} catch (Exception e) {
					e.printStackTrace();
					throw new IllegalStateException("Failded to export " + packEntry.getKey() +". "+ e.getMessage());
				}
			}
		}
	}
}