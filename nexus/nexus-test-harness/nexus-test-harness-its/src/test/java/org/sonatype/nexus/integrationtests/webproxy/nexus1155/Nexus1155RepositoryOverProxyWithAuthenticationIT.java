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
package org.sonatype.nexus.integrationtests.webproxy.nexus1155;

import org.sonatype.nexus.integrationtests.webproxy.nexus1146.Nexus1146RepositoryOverProxyIT;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

public class Nexus1155RepositoryOverProxyWithAuthenticationIT
    extends Nexus1146RepositoryOverProxyIT
{

    @Override
    @BeforeMethod
    public void startWebProxy()
        throws Exception
    {
        super.startWebProxy();
        server.getProxyServlet().setUseAuthentication( true );
        server.getProxyServlet().getAuthentications().put( "admin", "123" );
    }

    @Override
    @AfterMethod
    public void stopWebProxy()
        throws Exception
    {
        server.getProxyServlet().setUseAuthentication( false );
        server.getProxyServlet().setAuthentications( null );
        super.stopWebProxy();
    }

}
