package yesman.epicfight.client.gui.screen;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.neoforge.client.ClientHooks;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import yesman.epicfight.api.client.event.EpicFightClientEventHooks;
import yesman.epicfight.api.client.event.types.registry.RegisterAttributeIconEvent;
import yesman.epicfight.api.client.event.types.registry.RegisterWeaponCategoryIconEvent;
import yesman.epicfight.client.gui.datapack.widgets.Static;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.network.EpicFightNetworkManager;
import yesman.epicfight.network.client.CPChangeSkill;
import yesman.epicfight.registry.entries.EpicFightAttributes;
import yesman.epicfight.registry.entries.EpicFightItems;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillCategories;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.item.CapabilityItem.WeaponCategories;
import yesman.epicfight.world.capabilities.item.WeaponCategory;
import yesman.epicfight.world.item.SkillBookItem;

import javax.annotation.Nullable;
import java.util.*;

import static yesman.epicfight.generated.LangKeys.*;

public class SkillBookScreen extends Screen {
    private static final Map<WeaponCategory, ItemStack> WEAPON_CATEGORY_ICONS = new HashMap<> ();
    private static final Map<Holder<Attribute>, TextureInfo> ATTRIBUTE_ICONS = new HashMap<> ();
    private static final ResourceLocation SKILLBOOK_BACKGROUND = EpicFightMod.identifier("textures/gui/screen/skillbook.png");

    public static final TextureInfo HEALTH_TEXTURE_INFO = new TextureInfo(SKILLBOOK_BACKGROUND, 22, 205, 10, 10);
    public static final TextureInfo STAMINA_TEXTURE_INFO = new TextureInfo(SKILLBOOK_BACKGROUND, 32, 205, 10, 10);
    public static final TextureInfo COOLDOWN_TEXTURE_INFO = new TextureInfo(SKILLBOOK_BACKGROUND, 42, 205, 10, 10);

    public static void registerIconItems() {
        WEAPON_CATEGORY_ICONS.put(WeaponCategories.NOT_WEAPON, new ItemStack(Items.AIR));
        WEAPON_CATEGORY_ICONS.put(WeaponCategories.AXE, new ItemStack(Items.IRON_AXE));
        WEAPON_CATEGORY_ICONS.put(WeaponCategories.FIST, new ItemStack(EpicFightItems.GLOVE.get()));
        WEAPON_CATEGORY_ICONS.put(WeaponCategories.GREATSWORD, new ItemStack(EpicFightItems.IRON_GREATSWORD.get()));
        WEAPON_CATEGORY_ICONS.put(WeaponCategories.HOE, new ItemStack(Items.IRON_HOE));
        WEAPON_CATEGORY_ICONS.put(WeaponCategories.PICKAXE, new ItemStack(Items.IRON_PICKAXE));
        WEAPON_CATEGORY_ICONS.put(WeaponCategories.SHOVEL, new ItemStack(Items.IRON_SHOVEL));
        WEAPON_CATEGORY_ICONS.put(WeaponCategories.SWORD, new ItemStack(Items.IRON_SWORD));
        WEAPON_CATEGORY_ICONS.put(WeaponCategories.UCHIGATANA, new ItemStack(EpicFightItems.UCHIGATANA.get()));
        WEAPON_CATEGORY_ICONS.put(WeaponCategories.SPEAR, new ItemStack(EpicFightItems.IRON_SPEAR.get()));
        WEAPON_CATEGORY_ICONS.put(WeaponCategories.TACHI, new ItemStack(EpicFightItems.IRON_TACHI.get()));
        WEAPON_CATEGORY_ICONS.put(WeaponCategories.TRIDENT, new ItemStack(Items.TRIDENT));
        WEAPON_CATEGORY_ICONS.put(WeaponCategories.LONGSWORD, new ItemStack(EpicFightItems.IRON_LONGSWORD.get()));
        WEAPON_CATEGORY_ICONS.put(WeaponCategories.DAGGER, new ItemStack(EpicFightItems.IRON_DAGGER.get()));
        WEAPON_CATEGORY_ICONS.put(WeaponCategories.SHIELD, new ItemStack(Items.SHIELD));
        WEAPON_CATEGORY_ICONS.put(WeaponCategories.RANGED, new ItemStack(Items.BOW));

        ATTRIBUTE_ICONS.put(Attributes.MAX_HEALTH, new TextureInfo(SKILLBOOK_BACKGROUND, 22, 195, 10, 10));
        ATTRIBUTE_ICONS.put(EpicFightAttributes.MAX_STAMINA, new TextureInfo(SKILLBOOK_BACKGROUND, 42, 195, 10, 10));
        ATTRIBUTE_ICONS.put(Attributes.ATTACK_DAMAGE, new TextureInfo(SKILLBOOK_BACKGROUND, 52, 195, 10, 10));
        ATTRIBUTE_ICONS.put(EpicFightAttributes.STAMINA_REGEN, new TextureInfo(SKILLBOOK_BACKGROUND, 62, 195, 10, 10));
        ATTRIBUTE_ICONS.put(Attributes.ATTACK_SPEED, new TextureInfo(SKILLBOOK_BACKGROUND, 72, 195, 10, 10));

        RegisterWeaponCategoryIconEvent weaponCategoryIconRegisterEvent = new RegisterWeaponCategoryIconEvent(WEAPON_CATEGORY_ICONS);
        EpicFightClientEventHooks.Registry.WEAPON_CATEGORY_ICON.post(weaponCategoryIconRegisterEvent);

        RegisterAttributeIconEvent attributeIconRegisterEvent = new RegisterAttributeIconEvent(ATTRIBUTE_ICONS);
        EpicFightClientEventHooks.Registry.ATTRIBUTE_ICON.post(attributeIconRegisterEvent);
    }

