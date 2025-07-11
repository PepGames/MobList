package com.pepgames.moblist;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.text.Text;
import net.minecraft.client.option.KeyBinding;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import org.lwjgl.glfw.GLFW;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.screen.Screen;
import java.util.*;

public class MobListClient implements ClientModInitializer {
	private final MinecraftClient client = MinecraftClient.getInstance();
	public static float hudX = 0.05f;
	public static float hudY = 0.05f;
	public static float hudScale = 1.0f;
	public static Anchor anchor = Anchor.TOP_LEFT;
	public static boolean separateBabies = false;
	public static final Map<String, Integer> mobCounts = new HashMap<>();
	private static final Path CONFIG_PATH = Paths.get("config", "moblist.json");
	private static final Gson GSON = new Gson();
	private KeyBinding openConfigKey;

	public enum Anchor {
		TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, CENTER
	}

	@Override
	public void onInitializeClient() {
		loadConfig();

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player == null || client.world == null) return;

			mobCounts.clear();
			for (Entity entity : client.world.getEntities()) {
				if (entity instanceof MobEntity mob) {
					double dist = mob.squaredDistanceTo(client.player);
					if (dist < 128 * 128) {
						String name = mob.getType().getTranslationKey();
						if (separateBabies && mob instanceof LivingEntity living && living.isBaby()) {
							name += ".baby";
						}
						mobCounts.merge(name, 1, Integer::sum);
					}
				}
			}
		});

		openConfigKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.moblist.config",
				GLFW.GLFW_KEY_F9,
				"category.moblist"
		));

		HudRenderCallback.EVENT.register((context, tickDelta) -> {
			if (client.player == null || client.options == null) return;

			if (openConfigKey.wasPressed()) {
				client.setScreen(new HudConfigScreen(client.currentScreen));
			}

			int screenWidth = client.getWindow().getScaledWidth();
			int screenHeight = client.getWindow().getScaledHeight();

			float px = getAnchoredX(screenWidth);
			float py = getAnchoredY(screenHeight);


			MatrixStack matrices = context.getMatrices();
			matrices.push();
			matrices.translate(px, py, 0);
			matrices.scale(hudScale, hudScale, 1.0f);

			context.drawText(client.textRenderer, "Mob List:", 0, 0, 0xFFFFFF, false);
			int y = 12;

			for (Map.Entry<String, Integer> entry : new TreeMap<>(mobCounts).entrySet()) {
				String raw = entry.getKey().replace("entity.minecraft.", "");
				String display = raw.replace(".baby", " (baby)") + ": " + entry.getValue();
				context.drawText(client.textRenderer, display, 0, y, 0xAAAAAA, false);
				y += 10;
			}

			matrices.pop();
		});
	}

	public static float getAnchoredX(int screenWidth) {
		float px = hudX * screenWidth;
		return switch (anchor) {
			case TOP_RIGHT, BOTTOM_RIGHT -> screenWidth - px;
			case CENTER -> screenWidth / 2f + px;
			default -> px;
		};
	}

	public static float getAnchoredY(int screenHeight) {
		float py = hudY * screenHeight;
		return switch (anchor) {
			case BOTTOM_LEFT, BOTTOM_RIGHT -> screenHeight - py;
			case CENTER -> screenHeight / 2f + py;
			default -> py;
		};
	}

	public static void setAnchoredOffset(float px, float py, int screenWidth, int screenHeight) {
		switch (anchor) {
			case TOP_RIGHT -> {
				hudX = (screenWidth - px) / screenWidth;
				hudY = py / screenHeight;
			}
			case BOTTOM_LEFT -> {
				hudX = px / screenWidth;
				hudY = (screenHeight - py) / screenHeight;
			}
			case BOTTOM_RIGHT -> {
				hudX = (screenWidth - px) / screenWidth;
				hudY = (screenHeight - py) / screenHeight;
			}
			case CENTER -> {
				hudX = (px - screenWidth / 2f) / screenWidth;
				hudY = (py - screenHeight / 2f) / screenHeight;
			}
			default -> {
				hudX = px / screenWidth;
				hudY = py / screenHeight;
			}
		}
	}

	public static void clampHudToScreen(int screenWidth, int screenHeight) {
		float px = getAnchoredX(screenWidth);
		float py = getAnchoredY(screenHeight);

		int hudWidth = 100;
		int hudHeight = 100;
		float scaledWidth = hudWidth * hudScale;
		float scaledHeight = hudHeight * hudScale;

		float maxX = screenWidth - scaledWidth;
		float maxY = screenHeight - scaledHeight;

		float x = Math.max(0, Math.min(px, maxX));
		float y = Math.max(0, Math.min(py, maxY));

		setAnchoredOffset(x, y, screenWidth, screenHeight);
	}

	public static void saveConfig() {
		try {
			JsonObject obj = new JsonObject();
			obj.addProperty("hudX", hudX);
			obj.addProperty("hudY", hudY);
			obj.addProperty("hudScale", hudScale);
			obj.addProperty("anchor", anchor.name());
			obj.addProperty("separateBabies", separateBabies);
			Files.createDirectories(CONFIG_PATH.getParent());
			Files.writeString(CONFIG_PATH, GSON.toJson(obj));
		} catch (Exception e) {
			System.err.println("Failed to save MobList config: " + e);
		}
	}

	public static void loadConfig() {
		try {
			if (Files.exists(CONFIG_PATH)) {
				String json = Files.readString(CONFIG_PATH);
				JsonObject obj = GSON.fromJson(json, JsonObject.class);
				hudX = obj.get("hudX").getAsFloat();
				hudY = obj.get("hudY").getAsFloat();
				hudScale = obj.get("hudScale").getAsFloat();
				if (obj.has("anchor")) anchor = Anchor.valueOf(obj.get("anchor").getAsString());
				if (obj.has("separateBabies")) separateBabies = obj.get("separateBabies").getAsBoolean();
			}
		} catch (Exception e) {
			System.err.println("Failed to load MobList config: " + e);
		}
	}
}
