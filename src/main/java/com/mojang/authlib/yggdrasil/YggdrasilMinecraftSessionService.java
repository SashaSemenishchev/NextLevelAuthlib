package com.mojang.authlib.yggdrasil;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.mojang.authlib.AuthenticationService;
import com.mojang.authlib.Environment;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.HttpAuthenticationService;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.minecraft.HttpMinecraftSessionService;
import com.mojang.authlib.minecraft.InsecureTextureException;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.request.JoinMinecraftServerRequest;
import com.mojang.authlib.yggdrasil.response.HasJoinedMinecraftServerResponse;
import com.mojang.authlib.yggdrasil.response.MinecraftProfilePropertiesResponse;
import com.mojang.authlib.yggdrasil.response.MinecraftTexturesPayload;
import com.mojang.authlib.yggdrasil.response.Response;
import com.mojang.util.UUIDTypeAdapter;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.apache.commons.codec.Charsets;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class YggdrasilMinecraftSessionService extends HttpMinecraftSessionService {

    private static final Logger LOGGER = LogManager.getLogger();

    private final String baseUrl;

    private static final boolean isLocalhost = false;

    private final URL joinUrl;

    private final String checkString;

    private final Gson gson = (new GsonBuilder()).registerTypeAdapter(UUID.class, new UUIDTypeAdapter()).create();

    private final LoadingCache<GameProfile, GameProfile> insecureProfiles = CacheBuilder.newBuilder()
            .expireAfterWrite(6L, TimeUnit.HOURS)
            .build(new CacheLoader<GameProfile, GameProfile>() {
                public GameProfile load(GameProfile key) throws Exception {
                    return YggdrasilMinecraftSessionService.this.fillGameProfile(key, false);
                }
            });

    protected YggdrasilMinecraftSessionService(YggdrasilAuthenticationService service, Environment env) {
        super(service);
        this.baseUrl = env.getSessionHost() + "/session/minecraft/";
        this.joinUrl = HttpAuthenticationService.constantURL("http" + (isLocalhost ? "://localhost:8000/mojang/" : "s://nextlevel.su/game/api/v1/") + "session/join/");
        this.checkString = "http" + (isLocalhost ? "://localhost:8000/mojang/" : "s://nextlevel.su/game/api/v1/") +"session/hasJoined/";
        URL checkUrl = HttpAuthenticationService.constantURL("http" + (isLocalhost ? "://localhost:8000/mojang/" : "s://nextlevel.su/game/api/v1/") + "session/hasJoined/?username=%s&serverId=%s");
        System.out.println(joinUrl + " " + checkUrl);
    }

    public void joinServer(GameProfile profile, String authenticationToken, String serverId) throws AuthenticationException {
        JoinMinecraftServerRequest request = new JoinMinecraftServerRequest();
        request.accessToken = authenticationToken;
        request.selectedProfile = profile.getId();
        request.serverId = serverId;
        getAuthenticationService().makeRequest(this.joinUrl, request, Response.class, "application/json", authenticationToken);
    }

    public GameProfile hasJoinedServer(GameProfile user, String serverId, InetAddress address) throws AuthenticationUnavailableException {
        URL url;
        try {
            url = new URL(checkString + "?username=" + user.getName() + "&serverId=" + serverId);
        } catch (MalformedURLException e) {
            LOGGER.error("Malformed URL");
            throw new AuthenticationUnavailableException(e);
        }
        try {
            HasJoinedMinecraftServerResponse response = getAuthenticationService().makeRequest(url, null, HasJoinedMinecraftServerResponse.class);
            if (response != null && response.getId() != null) {
                GameProfile result = new GameProfile(response.getId(), user.getName());
                if (response.getProperties() != null) {
                    LOGGER.info("Properties: " + response.getProperties());;
                    result.getProperties().putAll(response.getProperties());
                }
                return result;
            }
            LOGGER.error("null");
            return null;
        } catch (AuthenticationUnavailableException e) {
            LOGGER.error("Cannot contact");
            throw e;
        } catch (AuthenticationException ignored) {
            LOGGER.error("Ignored");
            ignored.printStackTrace();
            return null;
        }
    }

    public Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> getTextures(GameProfile profile, boolean requireSecure) {
        MinecraftTexturesPayload result;
        Property textureProperty = Iterables.getFirst(profile.getProperties().get("textures"), null);
        if (textureProperty == null) {
            LOGGER.error("No property :(");
            return new HashMap<>();
        }
        try {
            String json = new String(Base64.decodeBase64(textureProperty.getValue()), StandardCharsets.UTF_8);
            result = this.gson.fromJson(json, MinecraftTexturesPayload.class);
        } catch (JsonParseException e) {
            LOGGER.error("Could not decode textures payload", e);
            return new HashMap<>();
        }
        if (result == null || result.getTextures() == null){
            LOGGER.error("Textures not found: " + (result == null) + " " + (result.getTextures() == null));
            return new HashMap<>();
        }
        LOGGER.info("Textures set for " + profile);
        return result.getTextures();
    }

    public GameProfile fillProfileProperties(GameProfile profile, boolean requireSecure) {
        LOGGER.info("Tried to fill profile");
        if (profile.getId() == null)
            return profile;
        return fillGameProfile(profile, true);
    }

    protected GameProfile fillGameProfile(GameProfile profile, boolean requireSecure) {
        LOGGER.info("Tried to fill profile2");
        try {
            URL url = HttpAuthenticationService.constantURL("https://nextlevel.su/game/api/v1/profile/?uuid=" + UUIDTypeAdapter.fromUUID(profile.getId()));
            MinecraftProfilePropertiesResponse response = getAuthenticationService().makeRequest(url, null, MinecraftProfilePropertiesResponse.class);
            if (response == null) {
                LOGGER.debug("Couldn't fetch profile properties for " + profile + " as the profile does not exist");
                return profile;
            }
            GameProfile result = new GameProfile(response.getId(), response.getName());
            result.getProperties().putAll(response.getProperties());
            profile.getProperties().putAll(response.getProperties());
            LOGGER.debug("Successfully fetched profile properties for " + profile);
            return result;
        } catch (AuthenticationException e) {
            LOGGER.warn("Couldn't look up profile properties for " + profile, e);
            return profile;
        }
    }

    public YggdrasilAuthenticationService getAuthenticationService() {
        return (YggdrasilAuthenticationService)super.getAuthenticationService();
    }

    private static boolean isWhitelistedDomain(String url) {
        return true;
    }
}
