package com.mojang.authlib.yggdrasil.request;

import com.mojang.authlib.GameProfile;

public class RefreshRequest {
    private String clientToken;

    private String accessToken;

    private GameProfile selectedProfile;

    private boolean requestUser = true;

    public RefreshRequest(String accessToken, String clientToken) {
        this(accessToken, clientToken, null);
    }

    public RefreshRequest(String accessToken, String clientToken, GameProfile profile) {
        this.clientToken = clientToken;
        this.accessToken = accessToken;
        this.selectedProfile = profile;
    }
}
