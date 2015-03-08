/*
 * Copyright (c) 2013 DataTorrent, Inc. ALL Rights Reserved.
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

import com.datatorrent.api.*;
import com.datatorrent.api.Context.OperatorContext;
import com.datatorrent.demos.twitter.schemas.TwitterDataValues;
import com.datatorrent.demos.twitter.schemas.TwitterOneTimeQuery;
import com.datatorrent.demos.twitter.schemas.TwitterOneTimeResult;
import com.datatorrent.demos.twitter.schemas.TwitterOneTimeResult.TwitterData;
import com.datatorrent.demos.twitter.schemas.TwitterSchemaResult;
import com.datatorrent.demos.twitter.schemas.TwitterUpdateQuery;
import com.datatorrent.demos.twitter.schemas.TwitterUpdateResult;
import com.datatorrent.lib.appdata.qr.Data;
import com.datatorrent.lib.appdata.qr.DataDeserializerFactory;
import com.datatorrent.lib.appdata.qr.DataSerializerFactory;
import com.datatorrent.lib.appdata.qr.Query;
import com.datatorrent.lib.appdata.qr.Result;
import com.datatorrent.lib.appdata.qr.processor.QueryComputer;
import com.datatorrent.lib.appdata.qr.processor.QueryProcessor;
import com.datatorrent.lib.appdata.qr.processor.WWEQueryQueueManager;
import com.datatorrent.lib.appdata.schemas.SchemaQuery;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.mutable.MutableLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.PriorityQueue;

/**
 *
 * WindowedTopCounter is an operator which counts the most often occurring tuples in a sliding window of a specific size.
 * The operator expects to receive a map object which contains a set of objects mapped to their respective frequency of
 * occurrences. e.g. if we are looking at most commonly occurring names then the operator expects to receive the tuples
 * of type Map<String, Intenger> on its input port, and at the end of the window it emits 1 object of type Map<String, Integer>
 * with a pre determined size. The emitted object contains the most frequently occurring keys.
 *
 * @param <T> Type of the key in the map object which is accepted on input port as payload. Note that this key must be HashMap friendly.
 * @since 0.3.2
 */
public class WindowedTopCounter<T> extends BaseOperator
{
  private static final Logger logger = LoggerFactory.getLogger(WindowedTopCounter.class);

  //==========================================================================
  // Query Processing - Start
  //==========================================================================

  private transient QueryProcessor<Query, Void, MutableLong, Void> queryProcessor;
  @SuppressWarnings("unchecked")
  private transient DataDeserializerFactory queryDeserializerFactory;
  private transient DataSerializerFactory resultSerializerFactory;
  private static final Long QUERY_QUEUE_WINDOW_COUNT = 30L;
  private static final int QUERY_QUEUE_WINDOW_COUNT_INT = (int) ((long) QUERY_QUEUE_WINDOW_COUNT);

  //==========================================================================
  // Query Processing - End
  //==========================================================================

  private PriorityQueue<SlidingContainer<T>> topCounter;
  private int windows;
  private int topCount = 10;
  private HashMap<T, SlidingContainer<T>> objects = new HashMap<T, SlidingContainer<T>>();

  public final transient DefaultOutputPort<String> resultOutput = new DefaultOutputPort<String>();

  /**
   * Input port on which map objects containing keys with their respective frequency as values will be accepted.
   */
  public final transient DefaultInputPort<Map<T, Integer>> input = new DefaultInputPort<Map<T, Integer>>()
  {
    @Override
    public void process(Map<T, Integer> map)
    {
      for (Map.Entry<T, Integer> e : map.entrySet()) {
        SlidingContainer<T> holder = objects.get(e.getKey());
        if (holder == null) {
          holder = new SlidingContainer<T>(e.getKey(), windows);
          objects.put(e.getKey(), holder);
        }
        holder.adjustCount(e.getValue());
      }
    }
  };

  public final transient DefaultInputPort<String> queryInput = new DefaultInputPort<String>() {
    @Override
    public void process(String s)
    {
      logger.info("Received: {}", s);

      Data query = queryDeserializerFactory.deserialize(s);

      //Query was not parseable
      if(query == null) {
        logger.info("Not parseable.");
        return;
      }

      if(query instanceof SchemaQuery) {
        String schemaResult = resultSerializerFactory.serialize(new TwitterSchemaResult((SchemaQuery) query));
        resultOutput.emit(schemaResult);
      }
      else if(query instanceof TwitterUpdateQuery) {
        queryProcessor.enqueue((TwitterOneTimeQuery) query, null, new MutableLong((long) QUERY_QUEUE_WINDOW_COUNT));
      }
      else if(query instanceof TwitterOneTimeQuery) {
        queryProcessor.enqueue((TwitterOneTimeQuery) query, null, new MutableLong(1L));
      }
    }
  };

