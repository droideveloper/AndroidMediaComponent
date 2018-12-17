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
package org.fs.component.media.view

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.VideoView
import com.bumptech.glide.Glide
import io.reactivex.Observable
import kotlinx.android.synthetic.main.view_next_activity.*
import org.fs.architecture.core.AbstractActivity
import org.fs.component.gallery.model.entity.Media
import org.fs.component.gallery.util.C.Companion.MEDIA_TYPE_IMAGE
import org.fs.component.gallery.util.C.Companion.MEDIA_TYPE_VIDEO
import org.fs.component.media.R
import org.fs.component.media.presenter.NextActivityPresenter
import org.fs.component.media.util.C.Companion.RENDER_FILL
import org.fs.component.media.util.C.Companion.ROTATION_270
import org.fs.component.media.util.C.Companion.ROTATION_90
import org.fs.component.media.util.Size
import org.fs.component.media.util.Timeline
import org.fs.rx.extensions.util.clicks

class NextActivity : AbstractActivity<NextActivityPresenter>(), NextActivityView {

  private val imageViewPreview by lazy { ImageView(this) }
  private val videoViewPreview by lazy { VideoView(this) }
  private val glide by lazy { Glide.with(this) }

  private val showOrHideProgress: (Boolean) -> Unit = { show ->
    viewProgress.isIndeterminate = show
    viewProgress.visibility = if (show) View.VISIBLE else View.GONE
    // will be disabled when we showing progress
    viewButtonNext.isEnabled = !show
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    overridePendingTransition(R.anim.translate_right_in, R.anim.scale_out)
    super.onCreate(savedInstanceState)
    setContentView(R.layout.view_next_activity)

    presenter.restoreState(savedInstanceState ?: intent.extras)
    presenter.onCreate()
  }

  override fun setUp(media: Media, renderMode: Int) {
    hideProgress()
    render(media, renderMode)
  }

  override fun render(media: Media, renderMode: Int) {
    viewPreview.removeAllViews()

    val lp = when(renderMode) {
      RENDER_FILL -> createFillLayoutParams(media)
      else -> createFixLayoutParams(media)
    }

    when(media.mediaType) {
      MEDIA_TYPE_IMAGE -> {
        viewPreview.addView(imageViewPreview, lp)
        glide.clear(imageViewPreview)
        glide.load(Uri.fromFile(media.file))
          .into(imageViewPreview)
      }
      MEDIA_TYPE_VIDEO -> {
        viewPreview.addView(videoViewPreview, lp)
        videoViewPreview.setVideoURI(Uri.fromFile(media.file))
        videoViewPreview.setOnCompletionListener { mp ->
          mp.seekTo(0)
          mp.start()
        }
        videoViewPreview.setOnPreparedListener { mp ->
          mp.setVolume(0f, 0f)
          mp.start()
        }
        videoViewPreview.start()
      }
      else -> Unit
    }
  }

  override fun observeChangeScale(): Observable<View> = viewChangeScale.clicks()

  override fun showProgress() = showOrHideProgress(true)
  override fun hideProgress() = showOrHideProgress(false)

  override fun observeNext(): Observable<View> = viewButtonNext.clicks()
  override fun observeCancel(): Observable<View> = viewButtonCancel.clicks()

  private fun createFillLayoutParams(media: Media): FrameLayout.LayoutParams = when(media.mediaType) {
    MEDIA_TYPE_VIDEO -> {
      val retriever = MediaMetadataRetriever()
      retriever.setDataSource(media.file.absolutePath)
      val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION).toInt()
      val width = when(rotation) {
        ROTATION_90, ROTATION_270 -> retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT).toInt()
        else -> retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH).toInt()
      }
      val height = when(rotation) {
        ROTATION_90, ROTATION_270 -> retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH).toInt()
        else -> retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT).toInt()
      }
      FrameLayout.LayoutParams(width, height, Gravity.CENTER).also {
        retriever.release()
      }
    }
    MEDIA_TYPE_IMAGE -> {
      val bitmap = BitmapFactory.decodeFile(media.file.absolutePath)
      FrameLayout.LayoutParams(bitmap.width, bitmap.height, Gravity.CENTER).also {
        bitmap.recycle()
      }
    }
    else -> FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER)
  }

  private fun createFixLayoutParams(media: Media): FrameLayout.LayoutParams = when(media.mediaType) {
    MEDIA_TYPE_VIDEO -> {
      val retriever = MediaMetadataRetriever()
      retriever.setDataSource(media.file.absolutePath)
      val metrics = resources.displayMetrics
      val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION).toInt()
      val width = when(rotation) {
        ROTATION_90, ROTATION_270 -> retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT).toInt()
        else -> retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH).toInt()
      }
      val height = when(rotation) {
        ROTATION_90, ROTATION_270 -> retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH).toInt()
        else -> retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT).toInt()
      }
      val ratio = if (height > width) width / height.toFloat() else height / width.toFloat()
      val max = Math.max(width, height)
      val wp = if (max == width) metrics.widthPixels else Math.round(metrics.widthPixels * ratio)
      val hp = if (max == height) metrics.widthPixels else Math.round(metrics.widthPixels * ratio)
      FrameLayout.LayoutParams(wp, hp, Gravity.CENTER).also {
        retriever.release()
      }
    }
    MEDIA_TYPE_IMAGE -> {
      val bitmap = BitmapFactory.decodeFile(media.file.absolutePath)
      val metrics = resources.displayMetrics
      val width = Math.round(bitmap.width / metrics.density)
      val height = Math.round(bitmap.height / metrics.density)
      val ratio = if (height > width) width / height.toFloat() else height / width.toFloat()
      val max = Math.max(width, height)
      val wp = if (max == width) metrics.widthPixels else Math.round(metrics.widthPixels * ratio)
      val hp = if (max == height) metrics.widthPixels else Math.round(metrics.widthPixels * ratio)
      FrameLayout.LayoutParams(wp, hp, Gravity.CENTER).also {
        bitmap.recycle()
      }
    }
    else -> FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER)
  }

  // TODO change this
  override fun retrieveTimeline(): Timeline = Timeline(1000L, 5000L) // this will not provide what I want but

  override fun displayMetrics(): DisplayMetrics = resources.displayMetrics

  override fun retrieveSize(mediaType: Int): Size = when(mediaType) {
    MEDIA_TYPE_VIDEO -> Size(videoViewPreview.width, videoViewPreview.height)
    MEDIA_TYPE_IMAGE -> Size(imageViewPreview.width, imageViewPreview.height)
    else -> Size(0, 0)
  }
  override fun retrieveXY(): Size = Size(viewXScrollLayout.scrollX, viewYScrollLayout.scrollY)
  override fun previewSize(): Size = Size(viewPreviewLayout.width, viewPreviewLayout.height)

  override fun setResultAndFinish(data: Intent?) {
    if (data != null) {
      setResult(Activity.RESULT_OK, data)
    } else {
      setResult(Activity.RESULT_CANCELED)
    }
    finish()
  }

  override fun finish() {
    super.finish()
    overridePendingTransition(R.anim.scale_in, R.anim.translate_right_out)
  }
}