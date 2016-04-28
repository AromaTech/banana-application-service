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

package tech.aroma.application.service.reactions.matchers;

import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import sir.wellington.alchemy.collections.lists.Lists;
import sir.wellington.alchemy.collections.maps.Maps;
import tech.aroma.thrift.Message;
import tech.aroma.thrift.reactions.AromaMatcher;
import tech.sirwellington.alchemy.test.junit.runners.AlchemyTestRunner;
import tech.sirwellington.alchemy.test.junit.runners.GeneratePojo;
import tech.sirwellington.alchemy.test.junit.runners.Repeat;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 *
 * @author SirWellington
 */
@Repeat(10)
@RunWith(AlchemyTestRunner.class)
public class MatchAlgorithmOrTest 
{
    @Mock
    private MatcherFactory matcherFactory;
    
    private List<AromaMatcher> matchers;
    
    private Map<AromaMatcher, MessageMatcher> mapOfMatchers = Maps.create();
    
    @GeneratePojo
    private Message message;
    
    private MatchAlgorithmOr instance;
    
    @Before
    public void setUp() throws Exception
    {
        instance = new MatchAlgorithmOr(matcherFactory);
        verifyZeroInteractions(matcherFactory);
        
        setupData();
        setupMocks();
    }


    private void setupData() throws Exception
    {
        mapOfMatchers.clear();
    }

    private void setupMocks() throws Exception
    {
        mapOfMatchers = MatchBehavior.createMatchersThatSometimesMatchFor(message);
        setupMatchers();
    }
    
    private void setupMatchers()
    {
        matchers = Lists.copy(mapOfMatchers.keySet());
        mapOfMatchers.entrySet()
            .stream()
            .forEach(m -> when(matcherFactory.matcherFor(m.getKey())).thenReturn(m.getValue()));
    }

    @Test
    public void testWhenSomeMatch()
    {
        boolean result = instance.matches(message, matchers);
        assertThat(result, is(true));
    }
    
    @Test
    public void testWhenAlwaysMatch()
    {
        mapOfMatchers = MatchBehavior.createMatchersThatAlwaysMatchFor(message);
        setupMatchers();
        
        assertThat(instance.matches(message, matchers), is(true));
    }
    
    @Test
    public void testWhenNoneMatch()
    {
        mapOfMatchers = MatchBehavior.createMatchersThatNeverMatchFor(message);
        setupMatchers();
        
        assertThat(instance.matches(message, matchers), is(false));
    }

    @Test
    public void testToString()
    {
        assertThat(instance.toString(), not(isEmptyOrNullString()));
    }

}