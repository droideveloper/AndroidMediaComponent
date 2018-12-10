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

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.VideoView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import io.reactivex.Observable
import kotlinx.android.synthetic.main.view_next_activity.*
import org.fs.architecture.core.AbstractActivity
import org.fs.component.media.R
import org.fs.component.media.model.entity.Media
import org.fs.component.media.presenter.NextActivityPresenter
import org.fs.component.media.util.C.Companion.MEDIA_TYPE_IMAGE
import org.fs.component.media.util.C.Companion.MEDIA_TYPE_VIDEO
import org.fs.component.media.util.C.Companion.RENDER_FILL
import org.fs.component.media.util.Size
import org.fs.rx.extensions.util.clicks

class NextActivity : AbstractActivity<NextActivityPresenter>(), NextActivityView {

  private val imageViewPreview by lazy { ImageView(this) }
  private val videoViewPreview by lazy { VideoView(this) }
  private val glide by lazy { Glide.with(this) }

  private val showOrHideProgress: (Boolean) -> Unit = { show ->
    viewProgress.isIndeterminate = show
    viewProgress.visibility = if (show) View.VISIBLE else View.GONE
  }

  override fun onCreate(savedInstanceState: Bundle?) {
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
      else -> createFixLayoutParams()
    }

    when(media.type) {
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

  private fun createFillLayoutParams(media: Media): FrameLayout.LayoutParams = when(media.type) {
    MEDIA_TYPE_VIDEO -> {
      val retriever = MediaMetadataRetriever()
      retriever.setDataSource(media.file.absolutePath)
      val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH).toIntOrNull() ?: FrameLayout.LayoutParams.WRAP_CONTENT
      val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT).toIntOrNull() ?: FrameLayout.LayoutParams.WRAP_CONTENT
      FrameLayout.LayoutParams(width, height, Gravity.CENTER).also {
        retriever.release()
      }
    }
    MEDIA_TYPE_IMAGE -> {
      val bitmap = BitmapFactory.decodeFile(media.file.absolutePath)
      val metrics = resources.displayMetrics
      val w = Math.round(bitmap.width / metrics.density)
      val h = Math.round(bitmap.height / metrics.density)
      FrameLayout.LayoutParams(w, h).also {
        bitmap.recycle()
      }
    }
    else -> FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT)
  }

  private fun createFixLayoutParams(): FrameLayout.LayoutParams = FrameLayout.LayoutParams(viewPreviewLayout.width, viewPreviewLayout.height, Gravity.CENTER)

  override fun retrieveSize(): Size = Size(viewPreviewLayout.width, viewPreviewLayout.height)
}