    protected final Player opener;
    protected final LocalPlayerPatch playerpatch;
    @NotNull
    protected final Skill skill;
    protected final Screen parentScreen;
    protected final SkillTooltipList skillTooltipList;
    protected final AvailableItemsList availableWeaponCategoryList;
    protected final AttributeIconList consumptionList;
    protected final AttributeIconList providingAttributesList;
    protected final InteractionHand hand;
    private double customScale;
    private Button learnButton;

    public Button getLearnButton() {
        return learnButton;
    }

    /// Avoid using this constructor, check the existance of skill component because [#skill] is @NotNull
    @Deprecated
    public SkillBookScreen(Player opener, ItemStack stack, @Nullable InteractionHand hand) {
        this(opener, SkillBookItem.getContainSkill(stack).orElseThrow().value(), hand, null);
    }

    public SkillBookScreen(Player opener, @NotNull Skill skill, @Nullable InteractionHand hand, @Nullable Screen parentScreen) {
        super(Component.empty());

        this.minecraft = Minecraft.getInstance();
        this.font = Minecraft.getInstance().font;

        this.opener = opener;
        this.playerpatch = EpicFightCapabilities.getEntityPatch(this.opener, LocalPlayerPatch.class);
        this.skill = skill;
        this.hand = hand;
        this.parentScreen = parentScreen;
        this.skillTooltipList = new SkillTooltipList(Minecraft.getInstance(), 0, 0, 0, Minecraft.getInstance().font.lineHeight);
        this.availableWeaponCategoryList = new AvailableItemsList(0, 0);
        this.consumptionList = new AttributeIconList(Minecraft.getInstance(), 0, 0, 100, 16);
        this.providingAttributesList = new AttributeIconList(Minecraft.getInstance(), 0, 0, 100, 16);

        List<FormattedCharSequence> list = Minecraft.getInstance().font.split(Component.translatable(this.skill.getTranslationKey() + ".tooltip", this.skill.getTooltipArgsOfScreen(new ArrayList<>()).toArray(new Object[0])), 148);
        list.forEach(this.skillTooltipList::add);

        if (this.skill.getAvailableWeaponCategories() != null) {
            this.skill.getAvailableWeaponCategories().forEach(this.availableWeaponCategoryList::addWeaponCategory);
        }

        if (!this.skill.getCustomConsumptionTooltips(this.consumptionList)) {
            this.consumptionList.children().clear();

            switch (this.skill.getResourceType()) {
                case WEAPON_CHARGE -> {

                }
                case COOLDOWN -> {
                    this.consumptionList.add(
                        Component.translatable(ATTRIBUTE_NAME_COOLDOWN_CONSUME_TOOLTIP),
                        Component.translatable(
                            ATTRIBUTE_NAME_COOLDOWN_CONSUME,
                            String.format("%.1f", this.skill.getConsumption())
                        ),
                        COOLDOWN_TEXTURE_INFO
                    );
                }
                case STAMINA -> {
                    this.consumptionList.add(
                        Component.translatable(ATTRIBUTE_NAME_STAMINA_CONSUME_TOOLTIP),
                        Component.translatable(
                            ATTRIBUTE_NAME_STAMINA_CONSUME,
                            String.format("%.1f", this.skill.getConsumption())
                        ),
                        STAMINA_TEXTURE_INFO
                    );
                }
                case HEALTH -> {
                    this.consumptionList.add(
                        Component.translatable(ATTRIBUTE_NAME_HEALTH_CONSUME_TOOLTIP),
                        Component.translatable(ATTRIBUTE_NAME_HEALTH_CONSUME),
                        HEALTH_TEXTURE_INFO
                    );
                }
                default -> {
                }
            }
        }

        this.skill.getModfierEntry().forEach((entry) -> {
            this.providingAttributesList.add(entry.getKey(), entry.getValue(), ATTRIBUTE_ICONS.get(entry.getKey()));
        });
    }

