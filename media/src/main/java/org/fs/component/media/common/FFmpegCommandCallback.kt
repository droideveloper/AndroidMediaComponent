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

import com.github.hiteshsondhi88.libffmpeg.FFmpegExecuteResponseHandler

class FFmpegCommandCallback(
    private val start: (() -> Unit)? = null,
    private val finish: (() -> Unit)? = null,
    private val success: ((String?) -> Unit)? = null,
    private val error: ((String?) -> Unit)? = null,
    private val progress: ((String?) -> Unit)? = null): FFmpegExecuteResponseHandler {

  override fun onFailure(message: String?) = error?.invoke(message) ?: Unit

  override fun onFinish() = finish?.invoke() ?: Unit

  override fun onProgress(message: String?) = progress?.invoke(message) ?: Unit

  override fun onStart() = start?.invoke() ?: Unit

  override fun onSuccess(message: String?) = success?.invoke(message) ?: Unit
}