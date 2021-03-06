package org.sonatype.nexus.security;

import java.io.Serializable;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.SessionContext;
import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.session.mgt.eis.JavaUuidSessionIdGenerator;
import org.apache.shiro.session.mgt.eis.SessionIdGenerator;
import org.apache.shiro.web.servlet.Cookie;
import org.apache.shiro.web.servlet.ShiroHttpServletRequest;
import org.apache.shiro.web.servlet.ShiroHttpSession;
import org.apache.shiro.web.servlet.SimpleCookie;
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;
import org.apache.shiro.web.util.WebUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatelessAndStatefulWebSessionManager
    extends DefaultWebSessionManager
{
    private static final Logger log = LoggerFactory.getLogger( DefaultWebSessionManager.class );

    private SessionIdGenerator fakeSessionIdGenerator = new JavaUuidSessionIdGenerator();

    protected Session doCreateSession( SessionContext context )
    {
        Session session = newSessionInstance( context );
        if ( log.isTraceEnabled() )
        {
            log.trace( "Creating session for host {}", session.getHost() );
        }

        if ( WebUtils.isHttp( context ) && isStatelessClient( WebUtils.getHttpRequest( context ) ) )
        {   
            // we still need to set the session id, WHY?
            ( (SimpleSession) session ).setId( fakeSessionIdGenerator.generateId( session ) );
            log.debug( "Stateless client sesion {} is not persisted.", session.getId() );
        }
        else
        {
            create( session );
        }

        return session;
    }

    @Override
    protected void onStart( Session session, SessionContext context )
    {
        if ( !WebUtils.isHttp( context ) )
        {
            log.debug( "SessionContext argument is not HTTP compatible or does not have an HTTP request/response "
                + "pair. No session ID cookie will be set." );
            return;

        }
        HttpServletRequest request = WebUtils.getHttpRequest( context );
        HttpServletResponse response = WebUtils.getHttpResponse( context );

        if ( isSessionIdCookieEnabled( request, response ) )
        {
            Serializable sessionId = session.getId();
            storeSessionId( sessionId, request, response );
        }
        else
        {
            log.debug( "Session ID cookie is disabled.  No cookie has been set for new session with id {}",
                       session.getId() );
        }

        request.removeAttribute( ShiroHttpServletRequest.REFERENCED_SESSION_ID_SOURCE );
        request.setAttribute( ShiroHttpServletRequest.REFERENCED_SESSION_IS_NEW, Boolean.TRUE );
    }

    public boolean isSessionIdCookieEnabled( ServletRequest request, ServletResponse response )
    {
        return isSessionIdCookieEnabled() && !isStatelessClient( request );
    }

    // //////////
    // access private methods
    // //////////

    protected Serializable getSessionId( ServletRequest request, ServletResponse response )
    {
        return getReferencedSessionId( request, response );
    }

    private String getSessionIdCookieValue( ServletRequest request, ServletResponse response )
    {
        if ( !isSessionIdCookieEnabled( request, response ) )
        {
            log.debug( "Session ID cookie is disabled - session id will not be acquired from a request cookie." );
            return null;
        }
        if ( !( request instanceof HttpServletRequest ) )
        {
            log.debug( "Current request is not an HttpServletRequest - cannot get session ID cookie.  Returning null." );
            return null;
        }
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        return getSessionIdCookie().readValue( httpRequest, WebUtils.toHttp( response ) );
    }

    private Serializable getReferencedSessionId( ServletRequest request, ServletResponse response )
    {

        String id = getSessionIdCookieValue( request, response );
        if ( id != null )
        {
            request.setAttribute( ShiroHttpServletRequest.REFERENCED_SESSION_ID_SOURCE,
                                  ShiroHttpServletRequest.COOKIE_SESSION_ID_SOURCE );
        }
        else
        {
            // not in a cookie, or cookie is disabled - try the request params as a fallback (i.e. URL rewriting):
            id = request.getParameter( ShiroHttpSession.DEFAULT_SESSION_ID_NAME );
            if ( id == null )
            {
                // try lowercase:
                id = request.getParameter( ShiroHttpSession.DEFAULT_SESSION_ID_NAME.toLowerCase() );
            }
            if ( id != null )
            {
                request.setAttribute( ShiroHttpServletRequest.REFERENCED_SESSION_ID_SOURCE,
                                      ShiroHttpServletRequest.URL_SESSION_ID_SOURCE );
            }
        }
        if ( id != null )
        {
            request.setAttribute( ShiroHttpServletRequest.REFERENCED_SESSION_ID, id );
            // automatically mark it valid here. If it is invalid, the
            // onUnknownSession method below will be invoked and we'll remove the attribute at that time.
            request.setAttribute( ShiroHttpServletRequest.REFERENCED_SESSION_ID_IS_VALID, Boolean.TRUE );
        }
        return id;
    }

    private void storeSessionId( Serializable currentId, HttpServletRequest request, HttpServletResponse response )
    {
        if ( currentId == null )
        {
            String msg = "sessionId cannot be null when persisting for subsequent requests.";
            throw new IllegalArgumentException( msg );
        }
        Cookie template = getSessionIdCookie();
        Cookie cookie = new SimpleCookie( template );
        String idString = currentId.toString();
        cookie.setValue( idString );
        cookie.saveTo( request, response );
        log.trace( "Set session ID cookie for session with id {}", idString );
    }

    protected boolean isStatelessClient( final ServletRequest request )
    {
        final String userAgent = getUserAgent( request );

        if ( userAgent != null && userAgent.trim().length() > 0 )
        {
            // maven 2.0.10+
            if ( userAgent.startsWith( "Apache-Maven" ) )
            {
                return true;
            }

            // maven pre 2.0.10 and all Java based clients relying on java.net.UrlConnection
            if ( userAgent.startsWith( "Java/" ) )
            {
                return true;
            }

            // ivy
            if ( userAgent.startsWith( "Apache Ivy/" ) )
            {
                return true;
            }

            // curl
            if ( userAgent.startsWith( "curl/" ) )
            {
                return true;
            }

            // wget
            if ( userAgent.startsWith( "Wget/" ) )
            {
                return true;
            }

        }

        // we can't decided for sure, let's return the safest
        return false;
    }

    private String getUserAgent( final ServletRequest request )
    {
        if ( request instanceof HttpServletRequest )
        {
            final String userAgent = ( (HttpServletRequest) request ).getHeader( "User-Agent" );
            return userAgent;
        }
        return null;
    }

}
