package com.hexmod.module;

/** Pojedynczy modul Hexmoda (widget lub funkcja), ktory mozna wlaczyc/wylaczyc w locie. */
public class Module {
    public final String id;
    public final String name;
    public final String description;
    private boolean enabled;

    public Module(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;
        this.enabled = enabled;
        if (enabled) onEnable(); else onDisable();
        ModuleManager.save();
    }

    /** Wczytanie stanu z configu bez wywolywania zapisu. */
    void setEnabledSilently(boolean enabled) {
        this.enabled = enabled;
    }

    protected void onEnable() {}

    protected void onDisable() {}
}
