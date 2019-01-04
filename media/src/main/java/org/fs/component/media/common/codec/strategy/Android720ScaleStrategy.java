package org.fs.component.media.common.codec.strategy;

import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import net.ypresto.androidtranscoder.format.MediaFormatStrategy;

/*
 * Android Media Kotlin Copyright (C) 2018 Fatih.
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
public class Android720ScaleStrategy implements MediaFormatStrategy {

  private final int width;
  private final int height;

  public Android720ScaleStrategy(int width, int height) {
    this.width = width;
    this.height = height;
  }

  @Override public MediaFormat createAudioOutputFormat(MediaFormat inputFormat) {
    return null;
  }

  @Override public MediaFormat createVideoOutputFormat(MediaFormat inputFormat) {
    MediaFormat format = MediaFormat.createVideoFormat("video/avc", width, height);
    format.setInteger(MediaFormat.KEY_BIT_RATE, 5500 * 1000);
    format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
    format.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
    format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
    return format;
  }
}
