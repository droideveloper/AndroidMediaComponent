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

package org.fs.component.media.common

import com.github.hiteshsondhi88.libffmpeg.FFmpegLoadBinaryResponseHandler

open class FFmpegBinaryCallback(
    private val start: (() -> Unit)? = null,
    private val finish: (() -> Unit)? = null,
    private val success: (() -> Unit)? = null,
    private val error: (() -> Unit)? = null): FFmpegLoadBinaryResponseHandler {

  override fun onFinish() = finish?.invoke() ?: Unit
  override fun onSuccess() = success?.invoke() ?: Unit
  override fun onFailure() = error?.invoke() ?: Unit
  override fun onStart() = start?.invoke() ?: Unit
}