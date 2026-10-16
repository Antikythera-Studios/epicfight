package yesman.epicfight.client.gui.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.NotNull;
import yesman.epicfight.api.data.reloader.SkillReloadListener;
import yesman.epicfight.generated.LangKeys;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.network.EpicFightNetworkManager;
import yesman.epicfight.network.client.CPChangeSkill;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillCategories;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillSlot;
import yesman.epicfight.skill.SkillSlots;
import yesman.epicfight.world.capabilities.skill.PlayerSkills;
import yesman.epicfight.world.gamerule.EpicFightGameRules;

import java.util.*;
import java.util.stream.Stream;

public class SkillEditScreen extends Screen {
    public static final ResourceLocation EMPTY_SKILL_SLOT_ICON = EpicFightMod.identifier("textures/gui/empty.png");
    public static final ResourceLocation SCROLL_ARROW_UP = EpicFightMod.identifier("textures/gui/scroll_arrow_up.png");
    public static final ResourceLocation SCROLL_ARROW_DOWN = EpicFightMod.identifier("textures/gui/scroll_arrow_down.png");

    private static final ResourceLocation SKILL_EDIT_UI = EpicFightMod.identifier("textures/gui/screen/skill_edit.png");
    private static final MutableComponent NO_SKILLS = Component.translatable(LangKeys.GUI_MESSAGE_SKILL_EDIT_NO_SKILLS);

    private static final int MAX_SKILL_OPTIONS_ROWS = 6;
    private static final int MAX_SLOT_ROWS = 9;
    private static final int STRIDE = SlotButton.SIZE;

    private final Player player;
    private final PlayerSkills skills;
    private final Map<SkillSlot, SlotButton> slotButtons = new LinkedHashMap<> ();
    private final List<EquipSkillButton> equipSkillButtons = new ArrayList<> ();

    private ScrollArrow up;
    private ScrollArrow down;
    private SlotButton selectedSlotButton;
    private int start;
    private int maxScroll;
    private int scroll = 0;

    public SkillEditScreen(Player player, PlayerSkills skills) {
        super(Component.translatable(LangKeys.GUI_TITLE_SKILL_EDIT));
        this.player = player;
        this.skills = skills;
    }