    @Override
    protected void init() {
        Optional<SkillContainer> skillContainer = this.playerpatch.getSkillContainerFor(this.skill);
        Optional<SkillContainer> priorSkillContainer = this.playerpatch.getSkillContainerFor(this.skill.getPriorSkill());

        boolean isUsing = skillContainer.isPresent();
        boolean meetsCondition = this.skill.getPriorSkill() == null || priorSkillContainer.isPresent();
        Component tooltip = CommonComponents.EMPTY;

        if (!isUsing) {
            if (meetsCondition) {
                tooltip = Component.translatable(GUI_MESSAGE_SKILL_BOOK_REPLACE, Component.translatable(this.skill.getTranslationKey()).getString());
            } else {
                tooltip = Component.translatable(GUI_MESSAGE_SKILL_BOOK_MUST_LEARN, Component.translatable(this.skill.getPriorSkill().getTranslationKey()).getString());
            }
        }

        Window window = Minecraft.getInstance().getWindow();

        if (window.getGuiScaledHeight() < 270 && window.getGuiScale() > 1.0F) {
            this.customScale = window.getGuiScale() - 1.0F;
            this.width = (int)(window.getWidth() / this.customScale);
            this.height = (int)(window.getHeight() / this.customScale);
        } else {
            this.customScale = window.getGuiScale();
        }

        String buttonMessage;

        if (isUsing) {
            buttonMessage = GUI_WIDGET_SKILL_BOOK_APPLIED;
        } else if (meetsCondition) {
            buttonMessage = GUI_WIDGET_SKILL_BOOK_LEARN;
        } else {
            buttonMessage = GUI_WIDGET_SKILL_BOOK_UNUSABLE;
        }

        this.learnButton =
            Button.builder(
                Component.translatable(buttonMessage),
                button -> {
                    Set<SkillContainer> skillContainers = this.playerpatch.getPlayerSkills().getSkillContainersFor(this.skill.getCategory());

                    if (skillContainers.size() == 1) {
                        this.acquireSkillTo(skillContainers.iterator().next());
                    } else {
                        SlotSelectScreen slotSelectScreen = new SlotSelectScreen(skillContainers, this);
                        this.minecraft.setScreen(slotSelectScreen);
                    }
                }
            )
            .bounds(this.width / 2 + 54, this.height / 2 + 90, 67, 21)
            .tooltip(Tooltip.create(tooltip, null))
            .build(LearnButton::new);

        if (isUsing || !meetsCondition) {
            this.learnButton.active = false;
        }

        if (this.hand == null) {
            this.learnButton.visible = false;
        }

        this.availableWeaponCategoryList.setX(this.width / 2 + 21);
        this.availableWeaponCategoryList.setY(this.height / 2 + 50);

        this.skillTooltipList.updateSizeAndPosition(210, (this.height + (this.availableWeaponCategoryList.availableCategories.isEmpty() ? 150 : 80)) / 2 - (this.height / 2 - 100), this.height / 2 - 100);
        this.skillTooltipList.setX(this.width / 2 - 40);

        int consumptionEndPos = this.height / 2 + 20 + (20 * Math.min(2, this.consumptionList.children().size()));

        this.consumptionList.updateSizeAndPosition(140, consumptionEndPos - this.height / 2 + 20, this.height / 2 + 20);
        this.consumptionList.setX(this.width / 2 - 160);

        this.providingAttributesList.updateSizeAndPosition(140, 60, consumptionEndPos);
        this.providingAttributesList.setX(this.width / 2 - 160);

        this.addRenderableWidget(learnButton);
        this.addRenderableWidget(this.skillTooltipList);
        this.addRenderableWidget(this.availableWeaponCategoryList);

        if (!this.consumptionList.children().isEmpty()) {
            this.addRenderableWidget(this.consumptionList);
        }

        if (!this.providingAttributesList.children().isEmpty()) {
            this.addRenderableWidget(this.providingAttributesList);
        }
    }

