package org.zzy.lib.bettercamera.config.calculator.impl;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.zzy.lib.bettercamera.bean.AspectRatio;
import org.zzy.lib.bettercamera.bean.Size;
import org.zzy.lib.bettercamera.config.calculator.CameraSizeCalculator;
import org.zzy.lib.bettercamera.utils.CameraHelper;
import org.zzy.lib.bettercamera.utils.Logger;

import java.util.List;

/**
 * @作者 ZhouZhengyi
 * @创建日期 2019/6/4
 */
public class CameraSizeCalculatorImpl implements CameraSizeCalculator {

    private static final String TAG = "CameraSizeCalculatorImp";

    /**
     * 上一次预览照片的尺寸
     */
    private Size lastPictureSizeForPreview;

    /**
     * 上一次预览视频的尺寸
     */
    private Size lastVideoSizeForPreview;

    /**
     * 上一次照片的尺寸
     */
    private Size lastPictureSize;

    /**
     * 上一次视频的尺寸
     */
    private Size lastVideoSize;
    /**
     * 上次视频尺寸计算方法
     */
    private SizeCalculatorMethod lastVideoSizeCalculatorMethod;
    /**
     * 上次照片尺寸计算方法
     */
    private SizeCalculatorMethod lastPictureSizeCalculatorMethod;

    @Override
    public Size getPicturePreviewSize(@NonNull List<Size> previewSizes, @NonNull Size pictureSize) {
        //如果上次照片尺寸为空或者本次的照片尺寸和上次的不一样，就重新计算照片尺寸
        //picturePreviewSize与lastPictureSizeForPreview的值不一样
        if (lastPictureSizeForPreview == null || !lastPictureSizeForPreview.equals(pictureSize)) {
            lastPictureSizeForPreview = pictureSize;
            lastPictureSizeForPreview = CameraHelper.getSizeWithClosestRatio(previewSizes, pictureSize);
        }
        return lastPictureSizeForPreview;
    }

    @Override
    public Size getVideoPreviewSize(@NonNull List<Size> previewSizes, @NonNull Size videoSize) {
        //如果上次视频尺寸为空或者本次视频的尺寸和上次的不一样，就重新计算视频尺寸
        if (lastVideoSizeForPreview == null || !lastVideoSizeForPreview.equals(videoSize)) {
            lastVideoSizeForPreview = videoSize;
            lastVideoSizeForPreview = CameraHelper.getSizeWithClosestRatio(previewSizes, videoSize);
        }
        return lastVideoSizeForPreview;
    }

    @Override
    public Size getPictureSize(@NonNull List<Size> pictureSizes, @NonNull AspectRatio expectAspectRatio,
            @Nullable Size expectSize) {
        SizeCalculatorMethod sizeCalculatorMethod = new SizeCalculatorMethod(expectAspectRatio, expectSize);
        if (lastPictureSize == null || !sizeCalculatorMethod.equals(lastPictureSizeCalculatorMethod)) {
            lastPictureSizeCalculatorMethod = sizeCalculatorMethod;
            if (expectSize == null) {
                //如果为空，给出默认值
                expectSize = Size.of((int) (4000 * expectAspectRatio.ratio()), 4000 );
            }
            lastPictureSize = CameraHelper.getSizeWithClosestRatio(pictureSizes, expectSize);
        }
        Logger.d(TAG, "getVideoSize : " + lastPictureSize);
        return lastPictureSize;
    }

    @Override
    public Size getVideoSize(@NonNull List<Size> videoSizes, @NonNull AspectRatio expectAspectRatio,
            @Nullable Size expectSize) {
        SizeCalculatorMethod sizeCalculatorMethod = new SizeCalculatorMethod(expectAspectRatio, expectSize);
        if (lastVideoSize == null || !sizeCalculatorMethod.equals(lastVideoSizeCalculatorMethod)) {
            lastVideoSizeCalculatorMethod = sizeCalculatorMethod;
            if (expectSize == null) {
                expectSize = Size.of((int) (4000 * expectAspectRatio.ratio()), 4000 );
            }
            lastVideoSize = CameraHelper.getSizeWithClosestRatio(videoSizes, expectSize);
        }
        Logger.d(TAG, "getVideoSize : " + lastVideoSize);
        return lastVideoSize;
    }

    private static class SizeCalculatorMethod {
        /**
         * 期望的宽高比
         */
        @NonNull
        private AspectRatio expectAspectRatio;
        /**
         * 期望的尺寸
         */
        @Nullable
        private Size expectSize;

        SizeCalculatorMethod(@NonNull AspectRatio expectAspectRatio, @Nullable Size expectSize) {
            this.expectAspectRatio = expectAspectRatio;
            this.expectSize = expectSize;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) { return true;}
            if (o == null || getClass() != o.getClass()) { return false;}

            SizeCalculatorMethod that = (SizeCalculatorMethod) o;

            if (!expectAspectRatio.equals(that.expectAspectRatio)) { return false;}
            return expectSize != null ? expectSize.equals(that.expectSize) : that.expectSize == null;
        }

        @Override
        public int hashCode() {
            int result = expectAspectRatio.hashCode();
            result = 31 * result + (expectSize != null ? expectSize.hashCode() : 0);
            return result;
        }
    }
}
