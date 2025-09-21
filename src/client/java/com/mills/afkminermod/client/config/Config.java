package com.mills.afkminermod.client.config;

import com.google.gson.annotations.Expose;

public class Config {
    @Expose
    public int duration = 860;
    @Expose
    public int checkForAbility = 100;
    @Expose
    public int minZCoordinate = 3999;
    @Expose
    public int maxZCoordinate = 3984;

    // pitch & yaw rotation
    @Expose
    public boolean rotationReset = true;
    @Expose
    public boolean instantRotationReset = false;
    @Expose
    public float rotationThreshold = 5.0f;

    // hotbar shuffle
    @Expose
    public boolean resetHotbarOnInvShuffle = true;
    @Expose
    public boolean instantHotbarReset = false;
}