    protected void acquireSkillTo(SkillContainer skillContainer) {
        skillContainer.setSkill(this.skill);
        this.minecraft.setScreen(null);
        this.playerpatch.getPlayerSkills().addLearnedSkill(this.skill);
        int i = this.hand == InteractionHand.MAIN_HAND ? this.opener.getInventory().selected : 40;
        EpicFightNetworkManager.sendToServer(new CPChangeSkill(skillContainer.getSlot(), this.skill.holder(), i));
    }

    protected boolean consumesItem() {
        return true;
    }

    @Override
    public void onClose() {
        if (this.parentScreen != null) {
            this.minecraft.setScreen(this.parentScreen);
        } else {
            super.onClose();
        }
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        Window window = Minecraft.getInstance().getWindow();
        return super.mouseClicked((int)(x * window.getGuiScale() / this.customScale), (int)(y * window.getGuiScale() / this.customScale), button);
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        Window window = Minecraft.getInstance().getWindow();
        return super.mouseReleased((int)(x * window.getGuiScale() / this.customScale), (int)(y * window.getGuiScale() / this.customScale), button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        Window window = Minecraft.getInstance().getWindow();
        return super.mouseDragged((int)(mouseX * window.getGuiScale() / this.customScale), (int)(mouseY * window.getGuiScale() / this.customScale), button, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double pMouseX, double pMouseY, double xScroll, double yScroll) {
        Window window = Minecraft.getInstance().getWindow();
        return super.mouseScrolled((int)(pMouseX * window.getGuiScale() / this.customScale), (int)(pMouseY * window.getGuiScale() / this.customScale), xScroll, yScroll);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.render(guiGraphics, mouseX, mouseY, partialTicks, false);
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, boolean asBackground) {
        guiGraphics.pose().pushPose();

        Window window = Minecraft.getInstance().getWindow();
        double originalScale = window.getGuiScale();

        if (originalScale != this.customScale) {
            window.setGuiScale(this.customScale);

            //Fix: expand extra far plane distance
            Matrix4f matrix4f = (new Matrix4f()).setOrtho(0.0F, (float)((double)window.getWidth() / window.getGuiScale()), (float)((double)window.getHeight() / window.getGuiScale()), 0.0F, 1000.0F, ClientHooks.getGuiFarPlane());
            RenderSystem.setProjectionMatrix(matrix4f, VertexSorting.ORTHOGRAPHIC_Z);

            mouseX = (int)(mouseX * (originalScale / this.customScale));
            mouseY = (int)(mouseY * (originalScale / this.customScale));
        }

        if (!asBackground) {
            this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        }

        int posX = (this.width - 284) / 2;
        int posY = (this.height - 165) / 2;

        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);

        guiGraphics.blit(SKILLBOOK_BACKGROUND, this.width / 2 - 192, this.height / 2 - 140, 384, 279, 0, 0, 256, 186, 256, 256);

        int iconStartX = 106;
        int iconStartY = 211;

        if (this.skill.getCategory() == SkillCategories.DODGE) {
            iconStartX += 9;
        } else if (this.skill.getCategory() == SkillCategories.GUARD) {
            iconStartX += 18;
        } else if (this.skill.getCategory() == SkillCategories.IDENTITY) {
            iconStartX += 27;
        } else if (this.skill.getCategory() == SkillCategories.MOVER) {
            iconStartX += 36;
        } else if (this.skill.getCategory() == SkillCategories.PASSIVE) {
            iconStartX += 45;
        }

        // skill category icon left
        guiGraphics.blit(SKILLBOOK_BACKGROUND, this.width / 2 - 160, this.height / 2 - 73, 12, 12, iconStartX, iconStartY, 9, 9, 256, 256);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(this.width / 2 - 16, this.height / 2 - 73, 0.0D);
        guiGraphics.pose().scale(-1.0F, 1.0F, 1.0F);
        RenderSystem.disableCull();
        // skill category icon right
        guiGraphics.blit(SKILLBOOK_BACKGROUND, 0, 0, 12, 12, iconStartX, iconStartY, 9, 9, 256, 256);
        RenderSystem.enableCull();
        guiGraphics.pose().popPose();

        RenderSystem.enableBlend();
        guiGraphics.blit(this.skill.getSkillTexture(), this.width / 2 - 122, this.height / 2 - 99, 68, 68, 0, 0, 128, 128, 128, 128);
        RenderSystem.disableBlend();

        String translationName = this.skill.getTranslationKey();
        String skillName = Component.translatable(translationName).getString();
        int width = this.font.width(skillName);
        guiGraphics.drawString(this.font, skillName, posX + 56 - width / 2, posY + 75, 0, false);

        String skillCategory = String.format("(%s)", this.skill.getCategory().getTranslationKey().getString());
        width = this.font.width(skillCategory);

        guiGraphics.drawString(this.font, skillCategory, posX + 56 - width / 2, posY + 90, 0, false);

        for (Renderable renderable : this.renderables) {
            renderable.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        guiGraphics.pose().popPose();

        // Recover the original projection matrix
        if (originalScale != this.customScale) {
            window.setGuiScale(originalScale);
            Matrix4f matrix4f = (new Matrix4f()).setOrtho(0.0F, (float)((double)window.getWidth() / window.getGuiScale()), (float)((double)window.getHeight() / window.getGuiScale()), 0.0F, 1000.0F, ClientHooks.getGuiFarPlane());
            RenderSystem.setProjectionMatrix(matrix4f, VertexSorting.ORTHOGRAPHIC_Z);
        }
    }

    protected class SkillTooltipList extends ObjectSelectionList<SkillTooltipList.TooltipLine> {
        public SkillTooltipList(Minecraft minecraft, int width, int height, int y, int itemHeight) {
            super(minecraft, width, height, y, itemHeight);

            this.setRenderHeader(false, 0);
        }

        @Override
        protected void renderListBackground(@NotNull GuiGraphics guiGraphics) {
        }

        @Override
        protected void renderListSeparators(@NotNull GuiGraphics guiGraphics) {
        }

        public void add(FormattedCharSequence tooltip) {
            this.addEntry(new TooltipLine(tooltip));
        }

        @Override
        protected int getScrollbarPosition() {
            return this.getRight() - 6;
        }

        private class TooltipLine extends ObjectSelectionList.Entry<SkillTooltipList.TooltipLine> {
            private final FormattedCharSequence tooltip;

            private TooltipLine(FormattedCharSequence string) {
                this.tooltip = string;
            }

            @Override
            public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isMouseOver, float partialTicks) {
                guiGraphics.drawString(SkillBookScreen.this.font, this.tooltip, left + 59, top, 0, false);
            }

            @Override
            public @NotNull Component getNarration() {
                return Component.empty();
            }
        }
    }

    protected class AvailableItemsList extends AbstractWidget {
        private static final float ICON_LENGTH = 21.25F;
        private final List<WeaponCategory> availableCategories = new ArrayList<>();
        private int startIdx;
        private int size;

        private AvailableItemsList(int x, int y) {
            super(x, y, 0, 0, Component.translatable(GUI_MESSAGE_SKILL_BOOK_AVAILABLE_WEAPON_TYPES));

            this.width = 0;
            this.height = 28;
        }

        public void addWeaponCategory(WeaponCategory weaopnCategory) {
            this.availableCategories.add(weaopnCategory);
            this.width += (int)ICON_LENGTH;
            this.size++;
        }

        @Override
        protected void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
            if (this.availableCategories.isEmpty()) {
                return;
            }

            float x = this.getX() + 3;
            int y = this.getY() + 13;
            boolean updatedTooltip = false;

            guiGraphics.blit(SKILLBOOK_BACKGROUND, SkillBookScreen.this.width / 2 + 20, SkillBookScreen.this.height / 2 + 60, 130, 22, 0, 223, 104, 17, 256, 256);

            int displayedCount = 0;

            for (int i = this.startIdx; i < this.size; i++) {
                if (displayedCount > 5) {
                    break;
                }

                WeaponCategory category = this.availableCategories.get(i);

                if (WEAPON_CATEGORY_ICONS.containsKey(category)) {
                    guiGraphics.renderItem(WEAPON_CATEGORY_ICONS.get(category), (int)x, y);
                }

                if (mouseX >= x && mouseX <= x + ICON_LENGTH && mouseY >= y && mouseY <= y + ICON_LENGTH) {
                    this.setTooltip(Tooltip.create(category.getTranslatable()));
                    updatedTooltip = true;
                }

                x += ICON_LENGTH;
                displayedCount++;
            }

            if (!updatedTooltip) {
                this.setTooltip(null);
            }

            if (this.availableCategories.size() > 5) {
                int x1 = SkillBookScreen.this.width / 2 + 20;
                int y1 = SkillBookScreen.this.height / 2 + 83;
                int scrollSize = (int)(130 * (6.0D / this.size));
                int scrollStart = this.startIdx * (130 - scrollSize) + 1;

                guiGraphics.fill(x1, y1, x1 + 130, y1 + 4, 0xFFE3D6B6);
                guiGraphics.fill(x1 + scrollStart, y1 + 1, x1 + scrollStart + scrollSize - 2, y1 + 3, 0xFFC2B79C);
            }

            guiGraphics.drawString(SkillBookScreen.this.font, this.getMessage(), this.getX(), this.getY(), 0, false);
        }

        @Override
        public boolean mouseScrolled(double x, double y, double xScroll, double yScroll) {
            if (this.isMouseOver(x, y) && this.size > 6) {
                this.startIdx = Mth.clamp((int)(this.startIdx - yScroll), 0, this.size - 6);
                return true;
            }

            return false;
        }

        @Override
        protected void updateWidgetNarration(@NotNull NarrationElementOutput narrationElementOutput) {
        }
    }

