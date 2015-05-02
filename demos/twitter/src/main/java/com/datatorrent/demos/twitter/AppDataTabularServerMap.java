/*
 * Copyright (c) 2015 DataTorrent, Inc. ALL Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.datatorrent.demos.twitter;

import com.datatorrent.lib.appdata.tabular.TabularMapConverter;

/**
 * @displayName App Data Tabular Map Server
 * @category App Data
 * @tags appdata, tabular, map
 */
public class AppDataTabularServerMap extends AppDataTabularServerConv<TabularMapConverter>
{
  public AppDataTabularServerMap()
  {
    this.converter = new TabularMapConverter();
  }
}
