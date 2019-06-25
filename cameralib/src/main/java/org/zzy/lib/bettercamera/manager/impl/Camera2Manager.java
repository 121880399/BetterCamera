package org.zzy.lib.bettercamera.manager.impl;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Camera;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCaptureSession.StateCallback;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.media.MediaActionSound;
import android.media.MediaRecorder;
import android.media.MediaRecorder.AudioSource;
import android.media.MediaRecorder.VideoSource;
import android.os.Build.VERSION_CODES;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.view.Surface;
import android.view.SurfaceHolder;

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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @作者 ZhouZhengyi
 * @创建日期 2019/6/4
 */
@RequiresApi(api = VERSION_CODES.LOLLIPOP)
@SuppressLint("MissingPermission")
public class Camera2Manager extends BaseCameraManager<String> implements OnImageAvailableListener {

    private static final String TAG = "Camera2Manager";

    /**
     * 代表系统摄像头。该类的功能类似于早期的Camera类。
     */
    private CameraDevice cameraDevice;

    private CaptureRequest.Builder previewRequestBuilder;

    private Surface workingSurface;

    private ImageReader imageReader;

    /**
     * 当程序需要预览、拍照时，都需要先通过该类的实例创建Session。
     * 而且不管预览还是拍照，也都是由该对象的方法进行控制的，其中控制预览的方法为setRepeatingRequest()；
     * 控制拍照的方法为capture()。
     */
    private CameraCaptureSession captureSession;

    /**
     * 代表了一次捕获请求，用于描述捕获图片的各种参数设置，比如对焦模式、曝光模式...
     */
    private CaptureRequest previewRequest;

    /**
     * 前置摄像头特性
     */
    private CameraCharacteristics frontCameraCharacteristics;

    /**
     * 后置摄像头特性
     */
    private CameraCharacteristics rearCameraCharacteristics;

    private SurfaceTexture surfaceTexture;

    private SurfaceHolder surfaceHolder;

    /**
     * 这是一个全新的系统管理器，专门用于检测系统摄像头、打开系统摄像头
     */
    private CameraManager cameraManager;

    private StreamConfigurationMap frontStreamConfigurationMap;

    private StreamConfigurationMap rearStreamConfigurationMap;

    public Camera2Manager(CameraPreview cameraPreview) {
        super(cameraPreview);
        cameraPreview.setCameraPreviewCallback(new CameraPreviewCallback() {
            @Override
            public void onAvailable(CameraPreview cameraPreview) {
                if (isCameraOpened()) {
                    createPreviewSession();
                }
            }
        });
    }

