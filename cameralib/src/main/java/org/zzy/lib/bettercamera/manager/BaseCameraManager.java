package org.zzy.lib.bettercamera.manager;

import android.content.Context;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;

import org.zzy.lib.bettercamera.bean.AspectRatio;
import org.zzy.lib.bettercamera.bean.Size;
import org.zzy.lib.bettercamera.config.ConfigProvider;
import org.zzy.lib.bettercamera.constant.CameraConstant;
import org.zzy.lib.bettercamera.constant.MediaConstant;
import org.zzy.lib.bettercamera.listener.CameraCloseListener;
import org.zzy.lib.bettercamera.listener.CameraOpenListener;
import org.zzy.lib.bettercamera.listener.CameraPhotoListener;
import org.zzy.lib.bettercamera.listener.CameraSizeListener;
import org.zzy.lib.bettercamera.listener.CameraVideoListener;
import org.zzy.lib.bettercamera.preview.CameraPreview;
import org.zzy.lib.bettercamera.utils.Logger;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

/**
 * 相机管理父类
 * @作者 ZhouZhengyi
 * @创建日期 2019/6/4
 */
public abstract class BaseCameraManager implements  CameraManager,MediaRecorder.OnInfoListener {

    private static final String TAG = "BaseCameraManager";

    protected Context context;


    /**
     * 媒体类型
     */
    @MediaConstant.Type int mediaType;
    /**
     * 媒体质量
     */
    @MediaConstant.Quality int mediaQuality;
    /**
     * 前、后摄像头
     */
    @CameraConstant.Face int cameraFace;
    /**
     * 后置摄像头方向
     */
    int rearCameraOrientation;
    /**
     * 前置摄像头方向
     */
    int frontCameraOrientation;

    /**
     * 预览尺寸集合
     */
    List<Size> previewSizes;
    /**
     * 照片尺寸集合
     */
    List<Size> pictureSizes;
    /**
     * 视频尺寸集合
     */
    List<Size> videoSizes;

    /**
     * 宽高比
     */
    AspectRatio expectAspectRatio;
    /**
     * 期望照片尺寸
     */
    Size expectPictureSize;
    /**
     * 预览尺寸
     */
    Size previewSize;
    /**
     * 照片尺寸
     */
    Size pictureSize;
    /**
     * 视频尺寸
     */
    Size videoSize;
    /**
     * 是否开启快门声
     */
    boolean voiceEnabled;
    /**
     * 是否自动对焦
     */
    boolean isAutoFocus;

    /**
     * 闪光灯模式
     */
    @CameraConstant.FlashMode int flashMode;

    /**
     * 缩放比例
     */
    float zoom = 1.0f;
    /**
     * 最大缩放比例
     */
    float maxZoom;
    /**
     * 视频文件尺寸
     */
    long videoFileSize;
    /**
     * 视频时长
     */
    int videoDuration;
    /**
     * 开启摄像头监听
     */
    CameraOpenListener cameraOpenListener;
    /**
     * 关闭摄像头监听
     */
    CameraCloseListener cameraCloseListener;
    /**
     * 拍照监听
     */
    private CameraPhotoListener cameraPhotoListener;
    /**
     * 录制视频监听
     */
    private CameraVideoListener cameraVideoListener;

    /**
     * 预览
     */
    CameraPreview cameraPreview;

    /**
     * 尺寸监听
     */
    private List<CameraSizeListener> cameraSizeListeners;

    /**
     * 后台线程
     */
    private HandlerThread backgroundThread;
    /**
     * 后台Handler
     */
    Handler backgroundHandler;

    /**
     * 主线程Handler
     */
    Handler uiHandler = new Handler(Looper.getMainLooper());

    /**
     * 视频录制
     */
    MediaRecorder videoRecorder;


    BaseCameraManager(CameraPreview cameraPreview) {
        this.cameraPreview = cameraPreview;
        cameraFace = ConfigProvider.getInstance().getDefaultCameraFace();
        expectAspectRatio = ConfigProvider.getInstance().getDefaultAspectRatio();
        mediaType = ConfigProvider.getInstance().getDefaultMediaType();
        mediaQuality = ConfigProvider.getInstance().getDefaultMediaQuality();
        voiceEnabled = ConfigProvider.getInstance().isVoiceEnable();
        isAutoFocus = ConfigProvider.getInstance().isAutoFocus();
        flashMode = ConfigProvider.getInstance().getDefaultFlashMode();
        cameraSizeListeners = new LinkedList<>();
        videoFileSize = ConfigProvider.getInstance().getDefaultVideoFileSize();
        videoDuration = ConfigProvider.getInstance().getDefaultVideoDuration();
    }

    @Override
    public void initialize(Context context) {
        this.context = context;
        startBackgroundThread();
    }

    private void startBackgroundThread() {
        backgroundThread = new HandlerThread(TAG, Process.THREAD_PRIORITY_BACKGROUND);
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        if (Build.VERSION.SDK_INT > 17) {
            backgroundThread.quitSafely();
        } else {
            backgroundThread.quit();
        }

        try {
            backgroundThread.join();
        } catch (InterruptedException e) {
            Logger.e(TAG, "stopBackgroundThread: " + e);
        } finally {
            backgroundThread = null;
            backgroundHandler = null;
        }
    }

