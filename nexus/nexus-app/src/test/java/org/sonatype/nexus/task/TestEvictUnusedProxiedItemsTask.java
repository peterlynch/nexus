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
package org.sonatype.nexus.task;

import org.junit.Test;
import org.sonatype.nexus.AbstractMavenRepoContentTests;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.scheduling.NexusScheduler;

public class TestEvictUnusedProxiedItemsTask
    extends AbstractMavenRepoContentTests
{
    NexusScheduler scheduler;

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        nexusConfiguration.setSecurityEnabled( false );

        nexusConfiguration.saveConfiguration();

        scheduler = ( NexusScheduler ) lookup( NexusScheduler.class );
    }

    @Test
    public void testDeleteEmptyFolder()
        throws Exception
    {
        fillInRepo();

        while ( scheduler.getActiveTasks().size() > 0 )
        {
            Thread.sleep( 100 );
        }

        long tsDeleting = System.currentTimeMillis() - 10000L;
        long tsToBeKept = tsDeleting + 1000L;
        long tsToBeDeleted = tsDeleting - 1000L;

        String[] itemsToBeKept = { "/org/sonatype/test-evict/1.0/test.txt" };
        String[] itemsToBeDeleted = {
            "/org/sonatype/test-evict/sonatype-test-evict_1.4_mail",
            "/org/sonatype/test-evict/sonatype-test-evict_1.4_mail/1.0-SNAPSHOT/sonatype-test-evict_1.4_mail-1.0-SNAPSHOT.jar" };

        for ( String item : itemsToBeKept )
        {
            ResourceStoreRequest request = new ResourceStoreRequest( item );

            StorageItem storageItem = apacheSnapshots.retrieveItem( request );

            storageItem.setLastRequested( tsToBeKept );

            apacheSnapshots.storeItem( false, storageItem );
        }

        for ( String item : itemsToBeDeleted )
        {
            ResourceStoreRequest request = new ResourceStoreRequest( item );

            StorageItem storageItem = apacheSnapshots.retrieveItem( request );

            storageItem.setLastRequested( tsToBeDeleted );

            apacheSnapshots.storeItem( false, storageItem );
        }

        defaultNexus.evictAllUnusedProxiedItems( new ResourceStoreRequest( "/" ), tsDeleting );

        for ( String item : itemsToBeKept )
        {
            try
            {
                assertNotNull( apacheSnapshots.retrieveItem( new ResourceStoreRequest( item ) ) );
            }
            catch ( ItemNotFoundException e )
            {
                fail( "Item should not have been deleted: " + item );
            }
        }

        for ( String item : itemsToBeDeleted )
        {
            try
            {
                apacheSnapshots.retrieveItem( new ResourceStoreRequest( item ) );

                fail( "Item should have been deleted: " + item );
            }
            catch ( ItemNotFoundException e )
            {
                // this is correct
            }
        }
    }

}
