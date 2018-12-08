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
package org.fs.component.media.view.adapter

import android.view.ViewGroup
import org.fs.architecture.common.scope.ForFragment
import org.fs.architecture.core.AbstractRecyclerViewAdapter
import org.fs.architecture.util.ObservableList
import org.fs.component.media.model.entity.Media
import org.fs.component.media.util.C
import org.fs.component.media.util.C.Companion.MEDIA_TYPE_IMAGE
import org.fs.component.media.util.C.Companion.MEDIA_TYPE_VIDEO
import org.fs.component.media.view.holder.BaseMediaViewHolder
import org.fs.component.media.view.holder.ImageMediaViewHolder
import org.fs.component.media.view.holder.VideoMediaViewHolder
import javax.inject.Inject

@ForFragment
class MediaAdapter @Inject constructor(dataSet: ObservableList<Media>): AbstractRecyclerViewAdapter<Media, BaseMediaViewHolder>(dataSet) {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseMediaViewHolder = when(viewType) {
    MEDIA_TYPE_VIDEO -> VideoMediaViewHolder(parent)
    MEDIA_TYPE_IMAGE -> ImageMediaViewHolder(parent)
    else -> throw IllegalArgumentException("we can not recognize viewType $viewType")
  }

  override fun getItemViewType(position: Int): Int = dataSet[position].type
}