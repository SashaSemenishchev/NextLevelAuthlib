package com.mojang.authlib.minecraft;

import com.mojang.authlib.AuthenticationService;
import com.mojang.authlib.HttpAuthenticationService;
import org.apache.logging.log4j.LogManager;

public abstract class HttpMinecraftSessionService extends BaseMinecraftSessionService {
    protected HttpMinecraftSessionService(HttpAuthenticationService authenticationService) {
        super((AuthenticationService)authenticationService);
    }

    public HttpAuthenticationService getAuthenticationService() {
        return (HttpAuthenticationService)super.getAuthenticationService();
    }
}
