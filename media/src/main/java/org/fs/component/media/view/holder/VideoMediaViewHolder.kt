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
package org.fs.component.media.view.holder

import android.view.View
import android.view.ViewGroup
import io.reactivex.Observable
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Consumer
import kotlinx.android.synthetic.main.view_video_item.view.*
import org.fs.architecture.common.BusManager
import org.fs.architecture.util.inflate
import org.fs.component.media.R
import org.fs.component.media.model.entity.Media
import org.fs.component.media.model.event.MediaSelectedEvent
import org.fs.component.media.util.plusAssign
import org.fs.rx.extensions.util.clicks

class VideoMediaViewHolder(view: View): BaseMediaViewHolder(view) {

  constructor(parent: ViewGroup): this(parent.inflate(R.layout.view_video_item))

  private val options by lazy { RequestOptions().apply {
      centerCrop()
      placeholder(R.drawable.ic_video_place_holder)
      diskCacheStrategy(DiskCacheStrategy.RESOURCE)
      dontAnimate()
    }
  }

  private val glide by lazy { Glide.with(view) }
  private val disposeBag by lazy { CompositeDisposable() }

  override fun bind(entity: Media) {
    // image preview
    val imageView = itemView.viewVideoPreview
    // clear
    glide.clear(imageView)
    // load
    glide.load(entity.file)
      .apply(options)
      .into(imageView)

    // selection
    disposeBag += bindMediaSelectedEvent(entity).subscribe(BusManager.Companion::send)
    disposeBag += BusManager.add(Consumer { evt -> when(evt) {
        is MediaSelectedEvent -> itemView.isSelected = entity == evt.media
      }
    })
  }

  override fun unbind() = disposeBag.clear()

  private fun bindMediaSelectedEvent(media: Media): Observable<MediaSelectedEvent> = itemView.clicks()
    .map { MediaSelectedEvent(media) }
}