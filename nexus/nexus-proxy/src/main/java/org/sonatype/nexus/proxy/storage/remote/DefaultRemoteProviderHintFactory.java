package org.sonatype.nexus.proxy.storage.remote;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.sonatype.nexus.proxy.storage.remote.commonshttpclient.CommonsHttpClientRemoteStorage;
import org.sonatype.nexus.util.SystemPropertiesHelper;

/**
 * TODO: for now, we have limited the RRS selection, but in future, this should be made dynamic!
 */
@Component( role = RemoteProviderHintFactory.class )
public class DefaultRemoteProviderHintFactory
    implements RemoteProviderHintFactory
{
    public final static String DEFAULT_HTTP_PROVIDER_KEY = "nexus.default.http.provider";

    public final static String DEFAULT_HTTP_PROVIDER_FORCED_KEY = "nexus.default.http.providerForced";

    @Requirement
    private Logger logger;

    private Boolean httpProviderForced = null;

    protected synchronized boolean isHttpProviderForced()
    {
        if ( httpProviderForced == null )
        {
            httpProviderForced = SystemPropertiesHelper.getBoolean( DEFAULT_HTTP_PROVIDER_FORCED_KEY, false );

            if ( httpProviderForced )
            {
                logger.warn( "HTTP Provider forcing is in effect (system property \""
                    + DEFAULT_HTTP_PROVIDER_FORCED_KEY
                    + "\" is set to \"true\"!), so regardless of your configuration, for HTTP RemoteRepositoryStorage the \""
                    + getDefaultHttpRoleHint()
                    + "\" provider will be used! Consider adjusting your configuration instead and stop using provider forcing." );
            }
        }

        return httpProviderForced;
    }

    public String getDefaultRoleHint( final String remoteUrl )
        throws IllegalArgumentException
    {
        if ( StringUtils.isBlank( remoteUrl ) )
        {
            throw new IllegalArgumentException( "Remote URL cannot be null!" );
        }

        final String remoteUrlLowered = remoteUrl.toLowerCase();

        if ( remoteUrlLowered.startsWith( "http:" ) || remoteUrlLowered.startsWith( "https:" ) )
        {
            return getDefaultHttpRoleHint();
        }

        throw new IllegalArgumentException( "No known remote repository storage provider for remote URL " + remoteUrl );
    }

    public String getRoleHint( final String remoteUrl, final String hint )
        throws IllegalArgumentException
    {
        if ( StringUtils.isBlank( remoteUrl ) )
        {
            throw new IllegalArgumentException( "Remote URL cannot be null!" );
        }

        final String remoteUrlLowered = remoteUrl.toLowerCase();

        if ( remoteUrlLowered.startsWith( "http:" ) || remoteUrlLowered.startsWith( "https:" ) )
        {
            return getHttpRoleHint( hint );
        }

        if ( StringUtils.isBlank( hint ) )
        {
            throw new IllegalArgumentException( "RemoteRepositoryStorage hint cannot be null!" );
        }

        logger.info( "Returning supplied \"{}\" hint for remote URL {}.",
            new Object[] { remoteUrl, hint } );

        return hint;
    }

    public String getDefaultHttpRoleHint()
    {
        return SystemPropertiesHelper.getString( DEFAULT_HTTP_PROVIDER_KEY,
            CommonsHttpClientRemoteStorage.PROVIDER_STRING );
    }

    public String getHttpRoleHint( final String hint )
    {
        if ( isHttpProviderForced() || StringUtils.isBlank( hint ) )
        {
            return getDefaultHttpRoleHint();
        }
        else
        {
            return hint;
        }
    }
}
