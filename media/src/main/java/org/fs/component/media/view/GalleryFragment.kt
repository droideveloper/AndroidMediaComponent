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

import android.net.Uri
import android.os.Bundle
import android.support.v7.widget.StaggeredGridLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.VideoView
import kotlinx.android.synthetic.main.view_gallery_fragment.*
import org.fs.architecture.core.AbstractFragment
import org.fs.component.media.R
import org.fs.component.media.common.GlideApp
import org.fs.component.media.common.annotation.MediaType
import org.fs.component.media.model.entity.Media
import org.fs.component.media.presenter.GalleryFragmentPresenter
import org.fs.component.media.presenter.GalleryFragmentPresenterImp
import org.fs.component.media.util.C
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
  private val glide by lazy { GlideApp.with(this) }
  private val lp by lazy { FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT) }

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
      layoutManager = StaggeredGridLayoutManager(ITEM_SPAN_SIZE, StaggeredGridLayoutManager.HORIZONTAL)
      adapter = mediaAdapter
    }
  }

  override fun showProgress() = showOrHide(true)
  override fun hideProgress() = showOrHide(false)

  override fun render(media: Media) {
    when (media) {
      Media.EMPTY -> viewPreviewLayout.removeAllViews()
      else -> when (media.type) {
        C.MEDIA_TYPE_IMAGE -> {
          viewPreviewLayout.removeAllViews()
          viewPreviewLayout.addView(imageViewPreview, lp)
          glide.clear(imageViewPreview) // clear first
          // load file from locale
          glide.load(Uri.fromFile(media.file))
            .into(imageViewPreview)
        }
        C.MEDIA_TYPE_VIDEO -> {
          viewPreviewLayout.removeAllViews()
          viewPreviewLayout.addView(videoViewPreview, lp)
          videoViewPreview.setVideoURI(Uri.fromFile(media.file))
          videoViewPreview.start()
        }
        else -> throw IllegalArgumentException(
            "we do not know why this is error for media type ${media.type}")
      }
    }
  }
}