    public class AttributeIconList extends ContainerObjectSelectionList<AttributeIconList.ProvidingAttributeEntry> {
        public AttributeIconList(Minecraft minecraft, int width, int height, int y, int itemHeight) {
            super(minecraft, width, height, y, itemHeight);

            this.setRenderHeader(false, 0);
        }

        @Override
        protected void renderListBackground(@NotNull GuiGraphics guiGraphics) {
        }

        @Override
        protected void renderListSeparators(@NotNull GuiGraphics guiGraphics) {
        }

        public void add(Holder<Attribute> attribute, AttributeModifier ability, TextureInfo textureInfo) {
            this.addEntry(new ProvidingAttributeEntry(attribute, ability, textureInfo));
        }

        public void add(Component tooltip, Component descriptor, TextureInfo textureInfo) {
            this.addEntry(new ProvidingAttributeEntry(tooltip, descriptor, textureInfo));
        }

        @Override
        protected int getScrollbarPosition() {
            return this.getRight() - 6;
        }

        @Override
        public int getRowLeft() {
            return this.getX() + 2;
        }

        private class ProvidingAttributeEntry extends ContainerObjectSelectionList.Entry<AttributeIconList.ProvidingAttributeEntry> {
            private final List<AbstractWidget> icons = new ArrayList<>();