    @Override
    public void init() {
        this.slotButtons.clear();
        this.equipSkillButtons.clear();
        this.up = null;
        this.down = null;
        this.maxScroll = Math.max(SkillSlot.ENUM_MANAGER.universalValues().stream().filter(skillSlot -> skillSlot.category().learnable()).toList().size() - MAX_SLOT_ROWS, 0);

        if (this.maxScroll > 0) {
            this.up = new ScrollArrow(this.width / 2 - 95, this.height / 2 - 114, 16, 16, button -> this.scrollUp(), true);
            this.down = new ScrollArrow(this.width / 2 - 95, this.height / 2 + 98, 16, 16, button -> this.scrollDown(), false);

            this.addRenderableWidget(this.up);
            this.addRenderableWidget(this.down);
        }

        int left = this.width / 2 - 96;
        int top = this.height / 2 - 82;

        boolean mergedPassiveCreated = false;

        for (SkillSlot skillSlot : SkillSlot.ENUM_MANAGER.universalValues()) {
            if ((this.player.isCreative() || !this.skills.getSkillContainersFor(skillSlot.category()).isEmpty()) && skillSlot.category().learnable()) {
                // The vanilla EF passive slots collapse into a single merged button.
                // Addon-defined SkillSlot implementations whose category() is PASSIVE keep their
                // own individual buttons, preserving the legacy behavior addons expect.
                boolean isCorePassive = skillSlot.category() == SkillCategories.PASSIVE && skillSlot instanceof SkillSlots;

                if (isCorePassive) {
                    if (mergedPassiveCreated) {
                        // already represented by the merged button
                        continue;
                    }
                    mergedPassiveCreated = true;

                    SlotButton mergedButton =
                        new MergedPassiveSlotButton(
                            left,
                            top,
                            button -> this.populatePassiveEquipList((SlotButton) button)
                        );

                    this.slotButtons.put(skillSlot, mergedButton);
                    this.addRenderableWidget(mergedButton);
                    top += STRIDE;

                    continue;
                }

                SkillContainer skillContainer = this.skills.getSkillContainerFor(skillSlot);

                SlotButton slotButton =
                    new SlotButton(
                        left,
                        top,
                        skillContainer,
                        button -> {
                            this.start = 0;

                            for (Button shownButton : this.equipSkillButtons) {
                                this.children().remove(shownButton);
                            }

                            this.equipSkillButtons.clear();
                            int k = this.width / 2 - 69;

                            MutableInt widgetHeight = new MutableInt(this.height / 2 - 78);
                            Stream<Skill> learnedSkill =
                                this.player.isCreative() ?
                                    SkillReloadListener.getSkills(skill -> skill.getCategory() == skillSlot.category()).stream() :
                                    this.skills.listAcquiredSkills().filter(skill -> skill.getCategory() == skillSlot.category());

                            learnedSkill.forEach(skill -> {
                                // Add skills that are included in this category
                                this.equipSkillButtons.add(
                                    new EquipSkillButton(
                                        k,
                                        widgetHeight.intValue(),
                                        147,
                                        24,
                                        skill,
                                        Component.translatable(skill.getTranslationKey()),
                                        replaceSkillButton -> {
                                            if (!this.isButtonVisible(replaceSkillButton)) {
                                                return;
                                            }

                                            skillContainer.setSkill(skill);
                                            EpicFightNetworkManager.sendToServer(new CPChangeSkill(skillSlot, skill.holder(), -1));
                                            this.skills.addLearnedSkill(skill);

                                            // Update active flags in place; rebuilding the list mid-click would invalidate
                                            // the button reference vanilla still holds for setFocused() and crash maybeScroll().
                                            this.refreshEquipSkillButtonStates();
                                        }
                                    )
                                    .setActive(this.skills.getSkillContainer(skill) == null)
                                );

                                widgetHeight.add(EquipSkillButton.SPACING);
                            });

                            for (Button shownButton : this.equipSkillButtons) {
                                this.addRenderableWidget(shownButton);
                            }

                            this.selectedSlotButton = (SlotButton)button;
                        },
                        Component.translatable(SkillSlot.ENUM_MANAGER.toTranslated(skillSlot))
                    );

                this.slotButtons.put(skillSlot, slotButton);
                this.addRenderableWidget(slotButton);
                top+=STRIDE;
            }

            this.scroll = 0;
            this.setScrollVisibilities();
        }

        if (this.selectedSlotButton != null) {
            this.selectedSlotButton = this.slotButtons.get(this.selectedSlotButton.skillContainer.getSlot());
            this.selectedSlotButton.onPress();
        }
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        if (this.canScroll()) {
            int scrollPosition = (int)(140 * (this.start / (float)(this.equipSkillButtons.size() - MAX_SKILL_OPTIONS_ROWS)));
            guiGraphics.blit(SKILL_EDIT_UI, this.width / 2 + 80, this.height / 2 - 80 + scrollPosition, 12, 15, 231, 2, 12, 15, 256, 256);
        }

        int maxShowingButtons = Math.min(this.equipSkillButtons.size(), MAX_SKILL_OPTIONS_ROWS);

        for (int i = this.start; i < maxShowingButtons + this.start; ++i) {
            this.equipSkillButtons.get(i).render(guiGraphics, mouseX, mouseY, partialTick);
        }

        for (SlotButton sb : this.slotButtons.values()) {
            sb.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        if (this.up != null) this.up.render(guiGraphics, mouseX, mouseY, partialTick);
        if (this.down != null) this.down.render(guiGraphics, mouseX, mouseY, partialTick);

        if (this.slotButtons.isEmpty()) {
            int lineHeight = 0;

            for (FormattedCharSequence s : this.font.split(NO_SKILLS, 140)) {
                guiGraphics.drawString(this.font, s, this.width / 2 - 65, this.height / 2 - 72 + lineHeight, 3158064, false);

                lineHeight += 10;
            }
        }
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderTransparentBackground(guiGraphics);
        guiGraphics.blit(SKILL_EDIT_UI, this.width / 2 - 104, this.height / 2 - 100, 0, 0, 208, 200);
    }

    private boolean canScroll() {
        return this.equipSkillButtons.size() > MAX_SKILL_OPTIONS_ROWS;
    }

    private void refreshEquipSkillButtonStates() {
        for (EquipSkillButton button : this.equipSkillButtons) {
            button.setActive(this.skills.getSkillContainer(button.skill) == null);
        }
    }

    /// Populates the equip-skill list for the merged Passive slot. Equipping a skill from this
    /// list goes to the first empty `SkillSlots` passive container and is rejected once the
    /// `maxPassiveSkills` game rule limit is reached.
    private void populatePassiveEquipList(SlotButton clickedButton) {
        this.start = 0;

        for (Button shownButton : this.equipSkillButtons) {
            this.children().remove(shownButton);
        }

        this.equipSkillButtons.clear();
        int k = this.width / 2 - 69;

        MutableInt widgetHeight = new MutableInt(this.height / 2 - 78);
        Stream<Skill> learnedSkill =
            this.player.isCreative() ?
                SkillReloadListener.getSkills(skill -> skill.getCategory() == SkillCategories.PASSIVE).stream() :
                this.skills.listAcquiredSkills().filter(skill -> skill.getCategory() == SkillCategories.PASSIVE);

        learnedSkill.forEach(skill -> {
            this.equipSkillButtons.add(
                new EquipSkillButton(
                    k,
                    widgetHeight.intValue(),
                    147,
                    24,
                    skill,
                    Component.translatable(skill.getTranslationKey()),
                    replaceSkillButton -> {
                        if (!this.isButtonVisible(replaceSkillButton)) {
                            return;
                        }
                        this.equipPassive(skill);
                    }
                )
                .setActive(this.skills.getSkillContainer(skill) == null)
            );

            widgetHeight.add(EquipSkillButton.SPACING);
        });

        for (Button shownButton : this.equipSkillButtons) {
            this.addRenderableWidget(shownButton);
        }

        this.selectedSlotButton = clickedButton;
    }

    /// Equips a passive skill into the first empty core passive slot, respecting the
    /// `maxPassiveSkills` game rule. No-op when the limit is reached or no slot is free.
    private void equipPassive(Skill skill) {
        if (this.countEquippedCorePassives() >= this.maxPassiveSkillsRule()) {
            return;
        }

        SkillSlot targetSlot = this.findFirstEmptyCorePassiveSlot();

        if (targetSlot == null) {
            return;
        }

        SkillContainer container = this.skills.getSkillContainerFor(targetSlot);
        container.setSkill(skill);
        EpicFightNetworkManager.sendToServer(new CPChangeSkill(targetSlot, skill.holder(), -1));
        this.skills.addLearnedSkill(skill);

        // In-place refresh; same reason as the per-slot equip path -- avoids invalidating the button
        // reference vanilla still holds for setFocused() after this lambda returns.
        this.refreshEquipSkillButtonStates();
    }

    private int countEquippedCorePassives() {
        int count = 0;

        for (SkillSlot slot : SkillSlot.ENUM_MANAGER.universalValues()) {
            if (slot.category() == SkillCategories.PASSIVE && slot instanceof SkillSlots) {
                if (!this.skills.getSkillContainerFor(slot).isEmpty()) {
                    count++;
                }
            }
        }

        return count;
    }

    private SkillSlot findFirstEmptyCorePassiveSlot() {
        for (SkillSlot slot : SkillSlot.ENUM_MANAGER.universalValues()) {
            if (slot.category() == SkillCategories.PASSIVE && slot instanceof SkillSlots) {
                SkillContainer container = this.skills.getSkillContainerFor(slot);

                if (container.isEmpty() && !container.onReplaceCooldown()) {
                    return slot;
                }
            }
        }

        return null;
    }

    private SkillContainer findFirstEquippedCorePassive() {
        for (SkillSlot slot : SkillSlot.ENUM_MANAGER.universalValues()) {
            if (slot.category() == SkillCategories.PASSIVE && slot instanceof SkillSlots) {
                SkillContainer container = this.skills.getSkillContainerFor(slot);

                if (!container.isEmpty()) {
                    return container;
                }
            }
        }

        return null;
    }

    private int maxPassiveSkillsRule() {
        return EpicFightGameRules.MAX_PASSIVE_SKILLS.getRuleValue(this.player.level());
    }

    private boolean isButtonVisible(Button button) {
        int buttonOrder = this.equipSkillButtons.indexOf(button);
        return buttonOrder >= this.start && buttonOrder <= this.start + MAX_SKILL_OPTIONS_ROWS;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double xScroll, double yScroll) {
        int left = this.width / 2 - 96;
        int top = this.height / 2 - 82;

        if (
            left <= mouseX &&
            top <= mouseY &&
            left + STRIDE >= mouseX &&
            top + MAX_SLOT_ROWS * STRIDE >= mouseY
        ) {
            if (yScroll > 0.0F) this.scrollUp();
            else this.scrollDown();
            return true;
        } else {
            if (!this.canScroll()) {
                return false;
            } else {
                if (yScroll > 0.0F) {
                    if (this.start > 0) {
                        --this.start;

                        for (Button button : this.equipSkillButtons) {
                            button.setY(button.getY() + EquipSkillButton.SPACING);
                        }

                        return true;
                    }
                } else {
                    if (this.start < (this.equipSkillButtons.size() - MAX_SKILL_OPTIONS_ROWS)) {
                        ++this.start;

                        for (Button button : this.equipSkillButtons) {
                            button.setY(button.getY() - EquipSkillButton.SPACING);
                        }

                        return true;
                    }
                }

                return false;
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    protected void scrollUp() {
        int nextScroll = Mth.clamp(this.scroll - 1, 0, this.maxScroll);

        if (this.scroll != nextScroll) {
            this.scroll = nextScroll;
            this.slotButtons.values().forEach(button -> button.setY(button.getY() + STRIDE));
            this.setScrollVisibilities();
        }
    }

    protected void scrollDown() {
        int nextScroll = Mth.clamp(this.scroll + 1, 0, this.maxScroll);

        if (this.scroll != nextScroll) {
            this.scroll = nextScroll;
            this.slotButtons.values().forEach(button -> button.setY(button.getY() - STRIDE));
            this.setScrollVisibilities();
        }
    }

    protected void setScrollVisibilities() {
        int i = 0;

        for (SlotButton slotButton : this.slotButtons.values()) {
            slotButton.visible = i >= this.scroll && i < this.scroll + MAX_SLOT_ROWS;
            i++;
        }
    }

    class SlotButton extends Button {
        private static final int SIZE = 18;
        private final SkillContainer skillContainer;
        private final Component slotExplanation;

        public SlotButton(int x, int y, SkillContainer skillContainer, OnPress pressedAction, Component tooltipMessage) {
            super(x, y, SIZE, SIZE, Component.empty(), pressedAction, Button.DEFAULT_NARRATION);

            this.skillContainer = skillContainer;
            this.slotExplanation = tooltipMessage;
            this.setTooltip(Tooltip.create(this.slotExplanation));
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            this.active = !this.skillContainer.onReplaceCooldown();
            int y = ((this.isHoveredOrFocused() || selectedSlotButton == this) && !this.skillContainer.onReplaceCooldown()) ? 35 : 17;
            guiGraphics.blit(SKILL_EDIT_UI, this.getX(), this.getY(), 237, y, this.width, this.height);

            if (!this.skillContainer.isEmpty()) {
                RenderSystem.enableBlend();
                guiGraphics.blit(this.skillContainer.getSkill().getSkillTexture(), this.getX() + 1, this.getY() + 1, this.getWidth() - 2, this.getHeight() - 2, 0, 0, 128, 128, 128, 128);
                RenderSystem.disableBlend();
            } else {
                guiGraphics.blit(EMPTY_SKILL_SLOT_ICON, this.getX() + 1, this.getY() + 1, this.getWidth() - 2, this.getHeight() - 2, 0, 0, 128, 128, 128, 128);
            }

            if (this.skillContainer.onReplaceCooldown()) {
                int maxCooldown = EpicFightGameRules.SKILL_REPLACE_COOLDOWN.getRuleValue(player.level());
                float lerp = Mth.clampedLerp(0.0F, 16.0F, 1.0F - (float)this.skillContainer.getReplaceCooldown() / maxCooldown);
                guiGraphics.fill(this.getX() + 1, this.getY() + 1 + (int)lerp, this.getX() + 17, this.getY() + 17, 0x78000000);

                if (this.isHoveredOrFocused()) {
                    this.setTooltip(Tooltip.create(Component.translatable(LangKeys.GUI_MESSAGE_SELECT_SKILL_CONTAINER_ON_COOLDOWN, this.skillContainer.getReplaceCooldown() / 20)));
                }
            } else {
                this.setTooltip(Tooltip.create(this.slotExplanation));
            }
        }

        @Override
        public void setFocused(boolean focused) {
            super.setFocused(focused);

            // Supports key arrow navigation. Skip on unfocus to avoid acting on stale callbacks.
            if (focused) {
                maybeScroll();
            }
        }

        private void maybeScroll() {
            if (SkillEditScreen.this.maxScroll == 0) {
                return;
            }
            final int scroll = SkillEditScreen.this.scroll;

            final int index = slotButtons.values().stream().toList().indexOf(this);

            if (index < 0) {
                return;
            }

            final int relativeIndex = index - scroll;

            final boolean needsScrollDown = relativeIndex >= (MAX_SLOT_ROWS - 1);
            final boolean needsScrollTop = relativeIndex == 0;

            if (needsScrollDown || needsScrollTop) {
                if (needsScrollDown) {
                    scrollDown();
                } else {
                    scrollUp();
                }
            }
        }
    }

    /// Single slot button that represents the entire core (`SkillSlots`) PASSIVE category.
    /// Visually shows the icon of the first equipped passive (or the empty icon when none),
    /// and renders a `<count>/<max>` tooltip. Forwards into [#equipPassive] when a skill from
    /// the equip-list is clicked.
    ///
    /// We pass `PASSIVE1`'s container to the parent constructor so the existing logic that
    /// looks up a slot button by `selectedSlotButton.skillContainer.getSlot()` keeps working.
    class MergedPassiveSlotButton extends SlotButton {
        public MergedPassiveSlotButton(int x, int y, OnPress pressedAction) {
            super(
                x,
                y,
                SkillEditScreen.this.skills.getSkillContainerFor(SkillSlots.PASSIVE1),
                pressedAction,
                SkillCategories.PASSIVE.getTranslationKey().copy()
            );
        }

        @Override
        protected void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            this.active = true;
            int yTex = (this.isHoveredOrFocused() || selectedSlotButton == this) ? 35 : 17;
            guiGraphics.blit(SKILL_EDIT_UI, this.getX(), this.getY(), 237, yTex, this.width, this.height);

            SkillContainer firstEquipped = SkillEditScreen.this.findFirstEquippedCorePassive();

            if (firstEquipped != null && firstEquipped.getSkill() != null) {
                RenderSystem.enableBlend();
                guiGraphics.blit(firstEquipped.getSkill().getSkillTexture(), this.getX() + 1, this.getY() + 1, this.getWidth() - 2, this.getHeight() - 2, 0, 0, 128, 128, 128, 128);
                RenderSystem.disableBlend();
            } else {
                guiGraphics.blit(EMPTY_SKILL_SLOT_ICON, this.getX() + 1, this.getY() + 1, this.getWidth() - 2, this.getHeight() - 2, 0, 0, 128, 128, 128, 128);
            }

            int equipped = SkillEditScreen.this.countEquippedCorePassives();
            int max = SkillEditScreen.this.maxPassiveSkillsRule();
            MutableComponent tip = SkillCategories.PASSIVE.getTranslationKey().copy().append(" (" + equipped + "/" + max + ")");
            this.setTooltip(Tooltip.create(tip));
        }
    }

    class ScrollArrow extends Button {
        final boolean up;

        protected ScrollArrow(int x, int y, int width, int height, Button.OnPress onPress, boolean up) {
            super(x, y, width, height, Component.empty(), onPress, Button.DEFAULT_NARRATION);
            this.up = up;
        }

        @Override
        protected void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            if (this.up && scroll != 0) guiGraphics.blit(SCROLL_ARROW_UP, this.getX(), this.getY(), this.width, this.height, 0, 0, 16, 16, 16, 16);
            else if (!this.up && scroll != maxScroll) guiGraphics.blit(SCROLL_ARROW_DOWN, this.getX(), this.getY(), this.width, this.height, 0, 0, 16, 16, 16, 16);
        }

        @Override
        protected boolean clicked(double mouseX, double mouseY) {
            return super.clicked(mouseX, mouseY) && (this.up && scroll != 0 || !this.up && scroll != maxScroll);
        }
    }

    public class EquipSkillButton extends Button {
        private static final int SPACING = 26;
        private static final int DEEQUIP_BUTTON_SIZE = 9;
        private static final int DEEQUIP_BUTTON_MARGIN = 3;

        private final Skill skill;

        public EquipSkillButton(int x, int y, int width, int height, Skill skill, Component title, OnPress pressedAction) {
            super(x, y, width, height, title, pressedAction, Button.DEFAULT_NARRATION);
            this.skill = skill;
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            this.isHovered = mouseX >= this.getX() && mouseY >= this.getY() && mouseX < this.getX() + this.width && mouseY < this.getY() + this.height;
            int texY = (this.isHoveredOrFocused() || !this.active) ? 224 : 200;
            guiGraphics.blit(SKILL_EDIT_UI, this.getX(), this.getY(), 0, texY, this.width, this.height);

            RenderSystem.enableBlend();
            guiGraphics.blit(this.skill.getSkillTexture(), this.getX() + 5, this.getY() + 4, 16, 16, 0, 0, 128, 128, 128, 128);
            guiGraphics.drawString(font, this.getMessage(), this.getX() + SPACING, this.getY() + 2, -1, false);

            if (!this.active) {
                guiGraphics.drawString(font, Component.literal(skills.getSkillContainer(this.skill).getSlot().toString().toLowerCase(Locale.ROOT)), this.getX() + EquipSkillButton.SPACING, this.getY() + 12, 16736352, false);

                if (this.isHovered) {
                    int dx = this.getDeequipButtonX();
                    int dy = this.getDeequipButtonY();
                    boolean overX = this.isMouseOverDeequip(mouseX, mouseY);
                    int bg = overX ? 0xFFAA0000 : 0x88000000;
                    guiGraphics.fill(dx, dy, dx + DEEQUIP_BUTTON_SIZE, dy + DEEQUIP_BUTTON_SIZE, bg);

                    // Pixel-perfect centered X. Two 1px diagonals crossing at the box center.
                    int cx = dx + DEEQUIP_BUTTON_SIZE / 2;
                    int cy = dy + DEEQUIP_BUTTON_SIZE / 2;
                    int armLen = 2;
                    int xColor = 0xFFFFFFFF;
                    for (int o = -armLen; o <= armLen; o++) {
                        guiGraphics.fill(cx + o, cy + o, cx + o + 1, cy + o + 1, xColor);
                        guiGraphics.fill(cx + o, cy - o, cx + o + 1, cy - o + 1, xColor);
                    }
                }
            }
        }

        @Override
        public boolean mouseClicked(double x, double y, int pressType) {
            if (this.visible && pressType == 0 && !this.active && this.isMouseOverDeequip(x, y)) {
                this.deequipSkill();
                return true;
            }

            if (this.visible && pressType == 1) {
                boolean flag = this.clickedNoCountActive(x, y);

                if (flag) {
                    openSkillInfoScreen();
                    return true;
                }
            }

            return super.mouseClicked(x, y, pressType);
        }

        private int getDeequipButtonX() {
            return this.getX() + this.width - DEEQUIP_BUTTON_SIZE - DEEQUIP_BUTTON_MARGIN;
        }

        private int getDeequipButtonY() {
            return this.getY() + DEEQUIP_BUTTON_MARGIN;
        }

        private boolean isMouseOverDeequip(double mouseX, double mouseY) {
            int dx = this.getDeequipButtonX();
            int dy = this.getDeequipButtonY();
            return mouseX >= dx && mouseX < dx + DEEQUIP_BUTTON_SIZE && mouseY >= dy && mouseY < dy + DEEQUIP_BUTTON_SIZE;
        }

        private void deequipSkill() {
            SkillContainer container = skills.getSkillContainer(this.skill);

            if (container == null) {
                return;
            }

            this.playDownSound(Minecraft.getInstance().getSoundManager());
            container.setSkill(null);
            EpicFightNetworkManager.sendToServer(new CPChangeSkill(container.getSlot(), null, -1));

            // In-place refresh: see comment on the equip path for why we don't call selectedSlotButton.onPress().
            SkillEditScreen.this.refreshEquipSkillButtonStates();
        }

        public void openSkillInfoScreen() {
            this.playDownSound(Minecraft.getInstance().getSoundManager());
            minecraft.setScreen(new SkillBookScreen(player, this.skill, null, SkillEditScreen.this));
        }

        @Override
        public void setFocused(boolean focused) {
            super.setFocused(focused);

            // Supports key arrow navigation. Skip on unfocus (and on stale callbacks
            // from removed buttons) to avoid corrupting `start` with -1.
            if (focused) {
                maybeScroll();
            }
        }

        private void maybeScroll() {
            final List<EquipSkillButton> buttons = SkillEditScreen.this.equipSkillButtons;
            final int start = SkillEditScreen.this.start;
            final int maxRows = MAX_SKILL_OPTIONS_ROWS;

            final int i = buttons.indexOf(this);

            // This button has already been removed from the list (e.g. category switch
            // rebuilt the equip buttons). Don't touch scroll — `start = -1` would crash render().
            if (i < 0) {
                return;
            }

            final boolean isOutsideVisibleRowsAtBottom = i >= start + maxRows;
            final boolean isOutsideVisibleRowsAtTop = i < start;

            if (isOutsideVisibleRowsAtBottom || isOutsideVisibleRowsAtTop) {
                int nextStart = (isOutsideVisibleRowsAtBottom) ? Math.max(0, i - maxRows + 1) : i;
                int diff = (start - nextStart);

                for (Button button : buttons) {
                    button.setY(button.getY() + EquipSkillButton.SPACING * diff);
                }

                SkillEditScreen.this.start = nextStart;
            }
        }

        protected boolean clickedNoCountActive(double x, double y) {
            return this.visible && x >= (double) this.getX() && y >= (double) this.getY() && x < (double) (this.getX() + this.width) && y < (double) (this.getY() + this.height);
        }

        public EquipSkillButton setActive(boolean active) {
            this.active = active;
            return this;
        }
    }
}