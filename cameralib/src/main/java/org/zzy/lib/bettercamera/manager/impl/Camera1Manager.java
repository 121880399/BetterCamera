package org.zzy.lib.bettercamera.manager.impl;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.sun.org.apache.bcel.internal.generic.RETURN;

import org.zzy.lib.bettercamera.bean.Size;
import org.zzy.lib.bettercamera.bean.SizeMap;
import org.zzy.lib.bettercamera.config.ConfigProvider;
import org.zzy.lib.bettercamera.config.calculator.CameraSizeCalculator;
import org.zzy.lib.bettercamera.constant.CameraConstant;
import org.zzy.lib.bettercamera.constant.MediaConstant;
import org.zzy.lib.bettercamera.constant.PreviewConstant;
import org.zzy.lib.bettercamera.listener.CameraOpenListener;
import org.zzy.lib.bettercamera.listener.CameraVideoListener;
import org.zzy.lib.bettercamera.preview.CameraPreview;
import org.zzy.lib.bettercamera.preview.CameraPreviewCallback;
import org.zzy.lib.bettercamera.utils.CameraHelper;
import org.zzy.lib.bettercamera.utils.Logger;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @作者 ZhouZhengyi
 * @创建日期 2019/6/4
 */
public class Camera1Manager extends BaseCameraManager<Integer> {

    private static final String TAG = "Camera1Manager";

    /**
     * 相机
     */
    private android.hardware.Camera camera;

    /**
     * 正在显示预览
     */
    private volatile boolean showingPreview;

    /**
     * 缩放比例
     */
    private List<Float> zoomRatios;

    public Camera1Manager(CameraPreview cameraPreview) {
        super(cameraPreview);
        cameraPreview.setCameraPreviewCallback(new CameraPreviewCallback() {
            @Override
            public void onAvailable(CameraPreview cameraPreview) {
                Logger.d(TAG, "onAvailable : " + cameraPreview.isAvailable());
                if(isCameraOpened()){
                    setupPreview();
                }
            }
        });
    }

    /**
     * 设置预览和方向
     */
    private void setupPreview(){
        try {
            //如果正在显示预览，先停止
            if (showingPreview) {
                camera.stopPreview();
                showingPreview = false;
            }
            //如果创建的是SurfaceView，给Camera设置相应的view
            if (cameraPreview.getPreviewType() == PreviewConstant.SURFACE_VIEW) {
                //为camera设置预览
                camera.setPreviewDisplay(cameraPreview.getSurfaceHolder());
            }else{
                camera.setPreviewTexture(cameraPreview.getSurfaceTexture());
            }
            if(!showingPreview){
                //camera启动预览
                camera.startPreview();
                showingPreview = true;
            }
            camera.setDisplayOrientation(CameraHelper.calDisplayOrientation(context,cameraFace,
                    cameraFace == CameraConstant.FACE_FRONT ? frontCameraOrientation : rearCameraOrientation));
        }catch (IOException e){
            notifyCameraOpenError(new RuntimeException(e));
        }
    }

    @Override
    public void initialize(Context context) {
        super.initialize(context);
        initCameraInfo();
    }

    /**
     * 初始化Camera信息
     */
    private void initCameraInfo(){
        // zzyToDo: 2019/6/5 这里没验证如果是双摄，或者三摄时的返回情况
        //得到本设备有几个可用相机
        numberOfCameras = Camera.getNumberOfCameras();
        for(int i=0;i<numberOfCameras;i++){
            CameraInfo cameraInfo = new CameraInfo();
            Camera.getCameraInfo(i,cameraInfo);
            if(cameraInfo.facing == CameraInfo.CAMERA_FACING_BACK){
                //如果是后置摄像头，记录是第几个
                rearCameraId = i;
                //记录后置摄像头的方向
                rearCameraOrientation = cameraInfo.orientation;
            }else if(cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT){
                //如果是前置摄像头，记录是第几个
                frontCameraId = i;
                //记录前置摄像头的方向
                frontCameraOrientation = cameraInfo.orientation;
            }
        }
        //设置当前使用的摄像头是第几个
        currentCameraId = cameraFace == CameraConstant.FACE_REAR ? rearCameraId : frontCameraId;
    }

