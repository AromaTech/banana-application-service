/*
 * Copyright 2015 Aroma Tech.
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

 
package tech.aroma.application.service;


import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import decorice.DecoratorModule;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.aroma.thrift.application.service.ApplicationService;
import tech.sirwellington.alchemy.http.AlchemyHttp;

/**
 *
 * @author SirWellington
 */
public final class ModuleApplicationService extends AbstractModule
{
    private final static Logger LOG = LoggerFactory.getLogger(ModuleApplicationService.class);

    @Override
    protected void configure()
    {
        install(new ServiceModule());
        
        bind(ExecutorService.class).toInstance(Executors.newWorkStealingPool(8));
    }
    
    @Singleton
    @Provides
    AlchemyHttp provideHttpClient(ExecutorService executor)
    {
        return AlchemyHttp.newBuilder()
            .enableAsyncCallbacks()
            .usingExecutorService(executor)
            .usingTimeout(45, TimeUnit.SECONDS)
            .build();
    }
    
    private static class ServiceModule extends DecoratorModule
    {
        {
            bind(ApplicationService.Iface.class)
                .to(ApplicationServiceBase.class)
                .decoratedBy(AuthenticationLayer.class);
        }
    }
    
}
