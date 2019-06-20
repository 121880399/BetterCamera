package org.zzy.lib.bettercamera.manager.impl;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.ShutterCallback;
import android.media.MediaRecorder;
import android.media.MediaRecorder.AudioSource;
import android.media.MediaRecorder.VideoSource;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.zzy.lib.bettercamera.bean.AspectRatio;
import org.zzy.lib.bettercamera.bean.Size;
import org.zzy.lib.bettercamera.bean.SizeMap;
import org.zzy.lib.bettercamera.config.ConfigProvider;
import org.zzy.lib.bettercamera.config.calculator.CameraSizeCalculator;
import org.zzy.lib.bettercamera.constant.CameraConstant;
import org.zzy.lib.bettercamera.constant.MediaConstant;
import org.zzy.lib.bettercamera.constant.PreviewConstant;
import org.zzy.lib.bettercamera.listener.CameraCloseListener;
import org.zzy.lib.bettercamera.listener.CameraOpenListener;
import org.zzy.lib.bettercamera.listener.CameraPhotoListener;
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
     * 变焦比例
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
     * 得到相机支持的尺寸和变焦比例
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
            pictureSize = cameraSizeCalculator.getPictureSize(pictureSizes,expectAspectRatio, expectSize);
            previewSize = cameraSizeCalculator.getPicturePreviewSize(previewSizes,pictureSize);
            parameters.setPictureSize(pictureSize.width,pictureSize.height);
            notifyPictureSizeUpdated(pictureSize);
        }else if(mediaType == MediaConstant.TYPE_VIDEO){
            if(camcorderProfile == null || forceCalculateSizes) {
                camcorderProfile = CameraHelper.getCamcorderProfile(mediaQuality, currentCameraId);
            }
            if(videoSize == null || forceCalculateSizes){
                videoSize = cameraSizeCalculator.getVideoSize(videoSizes, expectAspectRatio, expectSize);
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
     * 设置变焦比例
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
    public void setMediaType(@MediaConstant.Type int mediaType) {
        Logger.d(TAG, "setMediaType : " + mediaType + " with mediaType " + this.mediaType);
        if (this.mediaType == mediaType) {
            return;
        }
        this.mediaType = mediaType;
        if (isCameraOpened()) {
            backgroundHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        //重新设置媒体类型以后，要重新调整相机参数
                        adjustCameraParameters(true, false, false);
                    } catch (Exception ex) {
                        Logger.e(TAG, "setMediaType : " + ex);
                    }
                }
            });
        }
    }

    @Override
    public void setPictureOutputSize(Size outputSize) {
        super.setPictureOutputSize(outputSize);
        if(isCameraOpened()){
            adjustCameraParameters(true,false,false);
        }
    }

    @Override
    public void setPictureOutputAspectRatio(AspectRatio outputAspectRatio) {
        super.setPictureOutputAspectRatio(outputAspectRatio);
        if(isCameraOpened()){
            adjustCameraParameters(true,false,false);
        }
    }

    @Override
    public void takePicture(CameraPhotoListener cameraPhotoListener) {
        super.takePicture(cameraPhotoListener);
        if(!isCameraOpened()){
            notifyCameraCaptureFailed(new RuntimeException("Camera not open yet!"));
            return;
        }
        if(isCameraOpened()){
            backgroundHandler.post(new Runnable() {
                @Override
                public void run() {
                    try{
                        if(!takingPicture){
                            takingPicture = true;
                            camera.takePicture(voiceEnabled ? new ShutterCallback() {
                                @Override
                                public void onShutter() {

                                }
                            } : null ,null, new Camera.PictureCallback() {
                                @Override
                                public void onPictureTaken(byte[] data, Camera camera) {
                                    takingPicture = false;
                                    notifyCameraPictureTaken(data);
                                }
                            });
                        }else{
                            Logger.i(TAG, "takePicture : taking picture");
                        }
                    }catch (Exception e){
                        takingPicture = false;
                        Logger.e(TAG, "takePicture error : " + e);
                        notifyCameraCaptureFailed(new RuntimeException(e));
                    }
                }
            });
        }
    }

    @Override
    public void setVoiceEnable(boolean voiceEnable) {
        if (voiceEnabled == voiceEnable) {
            return;
        }
        this.voiceEnabled = voiceEnable;
    }

    @Override
    public boolean isVoiceEnable() {
        return voiceEnabled;
    }

    @Override
    public void setAutoFocus(boolean autoFocus) {
        if (this.isAutoFocus == autoFocus) {
            return;
        }
        this.isAutoFocus = autoFocus;
        if (isCameraOpened()) {
            backgroundHandler.post(new Runnable() {
                @Override
                public void run() {
                    setFocusModeInternal(null);
                }
            });
        }
    }

    @Override
    public boolean isAutoFocus() {
        return isAutoFocus;
    }

    @Override
    public void switchCamera(int cameraFace) {
        super.switchCamera(cameraFace);
        if(isCameraOpened()){
            //关闭旧的camera
            closeCamera(cameraCloseListener);
            //打开新的camera
            openCamera(cameraOpenListener);
        }
    }

    @Override
    public void setFlashMode(int flashMode) {
        if (this.flashMode == flashMode) {
            return;
        }
        this.flashMode = flashMode;
        if (isCameraOpened()) {
            backgroundHandler.post(new Runnable() {
                @Override
                public void run() {
                    setFlashModeInternal(null);
                }
            });
        }
    }

    @Override
    public int getFlashMode() {
        return flashMode;
    }

    @Override
    public void setZoom(float zoom) {
        if (zoom == this.zoom || zoom > getMaxZoom() || zoom < 1.f) {
            return;
        }
        this.zoom = zoom;
        if (isCameraOpened()) {
            backgroundHandler.post(new Runnable() {
                @Override
                public void run() {
                    setZoomInternal(null);
                }
            });
        }
    }

    @Override
    public float getZoom() {
        return zoom;
    }

    @Override
    public float getMaxZoom() {
        if(maxZoom == 0){
            maxZoom = zoomRatios.get(zoomRatios.size()-1);
        }
        return maxZoom;
    }

    @Override
    public Size getSize(@CameraConstant.SizeFor  int sizeFor) {
        switch(sizeFor){
            case CameraConstant.SIZE_FOR_PREVIEW:
                return previewSize;
            case CameraConstant.SIZE_FOR_PICTURE:
                return pictureSize;
            case CameraConstant.SIZE_FOR_VIDEO:
                return videoSize;
                default:
                    break;
        }
        return null;
    }

    @Override
    public SizeMap getSizes(int sizeFor) {
        switch (sizeFor) {
            case CameraConstant.SIZE_FOR_PREVIEW:
                if (previewSizeMap == null) {
                    previewSizeMap = CameraHelper.getSizeMapFromSizes(previewSizes);
                }
                return previewSizeMap;
            case CameraConstant.SIZE_FOR_PICTURE:
                if (pictureSizeMap == null) {
                    pictureSizeMap = CameraHelper.getSizeMapFromSizes(pictureSizes);
                }
                return pictureSizeMap;
            case CameraConstant.SIZE_FOR_VIDEO:
                if (videoSizeMap == null) {
                    videoSizeMap = CameraHelper.getSizeMapFromSizes(videoSizes);
                }
                return videoSizeMap;
        }
        return null;
    }

    @Override
    public void setDisplayOrientation(int displayOrientation) {
        if (this.displayOrientation == displayOrientation) {
            return;
        }
        this.displayOrientation = displayOrientation;
        if(isCameraOpened()){
            Camera.Parameters parameters = camera.getParameters();
            CameraHelper.onOrientationChanged(currentCameraId,displayOrientation,parameters);
            camera.setParameters(parameters);
            if(showingPreview){
                camera.stopPreview();
                showingPreview = false;
            }
            camera.setDisplayOrientation(CameraHelper.calDisplayOrientation(context,cameraFace,cameraFace ==
                    CameraConstant.FACE_FRONT ? frontCameraOrientation : rearCameraOrientation));
            if(!showingPreview){
                camera.startPreview();
                showingPreview=true;
            }
        }
    }

    @Override
    public void startVideoRecord(File file, CameraVideoListener cameraVideoListener) {
            super.startVideoRecord(file,cameraVideoListener);
            if(videoRecording){
                return;
            }
            if(isCameraOpened()){
                backgroundHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if(prepareVideoRecorder()){
                            videoRecorder.start();
                            videoRecording = true;
                            notifyVideoRecordStart();
                        }
                    }
                });
            }
    }


    private boolean prepareVideoRecorder(){
        videoRecorder = new MediaRecorder();
        try{

            videoRecorder.setCamera(camera);
            //音频源和视频源
            videoRecorder.setAudioSource(AudioSource.DEFAULT);
            videoRecorder.setVideoSource(VideoSource.DEFAULT);

            //输出文件格式
            videoRecorder.setOutputFormat(camcorderProfile.fileFormat);
            //设置视频帧率
            videoRecorder.setVideoFrameRate(camcorderProfile.videoFrameRate);
            videoRecorder.setVideoSize(videoSize.width,videoSize.height);
            //设置编码率
            videoRecorder.setVideoEncodingBitRate(camcorderProfile.videoBitRate);
            videoRecorder.setVideoEncoder(camcorderProfile.videoCodec);

            //设置音频编码率
            videoRecorder.setAudioEncodingBitRate(camcorderProfile.audioBitRate);
            //设置音频声道
            videoRecorder.setAudioChannels(camcorderProfile.audioChannels);
            //设置音频采样率
            videoRecorder.setAudioSamplingRate(camcorderProfile.audioSampleRate);
            videoRecorder.setAudioEncoder(camcorderProfile.audioCodec);

            videoRecorder.setOutputFile(videoOutFile.toString());

            //如果设置了视频文件大小
            if (videoFileSize > 0) {
                videoRecorder.setMaxFileSize(videoFileSize);
                videoRecorder.setOnInfoListener(this);
            }

            //如果设置了视频文件最大时间长度
            if (videoDuration > 0) {
                videoRecorder.setMaxDuration(videoDuration);
                videoRecorder.setOnInfoListener(this);
            }

            videoRecorder.setPreviewDisplay(cameraPreview.getSurface());
            videoRecorder.prepare();
            return true;
        }catch (IllegalStateException error){
            Logger.e(TAG, "IllegalStateException preparing MediaRecorder: " + error.getMessage());
            notifyVideoRecordError(error);
        }catch (IOException error) {
            Logger.e(TAG, "IOException preparing MediaRecorder: " + error.getMessage());
            notifyVideoRecordError(error);
        }catch (Throwable error) {
            Logger.e(TAG, "Error during preparing MediaRecorder: " + error.getMessage());
            notifyVideoRecordError(error);
        }
        //如果出现了异常释放掉
        releaseVideoRecorder();
        return false;
    }

    @Override
    public void stopVideoRecord() {
        if(videoRecording && isCameraOpened()){
            backgroundHandler.post(new Runnable() {
                @Override
                public void run() {
                    safeStopVideoRecorder();
                    releaseVideoRecorder();
                    videoRecording = false;
                    notifyVideoRecordStop(videoOutFile);
                }
            });
        }
    }

    @Override
    public void resumePreview() {
        if(isCameraOpened()){
            camera.startPreview();
        }
    }

    @Override
    public void closeCamera(CameraCloseListener cameraCloseListener) {
        super.closeCamera(cameraCloseListener);
        if(isCameraOpened()){
            camera.setPreviewCallback(null);
            camera.stopPreview();
        }

        showingPreview = false;
        if(uiHandler != null){
            uiHandler.removeCallbacksAndMessages(null);
        }
        if(backgroundHandler != null){
            backgroundHandler.removeCallbacksAndMessages(null);
        }
        releaseCameraInternal();
        notifyCameraClosed();
    }

    private void releaseCameraInternal(){
        if(camera != null){
            camera.release();
            camera = null;
            previewSize = null;
            pictureSize = null;
            videoSize = null;
            maxZoom = 0;
        }
    }
}
