package com.mills.afkminermod.client;

import com.mills.afkminermod.client.config.ConfigManager;
import com.mills.afkminermod.client.gui.ConfigScreenBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.util.concurrent.ThreadLocalRandom;

public class MainClient implements ClientModInitializer {
    private static ConfigManager configManager;

    private KeyBinding toggleMiningKey;
    private KeyBinding showConfigGUI;

    private boolean isMining = false;

    // Stored state for corrections
    private Float storedYaw = null;
    private Float storedPitch = null;
    private int storedSlot = -1;

    // Tick-based delays
    private int rotationDelayTicks = 0;
    private int slotDelayTicks = 0;

    // ability
    private boolean abilityActive = false;
    private int abilityTimer = 0;
    private boolean goingRight = true;

    // Configs
    private float ROTATION_THRESHOLD;
    private int CHECK_INTERVAL; // 5 seconds
    private int DURATION; // 43 seconds
    private double minZ;
    private double maxZ;
    private boolean rotationReset;
    private boolean instantRotationReset;
    private boolean resetHotbarOnInvShuffle;
    private boolean instantHotbarReset;

    @Override
    public void onInitializeClient() {
        configManager = new ConfigManager();
        loadConfig();

        toggleMiningKey = new KeyBinding(
                "key.afkminer.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_M,
                "category.afkminer"
        );
        net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper.registerKeyBinding(toggleMiningKey);

        showConfigGUI = new KeyBinding(
                "key.afkminer.config",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_N,
                "category.afkminer"
        );
        net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper.registerKeyBinding(showConfigGUI);

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    private void loadConfig() {
        ROTATION_THRESHOLD = configManager.getConfig().rotationThreshold;
        CHECK_INTERVAL = configManager.getConfig().checkForAbility;
        DURATION = configManager.getConfig().duration;
        minZ = configManager.getConfig().minZCoordinate;
        maxZ = configManager.getConfig().maxZCoordinate;
        rotationReset = configManager.getConfig().rotationReset;
        instantRotationReset = configManager.getConfig().instantRotationReset;
        resetHotbarOnInvShuffle = configManager.getConfig().resetHotbarOnInvShuffle;
        instantHotbarReset = configManager.getConfig().instantHotbarReset;
    }

    private void onTick(MinecraftClient client) {
        if (client.player == null) return;

        handleKeyPresses(client);
        handleMining(client);
    }

    private void handleKeyPresses(MinecraftClient client) {
        if (showConfigGUI.wasPressed()) {
            client.setScreen(ConfigScreenBuilder.create(null, configManager));
        }

        if (toggleMiningKey.wasPressed()) {
            int pickaxeSlot = findPickaxeInHotbar(client.player);
            if (pickaxeSlot == -1) {
                client.player.sendMessage(Text.literal("You don't have a pickaxe in hotbar!")
                        .styled(style -> style.withColor(Formatting.RED).withBold(true)), true);
                return;
            }

            isMining = !isMining;

            if (isMining) {
                storedYaw = client.player.getYaw();
                storedPitch = client.player.getPitch();
                storedSlot = client.player.getInventory().getSelectedSlot() != pickaxeSlot
                        ? pickaxeSlot
                        : client.player.getInventory().getSelectedSlot();
                client.player.getInventory().setSelectedSlot(storedSlot);
            } else {
                storedYaw = null;
                storedPitch = null;
                storedSlot = -1;
            }

            client.player.sendMessage(Text.literal("AFK Mining: " + (isMining ? "ON" : "OFF"))
                    .styled(style -> style.withColor(isMining ? Formatting.GREEN : Formatting.RED).withBold(true)), true);

            client.options.attackKey.setPressed(isMining);
        }
    }

    private void handleMining(MinecraftClient client) {
        if (!isMining) return;

        ClientPlayerEntity player = client.player;

        // Toggle mining off for screen/chat
        if (client.currentScreen != null) {
            isMining = false;
            player.sendMessage(Text.literal("AFK Mining: OFF")
                    .styled(style -> style.withColor(Formatting.RED).withBold(true)), true);
            client.options.attackKey.setPressed(false);
            return;
        }

        // Yaw/Pitch Correction
        if (storedYaw != null && storedPitch != null && rotationReset) {
            float yawDiff = Math.abs(player.getYaw() - storedYaw);
            float pitchDiff = Math.abs(player.getPitch() - storedPitch);

            if ((yawDiff > ROTATION_THRESHOLD || pitchDiff > ROTATION_THRESHOLD) && rotationDelayTicks == 0) {
                rotationDelayTicks = instantRotationReset ? 1 : ThreadLocalRandom.current().nextInt(10, 31);
            }

            if (rotationDelayTicks > 0) {
                rotationDelayTicks--;
                if (rotationDelayTicks == 0) {
                    player.setYaw(storedYaw);
                    player.setPitch(storedPitch);
                }
            }
        }

        // Selected Item Correction
        if (storedSlot != -1 && player.getInventory().getSelectedSlot() != storedSlot && slotDelayTicks == 0 && resetHotbarOnInvShuffle) {
            slotDelayTicks = instantHotbarReset ? 1 : ThreadLocalRandom.current().nextInt(10, 31);
        }

        if (slotDelayTicks > 0) {
            slotDelayTicks--;
            if (slotDelayTicks == 0) {
                player.getInventory().setSelectedSlot(storedSlot);
                player.getInventory().updateItems();
            }
        }

        // Mining ability
        abilityTimer++;

        // Check if ability should activate
        if (!abilityActive && abilityTimer % CHECK_INTERVAL == 0) {
            if (client.inGameHud != null) {
                Text overlay = HudAccess.getOverlayMessage(client.inGameHud); // Your action bar helper
                if (overlay != null && overlay.getString().toLowerCase().contains("you ready your pickaxe")) {
                    abilityActive = true;
                    abilityTimer = 0;
                    goingRight = true;
                }
            }
        }

        // Ability movement
        if (abilityActive) {
            double playerZ = player.getZ();

            if (goingRight) {
                client.options.leftKey.setPressed(false);
                client.options.rightKey.setPressed(true);
                if (playerZ >= maxZ) goingRight = false;
            } else {
                client.options.rightKey.setPressed(false);
                client.options.leftKey.setPressed(true);
                if (playerZ <= minZ) goingRight = true;
            }

            // Stop ability after duration
            if (abilityTimer >= DURATION) {
                abilityActive = false;
                client.options.leftKey.setPressed(false);
                client.options.rightKey.setPressed(false);
            }
        }
    }

    private int findPickaxeInHotbar(ClientPlayerEntity player) {
        for (int slot = 0; slot < 9; slot++) {
            Item item = player.getInventory().getStack(slot).getItem();
            if (isPickaxe(item)) {
                return slot;
            }
        }
        return -1;
    }

    private boolean isPickaxe(Item item) {
        return item == Items.NETHERITE_PICKAXE || item == Items.DIAMOND_PICKAXE;
    }
}