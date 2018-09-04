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

import android.os.Bundle
import android.support.v7.widget.StaggeredGridLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.view_gallery_fragment.*
import org.fs.architecture.core.AbstractFragment
import org.fs.component.media.R
import org.fs.component.media.common.GlideApp
import org.fs.component.media.model.entity.Media
import org.fs.component.media.presenter.GalleryFragmentPresenter
import org.fs.component.media.view.adapter.MediaAdapter
import javax.inject.Inject

class GalleryFragment: AbstractFragment<GalleryFragmentPresenter>(), GalleryFragmentView {

  companion object {
    private const val RECYCLER_VIEW_ITEM_CACHE_SIZE = 10
    private const val ITEM_SPAN_SIZE = 4
  }

  @Inject lateinit var mediaAdapter: MediaAdapter
  private val glide by lazy { GlideApp.with(this) }

  override fun onCreateView(factory: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? = factory.inflate(R.layout.view_gallery_fragment, container, false)

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)

    presenter.restoreState(savedInstanceState ?: arguments)
    presenter.onCreate()
  }

  override fun setUp() {
    viewRecycler.apply {
      setHasFixedSize(true)
      isDrawingCacheEnabled = true
      drawingCacheQuality = View.DRAWING_CACHE_QUALITY_HIGH
      setItemViewCacheSize(RECYCLER_VIEW_ITEM_CACHE_SIZE)
      layoutManager = StaggeredGridLayoutManager(ITEM_SPAN_SIZE, StaggeredGridLayoutManager.HORIZONTAL)
      adapter = mediaAdapter
    }
  }

  override fun render(media: Media) = when(media) {
    Media.EMPTY -> Unit // no opt
    else -> {
      // do render here
    }
  }
}