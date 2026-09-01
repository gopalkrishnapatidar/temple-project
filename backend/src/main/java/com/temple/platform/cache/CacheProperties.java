package com.temple.platform.cache;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.cache")
public class CacheProperties {

    private boolean enabled = true;
    private Duration templeIdTtl = Duration.ofMinutes(10);
    private Duration publicTempleListTtl = Duration.ofSeconds(60);
    private Duration darshanIdTtl = Duration.ofMinutes(5);
    private Duration publicDarshanListTtl = Duration.ofMinutes(5);
    private Duration ritualIdTtl = Duration.ofMinutes(5);
    private Duration eventIdTtl = Duration.ofMinutes(2);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Duration getTempleIdTtl() {
        return templeIdTtl;
    }

    public void setTempleIdTtl(Duration templeIdTtl) {
        this.templeIdTtl = templeIdTtl;
    }

    public Duration getPublicTempleListTtl() {
        return publicTempleListTtl;
    }

    public void setPublicTempleListTtl(Duration publicTempleListTtl) {
        this.publicTempleListTtl = publicTempleListTtl;
    }

    public Duration getDarshanIdTtl() {
        return darshanIdTtl;
    }

    public void setDarshanIdTtl(Duration darshanIdTtl) {
        this.darshanIdTtl = darshanIdTtl;
    }

    public Duration getPublicDarshanListTtl() {
        return publicDarshanListTtl;
    }

    public void setPublicDarshanListTtl(Duration publicDarshanListTtl) {
        this.publicDarshanListTtl = publicDarshanListTtl;
    }

    public Duration getRitualIdTtl() {
        return ritualIdTtl;
    }

    public void setRitualIdTtl(Duration ritualIdTtl) {
        this.ritualIdTtl = ritualIdTtl;
    }

    public Duration getEventIdTtl() {
        return eventIdTtl;
    }

    public void setEventIdTtl(Duration eventIdTtl) {
        this.eventIdTtl = eventIdTtl;
    }
}
