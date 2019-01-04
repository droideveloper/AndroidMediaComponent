package org.fs.component.media.common.codec.strategy;

import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
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
public class Android720CropStrategy implements MediaFormatStrategy {

  private final int width;
  private final int height;
  private final int x;
  private final int y;

  public Android720CropStrategy(int width, int height, int x, int y) {
    this.width = width;
    this.height = height;
    this.x = x;
    this.y = y;
  }

  @Override public MediaFormat createAudioOutputFormat(MediaFormat inputFormat) {
    return null;
  }

  @Override public MediaFormat createVideoOutputFormat(MediaFormat inputFormat) {
    MediaFormat format = MediaFormat.createVideoFormat("video/avc", width, height);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      format.setInteger(MediaFormat.KEY_STRIDE, x);
      format.setInteger(MediaFormat.KEY_SLICE_HEIGHT, y);
    }
    format.setInteger(MediaFormat.KEY_BIT_RATE, 5500 * 1000);
    format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
    format.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
    format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
    return format;
  }
}
