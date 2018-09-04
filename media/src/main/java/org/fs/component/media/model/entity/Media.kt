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
package org.fs.component.media.model.entity

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import org.fs.component.media.common.annotation.MediaType
import org.fs.rx.extensions.util.EMPTY
import java.io.File

@Parcelize
data class Media(
    @MediaType val type: Int,
    val file: File,
    val taken: Long,
    val displayName: String,
    val mime: String) : Parcelable
{
 companion object {
   val EMPTY = Media(Int.MAX_VALUE, File(String.EMPTY), Long.MAX_VALUE, String.EMPTY, String.EMPTY)
 }
}