    @Override
    public void openCamera(CameraOpenListener cameraOpenListener) {
        super.openCamera(cameraOpenListener);
        backgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                Logger.d(TAG, "openCamera");
                try{
                    //打开相机,得到相机实例
                    camera = Camera.open(currentCameraId);
                    //得到相机支持尺寸信息
                    getCameraSizesInfo();
                    //调整相机参数
                    adjustCameraParameters(false,true,true);
                    if(cameraPreview.isAvailable()){
                        setupPreview();
                        notifyCameraOpened();
                    }else {
                        throw new Exception("CameraPreview width or height  <= 0");
                    }
                }catch (Exception e){
                    Logger.e(TAG, "error : " + e);
                    notifyCameraOpenError(e);
                }
            }
        });

    }

    /**
     * 得到相机支持的尺寸和缩放比
     */
    private void getCameraSizesInfo(){
        try{
            long start = System.currentTimeMillis();
            previewSizes = ConfigProvider.getInstance().getSizes(camera,cameraFace,CameraConstant.SIZE_FOR_PREVIEW);
            pictureSizes = ConfigProvider.getInstance().getSizes(camera,cameraFace,CameraConstant.SIZE_FOR_PICTURE);
            videoSizes = ConfigProvider.getInstance().getSizes(camera,cameraFace,CameraConstant.SIZE_FOR_VIDEO);
            zoomRatios = ConfigProvider.getInstance().getZoomRatios(camera,cameraFace);
            Logger.d(TAG, "getCameraSizesInfo cost : " + (System.currentTimeMillis() - start) + " ms");
        }catch (Exception e){
            Logger.e(TAG, "error : " + e);
            notifyCameraOpenError(new RuntimeException(e));
        }
    }

    /**
     * 设置Camera的pictureSize
     * 设置Camera的previewSize
     * @param forceCalculateSizes 是否强制计算
     * @param changeFocusMode 是否改变对焦模式
     * @param changeFlashMode 是否改变闪光灯模式
     */
    private void adjustCameraParameters(boolean forceCalculateSizes,boolean changeFocusMode,boolean changeFlashMode){
        Size oldPreview = previewSize;
        long start = System.currentTimeMillis();
        CameraSizeCalculator cameraSizeCalculator = ConfigProvider.getInstance().getCameraSizeCalculator();
        Camera.Parameters parameters = camera.getParameters();
        //如果是照片，并且照片的尺寸为空，或者照片尺寸不为空强制计算尺寸
        if(mediaType == MediaConstant.TYPE_PICTURE && (pictureSize == null || forceCalculateSizes)){
            pictureSize = cameraSizeCalculator.getPictureSize(pictureSizes,expectAspectRatio,expectPictureSize);
            previewSize = cameraSizeCalculator.getPicturePreviewSize(previewSizes,pictureSize);
            parameters.setPictureSize(pictureSize.width,pictureSize.height);
            notifyPictureSizeUpdated(pictureSize);
        }else if(mediaType == MediaConstant.TYPE_VIDEO){
            if(camcorderProfile == null || forceCalculateSizes) {
                camcorderProfile = CameraHelper.getCamcorderProfile(mediaQuality, currentCameraId);
            }
            if(videoSize == null || forceCalculateSizes){
                videoSize = cameraSizeCalculator.getVideoSize(videoSizes, expectAspectRatio, expectPictureSize);
                previewSize = cameraSizeCalculator.getVideoPreviewSize(previewSizes, videoSize);
                notifyVideoSizeUpdated(previewSize);
            }
        }
        if(!previewSize.equals(oldPreview)){
            parameters.setPreviewSize(previewSize.width,previewSize.height);
            notifyPreviewSizeUpdated(previewSize);
        }
        Logger.d(TAG, "adjustCameraParameters size cost : " + (System.currentTimeMillis() - start) + " ms");

        start = System.currentTimeMillis();
        if(changeFocusMode){
            setFocusModeInternal(parameters);
        }
        Logger.d(TAG, "adjustCameraParameters focus cost : " + (System.currentTimeMillis() - start) + " ms");
        start = System.currentTimeMillis();
        if(changeFlashMode){
            setFlashModeInternal(parameters);
        }
        Logger.d(TAG, "adjustCameraParameters flash cost : " + (System.currentTimeMillis() - start) + " ms");

        start = System.currentTimeMillis();
        setZoomInternal(parameters);
        Logger.d(TAG, "adjustCameraParameters zoom cost : " + (System.currentTimeMillis() - start) + " ms");

        start = System.currentTimeMillis();
        if(showingPreview){
         camera.stopPreview();
         showingPreview = false;
        }
        camera.setParameters(parameters);
        if(!showingPreview){
            showingPreview = true;
            camera.startPreview();
        }
        Logger.d(TAG, "adjustCameraParameters restart preview cost : " + (System.currentTimeMillis() - start) + " ms");
    }

    private void setFocusModeInternal(Camera.Parameters parameters){
        boolean isNullParametes = parameters == null;
        parameters = isNullParametes ? camera.getParameters() : parameters;
        if(mediaType == MediaConstant.TYPE_VIDEO){
            if(!isVideoContinuousFocusMode(parameters)) {
                setAutoFocusMode(parameters);
            }
        }else if(mediaType == MediaConstant.TYPE_PICTURE){
            if(!isPictureContinuousFocusMode(parameters)) {
                setAutoFocusMode(parameters);
            }
        }
        if(isNullParametes){
            camera.setParameters(parameters);
        }
    }

    /**
     * 照片是否支持连续自动对焦模式
     */
    private boolean isPictureContinuousFocusMode(Camera.Parameters parameters){
        if(parameters.getSupportedFocusModes().contains(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)){
            parameters.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            return true;
        }
        return false;
    }

    /**
     * 视频是否支持连续自动对焦模式
     */
    private boolean isVideoContinuousFocusMode(@NonNull android.hardware.Camera.Parameters parameters) {
        if (parameters.getSupportedFocusModes().contains(
                android.hardware.Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
            parameters.setFocusMode(android.hardware.Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            return true;
        }
        return false;
    }

    /**
     * 设置相机的对焦模式
     */
    private void setAutoFocusMode(@NonNull android.hardware.Camera.Parameters parameters) {
        try {
            final List<String> modes = parameters.getSupportedFocusModes();
            if (isAutoFocus && modes.contains(android.hardware.Camera.Parameters.FOCUS_MODE_AUTO)) {
                parameters.setFocusMode(android.hardware.Camera.Parameters.FOCUS_MODE_AUTO);
            } else if (modes.contains(android.hardware.Camera.Parameters.FOCUS_MODE_FIXED)) {
                parameters.setFocusMode(android.hardware.Camera.Parameters.FOCUS_MODE_FIXED);
            } else if (modes.contains(android.hardware.Camera.Parameters.FOCUS_MODE_INFINITY)) {
                parameters.setFocusMode(android.hardware.Camera.Parameters.FOCUS_MODE_INFINITY);
            } else {
                parameters.setFocusMode(modes.get(0));
            }
        } catch (Exception ex) {
            Logger.e(TAG, "setAutoFocusInternal " + ex);
        }
    }

    /**
     * 设置闪光灯模式
     */
    private void setFlashModeInternal(@Nullable android.hardware.Camera.Parameters parameters) {
        boolean nullParameters = parameters == null;
        parameters = nullParameters ? camera.getParameters() : parameters;
        List<String> modes = parameters.getSupportedFlashModes();
        try {
            switch (flashMode) {
                case CameraConstant.FLASH_ON:
                    setFlashModeOrAuto(parameters, modes, android.hardware.Camera.Parameters.FLASH_MODE_ON);
                    break;
                case CameraConstant.FLASH_OFF:
                    setFlashModeOrAuto(parameters, modes, android.hardware.Camera.Parameters.FLASH_MODE_OFF);
                    break;
                case CameraConstant.FLASH_AUTO:
                default:
                    if (modes.contains(android.hardware.Camera.Parameters.FLASH_MODE_AUTO)) {
                        parameters.setFlashMode(android.hardware.Camera.Parameters.FLASH_MODE_AUTO);
                    }
                    break;
            }
            if (nullParameters) {
                camera.setParameters(parameters);
            }
        } catch (Exception ex) {
            Logger.e(TAG, "setFlashModeInternal : " + ex);
        }
    }


    private void setFlashModeOrAuto(android.hardware.Camera.Parameters parameters, List<String> supportModes, String mode) {
        if (supportModes.contains(mode)) {
            parameters.setFlashMode(mode);
        } else {
            if (supportModes.contains(android.hardware.Camera.Parameters.FLASH_MODE_AUTO)) {
                parameters.setFlashMode(android.hardware.Camera.Parameters.FLASH_MODE_AUTO);
            }
        }
    }

    /**
     * 设置缩放比例
     */
    private void setZoomInternal(@Nullable android.hardware.Camera.Parameters parameters) {
        boolean nullParameters = parameters == null;
        parameters = nullParameters ? camera.getParameters() : parameters;
        if (parameters.isZoomSupported()) {
            List<Integer> zoomRatios = parameters.getZoomRatios();
            int zoomIdx = CameraHelper.getZoomIdxForZoomFactor(zoomRatios, zoom);
            parameters.setZoom(zoomRatios.get(zoomIdx));
            if (nullParameters) {
                camera.setParameters(parameters);
            }
        }
    }

    @Override
    public boolean isCameraOpened() {
        return camera != null;
    }

    @Override
    public void setMediaType(int mediaType) {

    }

    @Override
    public void setVoiceEnable(boolean voiceEnable) {

    }

    @Override
    public boolean isVoiceEnable() {
        return false;
    }

    @Override
    public void setAutoFocus(boolean autoFocus) {

    }

    @Override
    public boolean isAutoFocus() {
        return false;
    }

    @Override
    public void setFlashMode(int flashMode) {

    }

    @Override
    public int getFlashMode() {
        return 0;
    }

    @Override
    public void setZoom(float zoom) {

    }

    @Override
    public float getZoom() {
        return 0;
    }

    @Override
    public float getMaxZoom() {
        return 0;
    }

    @Override
    public Size getSize(int sizeFor) {
        return null;
    }

    @Override
    public SizeMap getSizes(int sizeFor) {
        return null;
    }

    @Override
    public void setDisplayOrientation(int displayOrientation) {

    }

    @Override
    public void startVideoRecord(File file, CameraVideoListener cameraVideoListener) {

    }

    @Override
    public void stopVideoRecord() {

    }

    @Override
    public void startPreview() {

    }
}
