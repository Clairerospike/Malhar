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
package com.datatorrent.contrib.kafka;

import com.datatorrent.api.AppDataOperator;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import org.apache.http.client.utils.URIBuilder;

public class KafkaAppDataQuery extends KafkaSinglePortStringInputOperator implements AppDataOperator
{
  @Override
  public URI getAppDataURL()
  {
    String mainBroker = this.consumer.brokerSet.iterator().next();
    StringBuilder sb = new StringBuilder();

    Iterator<String> bi = this.consumer.brokerSet.iterator();

    while(bi.hasNext()) {
      sb.append(bi.next());

      if(!bi.hasNext()) {
        break;
      }

      sb.append(",");
    }

    URIBuilder ub = new URIBuilder();
    ub.setScheme("kafka");
    ub.setHost(mainBroker);
    ub.addParameter("brokerSet", sb.toString());
    ub.addParameter("topic", this.consumer.getTopic());

    URI uri = null;

    try {
      uri = ub.build();
    }
    catch(URISyntaxException ex) {
      throw new RuntimeException(ex);
    }

    return uri;
  }
}
