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
package org.fs.component.media.common;

import android.util.Size;
import java.util.Comparator;

public class CompareSizesByHeight implements Comparator<Size> {

  public final static CompareSizesByHeight BY_HEIGHT_COMPARATOR = new CompareSizesByHeight();

  private CompareSizesByHeight() { }

  @Override public int compare(Size lhs, Size rhs) {
    return Integer.compare(lhs.getHeight(), rhs.getHeight());
  }
}