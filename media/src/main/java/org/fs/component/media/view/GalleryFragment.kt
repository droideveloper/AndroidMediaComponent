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

import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.support.v4.content.res.ResourcesCompat
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.StaggeredGridLayoutManager
import android.util.Log
import android.view.*
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.VideoView
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.view_gallery_fragment.*
import kotlinx.android.synthetic.main.view_video_item.*
import org.fs.architecture.common.ThreadManager
import org.fs.architecture.core.AbstractFragment
import org.fs.component.media.R
import org.fs.component.media.common.annotation.MediaType
import org.fs.component.media.model.entity.Media
import org.fs.component.media.presenter.GalleryFragmentPresenter
import org.fs.component.media.presenter.GalleryFragmentPresenterImp
import org.fs.component.media.util.C.Companion.MEDIA_TYPE_IMAGE
import org.fs.component.media.util.C.Companion.MEDIA_TYPE_VIDEO
import org.fs.component.media.util.C.Companion.TOUCH_STEP
import org.fs.component.media.util.C.Companion.UI_THREAD_DELAY
import org.fs.component.media.util.cx
import org.fs.component.media.util.cy
import org.fs.component.media.view.adapter.MediaAdapter
import javax.inject.Inject

class GalleryFragment: AbstractFragment<GalleryFragmentPresenter>(), GalleryFragmentView {

  companion object {
    private const val RECYCLER_VIEW_ITEM_CACHE_SIZE = 10
    private const val ITEM_SPAN_SIZE = 4

    @JvmStatic fun newFragment(@MediaType style: Int): GalleryFragment = GalleryFragment().apply {
      arguments = Bundle().apply {
        putInt(GalleryFragmentPresenterImp.BUNDLE_ARGS_MEDIA_TYPE, style)
      }
    }
  }

  @Inject lateinit var mediaAdapter: MediaAdapter
  private val glide by lazy { Glide.with(this) }
  private val lp by lazy { FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT).apply {
      gravity = Gravity.CENTER
    }
  }

  private val verticalDividerDrawable by lazy { ResourcesCompat.getDrawable(resources, R.drawable.ic_divider_vertical, context?.theme) }
  private val horizontalDividerDrawable by lazy { ResourcesCompat.getDrawable(resources, R.drawable.ic_divider_horizontal, context?.theme) }

  private val imageViewPreview by lazy { ImageView(context) }
  private val videoViewPreview by lazy { VideoView(context) }

  private val showOrHide: (Boolean) -> Unit = { visible ->
    viewProgress.visibility = if (visible) View.VISIBLE else View.GONE
    viewProgress.isIndeterminate = visible
  }

  override fun onCreateView(factory: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? = factory.inflate(R.layout.view_gallery_fragment, container, false)

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)

    presenter.restoreState(savedInstanceState ?: arguments)
    presenter.onCreate()
  }

  override fun setUp() {
    viewRecycler.apply {
      setHasFixedSize(true)
      setItemViewCacheSize(RECYCLER_VIEW_ITEM_CACHE_SIZE)
      layoutManager = StaggeredGridLayoutManager(ITEM_SPAN_SIZE, StaggeredGridLayoutManager.VERTICAL)
      adapter = mediaAdapter
      applyDivider(verticalDividerDrawable, DividerItemDecoration.VERTICAL)
      applyDivider(horizontalDividerDrawable, DividerItemDecoration.HORIZONTAL)
    }
  }

  override fun showProgress() = showOrHide(true)
  override fun hideProgress() = showOrHide(false)

  override fun render(media: Media) {
    when (media) {
      Media.EMPTY -> viewPreviewLayout.removeAllViews()
      else -> when (media.type) {
        MEDIA_TYPE_IMAGE -> {
          viewPreviewLayout.removeAllViews()
          viewPreviewLayout.addView(imageViewPreview, lp)
          glide.clear(imageViewPreview) // clear first
          // load file from locale
          glide.load(Uri.fromFile(media.file))
            .into(imageViewPreview)
        }
        MEDIA_TYPE_VIDEO -> {
          viewPreviewLayout.removeAllViews()
          viewPreviewLayout.addView(videoViewPreview, lp)
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
        else -> throw IllegalArgumentException(
            "we do not know why this is error for media type ${media.type}")
      }
    }
  }

  override fun emulateTouch() {
    if (mediaAdapter.itemCount != 0) {
      viewRecycler.scrollToPosition(0)
      // we simulate touch event in here with this concept

      val startX = viewCoordinatorLayout.cx()
      val startY = Math.round(viewCoordinatorLayout.cy())
      val endY = Math.round(viewCoordinatorLayout.cy() * 1.5f)

      // down
      val eventDown = MotionEvent.obtain(SystemClock.uptimeMillis(),
          SystemClock.uptimeMillis(),
          MotionEvent.ACTION_DOWN,
          startX, startY.toFloat(), 0)
      viewCoordinatorLayout.dispatchTouchEvent(eventDown)
      eventDown.recycle()

      // move loop
      (startY..endY).step(TOUCH_STEP).forEach { y ->
        // move start
        val eventMove = MotionEvent.obtain(SystemClock.uptimeMillis(),
            SystemClock.uptimeMillis(),
            MotionEvent.ACTION_MOVE,
            startX, y.toFloat(), 0)
        viewCoordinatorLayout.dispatchTouchEvent(eventMove)
        eventMove.recycle()
      }

      // up
      val eventUp = MotionEvent.obtain(SystemClock.uptimeMillis(),
          SystemClock.uptimeMillis(),
          MotionEvent.ACTION_UP,
          startX, endY.toFloat(), 0)
      viewCoordinatorLayout.dispatchTouchEvent(eventUp)
      eventUp.recycle()
    }
  }

  private fun applyDivider(drawable: Drawable?, orientation: Int) {
    viewRecycler.apply {
      drawable?.let { dividerDrawable ->
        val divider = DividerItemDecoration(context, orientation)
        divider.setDrawable(dividerDrawable)
        addItemDecoration(divider)
      }
    }
  }
}