    private CaptureSessionCallback captureSessionCallback = new CaptureSessionCallback() {


        @Override
        void processCaptureResult(@NonNull CaptureResult result, int cameraPreviewState) {
            switch (cameraPreviewState) {
                case STATE_PREVIEW:
                    //预览状态
                    break;
                case STATE_WAITING_LOCK: {
                    //等待对焦状态
                    final Integer autoFocusState = result.get(CaptureResult.CONTROL_AF_STATE);
                    if (autoFocusState == null) {
                        captureStillPicture();
                    } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == autoFocusState
                            || CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == autoFocusState
                            || CaptureResult.CONTROL_AF_STATE_INACTIVE == autoFocusState
                            || CaptureResult.CONTROL_AF_STATE_PASSIVE_SCAN == autoFocusState
                            ) {
                        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                        if (aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                            setCameraPreviewState(STATE_PICTURE_TAKEN);
                            //对焦完成
                            captureStillPicture();
                        } else {
                            runPreCaptureSequence();
                        }
                    }
                    break;
                }
                case STATE_WAITING_PRE_CAPTURE: {
                    final Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null ||
                            aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                            aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                        setCameraPreviewState(STATE_WAITING_NON_PRE_CAPTURE);
                    }
                    break;
                }
                case STATE_WAITING_NON_PRE_CAPTURE: {
                    final Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                        setCameraPreviewState(STATE_PICTURE_TAKEN);
                        captureStillPicture();
                    }
                    break;
                }
                case STATE_PICTURE_TAKEN:
                    break;
                default:
                    break;
            }
        }
    };

    private void captureStillPicture() {
        if (isCameraOpened()) {
            try {
                CameraCharacteristics cameraCharacteristics = cameraFace == CameraConstant.FACE_FRONT ?
                                                              frontCameraCharacteristics : rearCameraCharacteristics;

                final CaptureRequest.Builder captureBuilder = cameraDevice
                        .createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                captureBuilder.addTarget(imageReader.getSurface());
                captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                captureBuilder.set(CaptureRequest.JPEG_ORIENTATION,
                        CameraHelper.getJpegOrientation(cameraCharacteristics, displayOrientation));
                // FIXME the zoomed result is invalid
                setZoomInternal();
                captureSession.stopRepeating();
                captureSession.capture(captureBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                    @Override
                    public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                            @NonNull CaptureRequest request,
                            @NonNull TotalCaptureResult result) {
                        Logger.d(TAG, "onCaptureCompleted: ");
                    }
                }, null);
            } catch (CameraAccessException e) {
                Logger.e(TAG, "Error during capturing picture");
                notifyCameraCaptureFailed(e);
            }
        } else {
            notifyCameraCaptureFailed(new RuntimeException("Camera not open."));
        }
    }

    @Override
    public void initialize(Context context) {
        super.initialize(context);
        initCameraInfo(context);
    }

    private void initCameraInfo(Context context){
        cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);

        long start = System.currentTimeMillis();
        try{
            assert cameraManager != null;
            final String[] ids = cameraManager.getCameraIdList();
            numberOfCameras = ids.length;
            for(String id : ids){
                final CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(id);
                final Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if(facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT){
                    frontCameraId = id;
                    Integer iFrontCameraOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                    frontCameraOrientation = iFrontCameraOrientation == null ? 0 : frontCameraOrientation;
                    frontCameraCharacteristics = characteristics;
                }else if(facing != null && facing == CameraCharacteristics.LENS_FACING_BACK){
                    rearCameraId = id;
                    Integer iRearCameraOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                    rearCameraOrientation = iRearCameraOrientation == null ? 0 : rearCameraOrientation;
                    rearCameraCharacteristics = characteristics;
                }
            }
        }catch (Exception e){
            Logger.e(TAG, "initCameraInfo error " + e);
        }
        Logger.d(TAG, "initCameraInfo basic cost : " + (System.currentTimeMillis() - start) + " ms");

        currentCameraId = cameraFace == CameraConstant.FACE_REAR ? rearCameraId : frontCameraId;
    }

    @Override
    public void openCamera(CameraOpenListener cameraOpenListener) {
        super.openCamera(cameraOpenListener);
        backgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                prepareCameraOutputs();
                adjustCameraConfiguration(false);
                try {
                    cameraManager.openCamera(currentCameraId, new CameraDevice.StateCallback() {
                        @Override
                        public void onOpened(@NonNull CameraDevice camera) {
                            Logger.d(TAG, "Camera opened.");
                            cameraDevice = camera;
                            if(cameraPreview.isAvailable()){
                                createPreviewSession();
                            }
                            notifyCameraOpened();
                        }

                        @Override
                        public void onDisconnected(@NonNull CameraDevice camera) {
                            Logger.e(TAG, "Camera disconnected.");
                            camera.close();
                            cameraDevice = null;
                            notifyCameraClosed();
                        }

                        @Override
                        public void onError(@NonNull CameraDevice camera, int error) {
                            Logger.e(TAG, "Camera open error : " + error);
                            camera.close();
                            cameraDevice = null;
                            notifyCameraOpenError(new RuntimeException("Camera error : " + error));
                        }
                    },backgroundHandler);
                }catch (Exception ex){
                    Logger.e(TAG, "error : " + ex);
                    notifyCameraOpenError(ex);
                }
            }
        });
    }

    private void prepareCameraOutputs(){
        boolean isFrontCamera = cameraFace == CameraConstant.FACE_FRONT;
        long start = System.currentTimeMillis();
        try{
            final CameraCharacteristics characteristics = isFrontCamera ? frontCameraCharacteristics :
                                                          rearCameraCharacteristics;
            if(isFrontCamera && frontStreamConfigurationMap == null){
                frontStreamConfigurationMap = characteristics.get(CameraCharacteristics
                        .SCALER_STREAM_CONFIGURATION_MAP);
            }else if(!isFrontCamera && rearStreamConfigurationMap == null){
                rearStreamConfigurationMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            }
        }catch (Exception e){
            Logger.e(TAG, "initCameraInfo error " + e);
            notifyCameraOpenError(new RuntimeException(e));
        }
        Logger.d(TAG, "initCameraInfo get map cost : " + (System.currentTimeMillis() - start) + " ms");

        start = System.currentTimeMillis();
        try{
            final StreamConfigurationMap  map = isFrontCamera ? frontStreamConfigurationMap :
                                                rearStreamConfigurationMap;
            previewSizes = ConfigProvider.getInstance().getSizes(map,cameraFace, CameraConstant.SIZE_FOR_PREVIEW);
            pictureSizes = ConfigProvider.getInstance().getSizes(map,cameraFace,CameraConstant.SIZE_FOR_PICTURE);
            videoSizes =  ConfigProvider.getInstance().getSizes(map,cameraFace,CameraConstant.SIZE_FOR_VIDEO);
        }catch (Exception ex){
            Logger.e(TAG, "error : " + ex);
            notifyCameraOpenError(new RuntimeException(ex));
        }
        Logger.d(TAG, "prepareCameraOutputs cost : " + (System.currentTimeMillis() - start) + " ms");
    }

    /**
     * 设置变焦
     */
    private boolean setZoomInternal() {
        float maxZoom = getMaxZoom();
        if (maxZoom == 1.0f || previewRequestBuilder == null) {
            return false;
        }

        CameraCharacteristics cameraCharacteristics = cameraFace == CameraConstant.FACE_FRONT ?
                                                      frontCameraCharacteristics : rearCameraCharacteristics;
        Rect rect = cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        if (rect == null) {
            return false;
        }

        zoom = zoom < 1.f ? 1.f : zoom;
        zoom = zoom > maxZoom ? maxZoom : zoom;

        int cropW = (rect.width() - (int) ((float) rect.width() / zoom)) / 2;
        int cropH = (rect.height() - (int) ((float) rect.height() / zoom)) / 2;

        Rect zoomRect = new Rect(cropW, cropH, rect.width() - cropW, rect.height() - cropH);
        previewRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoomRect);
        return true;
    }

    private void runPreCaptureSequence() {
        try {
            previewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            captureSessionCallback.setCameraPreviewState(CaptureSessionCallback.STATE_WAITING_PRE_CAPTURE);
            captureSession.capture(previewRequestBuilder.build(), captureSessionCallback, backgroundHandler);
        } catch (CameraAccessException e) {
            Logger.e(TAG, "runPreCaptureSequence error " + e);
        }
    }

    /**
     * 创建预览
     */
    private void createPreviewSession() {
        try {
            final Runnable sessionCreationTask = new Runnable() {
                @Override
                public void run() {
                    try {
                        previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                        previewRequestBuilder.addTarget(workingSurface);
                        cameraDevice.createCaptureSession(Arrays.asList(workingSurface, imageReader.getSurface()),
                                new StateCallback() {
                                    @Override
                                    public void onConfigured(
                                            @NonNull CameraCaptureSession session) {
                                        if (isCameraOpened()) {
                                            captureSession = session;
                                            setFlashModeInternal();
                                            previewRequest = previewRequestBuilder.build();
                                            try {
                                                captureSession.setRepeatingRequest(previewRequest,
                                                        captureSessionCallback, backgroundHandler);
                                            } catch (CameraAccessException ex) {
                                                Logger.e(TAG, "createPreviewSession error " + ex);
                                                notifyCameraOpenError(ex);
                                            } catch (IllegalStateException ex) {
                                                Logger.e(TAG, "createPreviewSession error " + ex);
                                                notifyCameraOpenError(ex);
                                            }
                                        }
                                    }

                                    @Override
                                    public void onConfigureFailed(
                                            @NonNull CameraCaptureSession session) {
                                        Logger.d(TAG, "onConfigureFailed");
                                        notifyCameraOpenError(
                                                new Throwable("Camera capture session configure failed."));
                                    }
                                }, backgroundHandler);
                    } catch (Exception e) {
                        Logger.e(TAG, "createPreviewSession error " + e);
                        notifyCameraOpenError(e);
                    }
                }
            };

            if (cameraPreview.getPreviewType() == PreviewConstant.TEXTURE_VIEW) {
                this.surfaceTexture = cameraPreview.getSurfaceTexture();
                assert surfaceTexture != null;
                surfaceTexture.setDefaultBufferSize(previewSize.width, previewSize.height);
                workingSurface = cameraPreview.getSurface();
                sessionCreationTask.run();
            } else if (cameraPreview.getPreviewType() == PreviewConstant.SURFACE_VIEW) {
                uiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        surfaceHolder = cameraPreview.getSurfaceHolder();
                        assert surfaceHolder != null;
                        surfaceHolder.setFixedSize(previewSize.width, previewSize.height);
                        workingSurface = cameraPreview.getSurface();
                        sessionCreationTask.run();
                    }
                });
            }
        } catch (Exception e) {
            Logger.e(TAG, "createPreviewSession error " + e);
            notifyCameraOpenError(e);
        }
    }

    @Override
    public void switchCamera(int cameraFace) {
        super.switchCamera(cameraFace);
        if(isCameraOpened()){
            closeCamera(cameraCloseListener);
            openCamera(cameraOpenListener);
        }
    }

    @Override
    public void closeCamera(CameraCloseListener cameraCloseListener) {
        super.closeCamera(cameraCloseListener);
        if(isCameraOpened()){
            cameraDevice.close();
            cameraDevice = null;
        }
        closePreviewSession();
        closeImageReader();
        releaseVideoRecorder();
        releaseCameraInternal();
        if (uiHandler != null) {
            uiHandler.removeCallbacksAndMessages(null);
        }
        if (backgroundHandler != null) {
            backgroundHandler.removeCallbacksAndMessages(null);
        }
        notifyCameraClosed();
    }

    private void closeImageReader() {
        if (null != imageReader) {
            imageReader.close();
            imageReader = null;
        }
    }

    private void releaseCameraInternal() {
        previewSize = null;
        pictureSize = null;
        videoSize = null;
        maxZoom = 0;
    }

    private void releaseTexture() {
        if (null != surfaceTexture) {
            surfaceTexture.release();
            surfaceTexture = null;
        }
    }

    /**
     * 设置闪光灯模式
     */
    private boolean setFlashModeInternal() {
        try {
            CameraCharacteristics cameraCharacteristics = cameraFace == CameraConstant.FACE_FRONT ?
                                                          frontCameraCharacteristics : rearCameraCharacteristics;
            Boolean isFlashAvailable = cameraCharacteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);

            if (isFlashAvailable == null || !isFlashAvailable) {
                Logger.i(TAG, "Flash is not available.");
                return false;
            }
            switch (flashMode) {
                case CameraConstant.FLASH_ON:
                    //设置自动曝光模式不控制闪光灯
                    previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                    //设置闪光灯模式，如果闪光灯可用，则打开闪光灯
                    previewRequestBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_SINGLE);
                    break;
                case CameraConstant.FLASH_OFF:
                    //设置自动曝光模式式不控制闪光灯
                    previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                    //设置闪光灯模式为关闭闪光灯
                    previewRequestBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF);
                    break;
                case CameraConstant.FLASH_AUTO:
                default:
                    //设置自动曝光模式控制闪光灯
                    previewRequestBuilder
                            .set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                    //设置闪光灯模式，如果闪光灯可用，则打开闪光灯
                    previewRequestBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_SINGLE);
                    break;
            }
            return true;
        } catch (Exception e) {
            Logger.e(TAG, "setFlashMode error : " + e);
        }
        return false;
    }

    @Override
    public boolean isCameraOpened() {
        return cameraDevice != null;
    }

    @Override
    public void setMediaType(int mediaType) {
        Logger.d(TAG, "setMediaType : " + mediaType + " with mediaType " + this.mediaType);
        if (this.mediaType == mediaType) {
            return;
        }
        if (isCameraOpened()) {
            backgroundHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        adjustCameraConfiguration(false);
                    } catch (Exception ex) {
                        Logger.e(TAG, "setMediaType : " + ex);
                    }
                }
            });
        }
    }

    /**
     * 调整相机配置
     *
     * @param forceCalculate 是否强制
     */
    private void adjustCameraConfiguration(boolean forceCalculate) {
        Size oldPreviewSize = previewSize;
        CameraSizeCalculator cameraSizeCalculator = ConfigProvider.getInstance().getCameraSizeCalculator();
        if (pictureSize == null || forceCalculate) {
            pictureSize = cameraSizeCalculator.getPictureSize(pictureSizes, expectAspectRatio, expectSize);
            previewSize = cameraSizeCalculator.getPicturePreviewSize(previewSizes, pictureSize);
            notifyPictureSizeUpdated(pictureSize);

            imageReader = ImageReader.newInstance(pictureSize.width, pictureSize.height, ImageFormat.JPEG, 2);
            imageReader.setOnImageAvailableListener(this, backgroundHandler);
        }

        if (mediaType == MediaConstant.TYPE_VIDEO && (videoSize == null || forceCalculate)) {
            camcorderProfile = CameraHelper.getCamcorderProfile(mediaQuality, currentCameraId);
            videoSize = cameraSizeCalculator.getVideoSize(videoSizes, expectAspectRatio, expectSize);
            previewSize = cameraSizeCalculator.getVideoPreviewSize(previewSizes, videoSize);
            notifyVideoSizeUpdated(videoSize);
        }

        if (!previewSize.equals(oldPreviewSize)) {
            notifyPreviewSizeUpdated(previewSize);
        }
    }

    @Override
    public void setVoiceEnable(boolean voiceEnable) {
        if (this.voiceEnabled == voiceEnabled) {
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
        if (isCameraOpened() && previewRequestBuilder != null) {
            setAutoFocusInternal();
            previewRequest = previewRequestBuilder.build();
            if (captureSession != null) {
                try {
                    captureSession.setRepeatingRequest(previewRequest, captureSessionCallback, backgroundHandler);
                } catch (CameraAccessException e) {
                    Logger.e(TAG, "setAutoFocus error : " + e);
                }
            } else {
                Logger.i(TAG, "setAutoFocus captureSession is null.");
            }
        } else {
            Logger.i(TAG, "setAutoFocus camera not open or previewRequestBuilder is null");
        }
    }

    private void setAutoFocusInternal() {
        if (isAutoFocus) {
            CameraCharacteristics cameraCharacteristics = cameraFace == CameraConstant.FACE_FRONT ?
                                                          frontCameraCharacteristics :
                                                          rearCameraCharacteristics;
            int[] modes = cameraCharacteristics.get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES);
            if (modes == null || modes.length == 0 || (modes.length == 1 && modes[0] == CameraCharacteristics
                    .CONTROL_AF_MODE_OFF)) {
                isAutoFocus = false;
                previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
            } else if (mediaType == MediaConstant.TYPE_PICTURE) {
                previewRequestBuilder
                        .set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            } else if (mediaType == MediaConstant.TYPE_VIDEO) {
                previewRequestBuilder
                        .set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO);
            }
        } else {
            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
        }
    }

    @Override
    public boolean isAutoFocus() {
        return isAutoFocus;
    }

    @Override
    public void setFlashMode(int flashMode) {
        if (this.flashMode == flashMode) {
            return;
        }
        this.flashMode = flashMode;
        if (isCameraOpened() && previewRequestBuilder != null) {
            boolean succeed = setFlashModeInternal();
            if (succeed) {
                previewRequest = previewRequestBuilder.build();
                if (captureSession != null) {
                    try {
                        captureSession.setRepeatingRequest(previewRequest, captureSessionCallback, backgroundHandler);
                    } catch (CameraAccessException e) {
                        Logger.e(TAG, "setFlashMode error : " + e);
                    }
                } else {
                    Logger.i(TAG, "setFlashMode captureSession is null.");
                }
            } else {
                Logger.i(TAG, "setFlashMode failed.");
            }
        } else {
            Logger.i(TAG, "setFlashMode camera not open or previewRequestBuilder is null");
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
            boolean succeed = setZoomInternal();
            if (succeed) {
                previewRequest = previewRequestBuilder.build();
                if (captureSession != null) {
                    try {
                        captureSession.setRepeatingRequest(previewRequest, captureSessionCallback, backgroundHandler);
                    } catch (CameraAccessException e) {
                        Logger.e(TAG, "setZoom error : " + e);
                    }
                } else {
                    Logger.i(TAG, "setZoom captureSession is null.");
                }
            } else {
                Logger.i(TAG, "setZoom failed : setZoomInternal failed.");
            }
        } else {
            Logger.i(TAG, "setZoom failed : camera not open.");
        }
    }

    @Override
    public float getZoom() {
        return zoom;
    }

    @Override
    public float getMaxZoom() {
        if (maxZoom == 0) {
            CameraCharacteristics cameraCharacteristics = cameraFace == CameraConstant.FACE_FRONT ?
                                                          frontCameraCharacteristics :
                                                          rearCameraCharacteristics;
            Float fMaxZoom = cameraCharacteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);
            if (fMaxZoom == null) {
                maxZoom = 1.0f;
            } else {
                maxZoom = fMaxZoom;
            }
        }
        return maxZoom;
    }

    @Override
    public void setExpectSize(Size outputSize) {
        super.setExpectSize(outputSize);
        if(isCameraOpened()){
            backgroundHandler.post(new Runnable() {
                @Override
                public void run() {
                    adjustCameraConfiguration(true);
                    createPreviewSession();
                }
            });
        }
    }

    @Override
    public void setExpectAspectRatio(AspectRatio outputAspectRatio) {
        super.setExpectAspectRatio(outputAspectRatio);
        if(isCameraOpened()){
            backgroundHandler.post(new Runnable() {
                @Override
                public void run() {
                    adjustCameraConfiguration(true);
                    createPreviewSession();
                }
            });
        }
    }

    @Override
    public Size getSize(int sizeFor) {
        switch (sizeFor) {
            case CameraConstant.SIZE_FOR_PREVIEW:
                return previewSize;
            case CameraConstant.SIZE_FOR_PICTURE:
                return pictureSize;
            case CameraConstant.SIZE_FOR_VIDEO:
                return videoSize;
            default:
                return null;
        }
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
            default:
                return null;
        }
    }

    @Override
    public void setDisplayOrientation(int displayOrientation) {
        if (this.displayOrientation == displayOrientation) {
            return;
        }
        this.displayOrientation = displayOrientation;
        // FIXME the display orientation
    }

    @Override
    public void stopVideoRecord() {
        if (videoRecording && isCameraOpened()) {
            backgroundHandler.post(new Runnable() {
                @Override
                public void run() {
                    closePreviewSession();
                    safeStopVideoRecorder();
                    releaseVideoRecorder();
                    videoRecording = false;
                    notifyVideoRecordStop(videoOutFile);
                    if (voiceEnabled) {
                        new MediaActionSound().play(MediaActionSound.STOP_VIDEO_RECORDING);
                    }
                    createPreviewSession();
                }
            });
        }
    }

    @Override
    public void takePicture(CameraPhotoListener cameraPhotoListener) {
        super.takePicture(cameraPhotoListener);
        backgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    lockFocus();
                    if (voiceEnabled) {
                        new MediaActionSound().play(MediaActionSound.SHUTTER_CLICK);
                    }
                }catch (Exception e){
                    notifyCameraCaptureFailed(e);
                }
            }
        });
    }

    private void lockFocus(){
        try{
            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,CameraMetadata.CONTROL_AF_TRIGGER_START);
            captureSessionCallback.setCameraPreviewState(CaptureSessionCallback.STATE_WAITING_LOCK);
            captureSession.capture(previewRequestBuilder.build(),captureSessionCallback,backgroundHandler);
        }catch (Exception e){
            Logger.e(TAG, "lockFocus : error during focus locking");
        }
    }

    @Override
    public void startVideoRecord(File file, CameraVideoListener cameraVideoListener) {
        super.startVideoRecord(file, cameraVideoListener);
        if(videoRecording){
            return;
        }
        if(isCameraOpened()){
            if(voiceEnabled){
                new MediaActionSound().play(MediaActionSound.START_VIDEO_RECORDING);
            }
            backgroundHandler.post(new Runnable() {
                @Override
                public void run() {
                    closePreviewSession();
                    if(prepareVideoRecorder()){
                        createRecordSession();
                    }else{
                        Logger.i(TAG, "startVideoRecord : failed when prepare video recorder.");
                        notifyVideoRecordError(new RuntimeException("Failed when prepare video recorder."));
                    }
                }
            });
        }
    }

    private boolean prepareVideoRecorder(){
        videoRecorder = new MediaRecorder();
        try{
            videoRecorder.setAudioSource(AudioSource.CAMCORDER);
            videoRecorder.setVideoSource(VideoSource.SURFACE);

            videoRecorder.setOutputFormat(camcorderProfile.fileFormat);
            videoRecorder.setVideoFrameRate(camcorderProfile.videoFrameRate);
            videoRecorder.setVideoSize(videoSize.width,videoSize.height);
            videoRecorder.setVideoEncodingBitRate(camcorderProfile.videoBitRate);
            videoRecorder.setVideoEncoder(camcorderProfile.videoCodec);

            videoRecorder.setAudioEncodingBitRate(camcorderProfile.audioBitRate);
            videoRecorder.setAudioChannels(camcorderProfile.audioChannels);
            videoRecorder.setAudioSamplingRate(camcorderProfile.audioSampleRate);
            videoRecorder.setAudioEncoder(camcorderProfile.audioCodec);


            videoRecorder.setOutputFile(videoOutFile.toString());

            if(videoFileSize > 0 ){
                videoRecorder.setMaxFileSize(videoFileSize);
                videoRecorder.setOnInfoListener(this);
            }

            if(videoDuration > 0 ){
                videoRecorder.setMaxDuration(videoDuration);
                videoRecorder.setOnInfoListener(this);
            }

            CameraCharacteristics cameraCharacteristics = cameraFace == CameraConstant.FACE_FRONT ? frontCameraCharacteristics
                                                          : rearCameraCharacteristics;
            videoRecorder.setOrientationHint(CameraHelper.getJpegOrientation(cameraCharacteristics,displayOrientation));
            videoRecorder.setPreviewDisplay(cameraPreview.getSurface());
            videoRecorder.prepare();
            return true;
        }catch (IllegalStateException error){
            Logger.e(TAG, "IllegalStateException preparing MediaRecorder: " + error.getMessage());
            notifyVideoRecordError(error);
        }catch (IOException error) {
            Logger.e(TAG, "IOException preparing MediaRecorder: " + error.getMessage());
            notifyVideoRecordError(error);
        } catch (Throwable error) {
            Logger.e(TAG, "Error during preparing MediaRecorder: " + error.getMessage());
            notifyVideoRecordError(error);
        }

        releaseVideoRecorder();
        return false;
    }

    private void createRecordSession(){
        final Runnable sessionCreationTask =new Runnable() {
            @Override
            public void run() {
                try{
                    final List<Surface> surfaces = new ArrayList<>();

                    previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);

                    final Surface previewSurface = workingSurface;
                    surfaces.add(previewSurface);
                    previewRequestBuilder.addTarget(previewSurface);

                    workingSurface = videoRecorder.getSurface();
                    surfaces.add(previewSurface);
                    previewRequestBuilder.addTarget(workingSurface);

                    cameraDevice.createCaptureSession(surfaces,new CameraCaptureSession.StateCallback(){

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            captureSession = session;

                            previewRequestBuilder.set(CaptureRequest.CONTROL_MODE,CameraMetadata.CONTROL_MODE_AUTO);
                            try{
                                captureSession.setRepeatingRequest(previewRequestBuilder.build(),null,
                                        backgroundHandler);
                            }catch (Exception e){
                                Logger.e(TAG, "videoRecorder.start(): " + e);
                                notifyVideoRecordError(e);
                            }

                            try{
                                videoRecorder.start();
                                videoRecording = true;
                                notifyVideoRecordStart();
                            }catch (Exception e) {
                                Logger.e(TAG, "videoRecorder.start(): " + e);
                                notifyVideoRecordError(e);
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            Logger.i(TAG, "onConfigureFailed");
                            notifyVideoRecordError(new RuntimeException("Video record configure failed."));
                        }
                    },backgroundHandler);
                }catch (Exception e) {
                    Logger.e(TAG, "startVideoRecord: " + e);
                    notifyVideoRecordError(e);
                }
            }
        };
        if(cameraPreview.getPreviewType() == PreviewConstant.TEXTURE_VIEW){
            final SurfaceTexture texture = Camera2Manager.this.surfaceTexture;
            texture.setDefaultBufferSize(videoSize.width,videoSize.height);
            sessionCreationTask.run();
        }else if(cameraPreview.getPreviewType() == PreviewConstant.SURFACE_VIEW){
            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    SurfaceHolder surfaceHolder = Camera2Manager.this.surfaceHolder;
                    surfaceHolder.setFixedSize(previewSize.width,previewSize.height);
                    sessionCreationTask.run();
                }
            });
        }
    }

    private void closePreviewSession() {
        if (captureSession != null) {
            captureSession.close();
            try {
                captureSession.abortCaptures();
            } catch (CameraAccessException e) {
                e.printStackTrace();
                Logger.e(TAG, "closePreviewSession error : " + e);
            } finally {
                captureSession = null;
            }
        }
    }

    @Override
    public void resumePreview() {
        if (isCameraOpened()) {
            unlockFocus();
        }
    }

    private void unlockFocus() {
        try {
            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            captureSession.capture(previewRequestBuilder.build(), captureSessionCallback, backgroundHandler);
            captureSessionCallback.setCameraPreviewState(CaptureSessionCallback.STATE_PREVIEW);
            captureSession.setRepeatingRequest(previewRequest, captureSessionCallback, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onImageAvailable(ImageReader reader) {
        try {
            try (Image image = reader.acquireNextImage()) {
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                notifyCameraPictureTaken(bytes);
            } catch (Exception e) {
                Logger.e(TAG, "onImageAvailable error" + e);
                notifyCameraCaptureFailed(e);
            }
        }catch (Exception e){
            Logger.e(TAG, "onImageAvailable error " + e);
            notifyCameraCaptureFailed(e);
        }finally {
            unlockFocus();
        }
    }

    private static abstract class CaptureSessionCallback extends CameraCaptureSession.CaptureCallback {
        /**
         * Camera state: Showing camera preview.
         */
        static final int STATE_PREVIEW = 0;

        /**
         * Camera state: Waiting for the focus to be locked.
         */
        static final int STATE_WAITING_LOCK = 1;

        /**
         * Camera state: Waiting for the exposure to be precapture state.
         */
        static final int STATE_WAITING_PRE_CAPTURE = 2;

        /**
         * Camera state: Waiting for the exposure state to be something other than precapture.
         */
        static final int STATE_WAITING_NON_PRE_CAPTURE = 3;

        /**
         * Camera state: Picture was taken.
         */
        static final int STATE_PICTURE_TAKEN = 4;

        @IntDef({STATE_PREVIEW, STATE_WAITING_LOCK, STATE_WAITING_PRE_CAPTURE, STATE_WAITING_NON_PRE_CAPTURE,
                STATE_PICTURE_TAKEN})
        @Retention(RetentionPolicy.SOURCE)
        @interface CameraState {
        }

        @CameraState
        private int cameraPreviewState;

        /**
         * 设置预览状态
         */
        void setCameraPreviewState(@CameraState int cameraPreviewState) {
            this.cameraPreviewState = cameraPreviewState;
        }

        /**
         * 处理捕获结果
         */
        abstract void processCaptureResult(@NonNull CaptureResult result, @CameraState int cameraPreviewState);

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session,
                @NonNull CaptureRequest request,
                @NonNull CaptureResult partialResult) {
            processCaptureResult(partialResult, cameraPreviewState);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                @NonNull CaptureRequest request,
                @NonNull TotalCaptureResult result) {
            processCaptureResult(result, cameraPreviewState);
        }
    }
}
