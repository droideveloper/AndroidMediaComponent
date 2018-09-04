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

import java.util.Comparator;
import org.fs.component.media.model.entity.Media;

public class CompareMediaByDateTaken implements Comparator<Media> {

  public final static CompareMediaByDateTaken COMPARE_MEDIA_BY_DATE_TAKEN = new CompareMediaByDateTaken();

  @Override public int compare(Media lhs, Media rhs) {
    return Long.compare(lhs.getTaken(), rhs.getTaken());
  }
}