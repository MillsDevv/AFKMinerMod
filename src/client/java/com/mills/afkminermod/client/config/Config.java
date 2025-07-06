package com.mills.afkminermod.client.config;

public class Config {
    public int duration = 860;
    public int checkForAbility = 100;
    public int minZCoordinate = 3999;
    public int maxZCoordinate = 3984;

    // pitch & yaw rotation
    public boolean autoRotationReset = true;
    public boolean instantRotationReset = false;
    public boolean windowsNotificationOnRotation = true;
    public float rotationThreshold = 5.0f;

    // hotbar shuffle
    public boolean autoResetHotbar = true;
    public boolean instantHotbarReset = false;
    public boolean windowsNotificationOnHotbarShuffle = true;
}
