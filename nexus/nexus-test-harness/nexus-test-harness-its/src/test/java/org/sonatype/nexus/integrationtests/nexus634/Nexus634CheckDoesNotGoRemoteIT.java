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
package org.sonatype.nexus.integrationtests.nexus634;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.mortbay.jetty.Server;
import org.restlet.data.MediaType;
import org.sonatype.nexus.rest.model.RepositoryProxyResource;
import org.sonatype.nexus.rest.model.ScheduledServicePropertyResource;
import org.sonatype.nexus.tasks.descriptors.ExpireCacheTaskDescriptor;
import org.sonatype.nexus.test.utils.RepositoryMessageUtil;
import org.sonatype.nexus.test.utils.TaskScheduleUtil;
import org.sonatype.nexus.test.utils.TestProperties;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Tests SnapshotRemoverTask to not go remote when checking for release existence.
 * 
 * @author cstamas
 */
public class Nexus634CheckDoesNotGoRemoteIT
    extends AbstractSnapshotRemoverIT
{
    protected String localStorageDir = null;

    protected Integer proxyPort;

    protected Server server = null;

    protected TouchTrackingHandler touchTrackingHandler;

    protected RepositoryMessageUtil repositoryMessageUtil;

    public Nexus634CheckDoesNotGoRemoteIT()
        throws Exception
    {
        super();

    }

    @BeforeClass
    public void init()
        throws ComponentLookupException
    {
        this.localStorageDir = TestProperties.getString( "proxy.repo.base.dir" );
        this.proxyPort = TestProperties.getInteger( "proxy.server.port" );
        this.repositoryMessageUtil = new RepositoryMessageUtil( this, getXMLXStream(), MediaType.APPLICATION_XML );
    }

    @BeforeMethod
    public void deploySnapshotArtifacts()
        throws Exception
    {
        super.deploySnapshotArtifacts();

        File remoteSnapshot = getTestFile( "remote-repo" );

        // Copying to keep an old timestamp
        FileUtils.copyDirectory( remoteSnapshot, repositoryPath );

        // update indexes?
        // RepositoryMessageUtil.updateIndexes( "nexus-test-harness-snapshot-repo" );
    }

    @BeforeMethod
    public void startProxy()
        throws Exception
    {
        touchTrackingHandler = new TouchTrackingHandler();
        server = new Server( proxyPort );
        server.setHandler( touchTrackingHandler );
        server.start();
    }

    @AfterMethod
    public void stopProxy()
        throws Exception
    {
        if ( server != null )
        {
            server.stop();
            server = null;
            touchTrackingHandler = null;
        }
    }

    @Test
    public void keepNewSnapshots()
        throws Exception
    {
        // set proxy reposes to point here
        RepositoryProxyResource proxy =
            (RepositoryProxyResource) repositoryMessageUtil.getRepository( REPO_RELEASE_PROXY_REPO1 );
        proxy.getRemoteStorage().setRemoteStorageUrl( "http://localhost:" + proxyPort + "/" );
        repositoryMessageUtil.updateRepo( proxy );

        // expire caches
        ScheduledServicePropertyResource repoOrGroupProp = new ScheduledServicePropertyResource();
        repoOrGroupProp.setKey( "repositoryId" );
        repoOrGroupProp.setValue( REPO_RELEASE_PROXY_REPO1 );
        TaskScheduleUtil.runTask( ExpireCacheTaskDescriptor.ID, repoOrGroupProp );

        // run snapshot remover
        runSnapshotRemover( "nexus-test-harness-snapshot-repo", 0, 0, true );

        // check is proxy touched
        Assert.assertEquals( touchTrackingHandler.getTouchedTargets().size(), 0,
            "Proxy should not be touched! It was asked for " + touchTrackingHandler.getTouchedTargets() );
    }
}
