package com.during.cityloader.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * NBT转换工具
 * 将JSON字符串转换为Bukkit可用的NBT数据结构
 * 
 * @author During
 * @since 1.5.0
 */
public class NBTConverter {

    /**
     * 将JSON字符串转换为NBT结构
     * 
     * @param jsonString JSON字符串
     * @return NBT对象（Map、List或基本类型）
     * @throws IllegalArgumentException 如果JSON格式无效
     */
    public static Object jsonToNBT(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return null;
        }

        try {
            JsonElement element = JsonParser.parseString(jsonString);
            return convertElement(element);
        } catch (Exception e) {
            throw new IllegalArgumentException("无效的JSON格式: " + e.getMessage(), e);
        }
    }

    /**
     * 递归转换JsonElement
     * 
     * @param element JSON元素
     * @return 对应的Java对象
     */
    private static Object convertElement(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }

        if (element.isJsonObject()) {
            return convertObject(element.getAsJsonObject());
        } else if (element.isJsonArray()) {
            return convertArray(element.getAsJsonArray());
        } else if (element.isJsonPrimitive()) {
            return convertPrimitive(element.getAsJsonPrimitive());
        }

        return null;
    }

    /**
     * 转换JsonObject为Map（CompoundTag）
     * 
     * @param obj JSON对象
     * @return Map<String, Object>
     */
    private static Map<String, Object> convertObject(JsonObject obj) {
        Map<String, Object> map = new HashMap<>();

        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            map.put(entry.getKey(), convertElement(entry.getValue()));
        }

        return map;
    }

    /**
     * 转换JsonArray为List（ListTag）
     * 
     * @param array JSON数组
     * @return List<Object>
     */
    private static List<Object> convertArray(JsonArray array) {
        List<Object> list = new ArrayList<>();

        for (JsonElement element : array) {
            list.add(convertElement(element));
        }

        return list;
    }

    /**
     * 转换JsonPrimitive为基本类型
     * 
     * @param primitive JSON基本类型
     * @return String、Number或Boolean
     */
    private static Object convertPrimitive(JsonPrimitive primitive) {
        if (primitive.isBoolean()) {
            return primitive.getAsBoolean();
        } else if (primitive.isNumber()) {
            Number number = primitive.getAsNumber();
            // 尝试保留原始数字类型
            if (number.doubleValue() == number.longValue()) {
                // 整数
                long longValue = number.longValue();
                if (longValue >= Integer.MIN_VALUE && longValue <= Integer.MAX_VALUE) {
                    return number.intValue();
                }
                return longValue;
            } else {
                // 浮点数
                return number.doubleValue();
            }
        } else if (primitive.isString()) {
            return primitive.getAsString();
        }

        return null;
    }

    /**
     * 将NBT结构转换回JSON字符串
     * 
     * @param nbt NBT对象（Map、List或基本类型）
     * @return JSON字符串
     */
    @SuppressWarnings("unchecked")
    public static String nbtToJson(Object nbt) {
        if (nbt == null) {
            return "null";
        }

        if (nbt instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) nbt;
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (!first)
                    sb.append(",");
                sb.append("\"").append(entry.getKey()).append("\":");
                sb.append(nbtToJson(entry.getValue()));
                first = false;
            }
            sb.append("}");
            return sb.toString();
        } else if (nbt instanceof List) {
            List<Object> list = (List<Object>) nbt;
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Object item : list) {
                if (!first)
                    sb.append(",");
                sb.append(nbtToJson(item));
                first = false;
            }
            sb.append("]");
            return sb.toString();
        } else if (nbt instanceof String) {
            return "\"" + nbt + "\"";
        } else if (nbt instanceof Boolean) {
            return nbt.toString();
        } else if (nbt instanceof Number) {
            return nbt.toString();
        }

        return "null";
    }
}
