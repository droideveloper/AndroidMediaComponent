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

import android.graphics.Point
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.VideoView
import com.bumptech.glide.Glide
import io.reactivex.Observable
import kotlinx.android.synthetic.main.view_next_activity.*
import org.fs.architecture.core.AbstractActivity
import org.fs.component.media.R
import org.fs.component.media.model.entity.Media
import org.fs.component.media.presenter.NextActivityPresenter
import org.fs.component.media.util.C
import org.fs.component.media.util.Size
import org.fs.rx.extensions.util.clicks

class NextActivity : AbstractActivity<NextActivityPresenter>(), NextActivityView {

  private val imageViewPreview by lazy { ImageView(this) }
  private val videoViewPreview by lazy { VideoView(this) }
  private val lp by lazy { FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT) }
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

  override fun setUp(media: Media?) {
    hideProgress()
    when(media?.type) {
      C.MEDIA_TYPE_VIDEO -> {
        viewScrollPreview.removeAllViews()
        viewScrollPreview.addView(imageViewPreview, lp)
        glide.clear(imageViewPreview)
        glide.load(Uri.fromFile(media.file))
          .into(imageViewPreview)
      }
      C.MEDIA_TYPE_IMAGE -> {
        viewScrollPreview.removeAllViews()
        viewScrollPreview.addView(videoViewPreview, lp)
        videoViewPreview.setVideoURI(Uri.fromFile(media.file))
        videoViewPreview.start()
      }
      C.MEDIA_TYPE_ALL -> viewScrollPreview.removeAllViews()
    }
  }

  override fun showProgress() = showOrHideProgress(true)
  override fun hideProgress() = showOrHideProgress(false)

  override fun position(): Point = Point(viewScrollPreview.scrollX, viewScrollPreview.scrollY)
  override fun size(): Size = Size(viewPreviewLayout.width, viewPreviewLayout.height)
  override fun observeNext(): Observable<View> = viewButtonNext.clicks()
  override fun observeCancel(): Observable<View> = viewButtonCancel.clicks()
}