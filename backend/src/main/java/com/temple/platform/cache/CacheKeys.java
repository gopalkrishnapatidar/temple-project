package com.temple.platform.cache;

public final class CacheKeys {

    private static final String PREFIX = "temple-platform:v1";

    private CacheKeys() {
    }

    public static String templeId(long templeId) {
        return PREFIX + ":temple:id:" + templeId;
    }

    public static String publicTempleList() {
        return PREFIX + ":temple:list:public";
    }

    public static String darshanId(long darshanId) {
        return PREFIX + ":darshan:id:" + darshanId;
    }

    public static String publicDarshanList(long templeId) {
        return PREFIX + ":darshan:list:temple:" + templeId + ":public";
    }

    public static String ritualId(long ritualId) {
        return PREFIX + ":ritual:id:" + ritualId;
    }

    public static String eventId(long eventId) {
        return PREFIX + ":event:id:" + eventId;
    }
}
