package com.during.cityloader.worldgen.lost;

/**
 * 多区块建筑位置
 * 表示一个区块在多区块建筑中的位置
 * 
 * @author During
 * @since 1.4.0
 */
public class MultiPos {
    
    private final int x;  // 在多建筑中的X偏移
    private final int z;  // 在多建筑中的Z偏移
    private final int w;  // 多建筑宽度
    private final int h;  // 多建筑高度
    
    /**
     * 构造多区块建筑位置
     * 
     * @param x X偏移
     * @param z Z偏移
     * @param w 宽度
     * @param h 高度
     */
    public MultiPos(int x, int z, int w, int h) {
        this.x = x;
        this.z = z;
        this.w = w;
        this.h = h;
    }
    
    /**
     * 获取X偏移
     * 
     * @return X偏移
     */
    public int getX() {
        return x;
    }
    
    /**
     * 获取Z偏移
     * 
     * @return Z偏移
     */
    public int getZ() {
        return z;
    }
    
    /**
     * 获取宽度
     * 
     * @return 宽度
     */
    public int getW() {
        return w;
    }
    
    /**
     * 获取高度
     * 
     * @return 高度
     */
    public int getH() {
        return h;
    }
    
    /**
     * 是否为左上角
     * 
     * @return 如果是左上角返回true
     */
    public boolean isTopLeft() {
        return x == 0 && z == 0;
    }
    
    /**
     * 是否为单区块建筑
     * 
     * @return 如果是单区块建筑返回true
     */
    public boolean isSingle() {
        return w == 1 && h == 1;
    }
    
    /**
     * 是否为多区块建筑
     * 
     * @return 如果是多区块建筑返回true
     */
    public boolean isMulti() {
        return w > 1 || h > 1;
    }
    
    @Override
    public String toString() {
        return "MultiPos{" +
                "x=" + x +
                ", z=" + z +
                ", w=" + w +
                ", h=" + h +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MultiPos multiPos = (MultiPos) o;
        return x == multiPos.x && z == multiPos.z && w == multiPos.w && h == multiPos.h;
    }
    
    @Override
    public int hashCode() {
        int result = x;
        result = 31 * result + z;
        result = 31 * result + w;
        result = 31 * result + h;
        return result;
    }
}
