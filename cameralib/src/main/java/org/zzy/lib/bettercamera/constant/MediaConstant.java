package org.zzy.lib.bettercamera.constant;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @作者 ZhouZhengyi
 * @创建日期 2019/6/4
 */
public final class MediaConstant {

    /**
     * 拍照
     */
    public static final int TYPE_PICTURE = 0;
    /**
     * 拍视频
     */
    public static final int TYPE_VIDEO = 1;

    /**
     * 自动
     */
    public static final int QUALITY_AUTO = 0;
    /**
     * 最低质量
     */
    public static final int QUALITY_LOWEST = 1;
    /**
     * 低质量
     */
    public static final int QUALITY_LOW = 2;
    /**
     * 中等质量
     */
    public static final int QUALITY_MEDIUM = 3;
    /**
     * 高质量
     */
    public static final int QUALITY_HIGH = 4;
    /**
     * 最高质量
     */
    public static final int QUALITY_HIGHEST = 5;

    @IntDef(value = {TYPE_PICTURE, TYPE_VIDEO})
    @Retention(value = RetentionPolicy.SOURCE)
    public @interface Type {
    }

    @IntDef(value = {QUALITY_AUTO, QUALITY_LOWEST, QUALITY_LOW, QUALITY_MEDIUM, QUALITY_HIGH, QUALITY_HIGHEST})
    @Retention(value = RetentionPolicy.SOURCE)
    public @interface Quality {
    }
}
