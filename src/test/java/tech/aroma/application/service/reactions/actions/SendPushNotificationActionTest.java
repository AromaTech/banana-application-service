/*
 * Copyright 2016 RedRoma, Inc.
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

package tech.aroma.application.service.reactions.actions;

import com.notnoop.apns.ApnsService;
import com.notnoop.exceptions.NetworkIOException;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import sir.wellington.alchemy.collections.sets.Sets;
import tech.aroma.data.UserPreferencesRepository;
import tech.aroma.thrift.Message;
import tech.aroma.thrift.channels.IOSDevice;
import tech.aroma.thrift.channels.MobileDevice;
import tech.aroma.thrift.exceptions.InvalidArgumentException;
import tech.sirwellington.alchemy.test.junit.runners.AlchemyTestRunner;
import tech.sirwellington.alchemy.test.junit.runners.DontRepeat;
import tech.sirwellington.alchemy.test.junit.runners.GenerateString;
import tech.sirwellington.alchemy.test.junit.runners.Repeat;

import static java.util.stream.Collectors.toSet;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static tech.aroma.thrift.generators.ChannelGenerators.mobileDevices;
import static tech.aroma.thrift.generators.MessageGenerators.messages;
import static tech.sirwellington.alchemy.generator.AlchemyGenerator.one;
import static tech.sirwellington.alchemy.generator.CollectionGenerators.listOf;
import static tech.sirwellington.alchemy.test.junit.ThrowableAssertion.assertThrows;
import static tech.sirwellington.alchemy.test.junit.runners.GenerateString.Type.UUID;


/**
 *
 * @author SirWellington
 */
@Repeat(100)
@RunWith(AlchemyTestRunner.class)
public class SendPushNotificationActionTest 
{

    @Mock
    private ApnsService apns;

    @Mock
    private UserPreferencesRepository userPreferencesRepo;

    @GenerateString(UUID)
    private String userId;

    private SendPushNotificationAction instance;

    private Set<MobileDevice> devices;
    private Set<IOSDevice> iosDevices;

    private Message message;

    @Captor
    private ArgumentCaptor<String> payloadCaptor;

    @Captor
    private ArgumentCaptor<String> deviceTokenCaptor;

    @Before
    public void setUp() throws Exception
    {
        
        setupData();
        setupMocks();
        
        instance = new SendPushNotificationAction(apns, userPreferencesRepo, userId);
        verifyZeroInteractions(apns, userPreferencesRepo);
    }


    private void setupData() throws Exception
    {
        List<MobileDevice> listOfDevices = listOf(mobileDevices(), 20);
        devices = Sets.copyOf(listOfDevices);
        
        iosDevices = devices.stream()
            .filter(MobileDevice::isSetIosDevice)
            .map(MobileDevice::getIosDevice)
            .filter(Objects::nonNull)
            .collect(toSet());
        
        message = one(messages());
    }

    private void setupMocks() throws Exception
    {
        when(userPreferencesRepo.getMobileDevices(userId))
            .thenReturn(devices);
    }

    @DontRepeat
    @Test
    public void testConstructor() throws Exception
    {
        assertThrows(() -> new SendPushNotificationAction(null, userPreferencesRepo, userId));
        assertThrows(() -> new SendPushNotificationAction(apns, null, userId));
        assertThrows(() -> new SendPushNotificationAction(apns, userPreferencesRepo, ""));
    }
    
    @Test
    public void testActOnMessage() throws Exception
    {
        instance.actOnMessage(message);
        
        for (IOSDevice device : iosDevices)
        {
            verify(apns).push(eq(device.deviceToken), payloadCaptor.capture());
            
            String payload = payloadCaptor.getValue();
            assertThat(payload, not(isEmptyOrNullString()));
            assertThat(payload, containsString(message.title));
            assertThat(payload, containsString(message.applicationName));
        }
    }
    
    //Expecting one failure to not affect the remaining devices from being processed
    @Test
    public void testWhenOneDeviceFails() throws Exception
    {
        if (iosDevices.isEmpty())
        {
            return;
        }
        
        IOSDevice failingDevice = Sets.oneOf(iosDevices);
        
        when(apns.push(eq(failingDevice.deviceToken), anyString()))
            .thenThrow(new NetworkIOException());
        
        instance.actOnMessage(message);
        
        for (IOSDevice device : iosDevices)
        {
            if (device.equals(failingDevice))
            {
                continue;
            }
            
            verify(apns).push(eq(device.deviceToken), Mockito.contains(message.title));
        }
    }
    
    @DontRepeat
    @Test
    public void testWhenNoDevices() throws Exception
    {
        when(userPreferencesRepo.getMobileDevices(userId))
            .thenReturn(Sets.emptySet());
        
        instance.actOnMessage(message);
        
        verifyZeroInteractions(apns);
    }
    
    
    @Test
    public void testWithBadArgs() throws Exception
    {
        assertThrows(() -> instance.actOnMessage(null)).isInstanceOf(InvalidArgumentException.class);
        
        Message emptyMessage = new Message();
        assertThrows(() -> instance.actOnMessage(emptyMessage)).isInstanceOf(InvalidArgumentException.class);
        
        Message messageWithoutId = new Message();
        assertThrows(() -> instance.actOnMessage(messageWithoutId)).isInstanceOf(InvalidArgumentException.class);
        
    }
    
    

}