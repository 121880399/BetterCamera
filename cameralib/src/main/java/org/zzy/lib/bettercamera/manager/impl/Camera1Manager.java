package org.zzy.lib.bettercamera.manager.impl;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;

import org.zzy.lib.bettercamera.bean.Size;
import org.zzy.lib.bettercamera.bean.SizeMap;
import org.zzy.lib.bettercamera.config.ConfigProvider;
import org.zzy.lib.bettercamera.config.calculator.CameraSizeCalculator;
import org.zzy.lib.bettercamera.constant.CameraConstant;
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
                    adjustCameraParameters();
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

    private void adjustCameraParameters(boolean foreceCalculateSizes,boolean changeFocusMode,boolean changeFlashMode){
        Size oldPreview = previewSize;
        long start = System.currentTimeMillis();
        CameraSizeCalculator cameraSizeCalculator = ConfigProvider.getInstance().getCameraSizeCalculator();
        Camera.Parameters parameters = camera.getParameters();
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
