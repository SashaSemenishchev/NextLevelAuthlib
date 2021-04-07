package com.mojang.authlib;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Scanner;
import javax.annotation.Nullable;

import com.mojang.authlib.exceptions.AuthenticationException;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class HttpAuthenticationService extends BaseAuthenticationService {
    private static final Logger LOGGER = LogManager.getRootLogger();

    private final Proxy proxy;

    protected HttpAuthenticationService(Proxy proxy) {
        Validate.notNull(proxy);
        this.proxy = proxy;
    }

    public Proxy getProxy() {
        return this.proxy;
    }

    protected HttpURLConnection createUrlConnection(URL url) throws IOException {
        Validate.notNull(url);
        LOGGER.debug("Opening connection to " + url);
        HttpURLConnection connection = (HttpURLConnection)url.openConnection(this.proxy);
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(15000);
        connection.setUseCaches(false);
        return connection;
    }

    public String performPostRequest(URL url, String post, String contentType) throws IOException {
        Validate.notNull(url);
        Validate.notNull(post);
        Validate.notNull(contentType);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.addRequestProperty("Content-Type", contentType);

        connection.addRequestProperty("User-Agent", "PostmanRuntime/7.6.28");
        connection.setDoOutput(true);
        LOGGER.info("\n\n\nWriting POST data to " + url + ": " + post);
        OutputStream out = connection.getOutputStream();
        out.write(post.getBytes(StandardCharsets.UTF_8));
        out.close();
        LOGGER.debug("Reading data from " + url);
        InputStream inputStream = null;
        try {
            inputStream = connection.getInputStream();
            String result = IOUtils.toString(inputStream, Charsets.UTF_8);
            LOGGER.debug("Successful read, server response was " + connection.getResponseCode());
            LOGGER.debug("Response: " + result);
            return result;
        } catch (IOException e) {
            IOUtils.closeQuietly(inputStream);
            inputStream = connection.getErrorStream();
            if (inputStream != null) {
                LOGGER.debug("Reading error page from " + url);
                String result = IOUtils.toString(inputStream, Charsets.UTF_8);
                LOGGER.debug("Successful read, server response was " + connection.getResponseCode());
                LOGGER.debug("Response: " + result);
                return result;
            }
            LOGGER.debug("Request failed", e);
            throw e;
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    public String performPostRequest(URL url, String post, String contentType, String token) throws IOException {
        LOGGER.info("\n\nNeeded request");
        Validate.notNull(url);
        Validate.notNull(post);
        Validate.notNull(contentType);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.addRequestProperty("Content-Type", contentType);
        connection.addRequestProperty("Authorization", "Bearer " + token);
        connection.addRequestProperty("User-Agent", "PostmanRuntime/7.6.28");
        connection.addRequestProperty("Accept", "application/json");
        connection.setDoOutput(true);
        LOGGER.info("\n\n\nWriting POST data to " + url + ": " + post);
        OutputStream out = null;
        try{
            out = connection.getOutputStream();
            out.write(post.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception){
            exception.printStackTrace();
        } finally {
            if(out != null)
                out.close();
        }

        LOGGER.debug("Reading data from " + url);
//        connection.connect();
//        if(!String.valueOf(connection.getResponseCode()).startsWith("2")){
//            throw new Error("Response code not 200");
//        }
        InputStream inputStream = null;
        try {
            inputStream = connection.getInputStream();
            StringBuilder sb = new StringBuilder();
            Scanner in = new Scanner(inputStream);
            while(in.hasNext()){
                sb.append(in.nextLine());
            }
            in.close();
            String result = sb.toString();
            LOGGER.debug("Successful read, server response was " + connection.getResponseCode());
            LOGGER.debug("Response: " + result);
            return result;
        } catch (Exception e) {
            if(inputStream != null){
                inputStream.close();
            }
            inputStream = connection.getErrorStream();
            if (inputStream != null) {
                StringBuilder sb = new StringBuilder();
                Scanner in = new Scanner(inputStream);
                while(in.hasNext()){
                    sb.append(in.nextLine());
                }
                in.close();
                String result = sb.toString();
                LOGGER.debug("Successful read, server response was " + connection.getResponseCode());
                LOGGER.debug("Response: " + result);
                return result;
            }
            LOGGER.debug("Request failed", e);
            throw e;
        } finally {
            connection.disconnect();
            if(inputStream != null)
                inputStream.close();
        }
    }

    public String performGetRequest(URL url) throws IOException {
        return performGetRequest(url, null);
    }

    public String performGetRequest(URL url, @Nullable String authentication) throws IOException {
        Validate.notNull(url);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.addRequestProperty("User-Agent", "PostmanRuntime/7.6.28");
        connection.addRequestProperty("Accept", "application/json");
        if (authentication != null)
            connection.addRequestProperty("Authorization", authentication);
        LOGGER.debug("Reading data from " + url);
        InputStream inputStream = null;
        try {
            inputStream = connection.getInputStream();
            LOGGER.info(connection.getResponseMessage() + " " + connection.getResponseCode());
            StringBuilder sb = new StringBuilder();
            Scanner in = new Scanner(inputStream);
            while(in.hasNext()){
                sb.append(in.nextLine());
            }
            String result = sb.toString();
            LOGGER.debug("Successful read, server response was " + connection.getResponseCode());
            LOGGER.debug("Response: " + result);
            return result;
        } catch (IOException e) {
            if(inputStream != null)
                inputStream.close();
            inputStream = connection.getErrorStream();
            if (inputStream != null) {
                LOGGER.debug("Reading error page from " + url);
                StringBuilder sb = new StringBuilder();
                Scanner in = new Scanner(inputStream);
                while(in.hasNext()){
                    sb.append(in.nextLine());
                }
                String result = sb.toString();
                LOGGER.debug("Successful read, server response was " + connection.getResponseCode());
                LOGGER.debug("Response: " + result);
                return result;
            }
            LOGGER.debug("Request failed", e);
            throw e;
        } finally {
            if(inputStream != null)
                inputStream.close();
        }
    }

    public static URL constantURL(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException ex) {
            throw new Error("Couldn't create constant for " + url, ex);
        }
    }

    public static String buildQuery(Map<String, Object> query) {
        if (query == null)
            return "";
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, Object> entry : query.entrySet()) {
            if (builder.length() > 0)
                builder.append('&');
            try {
                builder.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                LOGGER.error("Unexpected exception building query", e);
            }
            if (entry.getValue() != null) {
                builder.append('=');
                try {
                    builder.append(URLEncoder.encode(entry.getValue().toString(), "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    LOGGER.error("Unexpected exception building query", e);
                }
            }
        }
        return builder.toString();
    }

    public static URL concatenateURL(URL url, String query) {
        try {
            if (url.getQuery() != null && url.getQuery().length() > 0)
                return new URL(url.getProtocol(), url.getHost(), url.getPort(), url.getFile() + "&" + query);
            return new URL(url.getProtocol(), url.getHost(), url.getPort(), url.getFile() + "?" + query);
        } catch (MalformedURLException ex) {
            throw new IllegalArgumentException("Could not concatenate given URL with GET arguments!", ex);
        }
    }
}