            private ProvidingAttributeEntry(Holder<Attribute> attribute, AttributeModifier ability, TextureInfo textureInfo) {
                String amountString = "";
                String operator = "+";
                double amount = ability.amount();

                if (amount < 0) {
                    operator = "-";
                    amount = Math.abs(amount);
                }

                switch (ability.operation()) {
                case ADD_VALUE -> amountString = ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(amount);
                case ADD_MULTIPLIED_BASE, ADD_MULTIPLIED_TOTAL -> amountString = ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(amount * 100.0D) + "%";
                }

                this.icons.add(new AttributeIcon(0, 0, 12, 12, attribute, textureInfo));

                Static abilityString = new Static(SkillBookScreen.this, 0, 140, 0, 15, null, null, Component.literal(operator + amountString + " " + Component.translatable(attribute.value().getDescriptionId()).getString()));
                abilityString.setColor(0, 0, 0);

                this.icons.add(abilityString);
            }

            private ProvidingAttributeEntry(Component customTooltip, Component customDescription, TextureInfo textureInfo) {
                this.icons.add(new AttributeIcon(0, 0, 12, 12, customTooltip, textureInfo));

                Static abilityString = new Static(SkillBookScreen.this, 0, 140, 0, 15, null, null, customDescription);
                abilityString.setColor(0, 0, 0);

                this.icons.add(abilityString);
            }

