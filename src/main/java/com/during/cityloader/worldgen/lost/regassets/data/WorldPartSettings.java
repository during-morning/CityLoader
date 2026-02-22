package com.during.cityloader.worldgen.lost.regassets.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import com.google.gson.annotations.JsonAdapter;

import java.lang.reflect.Type;

/**
 * 世界风格中全局部件引用设置
 */
@JsonAdapter(WorldPartSettings.Adapter.class)
public class WorldPartSettings {

    @SerializedName("open")
    private String open;

    @SerializedName("open_bi")
    private String openBi;

    @SerializedName("bridge")
    private String bridge;

    @SerializedName("bridge_bi")
    private String bridgeBi;

    @SerializedName("tunnel")
    private String tunnel;

    @SerializedName("tunnel_bi")
    private String tunnelBi;

    @SerializedName("rails3split")
    private String rails3Split;

    @SerializedName("railsbend")
    private String railsBend;

    @SerializedName("railsdown1")
    private String railsDown1;

    @SerializedName("railsdown2")
    private String railsDown2;

    @SerializedName("railsflat")
    private String railsFlat;

    @SerializedName("railshorizontal")
    private String railsHorizontal;

    @SerializedName("railshorizontalend")
    private String railsHorizontalEnd;

    @SerializedName("railshorizontalwater")
    private String railsHorizontalWater;

    @SerializedName("railsvertical")
    private String railsVertical;

    @SerializedName("railsverticalwater")
    private String railsVerticalWater;

    @SerializedName("stationunderground")
    private String stationUnderground;

    @SerializedName("stationundergroundstairs")
    private String stationUndergroundStairs;

    public String getOpen() {
        return open;
    }

    public String getOpenBi() {
        return openBi;
    }

    public String getBridge() {
        return bridge;
    }

    public String getBridgeBi() {
        return bridgeBi;
    }

    public String getTunnel() {
        return tunnel;
    }

    public String getTunnelBi() {
        return tunnelBi;
    }

    public String getRails3Split() {
        return rails3Split;
    }

    public String getRailsBend() {
        return railsBend;
    }

    public String getRailsDown1() {
        return railsDown1;
    }

    public String getRailsDown2() {
        return railsDown2;
    }

    public String getRailsFlat() {
        return railsFlat;
    }

    public String getRailsHorizontal() {
        return railsHorizontal;
    }

    public String getRailsHorizontalEnd() {
        return railsHorizontalEnd;
    }

    public String getRailsHorizontalWater() {
        return railsHorizontalWater;
    }

    public String getRailsVertical() {
        return railsVertical;
    }

    public String getRailsVerticalWater() {
        return railsVerticalWater;
    }

    public String getStationUnderground() {
        return stationUnderground;
    }

    public String getStationUndergroundStairs() {
        return stationUndergroundStairs;
    }

    static class Adapter implements JsonDeserializer<WorldPartSettings> {

        @Override
        public WorldPartSettings deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
            WorldPartSettings result = new WorldPartSettings();
            if (json == null || !json.isJsonObject()) {
                return result;
            }

            JsonObject root = json.getAsJsonObject();
            JsonObject highways = asObject(root.get("highways"));
            JsonObject railways = asObject(root.get("railways"));

            result.open = pickString(root, highways, "open");
            result.openBi = pickString(root, highways, "open_bi");
            result.bridge = pickString(root, highways, "bridge");
            result.bridgeBi = pickString(root, highways, "bridge_bi");
            result.tunnel = pickString(root, highways, "tunnel");
            result.tunnelBi = pickString(root, highways, "tunnel_bi");

            result.rails3Split = pickString(root, railways, "rails3split");
            result.railsBend = pickString(root, railways, "railsbend");
            result.railsDown1 = pickString(root, railways, "railsdown1");
            result.railsDown2 = pickString(root, railways, "railsdown2");
            result.railsFlat = pickString(root, railways, "railsflat");
            result.railsHorizontal = pickString(root, railways, "railshorizontal");
            result.railsHorizontalEnd = pickString(root, railways, "railshorizontalend");
            result.railsHorizontalWater = pickString(root, railways, "railshorizontalwater");
            result.railsVertical = pickString(root, railways, "railsvertical");
            result.railsVerticalWater = pickString(root, railways, "railsverticalwater");
            result.stationUnderground = pickString(root, railways, "stationunderground");
            result.stationUndergroundStairs = pickString(root, railways, "stationundergroundstairs");

            return result;
        }

        private static String pickString(JsonObject root, JsonObject grouped, String key) {
            String fromRoot = extract(root == null ? null : root.get(key));
            if (fromRoot != null) {
                return fromRoot;
            }
            return extract(grouped == null ? null : grouped.get(key));
        }

        private static JsonObject asObject(JsonElement element) {
            if (element == null || !element.isJsonObject()) {
                return null;
            }
            return element.getAsJsonObject();
        }

        private static String extract(JsonElement element) {
            if (element == null || element.isJsonNull()) {
                return null;
            }
            if (element.isJsonPrimitive()) {
                String value = element.getAsString();
                if (value == null) {
                    return null;
                }
                String trimmed = value.trim();
                return trimmed.isEmpty() ? null : trimmed;
            }
            if (element.isJsonArray()) {
                JsonArray array = element.getAsJsonArray();
                for (JsonElement child : array) {
                    String value = extract(child);
                    if (value != null) {
                        return value;
                    }
                }
                return null;
            }
            if (element.isJsonObject()) {
                JsonObject obj = element.getAsJsonObject();
                return extract(obj.get("name"));
            }
            return null;
        }
    }
}
