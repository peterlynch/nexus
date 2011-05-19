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
package org.sonatype.nexus.proxy.item;

import java.util.Collection;

import org.sonatype.nexus.proxy.AccessDeniedException;
import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.NoSuchResourceStoreException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.StorageException;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.router.RepositoryRouter;

/**
 * The Class DefaultStorageCollectionItem.
 */
public class DefaultStorageCollectionItem
    extends AbstractStorageItem
    implements StorageCollectionItem
{

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = -7329636330511885938L;

    /**
     * Instantiates a new default storage collection item.
     * 
     * @param repository the repository
     * @param path the path
     * @param canRead the can read
     * @param canWrite the can write
     */
    public DefaultStorageCollectionItem( Repository repository, ResourceStoreRequest request, boolean canRead,
                                         boolean canWrite )
    {
        super( repository, request, canRead, canWrite );
    }

    /**
     * Shotuct method.
     * 
     * @param repository
     * @param path
     * @param canRead
     * @param canWrite
     * @deprecated supply resourceStoreRequest always
     */
    public DefaultStorageCollectionItem( Repository repository, String path, boolean canRead, boolean canWrite )
    {
        this( repository, new ResourceStoreRequest( path, true, false ), canRead, canWrite );
    }

    /**
     * Instantiates a new default storage collection item.
     * 
     * @param router the router
     * @param path the path
     * @param virtual the virtual
     * @param canRead the can read
     * @param canWrite the can write
     */
    public DefaultStorageCollectionItem( RepositoryRouter router, ResourceStoreRequest request, boolean canRead,
                                         boolean canWrite )
    {
        super( router, request, canRead, canWrite );
    }

    /**
     * Shortcut method.
     * 
     * @param router
     * @param path
     * @param canRead
     * @param canWrite
     * @deprecated supply resourceStoreRequest always
     */
    public DefaultStorageCollectionItem( RepositoryRouter router, String path, boolean canRead, boolean canWrite )
    {
        this( router, new ResourceStoreRequest( path, true, false ), canRead, canWrite );
    }

    /*
     * (non-Javadoc)
     * @see org.sonatype.nexus.item.StorageCollectionItem#list()
     */
    public Collection<StorageItem> list()
        throws AccessDeniedException, NoSuchResourceStoreException, IllegalOperationException, ItemNotFoundException,
        StorageException
    {
        if ( isVirtual() )
        {
            return getStore().list( getResourceStoreRequest() );
        }
        else
        {
            Repository repo = getRepositoryItemUid().getRepository();

            Collection<StorageItem> result = repo.list( false, this );

            correctPaths( result );

            return result;
        }
    }

    /**
     * This method "normalizes" the paths back to the "level" from where the original item was requested.
     * 
     * @param list
     */
    protected void correctPaths( Collection<StorageItem> list )
    {
        for ( StorageItem item : list )
        {
            if ( getPath().endsWith( RepositoryItemUid.PATH_SEPARATOR ) )
            {
                ( (AbstractStorageItem) item ).setPath( getPath() + item.getName() );
            }
            else
            {
                ( (AbstractStorageItem) item ).setPath( getPath() + RepositoryItemUid.PATH_SEPARATOR + item.getName() );
            }
        }
    }

    // --

    public String toString()
    {
        return String.format( "%s (coll)", super.toString() );
    }

}