    @Override
    public void openCamera(CameraOpenListener cameraOpenListener) {
        this.cameraOpenListener = cameraOpenListener;
    }

    @Override
    public int getCameraFace() {
        return cameraFace;
    }

    @Override
    public void switchCamera(int cameraFace) {
        if(this.cameraFace == cameraFace){
            return;
        }
        this.cameraFace = cameraFace;
    }

    @Override
    public void setPictureOutputSize(Size outputSize) {
        this.expectPictureSize = outputSize;
    }

    @Override
    public void setPictureOutputAspectRatio(AspectRatio outputAspectRatio) {
        this.expectAspectRatio = outputAspectRatio;
    }

    @Override
    public AspectRatio getAspectRatio() {
        return AspectRatio.of(previewSize);
    }

    @Override
    public void addCameraSizeListener(CameraSizeListener cameraSizeListener) {
        this.cameraSizeListeners.add(cameraSizeListener);
    }

    @Override
    public void takePicture(CameraPhotoListener cameraPhotoListener) {
        this.cameraPhotoListener = cameraPhotoListener;
    }

    @Override
    public void setVideoFileSize(long videoFileSize) {
        this.videoFileSize = videoFileSize;
    }

    @Override
    public void setVideoDuration(int videoDuration) {
        this.videoDuration = videoDuration;
    }

    @Override
    public void closeCamera(CameraCloseListener cameraCloseListener) {
        this.cameraCloseListener = cameraCloseListener;
    }

    @Override
    public void releaseCamera() {
        stopBackgroundThread();
    }

    @Override
    public void onInfo(MediaRecorder mr, int what, int extra) {
        if(MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED == what){
            onMaxDurationReached();
        }else if(MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED == what){
            onMaxFileSizeReached();
        }
    }
    /*-----------------------------------protected methods-----------------------------------*/

    void notifyCameraOpened(){
        if(cameraOpenListener != null){
            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    cameraOpenListener.onCameraOpened(cameraFace);
                }
            });
        }
    }

    void notifyCameraOpenError(final Throwable throwable){
        if(cameraOpenListener != null){
            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    cameraOpenListener.onCameraOpenError(throwable);
                }
            });
        }
    }

    void notifyCameraPictureTaken(final byte[] data){
        if(cameraPhotoListener != null){
            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    cameraPhotoListener.onPictureTaken(data);
                }
            });
        }
    }

    void notifyCameraCaptureFailed(final Throwable throwable) {
        if (cameraPhotoListener != null) {
            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    cameraPhotoListener.onCaptureFailed(throwable);
                }
            });
        }
    }

    void notifyVideoRecordStart() {
        if (cameraVideoListener != null) {
            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    cameraVideoListener.onVideoRecordStart();
                }
            });
        }
    }

    void notifyVideoRecordStop(final File file) {
        if (cameraVideoListener != null) {
            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    cameraVideoListener.onVideoRecordStop(file);
                }
            });
        }
    }

    void notifyVideoRecordError(final Throwable throwable) {
        if (cameraVideoListener != null) {
            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    cameraVideoListener.onVideoRecordError(throwable);
                }
            });
        }
    }
    void safeStopVideoRecorder() {
        try {
            if (videoRecorder != null) {
                videoRecorder.stop();
            }
        } catch (Exception ex) {
            notifyVideoRecordError(new RuntimeException(ex));
        }
    }

    void releaseVideoRecorder() {
        try {
            if (videoRecorder != null) {
                videoRecorder.reset();
                videoRecorder.release();
            }
        } catch (Exception ex) {
            notifyVideoRecordError(new RuntimeException(ex));
        } finally {
            videoRecorder = null;
        }
    }

    void notifyPreviewSizeUpdated(final Size previewSize) {
        uiHandler.post(new Runnable() {
            @Override
            public void run() {
                for (CameraSizeListener cameraSizeListener : cameraSizeListeners) {
                    cameraSizeListener.onPreviewSizeUpdated(previewSize);
                }
            }
        });
    }

    void notifyPictureSizeUpdated(final Size pictureSize) {
        uiHandler.post(new Runnable() {
            @Override
            public void run() {
                for (CameraSizeListener cameraSizeListener : cameraSizeListeners) {
                    cameraSizeListener.onPictureSizeUpdated(pictureSize);
                }
            }
        });
    }

    void notifyVideoSizeUpdated(final Size videoSize) {
        uiHandler.post(new Runnable() {
            @Override
            public void run() {
                for (CameraSizeListener cameraSizeListener : cameraSizeListeners) {
                    cameraSizeListener.onVideoSizeUpdated(videoSize);
                }
            }
        });
    }

    void notifyCameraClosed() {
        uiHandler.post(new Runnable() {
            @Override
            public void run() {
                if (cameraCloseListener != null) {
                    cameraCloseListener.onCameraClosed(cameraFace);
                }
            }
        });
    }

    void onMaxDurationReached() {
        stopVideoRecord();
    }

    void onMaxFileSizeReached() {
        stopVideoRecord();
    }


}
