package com.pepgames.moblist;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class HudConfigScreen extends Screen {
    private final Screen parent;
    private float dragOffsetX, dragOffsetY;
    private boolean dragging = false;
    private ButtonWidget anchorButton;
    private ButtonWidget babyToggleButton;
    private ConfigSliderWidget scaleSlider;

    public HudConfigScreen(Screen parent) {
        super(Text.literal("HUD Config"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = width / 2;
        int y = height - 60;

        anchorButton = ButtonWidget.builder(Text.literal("Anchor: " + MobListClient.anchor.name()), button -> {
            // Cycle to the next anchor
            MobListClient.anchor = MobListClient.Anchor.values()[(MobListClient.anchor.ordinal() + 1) % MobListClient.Anchor.values().length];
            button.setMessage(Text.literal("Anchor: " + MobListClient.anchor.name()));

            // Reset offset to a sane default for new anchor
            MobListClient.hudX = 0.05f;
            MobListClient.hudY = 0.05f;

            // Clamp it immediately so it's visible
            MobListClient.clampHudToScreen(client.getWindow().getScaledWidth(), client.getWindow().getScaledHeight());
        }).dimensions(centerX - 100, y, 200, 20).build();
        addDrawableChild(anchorButton);

        babyToggleButton = ButtonWidget.builder(Text.literal("Separate Babies: " + MobListClient.separateBabies), button -> {
            MobListClient.separateBabies = !MobListClient.separateBabies;
            button.setMessage(Text.literal("Separate Babies: " + MobListClient.separateBabies));
        }).dimensions(centerX - 100, y - 30, 200, 20).build();
        addDrawableChild(babyToggleButton);

        scaleSlider = new ConfigSliderWidget(centerX - 100, y - 60, 200, 20, MobListClient.hudScale, value -> {
            MobListClient.hudScale = (float) value;
        });
        scaleSlider.setSliderValue(MobListClient.hudScale);
        addDrawableChild(scaleSlider);

        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), button -> {
            MobListClient.clampHudToScreen(client.getWindow().getScaledWidth(), client.getWindow().getScaledHeight());
            MobListClient.saveConfig();
            client.setScreen(parent);
        }).dimensions(centerX - 100, y + 30, 200, 20).build());
    }

    private void updateButtonLabels() {
        if (anchorButton != null) {
            anchorButton.setMessage(Text.literal("Anchor: " + MobListClient.anchor.name()));
        }
        if (babyToggleButton != null) {
            babyToggleButton.setMessage(Text.literal("Separate Babies: " + MobListClient.separateBabies));
        }
        if (scaleSlider != null) {
            scaleSlider.setSliderValue(MobListClient.hudScale);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float scale = MobListClient.hudScale;
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        float px = MobListClient.getAnchoredX(screenWidth);
        float py = MobListClient.getAnchoredY(screenHeight);

        int hudWidth = 100;
        int hudHeight = 100;

        float scaledWidth = hudWidth * scale;
        float scaledHeight = hudHeight * scale;

        if (mouseX >= px && mouseX <= px + scaledWidth && mouseY >= py && mouseY <= py + scaledHeight) {
            dragging = true;
            dragOffsetX = (float) (mouseX - px);
            dragOffsetY = (float) (mouseY - py);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging) {
            int screenWidth = client.getWindow().getScaledWidth();
            int screenHeight = client.getWindow().getScaledHeight();

            float newX = (float) mouseX - dragOffsetX;
            float newY = (float) mouseY - dragOffsetY;

            MobListClient.setAnchoredOffset(newX, newY, screenWidth, screenHeight);
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        context.drawText(textRenderer, Text.literal("Drag the HUD box, then click Done."), width / 2 - 80, 10, 0xFFFFFF, false);

        float px = MobListClient.getAnchoredX(client.getWindow().getScaledWidth());
        float py = MobListClient.getAnchoredY(client.getWindow().getScaledHeight());

        MatrixStack matrices = context.getMatrices();

        matrices.push();
        matrices.translate(px, py, 0);
        matrices.scale(MobListClient.hudScale, MobListClient.hudScale, 1);

        context.drawText(textRenderer, Text.literal("Mob List:"), 0, 0, 0xFFFFFF, false);
        int y = 12;
        Map<String, Integer> sorted = MobListClient.mobCounts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        TreeMap::new));

        for (Map.Entry<String, Integer> entry : sorted.entrySet()) {
            String raw = entry.getKey().replace("entity.minecraft.", "");
            String display = raw.replace(".baby", " (baby)") + ": " + entry.getValue();
            context.drawText(textRenderer, Text.literal(display), 0, y, 0xAAAAAA, false);
            y += 10;
        }
        matrices.pop();
        super.render(context, mouseX, mouseY, delta);
    }

    private void cycleAnchor() {
        MobListClient.anchor = MobListClient.Anchor.values()[(MobListClient.anchor.ordinal() + 1) % MobListClient.Anchor.values().length];

        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        // Center HUD according to new anchor type
        float hudCenterX = screenWidth / 2f;
        float hudCenterY = screenHeight / 2f;
        MobListClient.setAnchoredOffset(hudCenterX, hudCenterY, screenWidth, screenHeight);

        MobListClient.saveConfig();
        updateButtonLabels();
    }
}
