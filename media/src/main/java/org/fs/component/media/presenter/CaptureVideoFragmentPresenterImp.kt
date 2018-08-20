/*
 * Media Component Copyright (C) 2018 Fatih.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fs.component.media.presenter

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Matrix
import android.graphics.RectF
import android.hardware.camera2.*
import android.media.MediaRecorder
import android.os.Handler
import android.os.HandlerThread
import android.support.v4.content.ContextCompat
import android.util.Size
import android.util.SparseIntArray
import android.view.Surface
import io.reactivex.disposables.CompositeDisposable
import org.fs.architecture.common.AbstractPresenter
import org.fs.architecture.common.scope.ForFragment
import org.fs.component.media.common.SimpleCameraDeviceStateListener
import org.fs.component.media.common.SimpleCaptureSessionStateListener
import org.fs.component.media.common.SimpleSurfaceTextureListener
import org.fs.component.media.common.annotation.Direction
import org.fs.component.media.common.annotation.FlashMode
import org.fs.component.media.util.C
import org.fs.component.media.util.component1
import org.fs.component.media.util.component2
import org.fs.component.media.util.plusAssign
import org.fs.component.media.view.CaptureVideoFragmentView
import java.io.File
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@ForFragment
class CaptureVideoFragmentPresenterImp @Inject constructor(
    view: CaptureVideoFragmentView) : AbstractPresenter<CaptureVideoFragmentView>(view), CaptureVideoFragmentPresenter {

  companion object {
    private const val CAMERA_BACKGROUND_THREAD = "CameraBackground"
    private const val REQUEST_CAMERA_PERMISSION = 0x99
    private const val CAMERA_TIMEOUT = 2500L
    private const val FILE_SUFFIX = ".mp4"

    private const val SENSOR_ORIENTATION_DEFAULT_DEGREES = 90
    private const val SENSOR_ORIENTATION_INVERSE_DEGREES = 270
    private val DEFAULT_ORIENTATIONS = SparseIntArray().apply {
      append(Surface.ROTATION_0, 90)
      append(Surface.ROTATION_90, 0)
      append(Surface.ROTATION_180, 270)
      append(Surface.ROTATION_270, 180)
    }
    private val INVERSE_ORIENTATIONS = SparseIntArray().apply {
      append(Surface.ROTATION_0, 270)
      append(Surface.ROTATION_90, 180)
      append(Surface.ROTATION_180, 90)
      append(Surface.ROTATION_270, 0)
    }
  }

  /** surfaceTextureListener **/
  private val whenAvailable: (width: Int, height: Int) -> Unit = { width, height -> openCamera(width, height) }

  private val whenSizeChanged: (width: Int, height: Int) -> Unit = { width, height ->  configureTransform(width, height) }

  private val surfaceTextureListener = SimpleSurfaceTextureListener(whenAvailable, whenSizeChanged)
  /** surfaceTextureListener **/

  /** stateListener **/
  private val whenOpened: (camera: CameraDevice?) -> Unit = { camera ->
    cameraOpenOrCloseLock.release()
    this.camera = camera
    startPreview()
    val (width, height) = previewSize
    configureTransform(width, height)
  }

  private val whenDisconnected: (camera: CameraDevice?) -> Unit = { camera ->
    cameraOpenOrCloseLock.release()
    camera?.close()
    this.camera = null
  }

  private val whenError: (camera: CameraDevice?, error: Int) -> Unit = { camera, _ -> whenDisconnected(camera) }

  private val stateListener = SimpleCameraDeviceStateListener(whenOpened, whenDisconnected, whenError)
  /** stateListener **/

  @SuppressWarnings("MissingPermission")
  private val openCamera: (width: Int, height: Int) -> Unit = { width, height ->
    if (checkIfPermissionGranted()) {
      if (!cameraOpenOrCloseLock.tryAcquire(CAMERA_TIMEOUT, TimeUnit.MILLISECONDS)) {
        throw RuntimeException("we can not create valid session for this device... timeout")
      }
      whenSizeChanged(width, height)
      setUpCameraOutputs()

      val context = view.getContext()
      context?.let { ctx ->
        val manager = ctx.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        manager.openCamera(cameraId, stateListener, null)
      }
    } else {
      view.requestPermissions(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO), REQUEST_CAMERA_PERMISSION)
    }
  }

  private val setUpCameraOutputs: () -> Unit = {
    val context = view.getContext()
    context?.let { ctx ->
      val manager = ctx.getSystemService(Context.CAMERA_SERVICE) as CameraManager

      manager.cameraIdList.forEach { cameraId ->
        val characteristics = manager.getCameraCharacteristics(cameraId)

        val direction = characteristics.get(CameraCharacteristics.LENS_FACING)
        if (direction != null && direction == cameraDirection) {

          mediaRecorder = MediaRecorder()

          sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)

          flashSupported = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true

          this.cameraId = cameraId
        }
      }
    }
  }

  private val configureTransform: (width: Int, height: Int) -> Unit = { width, height ->
    val rotation = view.activity().windowManager.defaultDisplay.rotation
    val matrix = Matrix()
    val viewRect = RectF(0f, 0f, width.toFloat(), height.toFloat())
    val bufferRect = RectF(0f, 0f, previewSize.width.toFloat(), previewSize.height.toFloat())
    val cx = viewRect.centerX()
    val cy = viewRect.centerY()

    if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
      bufferRect.offset(cx - bufferRect.centerX(), cy - bufferRect.centerY())
      matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)
      val scale = Math.max(viewRect.height() / previewSize.height, viewRect.width() / previewSize.width)
      matrix.apply {
        postScale(scale, scale, cx, cy)
        postRotate((90 * (rotation - 2)).toFloat(), cx, cy)
      }
    }
    view.transformTextureView(matrix)
  }

  private val startPreview: () -> Unit = {
    camera?.let { cameraDevice ->
      if (view.isTextureAvailable()) {
        closePreviewSession()
        val texture = view.surfaceTexture()
        val (width, height) = previewSize
        texture.setDefaultBufferSize(width, height)

        previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        val previewSurface = Surface(texture)

        previewRequestBuilder.addTarget(previewSurface)

        val whenConfigured: (session: CameraCaptureSession?) -> Unit = { captureSession ->
          this.captureSession = captureSession
          updatePreview()
        }

        cameraDevice.createCaptureSession(listOf(previewSurface), SimpleCaptureSessionStateListener(whenConfigured), backgroundHandler)
      }
    }
  }

  private val updatePreview: () -> Unit = {
    camera?.let { _ ->
      setUpCaptureRequestBuilder(previewRequestBuilder)
      captureSession?.setRepeatingRequest(previewRequestBuilder.build(), null, backgroundHandler)
    }
  }

  private val closePreviewSession: () -> Unit = {
    captureSession?.close()
    captureSession = null
  }

  private val startRecordingVideo: () -> Unit = {
    camera?.let { cameraDevice ->
      if (view.isTextureAvailable()) {

        closePreviewSession
        setUpMediaRecorder()

        val texture = view.surfaceTexture()
        val (width, height) = previewSize
        texture.setDefaultBufferSize(width, height)

        val previewSurface = Surface(texture)
        mediaRecorder?.let { recorder ->
          val recordSurface = recorder.surface
          val surfaces = arrayListOf(previewSurface, recordSurface)

          previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
          previewRequestBuilder.addTarget(previewSurface)
          previewRequestBuilder.addTarget(recordSurface)

          val whenConfigured: (session: CameraCaptureSession?) -> Unit = { captureSession ->
            this.captureSession = captureSession
            this.recording = true
            updatePreview()
          }

          cameraDevice.createCaptureSession(surfaces, SimpleCaptureSessionStateListener(whenConfigured), backgroundHandler)
        }
      }
    }
  }

  private val stopRecordingVideo: () -> Unit = {
    this.recording = false
    mediaRecorder?.apply {
      stop()
      reset()
    }
    startPreview()
  }

  private fun setUpCaptureRequestBuilder(builder: CaptureRequest.Builder) {
    builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
    if (flashSupported) {
      when(flash) {
        C.FLASH_MODE_AUTO -> autoFlash(builder)
        C.FLASH_MODE_DISABLED -> disableFlash(builder)
      }
    }
  }

  private val autoFlash: (requestBuilder: CaptureRequest.Builder) -> Unit = { requestBuilder ->
    if (flashSupported) {
      requestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)
    }
  }

  private val disableFlash: (requestBuilder: CaptureRequest.Builder) -> Unit = { requestBuilder ->
    if (flashSupported) {
      requestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
    }
  }

  private val startBackgroundThread: () -> Unit = {
    backgroundThread = HandlerThread(CAMERA_BACKGROUND_THREAD)
    backgroundThread?.start()
    backgroundHandler = Handler(backgroundThread?.looper)
  }

  private val stopBackgroundThread: () -> Unit = {
    backgroundThread?.quitSafely()
    backgroundThread?.join()
    backgroundThread = null
    backgroundHandler = null
  }

  private val closeCamera: () -> Unit = {
    cameraOpenOrCloseLock.acquire()
    closePreviewSession()
    camera?.close()
    camera = null
    mediaRecorder?.release()
    mediaRecorder = null
    cameraOpenOrCloseLock.release()
  }

  private val setUpMediaRecorder: () -> Unit = {
    val rotation = view.activity().windowManager.defaultDisplay.rotation
    when(sensorOrientation) {
      SENSOR_ORIENTATION_DEFAULT_DEGREES -> mediaRecorder?.setOrientationHint(DEFAULT_ORIENTATIONS.get(rotation))
      SENSOR_ORIENTATION_INVERSE_DEGREES -> mediaRecorder?.setOrientationHint(INVERSE_ORIENTATIONS.get(rotation))
    }

    mediaRecorder?.apply {
      setAudioSource(MediaRecorder.AudioSource.MIC)
      setVideoSource(MediaRecorder.VideoSource.SURFACE)
      setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
      setOutputFile(file.absolutePath) // LOL
      setVideoEncodingBitRate(10000000) // why 10m
      setVideoFrameRate(30) // might need to change those
      val (width, height) = previewSize
      setVideoSize(width, height)
      setVideoEncoder(MediaRecorder.VideoEncoder.H264)
      setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
      prepare()
    }
  }

  private val checkIfPermissionGranted: () -> Boolean = {
    view.getContext()?.let { ctx ->
      ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    } ?: false
  }

  private val cameraOpenOrCloseLock = Semaphore(1)

  private var camera: CameraDevice? = null
  private var captureSession: CameraCaptureSession? = null
  private var backgroundThread: HandlerThread? = null
  private var backgroundHandler: Handler? = null
  private var mediaRecorder: MediaRecorder? = null

  private lateinit var previewSize: Size
  private lateinit var previewRequestBuilder: CaptureRequest.Builder
  private lateinit var cameraId: String

  private var recording = false
  private var sensorOrientation = 0
  private var flashSupported = false
  @FlashMode private var flash = C.FLASH_MODE_AUTO
  @Direction private var cameraDirection = CameraCharacteristics.LENS_FACING_BACK

  private val file get() = File(File(view.getContext()?.filesDir, "vid_captures"),
    "${System.currentTimeMillis()}_VID$FILE_SUFFIX")

  private val disposeBag by lazy { CompositeDisposable() }

  override fun onCreate() {
    if (view.isAvailable()) {
      view.setUp()
      // we will create this for user storage to execute video and other components
      val directory = File(view.getContext()?.filesDir, "vid_captures")
      if (!directory.exists()) {
        directory.mkdirs()
      }
    }
  }

  override fun onStart() {
    if (view.isAvailable()) {
      // change flash
      disposeBag += view.observeToggleFlash()
        .map { toggle ->
          val newFlash = when(flash) {
            C.FLASH_MODE_AUTO -> C.FLASH_MODE_DISABLED
            C.FLASH_MODE_DISABLED -> C.FLASH_MODE_AUTO
            else -> C.FLASH_MODE_AUTO
          }
          toggle.isSelected = newFlash == C.FLASH_MODE_AUTO
          newFlash
        }
        .subscribe { mode ->
          flash = mode
          when(mode) {
            C.FLASH_MODE_AUTO -> autoFlash(previewRequestBuilder)
            C.FLASH_MODE_DISABLED -> disableFlash(previewRequestBuilder)
          }
        }
      // change camera
      disposeBag += view.observeChangeCamera()
        .map { _ ->
          when(cameraDirection) {
            CameraCharacteristics.LENS_FACING_BACK -> CameraCharacteristics.LENS_FACING_FRONT
            CameraCharacteristics.LENS_FACING_FRONT -> CameraCharacteristics.LENS_FACING_BACK
            else -> CameraCharacteristics.LENS_FACING_BACK
          }
        }
        .subscribe { direction ->
          cameraDirection = direction
          closeCamera()
          val (width, height) = view.textureSize()
          openCamera(width, height)
        }
      // stop or start recording
      disposeBag += view.observeStartOrStopRecord()
        .map { toggle -> toggle.isSelected == recording }
        .subscribe { record ->
          if (record) {
            startRecordingVideo()
          } else {
            stopRecordingVideo()
          }
        }
    }
  }

  override fun onStop() {
    disposeBag.clear()
  }

  override fun onResume() {
    startBackgroundThread()
    if (view.isTextureAvailable()) {
      previewSize = view.textureSize()
      val (width, height) = previewSize
      openCamera(width, height)
    } else {
      previewSize = view.textureSize()
      view.surfaceTextureListener(surfaceTextureListener)
    }
  }

  override fun onPause() {
    closeCamera()
    stopBackgroundThread()
  }
}  