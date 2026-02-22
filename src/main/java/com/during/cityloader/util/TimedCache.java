package com.during.cityloader.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 定时缓存工具类
 * 提供自动过期的线程安全缓存
 * 
 * @param <K> 键类型
 * @param <V> 值类型
 * 
 * @author During
 * @since 1.4.0
 */
public class TimedCache<K, V> {
    
    private final Map<K, CacheEntry<V>> cache = new ConcurrentHashMap<>();
    private final Supplier<Integer> timeoutSupplier;
    
    /**
     * 缓存条目
     * 
     * @param <V> 值类型
     */
    private static class CacheEntry<V> {
        private final V value;
        private final long timestamp;
        
        public CacheEntry(V value) {
            this.value = value;
            this.timestamp = System.currentTimeMillis();
        }
        
        public V getValue() {
            return value;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
    }
    
    /**
     * 构造定时缓存
     * 
     * @param timeoutSupplier 超时时间供应器（秒）
     */
    public TimedCache(Supplier<Integer> timeoutSupplier) {
        this.timeoutSupplier = timeoutSupplier;
    }
    
    /**
     * 获取缓存值
     * 
     * @param key 键
     * @return 缓存值，如果不存在或已过期则返回null
     */
    public V get(K key) {
        CacheEntry<V> entry = cache.get(key);
        if (entry == null) {
            return null;
        }
        
        // 检查是否过期
        long timeout = timeoutSupplier.get() * 1000L;
        if (System.currentTimeMillis() - entry.getTimestamp() > timeout) {
            cache.remove(key);
            return null;
        }
        
        return entry.getValue();
    }
    
    /**
     * 放入缓存值
     * 
     * @param key 键
     * @param value 值
     */
    public void put(K key, V value) {
        cache.put(key, new CacheEntry<>(value));
    }
    
    /**
     * 计算并缓存值
     * 如果键已存在且未过期，返回缓存值；否则计算新值并缓存
     * 
     * @param key 键
     * @param mappingFunction 计算函数
     * @return 缓存值或新计算的值
     */
    public V computeIfAbsent(K key, java.util.function.Function<? super K, ? extends V> mappingFunction) {
        long timeout = timeoutSupplier.get() * 1000L;
        long now = System.currentTimeMillis();
        CacheEntry<V> entry = cache.compute(key, (k, existing) -> {
            if (existing != null && now - existing.getTimestamp() <= timeout) {
                return existing;
            }
            V computed = mappingFunction.apply(k);
            if (computed == null) {
                return null;
            }
            return new CacheEntry<>(computed);
        });
        if (entry == null) {
            return null;
        }
        return entry.getValue();
    }
    
    /**
     * 移除缓存值
     * 
     * @param key 键
     */
    public void remove(K key) {
        cache.remove(key);
    }
    
    /**
     * 清空缓存
     */
    public void clear() {
        cache.clear();
    }
    
    /**
     * 获取缓存大小
     * 
     * @return 缓存条目数量
     */
    public int size() {
        return cache.size();
    }
    
    /**
     * 清理过期条目
     */
    public void cleanup() {
        long timeout = timeoutSupplier.get() * 1000L;
        long now = System.currentTimeMillis();
        
        cache.entrySet().removeIf(entry -> 
            now - entry.getValue().getTimestamp() > timeout
        );
    }
}
