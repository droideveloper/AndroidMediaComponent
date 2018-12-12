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
package org.fs.component.media.util

sealed class C {
  companion object {
    const val STATE_PREVIEW = 0x00
    const val STATE_WAITING_LOCK = 0x01
    const val STATE_WAITING_PRE_CAPTURE = 0x02
    const val STATE_WAITING_NON_PRE_CAPTURE = 0x03
    const val STATE_PICTURE_TAKEN = 0x04

    const val FLASH_MODE_AUTO = 0x00
    const val FLASH_MODE_DISABLED = 0x01

    const val MEDIA_TYPE_IMAGE = 0x01
    const val MEDIA_TYPE_VIDEO = 0x02
    const val MEDIA_TYPE_ALL = 0x03

    const val COMPONENT_ALL = 0x01
    const val COMPONENT_PHOTO = 0x02
    const val COMPONENT_VIDEO = 0x03

    const val RENDER_FILL = 0x01
    const val RENDER_FIX  = 0x02

    const val UI_THREAD_DELAY = 250L
  }
}