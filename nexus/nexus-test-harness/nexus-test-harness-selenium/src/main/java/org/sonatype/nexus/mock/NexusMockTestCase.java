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
package org.sonatype.nexus.mock;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;

import org.apache.log4j.Logger;
import org.apache.maven.index.artifact.Gav;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.sonatype.appbooter.PlexusAppBooter;
import org.sonatype.guice.bean.containers.InjectedTest;
import org.sonatype.nexus.mock.rest.MockHelper;
import org.sonatype.nexus.mock.util.PropUtil;
import org.sonatype.nexus.test.utils.EventInspectorsUtil;
import org.sonatype.nexus.test.utils.FileTestingUtils;
import org.sonatype.nexus.test.utils.GavUtil;
import org.sonatype.nexus.test.utils.TestProperties;
import org.sonatype.nexus.test.utils.WagonDeployer;
import org.sonatype.nexus.testng.PlexusObjectFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;

public abstract class NexusMockTestCase
    implements Initializable
{
    private static MockNexusEnvironment env;

    public static String nexusBaseURL;

    protected String testId;

    protected String testName;

    protected static Logger log = Logger.getLogger( NexusMockTestCase.class );

    @Requirement
    private PlexusContainer container = PlexusObjectFactory.getContainer();

    @BeforeSuite
    public synchronized void startNexus()
        throws Exception
    {
        if ( env == null )
        {
            File webappRoot;
            String pathname = PropUtil.get( "webappRoot", null );
            File root = new File( TestProperties.getString( "nexus.base.dir" ) );
            if ( pathname != null )
            {
                webappRoot = new File( pathname ).getAbsoluteFile();
            }
            else
            {
                webappRoot = new File( "../nexus-webapp/src/main/webapp" );
                if ( !webappRoot.exists() )
                {
                    webappRoot = new File( root, "runtime/apps/nexus/webapp" );
                }
            }

            nexusBaseURL = TestProperties.getString( "nexus.base.url" );

            Context context = container.getContext();
            Assert.assertNotNull(context);
            
            env = new MockNexusEnvironment( (PlexusAppBooter) context.get( "plexus.app.booter" ) );
            // Don't do this env.start();

            Runtime.getRuntime().addShutdownHook( new Thread( new Runnable()
            {
                public void run()
                {
                    try
                    {
                        env.stop();
                    }
                    catch ( Exception e )
                    {
                        e.printStackTrace();
                    }
                }
            } ) );

        }
    }

    @BeforeClass
    public void prepareEnv()
        throws Exception
    {
        copyTestResources();
        deployArtifacts();
    }

    protected void copyTestResources()
        throws IOException
    {
        File source = new File( RESOURCES_SOURCE_DIR, testId );
        if ( !source.exists() )
        {
            return;
        }

        File destination = new File( RESOURCES_DIR, testId );

        FileTestingUtils.interpolationDirectoryCopy( source, destination, TestProperties.getAll() );
    }

    protected void deployArtifacts()
        throws Exception
    {
        // test the test directory
        File projectsDir = getTestResourceAsFile( "projects" );
        log.debug( "projectsDir: " + projectsDir );

        // if null there is nothing to deploy...
        if ( projectsDir != null )
        {

            // we have the parent dir, for each child (one level) we need to grab the pom.xml out of it and parse it,
            // and then deploy the artifact, sounds like fun, right!

            File[] projectFolders = projectsDir.listFiles( new FileFilter()
            {

                public boolean accept( File pathname )
                {
                    return ( !pathname.getName().endsWith( ".svn" ) && pathname.isDirectory() && new File( pathname,
                                                                                                           "pom.xml" ).exists() );
                }
            } );

            for ( int ii = 0; ii < projectFolders.length; ii++ )
            {
                File project = projectFolders[ii];

                // we already check if the pom.xml was in here.
                File pom = new File( project, "pom.xml" );

                MavenXpp3Reader reader = new MavenXpp3Reader();
                FileInputStream fis = new FileInputStream( pom );
                Model model = reader.read( new FileReader( pom ) );
                fis.close();

                // a helpful note so you don't need to dig into the code to much.
                if ( model.getDistributionManagement() == null
                    || model.getDistributionManagement().getRepository() == null )
                {
                    Assert.fail( "The test artifact is either missing or has an invalid Distribution Management section." );
                }
                String deployUrl = model.getDistributionManagement().getRepository().getUrl();

                // FIXME, this needs to be fluffed up a little, should add the classifier, etc.
                String artifactFileName = model.getArtifactId() + "." + model.getPackaging();
                File artifactFile = new File( project, artifactFileName );

                log.debug( "wow, this is working: " + artifactFile.getName() );

                Gav gav =
                    new Gav( model.getGroupId(), model.getArtifactId(), model.getVersion(), null, model.getPackaging(),
                             0, new Date().getTime(), model.getName(), false, null, false, null );

                // the Restlet Client does not support multipart forms:
                // http://restlet.tigris.org/issues/show_bug.cgi?id=71

                // int status = DeployUtils.deployUsingPomWithRest( deployUrl, repositoryId, gav, artifactFile, pom );

                if ( !artifactFile.isFile() )
                {
                    throw new FileNotFoundException( "File " + artifactFile.getAbsolutePath() + " doesn't exists!" );
                }

                File artifactSha1 = new File( artifactFile.getAbsolutePath() + ".sha1" );
                File artifactMd5 = new File( artifactFile.getAbsolutePath() + ".md5" );
                File artifactAsc = new File( artifactFile.getAbsolutePath() + ".asc" );

                File pomSha1 = new File( pom.getAbsolutePath() + ".sha1" );
                File pomMd5 = new File( pom.getAbsolutePath() + ".md5" );
                File pomAsc = new File( pom.getAbsolutePath() + ".asc" );

                try
                {
                    if ( artifactSha1.exists() )
                    {
                        deployWithWagon( container, "http", deployUrl, artifactSha1,
                                         GavUtil.getRelitiveArtifactPath( gav ) + ".sha1" );
                    }
                    if ( artifactMd5.exists() )
                    {
                        deployWithWagon( container, "http", deployUrl, artifactMd5,
                                         GavUtil.getRelitiveArtifactPath( gav ) + ".md5" );
                    }
                    if ( artifactAsc.exists() )
                    {
                        deployWithWagon( container, "http", deployUrl, artifactAsc,
                                         GavUtil.getRelitiveArtifactPath( gav ) + ".asc" );
                    }

                    deployWithWagon( container, "http", deployUrl, artifactFile, GavUtil.getRelitiveArtifactPath( gav ) );

                    if ( pomSha1.exists() )
                    {
                        deployWithWagon( container, "http", deployUrl, pomSha1, GavUtil.getRelitivePomPath( gav )
                            + ".sha1" );
                    }
                    if ( pomMd5.exists() )
                    {
                        deployWithWagon( container, "http", deployUrl, pomMd5, GavUtil.getRelitivePomPath( gav )
                            + ".md5" );
                    }
                    if ( pomAsc.exists() )
                    {
                        deployWithWagon( container, "http", deployUrl, pomAsc, GavUtil.getRelitivePomPath( gav )
                            + ".asc" );
                    }

                    deployWithWagon( container, "http", deployUrl, pom, GavUtil.getRelitivePomPath( gav ) );
                }
                catch ( Exception e )
                {
                    log.error( testId + " Unable to deploy " + artifactFileName, e );
                    throw e;
                }
            }
        }

        getEventInspectorsUtil().waitForCalmPeriod();
    }

    private void deployWithWagon( PlexusContainer container, String wagonHint, String deployUrl, File fileToDeploy,
                                  String artifactPath )
        throws ConnectionException, AuthenticationException, TransferFailedException, ResourceDoesNotExistException,
        AuthorizationException, ComponentLookupException
    {
        new WagonDeployer( container, wagonHint, "admin", "admin123", deployUrl, fileToDeploy, artifactPath ).deploy();
    }

    @BeforeMethod
    public void mockSetup()
    {
        MockHelper.clearMocks();
    }

    protected <E> E lookup( Class<E> role )
        throws ComponentLookupException
    {
        return env.getPlexusContainer().lookup( role );
    }

    protected <E> E lookup( Class<E> role, String hint )
        throws ComponentLookupException
    {
        return env.getPlexusContainer().lookup( role, hint );
    }

    @BeforeClass
    @Override
    public void initialize()
        throws InitializationException
    {
        String packageName = this.getClass().getPackage().getName();
        this.testId = packageName.substring( packageName.lastIndexOf( '.' ) + 1, packageName.length() );

        this.testName = getClass().getSimpleName();
    }

    protected static final File RESOURCES_DIR = new File( "target/resources" );

    protected static final File RESOURCES_SOURCE_DIR = new File( "resources" );

    protected File getTestResourceAsFile( String relativePath )
    {
        String resource = testId + "/" + relativePath;
        return getResource( resource );
    }

    protected File getResource( String resource )
    {
        log.debug( "Looking for resource: " + resource );

        File file = new File( RESOURCES_DIR, resource );

        if ( !file.exists() )
        {
            return null;
        }

        log.debug( "found: " + file );

        return file.getAbsoluteFile();
    }

    @Deprecated
    protected File getTestFile( String relativePath )
    {
        return getFile( relativePath );
    }

    protected File getFile( String relativePath )
    {
        return getTestResourceAsFile( "files/" + relativePath );
    }

    private EventInspectorsUtil eventInspectorsUtil;

    public EventInspectorsUtil getEventInspectorsUtil()
    {
        if ( eventInspectorsUtil == null )
        {
            eventInspectorsUtil = new EventInspectorsUtil( null );
        }

        return eventInspectorsUtil;
    }
}
