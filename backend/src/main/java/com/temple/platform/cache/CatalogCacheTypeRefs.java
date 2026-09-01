package com.temple.platform.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.temple.platform.darshan.domain.Darshan;
import com.temple.platform.temple.domain.Temple;

import java.util.List;

public final class CatalogCacheTypeRefs {

    public static final TypeReference<List<Temple>> TEMPLE_LIST = new TypeReference<>() {
    };

    public static final TypeReference<List<Darshan>> DARSHAN_LIST = new TypeReference<>() {
    };

    private CatalogCacheTypeRefs() {
    }
}
