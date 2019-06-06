package org.zzy.lib.bettercamera.manager.impl;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;

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

    Camera1Manager(CameraPreview cameraPreview) {
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
            //如果创建的是SurfaceView，给Camera设置相应的view
            if (cameraPreview.getPreviewType() == PreviewConstant.SURFACE_VIEW) {
                //如果正在显示预览，先停止
                if (showingPreview) {
                    camera.stopPreview();
                    showingPreview = false;
                }
                //为camera设置预览
                camera.setPreviewDisplay(cameraPreview.getSurfaceHolder());
                if(!showingPreview){
                    //camera启动预览
                    camera.startPreview();
                    showingPreview = true;
                }
            }else{
                camera.setPreviewTexture(cameraPreview.getSurfaceTexture());
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
                    getCameraSizesInfo();
                    adjustCameraParameters(false,true,true);
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
            if(!turnVideoCameraFeaturesOn(parameters)) {
                setAutoFocusInternal(parameters);
            }
        }else if(mediaType == MediaConstant.TYPE_PICTURE){
            if(!turnPhotoCameraFeaturesOn(parameters)) {
                setAutoFocusInternal(parameters);
            }
        }
        if(isNullParametes){
            camera.setParameters(parameters);
        }
    }

    private boolean turnPhotoCameraFeaturesOn(Camera.Parameters parameters){
        if(parameters.getSupportedFocusModes().contains(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)){
            parameters.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            return true;
        }
        return false;
    }

    @Override
    public boolean isCameraOpened() {
        return false;
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
