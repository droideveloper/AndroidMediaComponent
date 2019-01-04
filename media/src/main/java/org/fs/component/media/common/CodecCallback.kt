/*
 * Android Media Kotlin Copyright (C) 2018 Fatih, Playz.
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

import net.ypresto.androidtranscoder.MediaTranscoder
import java.lang.Exception

class CodecCallback(
    private val completed: (() -> Unit)? = null,
    private val progress: ((Double) -> Unit)? = null,
    private val failed: ((Throwable?) -> Unit)? = null,
    private val canceled: (() -> Unit)? = null): MediaTranscoder.Listener {

  override fun onTranscodeCanceled() = canceled?.invoke() ?: Unit

  override fun onTranscodeProgress(p: Double) = progress?.invoke(p) ?: Unit

  override fun onTranscodeFailed(exception: Exception?) = failed?.invoke(exception) ?: Unit

  override fun onTranscodeCompleted() = completed?.invoke() ?: Unit
}