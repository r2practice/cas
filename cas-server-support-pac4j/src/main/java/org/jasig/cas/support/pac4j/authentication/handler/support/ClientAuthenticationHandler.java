/*
 * Licensed to Jasig under one or more contributor license
 * agreements. See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.
 * Jasig licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License.  You may obtain a
 * copy of the License at the following location:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jasig.cas.support.pac4j.authentication.handler.support;

import javax.validation.constraints.NotNull;

import org.apache.commons.lang.StringUtils;
import org.jasig.cas.authentication.handler.AuthenticationException;
import org.jasig.cas.authentication.handler.support.AbstractPreAndPostProcessingAuthenticationHandler;
import org.jasig.cas.authentication.principal.Credentials;
import org.jasig.cas.support.pac4j.authentication.principal.ClientCredentials;
import org.pac4j.core.client.Client;
import org.pac4j.core.client.Clients;
import org.pac4j.core.profile.UserProfile;

/**
 * This handler authenticates the client credentials : it uses them to get the user profile returned by the provider
 * for an authenticated user.
 *
 * @author Jerome Leleu
 * @since 3.5.0
 */
@SuppressWarnings("unchecked")
public final class ClientAuthenticationHandler extends AbstractPreAndPostProcessingAuthenticationHandler {

    /**
     * The clients for authentication.
     */
    @NotNull
    private final Clients clients;

    /**
     * Define the clients.
     *
     * @param theClients The clients for authentication
     */
    public ClientAuthenticationHandler(final Clients theClients) {
        this.clients = theClients;
    }

    @Override
    public boolean supports(final Credentials credentials) {
        return credentials != null && ClientCredentials.class.isAssignableFrom(credentials.getClass());
    }

    @Override
    protected boolean doAuthentication(final Credentials credentials) throws AuthenticationException {
        final ClientCredentials clientCredentials = (ClientCredentials) credentials;
        log.debug("clientCredentials : {}", clientCredentials);

        final String clientName = clientCredentials.getCredentials().getClientName();
        log.debug("clientName : {}", clientName);

        // get client
        final Client<org.pac4j.core.credentials.Credentials, UserProfile> client = this.clients.findClient(clientName);
        log.debug("client : {}", client);

        // get user profile
        final UserProfile userProfile = client.getUserProfile(clientCredentials.getCredentials());
        log.debug("userProfile : {}", userProfile);

        if (userProfile != null && StringUtils.isNotBlank(userProfile.getId())) {
            clientCredentials.setUserProfile(userProfile);
            return true;
        }

        return false;
    }
}
