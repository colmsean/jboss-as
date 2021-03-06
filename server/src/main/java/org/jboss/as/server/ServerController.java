/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.server;

import java.util.List;

import java.util.concurrent.TimeUnit;
import org.jboss.as.model.AbstractServerModelUpdate;
import org.jboss.as.model.ServerModel;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.as.model.UpdateResultHandlerResponse;

/**
 * The API entry point for a server controller, which manages the state of running server.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface ServerController {

    /**
     * Get the server model
     *
     * @return the server model
     * @deprecated this is an abstraction leak; in the detyped system, this should return a *copy* of the model only
     */
    @Deprecated
    ServerModel getServerModel();

    /**
     * Get this server's environment.
     *
     * @return the environment
     */
    ServerEnvironment getServerEnvironment();

    /**
     * Apply a series of updates.
     *
     * @param updates the updates
     * @param rollbackOnFailure <code>true</code> if successfully applied updates
     *                          should be rolled back if an update later in the list fails
     * @param modelOnly <code>true</code> if the updates should only be applied
     *                  to the ServerModel and should not be applied
     *                to the runtime, <code>false</code> if they should also
     *                be applied to the runtime. A {@code true} value is only
     *                legal on a standalone server
     *
     * @return the results of the updates
     *
     * @throws IllegalStateException if this is not a standalone server and
     *           {@code modelOnly} is {@code true}
     */
    // TODO: Change this to accept only one update at a time; multi-step updates should be a composite object of some sort
    List<UpdateResultHandlerResponse<?>> applyUpdates(List<AbstractServerModelUpdate<?>> updates,
            boolean rollbackOnFailure, boolean modelOnly);

    /**
     * Apply a persistent update.
     *
     * @param update the update to apply
     * @param resultHandler the result handler
     * @param param the result handler parameter
     * @param <R> the result type
     * @param <P> the result handler parameter type
     */
    <R, P> void update(AbstractServerModelUpdate<R> update, UpdateResultHandler<R, P> resultHandler, P param);

    /**
     * Shut down the server.
     */
    void shutdown();

    /**
     * Determine whether shutdown is complete.
     *
     * @return {@code true} if shutdown is complete
     */
    boolean isShutdownComplete();

    /**
     * Wait for termination to complete.
     *
     * @throws InterruptedException if the current thread was interrupted while waiting
     */
    void awaitTermination() throws InterruptedException;

    /**
     * Wait for a certain amount of time for termination to complete.
     *
     * @throws InterruptedException if the current thread was interrupted while waiting
     */
    void awaitTermination(long time, TimeUnit unit) throws InterruptedException;
}
