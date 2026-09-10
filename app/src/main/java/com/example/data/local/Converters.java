package com.example.data.local;

import androidx.room.TypeConverter;
import com.example.domain.model.CapabilitySet;
import com.example.domain.model.CapabilitySource;
import com.example.domain.model.ContentPart;
import com.example.domain.model.Role;
import com.example.domain.model.SpeedTier;
import com.example.domain.model.TokenUsage;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Converters {
    private static final Gson gson = new Gson();

    @TypeConverter
    public static String fromContentPartList(List<ContentPart> parts) {
        if (parts == null) return "[]";
        return gson.toJson(parts);
    }

    @TypeConverter
    public static List<ContentPart> toContentPartList(String json) {
        if (json == null || json.isEmpty()) return Collections.emptyList();
        Type listType = new TypeToken<List<ContentPart>>() {}.getType();
        return gson.fromJson(json, listType);
    }

    @TypeConverter
    public static String fromRole(Role role) {
        return role == null ? Role.USER.name() : role.name();
    }

    @TypeConverter
    public static Role toRole(String name) {
        if (name == null) return Role.USER;
        try {
            return Role.valueOf(name);
        } catch (Exception e) {
            return Role.USER;
        }
    }

    @TypeConverter
    public static String fromTokenUsage(TokenUsage usage) {
        if (usage == null) return "{}";
        return gson.toJson(usage);
    }

    @TypeConverter
    public static TokenUsage toTokenUsage(String json) {
        if (json == null || json.isEmpty()) return new TokenUsage();
        return gson.fromJson(json, TokenUsage.class);
    }

    @TypeConverter
    public static String fromCapabilitySet(CapabilitySet set) {
        if (set == null) return "{}";
        return gson.toJson(set);
    }

    @TypeConverter
    public static CapabilitySet toCapabilitySet(String json) {
        if (json == null || json.isEmpty()) return new CapabilitySet();
        return gson.fromJson(json, CapabilitySet.class);
    }

    @TypeConverter
    public static String fromCapabilitySource(CapabilitySource source) {
        return source == null ? CapabilitySource.HEURISTIC.name() : source.name();
    }

    @TypeConverter
    public static CapabilitySource toCapabilitySource(String name) {
        if (name == null) return CapabilitySource.HEURISTIC;
        try {
            return CapabilitySource.valueOf(name);
        } catch (Exception e) {
            return CapabilitySource.HEURISTIC;
        }
    }

    @TypeConverter
    public static String fromSpeedTier(SpeedTier tier) {
        return tier == null ? SpeedTier.BALANCED.name() : tier.name();
    }

    @TypeConverter
    public static SpeedTier toSpeedTier(String name) {
        if (name == null) return SpeedTier.BALANCED;
        try {
            return SpeedTier.valueOf(name);
        } catch (Exception e) {
            return SpeedTier.BALANCED;
        }
    }

    @TypeConverter
    public static String fromStringMap(Map<String, String> map) {
        if (map == null) return "{}";
        return gson.toJson(map);
    }

    @TypeConverter
    public static Map<String, String> toStringMap(String json) {
        if (json == null || json.isEmpty()) return Collections.emptyMap();
        Type mapType = new TypeToken<Map<String, String>>() {}.getType();
        return gson.fromJson(json, mapType);
    }
}
