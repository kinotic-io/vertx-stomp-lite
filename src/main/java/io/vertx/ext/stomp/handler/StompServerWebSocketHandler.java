/*
 *  Copyright (c) 2011-2015 The original author or authors
 *  ------------------------------------------------------
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  and Apache License v2.0 which accompanies this distribution.
 *
 *       The Eclipse Public License is available at
 *       http://www.eclipse.org/legal/epl-v10.html
 *
 *       The Apache License v2.0 is available at
 *       http://www.opensource.org/licenses/apache2.0.php
 *
 *  You may elect to redistribute this code under either of these licenses.
 */

package io.vertx.ext.stomp.handler;

import io.vertx.ext.stomp.frame.FrameParser;
import io.vertx.ext.stomp.frame.Frames;
import io.vertx.ext.stomp.StompServerHandlerFactory;
import io.vertx.ext.stomp.StompServerOptions;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.ServerWebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * Created by Navid Mitchell on 2019-02-04.
 */
public class StompServerWebSocketHandler implements Handler<ServerWebSocket> {

    private static final Logger log = LoggerFactory.getLogger(StompServerWebSocketHandler.class);

    private Vertx vertx;
    private StompServerOptions options;
    private StompServerHandlerFactory factory;

    public StompServerWebSocketHandler(Vertx vertx,
                                       StompServerOptions options,
                                       StompServerHandlerFactory factory) {
        this.vertx = vertx;
        this.options = options;
        this.factory = factory;
    }

    @Override
    public void handle(ServerWebSocket socket) {
        if (!socket.path().equals(options.getWebsocketPath())) {
            log.error("Receiving a web socket connection on an invalid path (" + socket.path() + "), the path is " +
                      "configured to " + options.getWebsocketPath() + ". Rejecting connection");

            socket.reject();

        }else{

            DefaultStompServerConnection defaultStompServerConnection = new DefaultStompServerConnection(socket,
                                                                                                         vertx,
                                                                                                         options,
                                                                                                         factory);

            socket.exceptionHandler((exception) -> {
                log.error("The STOMP server caught a WebSocket error - closing connection", exception);
                defaultStompServerConnection.close();
            });

            socket.endHandler(v -> defaultStompServerConnection.close());

            FrameParser parser = new FrameParser(options);
            parser.errorHandler((exception) -> {
                // TODO: telemetry - keep track of this
                defaultStompServerConnection.write(Frames.createInvalidFrameErrorFrame(exception, options.isDebugEnabled()));
                defaultStompServerConnection.close();

            }).handler(defaultStompServerConnection);

            socket.handler(parser);
        }
    }
}
