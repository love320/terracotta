/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.net.protocol.delivery;

import com.tc.async.api.Sink;

/**
 * Creates new instances of OnceAndOnlyOnceProtocolNetworkLayers.  This is used so that a mock one may be injected
 * into the once and only once network stack harness for testing.
 */
public class OnceAndOnlyOnceProtocolNetworkLayerFactoryImpl implements OnceAndOnlyOnceProtocolNetworkLayerFactory {

  public OnceAndOnlyOnceProtocolNetworkLayer createNewInstance(Sink workSink) {
    
    OOOProtocolMessageHeader.ProtocolMessageHeaderFactory headerFactory = new OOOProtocolMessageHeader.ProtocolMessageHeaderFactory();
    OOOProtocolMessageFactory messageFactory = new OOOProtocolMessageImpl.ProtocolMessageFactoryImpl(headerFactory);
    OOOProtocolMessageParser messageParser = new OOOProtocolMessageImpl.ProtocolMessageParserImpl(headerFactory,
                                                                                            messageFactory);
    return new OnceAndOnlyOnceProtocolNetworkLayerImpl(messageFactory, messageParser, workSink);
  }

}
