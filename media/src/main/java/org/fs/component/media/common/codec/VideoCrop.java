package org.fs.component.media.common.codec;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.media.MediaMuxer;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;

/*
 * Playz.lol Android Kotlin Copyright (C) 2018 Fatih, Playz.
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
public class VideoCrop {

  private final static int DEFAULT_BUFFER_SIZE = 8196;

  /**
   * @param srcPath  the path of source video file.
   * @param dstPath  the path of destination video file.
   * @param startMs  starting time in milliseconds for trimming. Set to
   *                 negative if starting from beginning.
   * @param endMs    end time for trimming in milliseconds. Set to negative if
   *                 no trimming at the end.
   * @param useAudio true if keep the audio track from the source.
   * @param useVideo true if keep the video track from the source.
   * @throws IOException
   */
  private static void genVideoUsingMuxer(String srcPath, String dstPath,
      int startMs, int endMs, boolean useAudio, boolean
      useVideo)
      throws IOException {
    // Set up MediaExtractor to read from the source.
    MediaExtractor extractor = new MediaExtractor();
    extractor.setDataSource(srcPath);
    int trackCount = extractor.getTrackCount();
    // Set up MediaMuxer for the destination.
    MediaMuxer muxer;
    muxer = new MediaMuxer(dstPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
    // Set up the tracks and retrieve the max buffer size for selected
    // tracks.
    HashMap<Integer, Integer> indexMap = new HashMap<>(trackCount);
    int bufferSize = -1;
    for (int i = 0; i < trackCount; i++) {
      MediaFormat format = extractor.getTrackFormat(i);
      String mime = format.getString(MediaFormat.KEY_MIME);
      boolean selectCurrentTrack = false;
      if (mime.startsWith("audio/") && useAudio) {
        selectCurrentTrack = true;
      } else if (mime.startsWith("video/") && useVideo) {
        selectCurrentTrack = true;
      }
      if (selectCurrentTrack) {
        extractor.selectTrack(i);
        int dstIndex = muxer.addTrack(format);
        indexMap.put(i, dstIndex);
        if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
          int newSize = format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
          bufferSize = newSize > bufferSize ? newSize : bufferSize;
        }
      }
    }
    if (bufferSize < 0) {
      bufferSize = DEFAULT_BUFFER_SIZE;
    }
    // Set up the orientation and starting time for extractor.
    MediaMetadataRetriever retrieverSrc = new MediaMetadataRetriever();
    retrieverSrc.setDataSource(srcPath);
    String degreesString = retrieverSrc.extractMetadata(
        MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
    if (degreesString != null) {
      int degrees = Integer.parseInt(degreesString);
      if (degrees >= 0) {
        muxer.setOrientationHint(degrees);
      }
    }
    if (startMs > 0) {
      extractor.seekTo(startMs * 1000, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
    }
    // Copy the samples from MediaExtractor to MediaMuxer. We will loop
    // for copying each sample and stop when we get to the end of the source
    // file or exceed the end time of the trimming.
    int offset = 0;
    int trackIndex = -1;
    ByteBuffer dstBuf = ByteBuffer.allocate(bufferSize);
    MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
    try {
      muxer.start();
      while (true) {
        bufferInfo.offset = offset;
        bufferInfo.size = extractor.readSampleData(dstBuf, offset);
        if (bufferInfo.size < 0) {
          bufferInfo.size = 0;
          break;
        } else {
          bufferInfo.presentationTimeUs = extractor.getSampleTime();
          if (endMs > 0 && bufferInfo.presentationTimeUs > (endMs * 1000)) {
            break;
          } else {
            bufferInfo.flags = extractor.getSampleFlags();
            trackIndex = extractor.getSampleTrackIndex();
            muxer.writeSampleData(indexMap.get(trackIndex), dstBuf,
                bufferInfo);
            extractor.advance();
          }
        }
      }
      muxer.stop();

      //deleting the old file
      File file = new File(srcPath);
      file.delete();
    } catch (IllegalStateException e) {
      // Swallow the exception due to malformed source.
    } finally {
      muxer.release();
    }
    return;
  }
}
