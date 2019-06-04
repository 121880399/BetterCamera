package org.zzy.lib.bettercamera.constant;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @作者 ZhouZhengyi
 * @创建日期 2019/6/4
 */
public final class CameraConstant {
    /**
     * 后置摄像头
     */
    public static final int FACE_REAR  = 0x0;
    /**
     * 前置摄像头
     */
    public static final int FACE_FRONT  = 0x1;

    /**
     * 打开闪光灯
     */
    public static final int FLASH_ON = 0x3;
    /**
     * 关闭闪光灯
     */
    public static final int FLASH_OFF = 0x4;
    /**
     * 自动闪光灯
     */
    public static final int FLASH_AUTO = 0x5;

    /**
     * 预览size
     */
    public static final int SIZE_FOR_PREVIEW    = 0x10;
    /**
     * 照片size
     */
    public static final int SIZE_FOR_PICTURE    = 0x20;
    /**
     * 视频size
     */
    public static final int SIZE_FOR_VIDEO      = 0x40;

    /**
     * Camera1
     */
    public static final int TYPE_CAMERA1        = 0x100;
    /**
     * Camera2
     */
    public static final int TYPE_CAMERA2        = 0x200;

    @IntDef(value = {FACE_REAR, FACE_FRONT})
    @Retention(value = RetentionPolicy.SOURCE)
    public @interface Face {
    }

    @IntDef({FLASH_ON, FLASH_OFF, FLASH_AUTO})
    @Retention(RetentionPolicy.SOURCE)
    public @interface FlashMode {
    }

    @IntDef(value = {SIZE_FOR_PREVIEW, SIZE_FOR_PICTURE, SIZE_FOR_VIDEO})
    @Retention(value = RetentionPolicy.SOURCE)
    public @interface SizeFor {
    }
}
