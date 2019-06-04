package org.zzy.lib.bettercamera.config.calculator;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.zzy.lib.bettercamera.bean.AspectRatio;
import org.zzy.lib.bettercamera.bean.Size;

import java.util.List;

/**
 * @作者 ZhouZhengyi
 * @创建日期 2019/6/4
 */
public interface CameraSizeCalculator {
    /**
     * 得到照片预览的尺寸
     */
    Size getPicturePreviewSize (@NonNull List<Size> previewSizes,@NonNull Size pictureSize);

    /**
     * 得到视频预览的尺寸
     */
    Size getVideoPreviewSize(@NonNull List<Size> previewSizes, @NonNull Size videoSize);

    /**
     * 得到照片尺寸
     */
    Size getPictureSize(@NonNull List<Size> pictureSizes, @NonNull AspectRatio expectAspectRatio, @Nullable Size expectSize);


    /**
     * 得到视频尺寸
     */
    Size getVideoSize(@NonNull List<Size> videoSizes, @NonNull AspectRatio expectAspectRatio, @Nullable Size expectSize);
}
