/*
 *  Copyright (c) 2012-2015 Malhar, Inc.
 *  All Rights Reserved.
 */

package com.datatorrent.lib.appdata.dimensions;

import com.datatorrent.api.Context.OperatorContext;
import com.datatorrent.api.DefaultInputPort;
import com.datatorrent.api.DefaultOutputPort;
import com.datatorrent.api.Operator;
import com.datatorrent.api.annotation.OperatorAnnotation;
import com.datatorrent.lib.appdata.dimensions.GenericAggregateEvent.EventKey;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

/**
 *
 * @author Timothy Farkas: tim@datatorrent.com
 */
@OperatorAnnotation(checkpointableWithinAppWindow=false)
public abstract class GenericDimensionsComputation<INPUT_EVENT> implements Operator
{
  public static long DEFAULT_CACHE_SIZE = 50000;

  private long cacheSize = DEFAULT_CACHE_SIZE;

  private transient Cache<EventKey, GenericAggregateEvent> cache =
  CacheBuilder.newBuilder().maximumSize(cacheSize).removalListener(new CacheRemovalListener()).build();

  public transient final DefaultInputPort<INPUT_EVENT> inputEvent = new DefaultInputPort<INPUT_EVENT>() {
    @Override
    public void process(INPUT_EVENT tuple)
    {
      processInputEvent(tuple);
    }
  };

  public final transient DefaultOutputPort<GenericAggregateEvent> aggregateOutput = new DefaultOutputPort<GenericAggregateEvent>();

  public GenericDimensionsComputation()
  {
  }

  /**
   * @return the cacheSize
   */
  public long getCacheSize()
  {
    return cacheSize;
  }

  /**
   * @param cacheSize the cacheSize to set
   */
  public void setCacheSize(long cacheSize)
  {
    this.cacheSize = cacheSize;
  }

  @Override
  public void setup(OperatorContext context)
  {
  }

  @Override
  public void beginWindow(long windowId)
  {
  }

  @Override
  public void endWindow()
  {
    cache.invalidateAll();
  }

  @Override
  public void teardown()
  {
  }

  public abstract GenericAggregateEvent[] convertInputEvent(INPUT_EVENT inputEvent);
  public abstract DimensionsAggregator<GenericAggregateEvent> getAggregator(int aggregatorID);

  public void processInputEvent(INPUT_EVENT inputEvent)
  {
    GenericAggregateEvent[] gaes = convertInputEvent(inputEvent);

    for(GenericAggregateEvent gae: gaes) {
      processGenericEvent(gae);
    }
  }

  public void processGenericEvent(GenericAggregateEvent gae)
  {
    GenericAggregateEvent aggregate = cache.getIfPresent(gae.getEventKey());

    if(aggregate == null) {
      cache.put(gae.getEventKey(), gae);
      return;
    }

    DimensionsAggregator<GenericAggregateEvent> aggregator = getAggregator(gae.getAggregatorIndex());
    aggregator.aggregate(aggregate, gae);
  }

  public class CacheRemovalListener implements RemovalListener<EventKey, GenericAggregateEvent>
  {
    public CacheRemovalListener()
    {
    }

    @Override
    public void onRemoval(RemovalNotification<EventKey, GenericAggregateEvent> notification)
    {
      aggregateOutput.emit(notification.getValue());
    }
  }
}
