package org.zzy.lib.bettercamera.config;


import android.annotation.TargetApi;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.MediaRecorder;
import android.os.Build;
import android.util.SparseArray;

import org.zzy.lib.bettercamera.bean.AspectRatio;
import org.zzy.lib.bettercamera.bean.Size;
import org.zzy.lib.bettercamera.config.calculator.CameraSizeCalculator;
import org.zzy.lib.bettercamera.config.calculator.impl.CameraSizeCalculatorImpl;
import org.zzy.lib.bettercamera.config.creator.CameraManagerCreator;
import org.zzy.lib.bettercamera.config.creator.CameraPreviewCreator;
import org.zzy.lib.bettercamera.config.creator.impl.CameraManagerCreatorImpl;
import org.zzy.lib.bettercamera.config.creator.impl.CameraPreviewCreatorImpl;
import org.zzy.lib.bettercamera.constant.CameraConstant;
import org.zzy.lib.bettercamera.constant.MediaConstant;

import java.util.ArrayList;
import java.util.List;

/**
 * @作者 ZhouZhengyi
 * @创建日期 2019/6/4
 */
public class ConfigProvider {

    private  static volatile ConfigProvider configProvider;

    private CameraManagerCreator mCameraManagerCreator;

    private CameraPreviewCreator mCameraPreviewCreator;

    private CameraSizeCalculator mCameraSizeCalculator;

    /**
     * 使用缓存
     */
    private boolean useCache;

    private SparseArray<List<Size>> sizeMap;

    private SparseArray<List<Float>> ratioMap;


    /**
     * 默认的摄像头前后
     */
    @CameraConstant.Face
    private int defaultCameraFace;

    /**
     * 默认的类型
     */
    @MediaConstant.Type
    private int defaultMediaType;

    /**
     * 默认质量
     */
    @MediaConstant.Quality
    private int defaultMediaQuality;

    /**
     * 默认宽高比
     */
    private AspectRatio defaultAspectRatio;

    /**
     * 是否开启快门声
     */
    private boolean isVoiceEnable;

    /**
     * 是否自动对焦
     */
    private boolean isAutoFocus;

    /**
     * 闪光灯模式
     */
    @CameraConstant.FlashMode
    private int defaultFlashMode;

    /**
     * 默认视频尺寸
     */
    private long defaultVideoFileSize = -1;

    /**
     * 默认视频时长
     */
    private int defaultVideoDuration = -1;

    /**
     * 是否是debug模式
     */
    private boolean isDebug;


    private ConfigProvider(){
        initWithDefaultValues();
    }

    public static ConfigProvider getInstance(){
        if(configProvider == null){
            synchronized (ConfigProvider.class){
                if(configProvider == null){
                    configProvider = new ConfigProvider();
                }
            }
        }
        return configProvider;
    }

    /**
     * 用默认值进行初始化
     * 默认使用缓存
     * 默认后置摄像头
     * 默认拍照
     * 默认高质量
     * 默认宽高比为3：4
     * 默认开启快门声
     * 默认自动对焦
     * 默认闪光灯自动模式
     */
    private void initWithDefaultValues(){
        sizeMap = new SparseArray<>();
        ratioMap = new SparseArray<>();
        mCameraManagerCreator = new CameraManagerCreatorImpl();
        mCameraPreviewCreator = new CameraPreviewCreatorImpl();
        mCameraSizeCalculator = new CameraSizeCalculatorImpl();
        useCache = true;
        defaultCameraFace = CameraConstant.FACE_REAR;
        defaultMediaType = MediaConstant.TYPE_PICTURE;
        defaultMediaQuality = MediaConstant.QUALITY_HIGH;
        defaultAspectRatio = AspectRatio.of(3, 4);
        isVoiceEnable = true;
        isAutoFocus = true;
        defaultFlashMode = CameraConstant.FLASH_AUTO;
    }

    public CameraManagerCreator getCameraManagerCreator() {
        return mCameraManagerCreator;
    }

    public void setCameraManagerCreator(CameraManagerCreator cameraManagerCreator) {
        mCameraManagerCreator = cameraManagerCreator;
    }

    public CameraPreviewCreator getCameraPreviewCreator() {
        return mCameraPreviewCreator;
    }

    public void setCameraPreviewCreator(CameraPreviewCreator cameraPreviewCreator) {
        mCameraPreviewCreator = cameraPreviewCreator;
    }

    public CameraSizeCalculator getCameraSizeCalculator() {
        return mCameraSizeCalculator;
    }

    public void setCameraSizeCalculator(CameraSizeCalculator cameraSizeCalculator) {
        mCameraSizeCalculator = cameraSizeCalculator;
    }

    public boolean isUseCache() {
        return useCache;
    }

    public void setUseCache(boolean useCache) {
        this.useCache = useCache;
    }

    @CameraConstant.Face
    public int getDefaultCameraFace() {
        return defaultCameraFace;
    }

    public void setDefaultCameraFace(@CameraConstant.Face int defaultCameraFace) {
        this.defaultCameraFace = defaultCameraFace;
    }

