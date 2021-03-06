/**
 * Copyright (c) 2008-2011 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://www.sonatype.com/products/nexus/attributions.
 *
 * This program is free software: you can redistribute it and/or modify it only under the terms of the GNU Affero General
 * Public License Version 3 as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License Version 3
 * for more details.
 *
 * You should have received a copy of the GNU Affero General Public License Version 3 along with this program.  If not, see
 * http://www.gnu.org/licenses.
 *
 * Sonatype Nexus (TM) Open Source Version is available from Sonatype, Inc. Sonatype and Sonatype Nexus are trademarks of
 * Sonatype, Inc. Apache Maven is a trademark of the Apache Foundation. M2Eclipse is a trademark of the Eclipse Foundation.
 * All other trademarks are the property of their respective owners.
 */
package org.sonatype.security.realms;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.realm.Realm;
import org.junit.Test;
import org.sonatype.nexus.test.PlexusTestCaseSupport;

public class MemoryAuthenticationOnlyRealmTest
    extends PlexusTestCaseSupport
{
    private MemoryAuthenticationOnlyRealm realm;

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        realm = ( MemoryAuthenticationOnlyRealm ) lookup( Realm.class, "MemoryAuthenticationOnlyRealm" );
    }

    @Test
    public void testSuccessfulAuthentication()
        throws Exception
    {
        UsernamePasswordToken upToken = new UsernamePasswordToken( "admin", "admin321" );

        AuthenticationInfo ai = realm.getAuthenticationInfo( upToken );

        String password = ( String ) ai.getCredentials();

        assertEquals( "admin321", password );
    }

    @Test
    public void testFailedAuthentication()
        throws Exception
    {
        UsernamePasswordToken upToken = new UsernamePasswordToken( "admin", "admin123" );

        try
        {
            realm.getAuthenticationInfo( upToken );

            fail( "Authentication should have failed" );
        }
        catch( AuthenticationException e )
        {
            // good
        }
    }
}
