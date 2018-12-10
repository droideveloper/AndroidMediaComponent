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
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.util.Size
import android.view.TextureView
import android.view.View
import io.reactivex.Observable
import org.fs.architecture.common.ViewType
import java.io.File

interface CapturePhotoFragmentView: ViewType {
  fun setUp()
  fun activity(): Activity
  fun transformTextureView(matrix: Matrix)
  fun textureSize(): Size
  fun surfaceTexture(): SurfaceTexture
  fun surfaceTextureListener(listener: TextureView.SurfaceTextureListener)
  fun isTextureAvailable(): Boolean
  fun observeToggleFlash(): Observable<View>
  fun observeChangeCamera(): Observable<View>
  fun observeCapture(): Observable<View>
  fun bindPreview(file: File)
}