    @MediaConstant.Type
    public int getDefaultMediaType() {
        return defaultMediaType;
    }

    public void setDefaultMediaType(@MediaConstant.Type int defaultMediaType) {
        this.defaultMediaType = defaultMediaType;
    }

    @MediaConstant.Quality
    public int getDefaultMediaQuality() {
        return defaultMediaQuality;
    }

    public void setDefaultMediaQuality(@MediaConstant.Quality  int defaultMediaQuality) {
        this.defaultMediaQuality = defaultMediaQuality;
    }

    public AspectRatio getDefaultAspectRatio() {
        return defaultAspectRatio;
    }

    public void setDefaultAspectRatio(AspectRatio defaultAspectRatio) {
        this.defaultAspectRatio = defaultAspectRatio;
    }

    public boolean isVoiceEnable() {
        return isVoiceEnable;
    }

    public void setVoiceEnable(boolean voiceEnable) {
        isVoiceEnable = voiceEnable;
    }

    public boolean isAutoFocus() {
        return isAutoFocus;
    }

    public void setAutoFocus(boolean autoFocus) {
        isAutoFocus = autoFocus;
    }

    @CameraConstant.FlashMode
    public int getDefaultFlashMode() {
        return defaultFlashMode;
    }

    public void setDefaultFlashMode(@CameraConstant.FlashMode int defaultFlashMode) {
        this.defaultFlashMode = defaultFlashMode;
    }

    public long getDefaultVideoFileSize() {
        return defaultVideoFileSize;
    }

    public void setDefaultVideoFileSize(long defaultVideoFileSize) {
        this.defaultVideoFileSize = defaultVideoFileSize;
    }

    public int getDefaultVideoDuration() {
        return defaultVideoDuration;
    }

    public void setDefaultVideoDuration(int defaultVideoDuration) {
        this.defaultVideoDuration = defaultVideoDuration;
    }

    public boolean isDebug() {
        return isDebug;
    }

    public void setDebug(boolean debug) {
        isDebug = debug;
    }

    /**
     * 得到相机支持的预览，照片，视频尺寸，并转换成我们自己定义的Size
     */
    public List<Size> getSizes(android.hardware.Camera camera, @CameraConstant.Face int cameraFace, @CameraConstant.SizeFor int
            sizeFor) {
        int hash = cameraFace | sizeFor | CameraConstant.TYPE_CAMERA1;
        if (useCache) {
            List<Size> sizes = sizeMap.get(hash);
            if (sizes != null) {
                return sizes;
            }
        }
        List<Size> sizes;
        switch (sizeFor) {
            case CameraConstant.SIZE_FOR_PICTURE:
                sizes = Size.fromList(camera.getParameters().getSupportedPictureSizes());
                break;
            case CameraConstant.SIZE_FOR_PREVIEW:
                sizes = Size.fromList(camera.getParameters().getSupportedPreviewSizes());
                break;
            case CameraConstant.SIZE_FOR_VIDEO:
                sizes = Size.fromList(camera.getParameters().getSupportedVideoSizes());
                break;
            default:
                throw new IllegalArgumentException("Unsupported size for " + sizeFor);
        }
        if (useCache) {
            sizeMap.put(hash, sizes);
        }
        return sizes;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public List<Size> getSizes(StreamConfigurationMap configurationMap, @CameraConstant.Face int cameraFace, @CameraConstant.SizeFor int sizeFor) {
        int hash = cameraFace | sizeFor | CameraConstant.TYPE_CAMERA2;
        if (useCache) {
            List<Size> sizes = sizeMap.get(hash);
            if (sizes != null) {
                return sizes;
            }
        }
        List<Size> sizes;
        switch (sizeFor) {
            case CameraConstant.SIZE_FOR_PICTURE:
                sizes = Size.fromList(configurationMap.getOutputSizes(ImageFormat.JPEG));
                break;
            case CameraConstant.SIZE_FOR_PREVIEW:
                sizes = Size.fromList(configurationMap.getOutputSizes(SurfaceTexture.class));
                break;
            case CameraConstant.SIZE_FOR_VIDEO:
                sizes = Size.fromList(configurationMap.getOutputSizes(MediaRecorder.class));
                break;
            default:
                throw new IllegalArgumentException("Unsupported size for " + sizeFor);
        }
        if (useCache) {
            sizeMap.put(hash, sizes);
        }
        return sizes;
    }


    /**
     * 得到相机支持的缩放比列
     */
    public List<Float> getZoomRatios(android.hardware.Camera camera, @CameraConstant.Face int cameraFace) {
        int hash = cameraFace | CameraConstant.TYPE_CAMERA1;
        if (useCache) {
            List<Float> zoomRatios = ratioMap.get(hash);
            if (zoomRatios != null) {
                return zoomRatios;
            }
        }
        List<Integer> ratios = camera.getParameters().getZoomRatios();
        List<Float> result = new ArrayList<>(ratios.size());
        for (Integer ratio : ratios) {
            result.add(ratio * 0.01f);
        }
        if (useCache) {
            ratioMap.put(hash, result);
        }
        return result;
    }

}
