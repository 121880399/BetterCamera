package org.zzy.lib.bettercamera.manager;

import android.content.Context;
import android.hardware.Camera;

import org.zzy.lib.bettercamera.bean.AspectRatio;
import org.zzy.lib.bettercamera.bean.Size;
import org.zzy.lib.bettercamera.bean.SizeMap;
import org.zzy.lib.bettercamera.constant.CameraConstant;
import org.zzy.lib.bettercamera.constant.MediaConstant;
import org.zzy.lib.bettercamera.listener.CameraCloseListener;
import org.zzy.lib.bettercamera.listener.CameraOpenListener;
import org.zzy.lib.bettercamera.listener.CameraPhotoListener;
import org.zzy.lib.bettercamera.listener.CameraSizeListener;
import org.zzy.lib.bettercamera.listener.CameraVideoListener;

import java.io.File;

/**
 * 相机接口
 * @作者 ZhouZhengyi
 * @创建日期 2019/6/4
 */
public interface CameraManager {

    /**
     * 初始化
     * @param context
     */
    void initialize(Context context);

    /**
     * 打开相机
     */
    void openCamera(CameraOpenListener cameraOpenListener);

    /**
     * 相机是否打开
     */
    boolean isCameraOpened();

    /**
     * 切换前后置摄像头
     */
    void switchCamera(@CameraConstant.Face int cameraFace);

    /**
     * 得到当前是前置摄像头还是后置摄像头
     */
    @CameraConstant.Face int getCameraFace();

    /**
     * 设置媒体类型
     * @param mediaType 拍照/视频
     */
    void setMediaType(@MediaConstant.Type int mediaType);

    /**
     * 设置快门声音是否的打开
     */
    void setVoiceEnable(boolean voiceEnable);

    /**
     * 得到快门声音是否的打开
     */
    boolean isVoiceEnable();

    /**
     * 设置是否自动对焦
     */
    void setAutoFocus(boolean autoFocus);

    /**
     * 得到目前是否自动对焦
     */
    boolean isAutoFocus();

    /**
     * 设置闪光灯模式
     */
    void setFlashMode(@CameraConstant.FlashMode int flashMode);

    /**
     * 得到当前闪光灯模式
     */
    @CameraConstant.FlashMode
    int getFlashMode();

    /**
     * 设置变焦
     */
    void setZoom(float zoom);

    /**
     * 得到当前缩放大小
     */
    float getZoom();

    /**
     * 得到最大变焦
     */
    float getMaxZoom();

    /**
     * 设置期望大小(图片，视频)
     */
    void setExpectSize(Size outputSize);

    /**
     * 设置照片输出的宽高比
     */
    void setExpectAspectRatio(AspectRatio outputAspectRatio);

    /**
     * 得到对应尺寸
     */
    Size getSize(@CameraConstant.SizeFor int sizeFor);

    /**
     * 得到对应尺寸集合
     */
    SizeMap getSizes(@CameraConstant.SizeFor int sizeFor);

    /**
     * 得到宽高比
     */
    AspectRatio getAspectRatio();

    /**
     * 设置显示方向
     * @param displayOrientation 横屏、竖屏
     */
    void setDisplayOrientation(int displayOrientation);

    /**
     * 添加相机尺寸改变监听
     */
    void addCameraSizeListener(CameraSizeListener cameraSizeListener);

    /**
     * 得到所拍照片
     */
    void takePicture(CameraPhotoListener cameraPhotoListener);

    /**
     * 设置视频文件大小
     * @param videoFileSize
     */
    void setVideoFileSize(long videoFileSize);

    /**
     * 设置视频时长
     * @param videoDuration
     */
    void setVideoDuration(int videoDuration);

    /**
     * 开始录制
     * @param file
     * @param cameraVideoListener
     */
    void startVideoRecord(File file, CameraVideoListener cameraVideoListener);

    /**
     * 停止录制
     */
    void stopVideoRecord();

    /**
     * 恢复预览
     */
    void resumePreview();

    /**
     * 关闭相机
     */
    void closeCamera(CameraCloseListener cameraCloseListener);

    /**
     * 释放相机
     */
    void releaseCamera();
}
