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
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.os.Bundle
import android.util.Size
import android.view.LayoutInflater
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import io.reactivex.Observable
import kotlinx.android.synthetic.main.view_capture_photo_fragment.*
import org.fs.architecture.core.AbstractFragment
import org.fs.component.media.R
import org.fs.component.media.presenter.CapturePhotoFragmentPresenter
import org.fs.rx.extensions.util.clicks
import java.io.File

class CapturePhotoFragment : AbstractFragment<CapturePhotoFragmentPresenter>(), CapturePhotoFragmentView {

  override fun onCreateView(factory: LayoutInflater, parent: ViewGroup?,
      savedInstanceState: Bundle?): View? = factory.inflate(R.layout.view_capture_photo_fragment,
      parent, false)

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)

    presenter.restoreState(savedInstanceState ?: arguments)
    presenter.onCreate()
  }

  override fun setUp() = Unit

  override fun textureSize(): Size = Size(viewTexture.width, viewTexture.height)

  override fun activity(): Activity = activity ?: throw IllegalStateException(
      "fragment did not attached yet")

  override fun transformTextureView(matrix: Matrix) = viewTexture.setTransform(matrix)

  override fun surfaceTexture(): SurfaceTexture = viewTexture.surfaceTexture

  override fun surfaceTextureListener(listener: TextureView.SurfaceTextureListener) {
    viewTexture.surfaceTextureListener = listener
  }

  override fun bindPreview(file: File) {
    val thumb = BitmapFactory.decodeFile(file.absolutePath)
    viewPreview.setImageBitmap(thumb)
    viewPreview.isSelected = true
  }

  override fun isTextureAvailable(): Boolean = viewTexture.isAvailable

  override fun observeToggleFlash(): Observable<View> = viewButtonFlash.clicks()

  override fun observeChangeCamera(): Observable<View> = viewButtonCamera.clicks()

  override fun observeCapture(): Observable<View> = viewButtonCapture.clicks()
}
