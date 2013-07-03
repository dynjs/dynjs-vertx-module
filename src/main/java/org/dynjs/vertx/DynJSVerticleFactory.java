/*
 * Copyright 2011-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dynjs.vertx;

import org.dynjs.Config;
import org.dynjs.runtime.*;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Container;
import org.vertx.java.platform.Verticle;
import org.vertx.java.platform.VerticleFactory;

/**
 * @author Lance Ball lball@redhat.com
 */
public class DynJSVerticleFactory implements VerticleFactory {

    public static Container container;
    public static Vertx vertx;

    private DynJS runtime;
    private Config config;
    private ClassLoader mcl;
    private GlobalObjectFactory globalObjectFactory = new DynJSGlobalObjectFactory();
    
    @Override
    public void init(Vertx vertx, Container container, ClassLoader classloader) {
        this.mcl = classloader;
        DynJSVerticleFactory.container = container;
        DynJSVerticleFactory.vertx = vertx;
        
        this.config = new Config(getClassLoader());
        this.config.setGlobalObjectFactory(getGlobalObjectFactory());
        this.runtime = new DynJS(this.config);
    }

    @Override
    public Verticle createVerticle(String main) throws Exception {
        return new DynJSVerticle(this, main);
    }

    @Override
    public void reportException(Logger logger, Throwable t) {
        logger.error("Exception in DynJS JavaScript verticle", t);
    }

    @Override
    public void close() {
    }
    
    public DynJS getRuntime() {
        return this.runtime;
    }
    
    public Config getConfig() {
        return this.config;
    }
    
    protected GlobalObjectFactory getGlobalObjectFactory() {
        return globalObjectFactory;
    }

    protected ClassLoader getClassLoader() {
        return this.mcl;
    }

    protected class DynJSGlobalObjectFactory implements GlobalObjectFactory {

        @Override
        public GlobalObject newGlobalObject(final DynJS runtime) {
            final GlobalObject globalObject = new GlobalObject(runtime);
            final VertxLoad loader = new VertxLoad(globalObject);
            globalObject.defineGlobalProperty("__dirname", System.getProperty("user.dir"));
            globalObject.defineReadOnlyGlobalProperty("stdout", System.out);
            globalObject.defineReadOnlyGlobalProperty("stderr", System.err);
            globalObject.defineGlobalProperty("global", globalObject);
            globalObject.defineGlobalProperty("runtime", runtime);
            globalObject.defineGlobalProperty("load", new AbstractNativeFunction(globalObject) {
                @Override
                public Object call(ExecutionContext context, Object self, Object... args) {
                    return context.call(loader, context.getGlobalObject(), args[0]);
                }
            });
            globalObject.defineGlobalProperty("__jvertx", vertx);
            globalObject.defineGlobalProperty("__jcontainer", container);
            return globalObject;
        }
    }
}