            @Override
            public void render(@NotNull GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isMouseOver, float partialTicks) {
                int widgetLeft = left;

                for (AbstractWidget widget : this.icons) {
                    widget.setPosition(widgetLeft, top);
                    widget.render(guiGraphics, mouseX, mouseY, partialTicks);

                    widgetLeft += widget.getWidth() + 4;
                }
            }

            @Override
            public @NotNull List<? extends GuiEventListener> children() {
                return this.icons;
            }

            @Override
            public @NotNull List<? extends NarratableEntry> narratables() {
                return this.icons;
            }
        }
    }

    private static class AttributeIcon extends AbstractWidget {
        private final TextureInfo textureInfo;

        public AttributeIcon(int x, int y, int width, int height, Holder<Attribute> attribute, TextureInfo textureInfo) {
            super(x, y, width, height, Component.translatable(attribute.value().getDescriptionId() + ".skillbook_tooltip"));
            this.textureInfo = textureInfo;
        }

        public AttributeIcon(int x, int y, int width, int height, Component customTooltip, TextureInfo textureInfo) {
            super(x, y, width, height, customTooltip);
            this.textureInfo = textureInfo;
        }

        @Override
        protected void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
            if (this.textureInfo != null) {
                guiGraphics.blit(this.textureInfo.resourceLocation, this.getX(), this.getY(), this.getWidth(), this.getHeight(), this.textureInfo.u, this.textureInfo.v, this.textureInfo.width, this.textureInfo.height, 256, 256);
            }

            if (this.isHovered()) {
                this.setTooltip(this.getMessage() == null ? null : Tooltip.create(this.getMessage()));
            } else {
                this.setTooltip(null);
            }
        }

        @Override
        protected void updateWidgetNarration(@NotNull NarrationElementOutput narrationElementOutput) {
        }
    }

    public record TextureInfo(ResourceLocation resourceLocation, int u, int v, int width, int height) {
    }

    private static class LearnButton extends Button {
        protected static final WidgetSprites SPRITES = new WidgetSprites(
            EpicFightMod.identifier("widget/skillbook_button"),
            EpicFightMod.identifier("widget/skillbook_button_disabled"),
            EpicFightMod.identifier("widget/skillbook_button_highlighted")
        );

        protected LearnButton(Builder builder) {
            super(builder);
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
            Minecraft minecraft = Minecraft.getInstance();
            guiGraphics.setColor(1.0F, 1.0F, 1.0F, this.alpha);
            RenderSystem.enableBlend();
            RenderSystem.enableDepthTest();

            guiGraphics.pose().pushPose();
            guiGraphics.blitSprite(SPRITES.get(this.active, this.isHoveredOrFocused()), this.getX(), this.getY(), this.getWidth(), this.getHeight());
            guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            guiGraphics.pose().popPose();

            int i = this.getFGColor();
            this.renderString(guiGraphics, minecraft.font, i | Mth.ceil(this.alpha * 255.0F) << 24);
        }
    }
}