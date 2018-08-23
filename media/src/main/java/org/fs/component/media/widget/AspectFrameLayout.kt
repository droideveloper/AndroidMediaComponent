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
package org.fs.component.media.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout

class AspectFrameLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, style: Int = 0): FrameLayout(context, attrs, style) {

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    val width = MeasureSpec.getSize(widthMeasureSpec)
    val height = MeasureSpec.getSize(heightMeasureSpec)

    val size = Math.min(width, height)
    val sizeSpec = MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY)
    super.onMeasure(sizeSpec, sizeSpec)
  }
}