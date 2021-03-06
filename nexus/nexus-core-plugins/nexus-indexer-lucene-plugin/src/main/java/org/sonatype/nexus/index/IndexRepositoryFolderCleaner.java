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
package org.sonatype.nexus.index;

import java.io.File;
import java.io.IOException;

import org.codehaus.plexus.component.annotations.Component;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.repository.ShadowRepository;
import org.sonatype.nexus.proxy.wastebasket.AbstractRepositoryFolderCleaner;
import org.sonatype.nexus.proxy.wastebasket.RepositoryFolderCleaner;

@Component( role = RepositoryFolderCleaner.class, hint = "indexer-lucene" )
public class IndexRepositoryFolderCleaner
    extends AbstractRepositoryFolderCleaner
{
    public void cleanRepositoryFolders( Repository repository, boolean deleteForever )
        throws IOException
    {
        if ( repository.getRepositoryKind().isFacetAvailable( ShadowRepository.class ) )
        {
            return;
        }

        File indexContextFolder =
            new File( getApplicationConfiguration().getWorkingDirectory(
                DefaultIndexerManager.INDEXER_WORKING_DIRECTORY_KEY ), repository.getId()
                + DefaultIndexerManager.CTX_SUFIX );

        if ( indexContextFolder.isDirectory() )
        {
            // indexes are not preserved
            delete( indexContextFolder, true );
        }
    }

}
