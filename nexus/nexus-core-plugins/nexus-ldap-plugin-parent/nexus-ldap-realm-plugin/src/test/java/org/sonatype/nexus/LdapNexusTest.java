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
package org.sonatype.nexus;

import org.junit.Assert;
import org.junit.Test;

import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.sonatype.nexus.security.ldap.realms.NexusLdapAuthenticationRealm;
import org.sonatype.security.SecuritySystem;
import org.sonatype.security.authentication.AuthenticationException;

import org.sonatype.security.ldap.realms.AbstractLdapAuthenticatingRealm;

public class LdapNexusTest
    extends AbstractNexusTestCase
{

    @Test
    public void testAuthentication()
        throws Exception
    {
        SecuritySystem security = lookup( SecuritySystem.class );
        security.start();

        Assert.assertNotNull( security.authenticate( new UsernamePasswordToken( "cstamas", "cstamas123" ) ) );
    }

    @Test
    public void testAuthenticationFailure()
        throws Exception
    {
        SecuritySystem security = lookup( SecuritySystem.class );
        security.start();

        try
        {
            Assert.assertNull( security.authenticate( new UsernamePasswordToken( "cstamas", "INVALID" ) ) );
        }
        catch ( AuthenticationException e )
        {
            // expected
        }
    }

    @Test
    public void testAuthorization()
        throws Exception
    {
        SecuritySystem security = lookup( SecuritySystem.class );
        security.start();

        SimplePrincipalCollection principals = new SimplePrincipalCollection();
        principals.add( "cstamas", new NexusLdapAuthenticationRealm().getName() );

        Assert.assertTrue( security.hasRole( principals, "developer" ) );
        Assert.assertFalse( security.hasRole( principals, "JUNK" ) );
    }

    @Test
    public void testAuthorizationPriv()
        throws Exception
    {
        SecuritySystem security = lookup( SecuritySystem.class );
        security.start();

        SimplePrincipalCollection principals = new SimplePrincipalCollection();
        principals.add( "cstamas", new NexusLdapAuthenticationRealm().getName() );

        Assert.assertTrue( security.isPermitted( principals, "security:usersforgotpw:create" ) );
        Assert.assertFalse( security.isPermitted( principals, "security:usersforgotpw:delete" ) );
    }
}
