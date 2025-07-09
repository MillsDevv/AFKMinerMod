package com.mills.afkminermod.client;

import com.mills.afkminermod.client.config.ConfigManager;
import com.mills.afkminermod.client.gui.ConfigScreenBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.concurrent.ThreadLocalRandom;

public class MainClient implements ClientModInitializer {
    private static ConfigManager configManager;
    private KeyBinding toggleMiningKey;
    private KeyBinding showConfigGUI;

    // Data
    private boolean isMining = false;
    private Float storedYaw = null;
    private Float storedPitch = null;
    private int storedSlot = -1;
    private int rotationCorrectionDelayTicks = 0;
    private boolean shouldCorrectRotation = false;
    private int slotCorrectionDelayTicks = 0;
    private boolean shouldCorrectSlot = false;
    private int abilityTickCounter = 0;
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
    private boolean windowsNotificationOnRotation;
    private boolean resetHotbarOnInvShuffle;
    private boolean instantHotbarReset;
    private boolean windowsNotificationOnHotbarShuffle;

    @Override
    public void onInitializeClient() {
        configManager = new ConfigManager();

        ROTATION_THRESHOLD = configManager.getConfig().rotationThreshold;
        CHECK_INTERVAL = configManager.getConfig().checkForAbility;
        DURATION = configManager.getConfig().duration;
        minZ = configManager.getConfig().minZCoordinate;
        maxZ = configManager.getConfig().maxZCoordinate;
        rotationReset = configManager.getConfig().rotationReset;
        instantRotationReset = configManager.getConfig().instantRotationReset;
        windowsNotificationOnRotation = configManager.getConfig().windowsNotificationOnRotation;
        resetHotbarOnInvShuffle = configManager.getConfig().resetHotbarOnInvShuffle;
        instantHotbarReset = configManager.getConfig().instantHotbarReset;
        windowsNotificationOnHotbarShuffle = configManager.getConfig().windowsNotificationOnHotbarShuffle;

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

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            if (showConfigGUI.wasPressed()) {
                MinecraftClient.getInstance().setScreen(ConfigScreenBuilder.create(null, configManager));
            }
            if (toggleMiningKey.wasPressed()) {
                int pickaxeSlot = findPickaxeInHotbar(client.player);
                if (pickaxeSlot == -1) {
                    client.player.sendMessage(Text.literal("You don't have a pickaxe in hotbar!")
                            .styled(style -> style.withColor(isMining ? Formatting.GREEN : Formatting.RED).withBold(true)), true);
                    return;
                }
                isMining = !isMining;
                if (isMining) {
                    storedYaw = client.player.getYaw();
                    storedPitch = client.player.getPitch();
                    if (client.player.getInventory().getSelectedSlot() != pickaxeSlot) {
                        client.player.getInventory().setSelectedSlot(pickaxeSlot);
                        client.player.getInventory().updateItems();
                        storedSlot = pickaxeSlot;
                    }
                } else {
                    storedYaw = null;
                    storedPitch = null;
                    storedSlot = -1;
                }
                client.player.sendMessage(Text.literal("AFK Mining: " + (isMining ? "ON" : "OFF"))
                        .styled(style -> style.withColor(isMining ? Formatting.GREEN : Formatting.RED).withBold(true)), true);
                client.options.attackKey.setPressed(isMining);
            }

            abilityTimer++;

            if (!abilityActive && abilityTickCounter % CHECK_INTERVAL == 0) {
                InGameHud hud = client.inGameHud;
                Text overlay = HudAccess.getOverlayMessage(hud);
                if (overlay != null && overlay.toString().toLowerCase().contains("you ready your pickaxe")) {
                    abilityActive = true;
                    abilityTimer = 0;
                    goingRight = true;
                }
            }

            if (abilityActive) {
                abilityTimer++;
                double playerZ = client.player.getZ();

                if (playerZ == maxZ) goingRight = false;

                if (goingRight) {
                    client.options.leftKey.setPressed(false);
                    client.options.rightKey.setPressed(true);
                    if (playerZ == maxZ) goingRight = false;
                } else {
                    client.options.rightKey.setPressed(false);
                    client.options.leftKey.setPressed(true);
                    if (playerZ == minZ) goingRight = true;
                }

                if (abilityTimer >= DURATION) {
                    abilityActive = false;
                    client.options.leftKey.setPressed(false);
                    client.options.rightKey.setPressed(false);
                }
            }

            // if a gui/screen shows up
            if (client.currentScreen != null) {
                if (isMining) {
                    isMining = false;
                    client.player.sendMessage(Text.literal("AFK Mining: OFF")
                            .styled(style -> style.withColor(Formatting.RED).withBold(true)), true);
                    client.options.attackKey.setPressed(false);
                    Screen screen = client.currentScreen;
                    Text title = screen.getTitle();
                    if (title != null && title.getString().equalsIgnoreCase("captcha")) {
                        sendNotification("You have a captcha!");
                    }
                }
            }

            // if you got rotated
            if (isMining && storedYaw != null && storedPitch != null && !rotationReset) {
                float currentYaw = client.player.getYaw();
                float currentPitch = client.player.getPitch();

                float yawDiff = Math.abs(currentYaw - storedYaw);
                float pitchDiff = Math.abs(currentPitch - storedPitch);

                if ((yawDiff > ROTATION_THRESHOLD || pitchDiff > ROTATION_THRESHOLD) && rotationCorrectionDelayTicks == 0) {
                    if (windowsNotificationOnRotation) {
                        sendNotification("You have been rotated!");
                    }
                    if (!instantRotationReset) {
                        rotationCorrectionDelayTicks = ThreadLocalRandom.current().nextInt(10, 30); // 0.5s - 1.5s
                    } else {
                        rotationCorrectionDelayTicks = 1;
                    }
                    shouldCorrectRotation = true;
                }
            }

            if (rotationCorrectionDelayTicks > 0) {
                rotationCorrectionDelayTicks--;
                if (rotationCorrectionDelayTicks == 0 && shouldCorrectRotation) {
                    client.player.setYaw(storedYaw);
                    client.player.setPitch(storedPitch);
                    shouldCorrectRotation = false;
                }
            }

            // if you got shuffled
            if (isMining && client.player.getInventory().getSelectedSlot() != storedSlot && slotCorrectionDelayTicks == 0 && !resetHotbarOnInvShuffle) {
                if (windowsNotificationOnHotbarShuffle) {
                    sendNotification("Your hotbar has been shuffled");
                }
                if (!instantHotbarReset) {
                    slotCorrectionDelayTicks = ThreadLocalRandom.current().nextInt(10, 30); // sets the delay
                } else {
                    slotCorrectionDelayTicks = 1;
                }
                shouldCorrectSlot = true;
            }

            if (slotCorrectionDelayTicks > 0) { // if delay was set count down
                slotCorrectionDelayTicks--;
                if (slotCorrectionDelayTicks == 0 && shouldCorrectSlot) {
                    client.player.getInventory().setSelectedSlot(storedSlot);
                    client.player.getInventory().updateItems();
                    shouldCorrectSlot = false;
                }
            }
        });
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

    public void sendNotification(String message) {
        if (!SystemTray.isSupported()) {
            System.out.println("System tray not supported!");
            return;
        }
        SystemTray tray = SystemTray.getSystemTray();
        Image image = Toolkit.getDefaultToolkit().createImage("icon.png");
        TrayIcon trayIcon = new TrayIcon(image, "Tray Demo");
        trayIcon.setImageAutoSize(true);
        try {
            tray.add(trayIcon);
            trayIcon.displayMessage("AFK Miner Mod", message, TrayIcon.MessageType.INFO);
        } catch (AWTException e) {
            System.err.println("TrayIcon could not be added.");
        }
    }
}