  /**
   * Set the width of the sliding window.
   *
   * Sliding window is typically much larger than the dag window. e.g. One may want to measure the most frequently
   * occurring keys over the period of 5 minutes. So if dagWindowWidth (which is by default 500ms) is set to 500ms,
   * the slidingWindowWidth would be (60 * 5 * 1000 =) 300000.
   *
   * @param slidingWindowWidth - Sliding window width to be set for this operator, recommended to be multiple of DAG window.
   * @param dagWindowWidth - DAG's native window width. It has to be the value of the native window set at the application level.
   */
  public void setSlidingWindowWidth(long slidingWindowWidth, int dagWindowWidth)
  {
    windows = (int)(slidingWindowWidth / dagWindowWidth) + 1;
    if (slidingWindowWidth % dagWindowWidth != 0) {
      logger.warn("slidingWindowWidth(" + slidingWindowWidth + ") is not exact multiple of dagWindowWidth(" + dagWindowWidth + ")");
    }
  }

  @Override
  public void setup(OperatorContext context)
  {
    topCounter = new PriorityQueue<SlidingContainer<T>>(this.topCount, new TopSpotComparator());


    //Setup for query processing
    queryProcessor = new QueryProcessor<Query, Void, MutableLong, Void>(
                     new WindowTopCounterComputer(),
                     new WWEQueryQueueManager<Query, Void>());
    queryDeserializerFactory = new DataDeserializerFactory(SchemaQuery.class,
                                                            TwitterUpdateQuery.class,
                                                            TwitterOneTimeQuery.class);
    resultSerializerFactory = new DataSerializerFactory();

    queryProcessor.setup(context);
  }

  @Override
  public void beginWindow(long windowId)
  {
    topCounter.clear();
    queryProcessor.beginWindow(windowId);
  }

  @Override
  public void endWindow()
  {
    Iterator<Map.Entry<T, SlidingContainer<T>>> iterator = objects.entrySet().iterator();
    int i = topCount;

    /*
     * Try to fill the priority queue with the first topCount URLs.
     */
    SlidingContainer<T> holder;
    while (iterator.hasNext()) {
      holder = iterator.next().getValue();
      holder.slide();

      if (holder.totalCount == 0) {
        iterator.remove();
      }
      else {
        topCounter.add(holder);
        if (--i == 0) {
          break;
        }
      }
    }
    logger.debug("objects.size(): {}", objects.size());

    /*
     * Make room for the new element in the priority queue by deleting the
     * smallest one, if we KNOW that the new element is useful to us.
     */
    if (i == 0) {
      int smallest = topCounter.peek().totalCount;
      while (iterator.hasNext()) {
        holder = iterator.next().getValue();
        holder.slide();

        if (holder.totalCount > smallest) {
          topCounter.poll();
          topCounter.add(holder);
          smallest = topCounter.peek().totalCount;
        }
        else if (holder.totalCount == 0) {
          iterator.remove();
        }
      }
    }

    {
      Result result = null;

      while((result = queryProcessor.process(null)) != null) {
        resultOutput.emit(resultSerializerFactory.serialize(result));
      }
    }

    queryProcessor.endWindow();
    topCounter.clear();
  }

  @Override
  public void teardown()
  {
    topCounter = null;
    objects = null;
    queryProcessor.teardown();
  }

  /**
   * Set the count of most frequently occurring keys to emit per map object.
   *
   * @param count count of the objects in the map emitted at the output port.
   */
  public void setTopCount(int count)
  {
    topCount = count;
  }

  class WindowTopCounterComputer implements QueryComputer<Query, Void, MutableLong, Void>
  {
    @Override
    public Result processQuery(Query query, Void metaQuery, MutableLong queueContext, Void context)
    {
      TwitterOneTimeQuery totq = (TwitterOneTimeQuery) query;

      List<String> fields = totq.getFields();
      Set<String> fieldSet = null;

      if(fields == null) {
        fieldSet = Sets.newHashSet();
      }
      else {
        fieldSet = Sets.newHashSet(fields);
      }

      List<TwitterDataValues> tdvss = Lists.newArrayList();
      Iterator<SlidingContainer<T>> topIter = topCounter.iterator();

      while(topIter.hasNext()) {
        final SlidingContainer<T> wh = topIter.next();
        TwitterDataValues tdvs = new TwitterDataValues();

        if(fieldSet.isEmpty() || fieldSet.contains(TwitterSchemaResult.URL)) {
          tdvs.setUrl(wh.identifier.toString());
        }

        if(fieldSet.isEmpty() || fieldSet.contains(TwitterSchemaResult.COUNT)) {
          tdvs.setCount(wh.totalCount);
        }

        tdvss.add(tdvs);
      }

      TwitterData td = new TwitterData();
      Result result = null;

      if(query instanceof TwitterOneTimeQuery) {
        TwitterOneTimeResult totr = new TwitterOneTimeResult(query);
        td.setValues(tdvss);
        totr.setData(td);

        result = (Result) totr;
      }
      else if(query instanceof TwitterUpdateQuery) {
        TwitterUpdateResult tur = new TwitterUpdateResult(query);
        td.setValues(tdvss);
        tur.setData(td);
        tur.setCountdown(queueContext.longValue());

        result = (Result) tur;
      }

      return result;
    }

    @Override
    public void queueDepleted(Void context)
    {
    }
  }

  static class TopSpotComparator implements Comparator<SlidingContainer<?>>
  {
    @Override
    public int compare(SlidingContainer<?> o1, SlidingContainer<?> o2)
    {
      if (o1.totalCount > o2.totalCount) {
        return 1;
      }
      else if (o1.totalCount < o2.totalCount) {
        return -1;
      }

      return 0;
    }
  }
}
