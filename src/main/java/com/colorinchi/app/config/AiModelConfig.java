package com.colorinchi.app.config;

/**
 * Configuration for a single AI model in the allowlist catalog.
 * Uses a class (not a record) because "default" is a Java keyword
 * and cannot be a record component name.
 */
public class AiModelConfig {

    private String id;
    private String name;
    private String provider;
    private boolean isDefault;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean isDefault) { this.isDefault = isDefault; }
}
