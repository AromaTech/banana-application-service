/*
 * Copyright 2016 RedRoma.
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

import java.util.List;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sir.wellington.alchemy.collections.lists.Lists;
import tech.aroma.data.FollowerRepository;
import tech.aroma.thrift.Message;
import tech.aroma.thrift.User;
import tech.sirwellington.alchemy.annotations.access.Internal;
import tech.sirwellington.alchemy.annotations.designs.patterns.StrategyPattern;

import static java.util.stream.Collectors.toList;
import static tech.sirwellington.alchemy.annotations.designs.patterns.StrategyPattern.Role.CONCRETE_BEHAVIOR;
import static tech.sirwellington.alchemy.arguments.Arguments.checkThat;
import static tech.sirwellington.alchemy.arguments.assertions.Assertions.notNull;

/**
 *
 * @author SirWellington
 */
@Internal
@StrategyPattern(role = CONCRETE_BEHAVIOR)
final class RunThroughFollowerInboxesActions implements Action
{

    private final static Logger LOG = LoggerFactory.getLogger(RunThroughFollowerInboxesActions.class);

    private final ActionFactory factory;
    private final FollowerRepository followerRepo;

    RunThroughFollowerInboxesActions(ActionFactory factory, FollowerRepository followerRepo)
    {
        checkThat(factory, followerRepo)
            .are(notNull());

        this.factory = factory;
        this.followerRepo = followerRepo;
    }

    @Override
    public List<Action> actOnMessage(Message message) throws TException
    {
        Action.checkMessage(message);

        String appId = message.applicationId;

        List<User> followers = followerRepo.getApplicationFollowers(appId);
        followers = Lists.nullToEmpty(followers);

        LOG.debug("Creating {} additional actions to run through Follower Inboxes for App {}", followers.size(), appId);

        return followers.stream()
            .map(user -> factory.actionToRunThroughInbox(user))
            .collect(toList());
    }

    @Override
    public String toString()
    {
        return "RunThroughFollowerInboxesActions{" + "factory=" + factory + ", followerRepo=" + followerRepo + '}';
    }

}
