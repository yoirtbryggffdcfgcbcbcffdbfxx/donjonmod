package com.dungeonmod.client;

import com.dungeonmod.network.OpenCyclopsShopPayload;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.glfw.GLFW;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class SubtitleOverlay {
    private static List<String> currentLines = List.of();
    private static String speakerName = "";
    private static int currentIndex = 0;
    private static boolean active = false;
    private static final int FADE_OUT = 10;
    private static int fadeTimer = 0;
    private static boolean fadingOut = false;
    private static float charIndex = 0f;
    private static int clickCooldown = 0;
    private static boolean wasEscapePressed = false;
    private static boolean hadScreen = false;
    private static boolean canOpenShopOnEnd = true;
    private static boolean wasAttackPressed = false;
    private static boolean dialogueActive = false;
    private static int prevHurtTime = 0;
    private static final int TICKS_PER_LINE = 90;

    private static final Identifier DIALOGUE_BOX = Identifier.of("dungeonmod", "textures/gui/dialogue_box.png");
    private static final Identifier BTN_NEXT = Identifier.of("dungeonmod", "textures/gui/bouton_passer_a_la_suivante.png");
    private static final Identifier BTN_SPEED = Identifier.of("dungeonmod", "textures/gui/bouton_aller_plus_vite.png");
    private static final Identifier BTN_ESCAPE = Identifier.of("dungeonmod", "textures/gui/bouton_echap.png");
    private static final int BTN_W = 38;
    private static final int BTN_H = 11;
    private static final int BOX_W = 216;
    private static final int BOX_H = 50;
    private static float boxScale = 1.4f;

    public static void init() {
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            if (!active || currentLines.isEmpty() || currentIndex >= currentLines.size()) return;
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.options.hudHidden || client.currentScreen != null) return;

            TextRenderer renderer = client.textRenderer;
            int sw = client.getWindow().getScaledWidth();
            int sh = client.getWindow().getScaledHeight();

            String line = currentLines.get(currentIndex);
            boolean isLastLine = (currentIndex >= currentLines.size() - 1);
            float alpha = 1.0f;

            if (isLastLine && fadingOut) {
                alpha = 1.0f - (float) fadeTimer / FADE_OUT;
                fadeTimer++;
                if (fadeTimer > FADE_OUT) { stop(); return; }
            }

            // Boutons HUD en haut à gauche (escape + espace)
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
            drawContext.drawTexture(RenderLayer::getGuiTextured, BTN_ESCAPE, 4, 4, 0, 0, BTN_W, BTN_H, BTN_W, BTN_H);
            drawContext.drawTexture(RenderLayer::getGuiTextured, BTN_SPEED, 4 + BTN_W + 2, 4, 0, 0, BTN_W, BTN_H, BTN_W, BTN_H);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

            drawContext.getMatrices().push();
            drawContext.getMatrices().scale(boxScale, boxScale, 1.0f);

            int nx = (int)((sw / boxScale - BOX_W) / 2);
            int ny = (int)((sh / boxScale - BOX_H - 20 / boxScale));

            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
            drawContext.drawTexture(RenderLayer::getGuiTextured, DIALOGUE_BOX, nx, ny, 0, 0, BOX_W, BOX_H, BOX_W, BOX_H);
            // Bouton suivant en bas à droite (scalé plus petit)
            drawContext.drawTexture(RenderLayer::getGuiTextured, BTN_NEXT, nx + BOX_W - 30 - 3, ny + BOX_H - 9 - 2, 0f, 0f, 30, 9, BTN_W, BTN_H, BTN_W, BTN_H);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

            int nColor = ((int)(alpha * 255) << 24) | 0xFFFFFF;
            drawContext.drawText(renderer, speakerName, nx + 36, ny + 3, nColor, false);

            String displayText = line.substring(0, Math.min((int)charIndex, line.length()));
            int tColor = ((int)(alpha * 255) << 24) | 0x3C3C3C;
            int textAreaW = BOX_W - 24;
            int textStartY = ny + 14;
            int textAreaH = (ny + BOX_H - 6) - textStartY;
            var wrapped = wrapText(renderer, displayText, textAreaW);
            int lineH = renderer.fontHeight + 2;
            int blockH = wrapped.size() * lineH;
            int ty = textStartY + (textAreaH - blockH) / 2;
            for (String wl : wrapped) {
                int tx = nx + BOX_W / 2 - renderer.getWidth(wl) / 2;
                drawContext.drawText(renderer, wl, tx, ty, tColor, false);
                ty += lineH;
            }

            drawContext.getMatrices().pop();
        });



        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (active && client.player != null) {
                // Interruption sur dégât
                if (dialogueActive && client.player.hurtTime > prevHurtTime) {
                    stop();
                    dialogueActive = false;
                    return;
                }
                prevHurtTime = client.player.hurtTime;
            } else if (!active && dialogueActive) {
                dialogueActive = false;
            }

            if (!active || currentIndex >= currentLines.size()) return;
            String line = currentLines.get(currentIndex);
            boolean isLast = (currentIndex >= currentLines.size() - 1);

            if (clickCooldown > 0) clickCooldown--;

            // Space → 2x typing speed
            boolean spaceHeld = client.options.jumpKey.isPressed();
            if ((int)charIndex < line.length()) {
                float speed = (float)line.length() / TICKS_PER_LINE;
                if (spaceHeld) speed *= 2;
                charIndex += speed;
            }

            // Échap → fermer le dialogue
            long window = client.getWindow().getHandle();
            boolean escapeNow = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_ESCAPE) == GLFW.GLFW_PRESS;
            boolean screenJustClosed = hadScreen && client.currentScreen == null;
            hadScreen = client.currentScreen != null;

            if (escapeNow && !wasEscapePressed && !screenJustClosed) {
                stop();
                wasEscapePressed = true;
                return;
            }
            if (!escapeNow) wasEscapePressed = false;

            // Clic gauche → compléter / ligne suivante / shop
            boolean attackNow = client.options.attackKey.isPressed();
            boolean leftClick = attackNow && !wasAttackPressed && clickCooldown == 0;
            wasAttackPressed = attackNow;
            if (leftClick) {
                if ((int)charIndex < line.length()) {
                    charIndex = line.length();
                } else {
                    if (isLast) {
                        stop();
                        if (canOpenShopOnEnd) ClientPlayNetworking.send(new OpenCyclopsShopPayload());
                    } else {
                        advanceLine();
                    }
                }
                clickCooldown = 3;
            }
        });
    }



    public static void setCanOpenShopOnEnd(boolean v) { canOpenShopOnEnd = v; }

    public static void showSubtitles(String name, List<String> lines, boolean openShopOnEnd) {
        if (lines.isEmpty()) { active = false; currentLines = List.of(); return; }
        if (dialogueActive) return;
        canOpenShopOnEnd = openShopOnEnd;
        wasAttackPressed = true;
        speakerName = name;
        currentLines = lines;
        currentIndex = 0;
        fadingOut = false;
        fadeTimer = 0;
        charIndex = 0f;
        clickCooldown = 5;
        wasEscapePressed = false;
        prevHurtTime = 0;
        active = true;
        dialogueActive = true;
    }

    public static boolean isActive() { return active; }
    public static boolean isDialogueActive() { return dialogueActive; }
    public static void setBoxScale(float scale) { boxScale = scale; }
    public static void stop() {
        active = false; currentLines = List.of(); dialogueActive = false;
    }

    private static List<String> wrapText(TextRenderer renderer, String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        for (String raw : text.split("\n", -1)) {
            if (raw.isEmpty()) { lines.add(""); continue; }
            if (renderer.getWidth(raw) <= maxWidth) { lines.add(raw); continue; }
            StringBuilder line = new StringBuilder();
            for (String word : raw.split(" ")) {
                String test = line.isEmpty() ? word : line + " " + word;
                if (renderer.getWidth(test) > maxWidth) {
                    if (!line.isEmpty()) lines.add(line.toString());
                    line = new StringBuilder(word);
                } else {
                    line = new StringBuilder(test);
                }
            }
            if (!line.isEmpty()) lines.add(line.toString());
        }
        return lines;
    }

    private static void advanceLine() {
        currentIndex++;
        fadingOut = false;
        fadeTimer = 0;
        charIndex = 0f;
        if (currentIndex >= currentLines.size()) {
            active = false; currentLines = List.of(); dialogueActive = false;
        }
    }
}
