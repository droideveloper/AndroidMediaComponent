/*
 * Playz Android Kotlin Copyright (C) 2018 Fatih, Playz.lol.
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
import android.graphics.Canvas
import android.support.v4.content.res.ResourcesCompat
import android.support.v7.widget.AppCompatImageView
import android.util.AttributeSet
import org.fs.component.media.R

class SelectableImageView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, style: Int = 0): AppCompatImageView(context, attrs, style) {

  private val rectangle by lazy { ResourcesCompat.getDrawable(resources, R.drawable.ic_selected_media, context.theme) }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    val w = MeasureSpec.getSize(widthMeasureSpec)
    val h = MeasureSpec.getSize(heightMeasureSpec)

    rectangle?.setBounds(0, 0, w, h)

    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
  }

  override fun setSelected(selected: Boolean) {
    super.setSelected(selected)
    invalidate()
  }

  override fun onDraw(canvas: Canvas?) {
    super.onDraw(canvas)
    if (isSelected) {
      canvas?.let { c -> rectangle?.draw(c) }
    }
  }
}