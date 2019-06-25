package org.zzy.lib.bettercamera.bean;

import android.support.annotation.IntRange;
import android.support.annotation.NonNull;

/**
 * @作者 ZhouZhengyi
 * @创建日期 2019/6/4
 */
public class AspectRatio {

    private double ratio;

    public final int widthRatio;

    public final int heightRatio;

    private AspectRatio(int widthRatio, int heightRatio) {
        this.widthRatio = widthRatio;
        this.heightRatio = heightRatio;
    }

    public static AspectRatio of(@NonNull Size size) {
        return new AspectRatio(size.width, size.height);
    }

    public static AspectRatio of(@IntRange(from = 1) int widthRatio, @IntRange(from = 1) int heightRatio) {
        return new AspectRatio(widthRatio, heightRatio);
    }

    public double ratio() {
        if (ratio == 0 && widthRatio != 0) {
            ratio = (double) heightRatio / widthRatio;
        }
        return ratio;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o){ return true;}
        if (o == null || getClass() != o.getClass()){ return false;}

        AspectRatio that = (AspectRatio) o;

        return Double.compare(that.ratio(), ratio()) == 0;
    }

    @Override
    public int hashCode() {
        long temp = Double.doubleToLongBits(ratio());
        return (int) (temp ^ (temp >>> 32));
    }

    public static AspectRatio parse(String s) {
        int position = s.indexOf(':');
        if (position == -1) {
            throw new IllegalArgumentException("Malformed aspect ratio: " + s);
        }
        try {
            int x = Integer.parseInt(s.substring(0, position));
            int y = Integer.parseInt(s.substring(position + 1));
            return AspectRatio.of(x, y);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Malformed aspect ratio: " + s, e);
        }
    }

    public AspectRatio inverse() {
        return AspectRatio.of(heightRatio, widthRatio);
    }
}
