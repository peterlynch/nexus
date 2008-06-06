/*
 * Nexus: Maven Repository Manager
 * Copyright (C) 2008 Sonatype Inc.                                                                                                                          
 * 
 * This file is part of Nexus.                                                                                                                                  
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 */
package org.sonatype.scheduling;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Startable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.StartingException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.StoppingException;
import org.codehaus.plexus.util.StringUtils;
import org.sonatype.scheduling.schedules.RunNowSchedule;
import org.sonatype.scheduling.schedules.Schedule;

/**
 * A simple facade to ScheduledThreadPoolExecutor as Plexus component.
 * 
 * @author cstamas
 * @plexus.component
 */
public class DefaultScheduler
    extends AbstractLogEnabled
    implements Scheduler, Contextualizable, Startable
{
    private PlexusContainer plexusContainer;

    private PlexusThreadFactory plexusThreadFactory;

    private ScheduledThreadPoolExecutor scheduledExecutorService;

    private Map<String, List<ScheduledTask<?>>> tasksMap;
    
    /**
     * @plexus.requirement
     */
    private TaskConfigManager taskConfig;

    public void contextualize( Context context )
        throws ContextException
    {
        plexusContainer = (PlexusContainer) context.get( PlexusConstants.PLEXUS_KEY );
    }

    public void start()
        throws StartingException
    {        
        tasksMap = taskConfig.getTasks();
        
        plexusThreadFactory = new PlexusThreadFactory( plexusContainer );

        scheduledExecutorService = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(
            10,
            plexusThreadFactory );
    }

    public void stop()
        throws StoppingException
    {
        getScheduledExecutorService().shutdown();

        try
        {
            boolean stopped = getScheduledExecutorService().awaitTermination( 15, TimeUnit.SECONDS );

            if ( !stopped )
            {
                List<Runnable> queueds = getScheduledExecutorService().shutdownNow();

                getLogger().warn( "Scheduler shut down with " + queueds.size() + " tasks queued." );
            }
        }
        catch ( InterruptedException e )
        {
            getLogger().info( "Termination interrupted", e );
        }
    }

    public PlexusThreadFactory getPlexusThreadFactory()
    {
        return plexusThreadFactory;
    }

    public ScheduledThreadPoolExecutor getScheduledExecutorService()
    {
        return scheduledExecutorService;
    }

    protected <T> void addToTasksMap( ScheduledTask<T> task )
    {
        synchronized ( tasksMap )
        {
            if ( !tasksMap.containsKey( task.getType() ) )
            {
                tasksMap.put( task.getType(), new ArrayList<ScheduledTask<?>>() );
            }
            tasksMap.get( task.getType() ).add( task );
            taskConfig.addTask( task );
        }
    }

    protected <T> void removeFromTasksMap( ScheduledTask<T> task )
    {
        synchronized ( tasksMap )
        {
            if ( tasksMap.containsKey( task.getType() ) )
            {
                tasksMap.get( task.getType() ).remove( task );

                if ( tasksMap.get( task.getType() ).size() == 0 )
                {
                    tasksMap.remove( task.getType() );
                }
                
                taskConfig.removeTask( task );
            }
        }
    }

    public ScheduledTask<Object> submit( String name, Runnable runnable )
    {
        return schedule( name, runnable, new RunNowSchedule() );
    }

    public ScheduledTask<Object> schedule( String name, Runnable runnable, Schedule schedule )
    {
        DefaultScheduledTask<Object> drt = new DefaultScheduledTask<Object>(
            name,
            runnable.getClass().getName(),
            this,
            Executors.callable( runnable ),
            schedule );

        addToTasksMap( drt );

        drt.start();

        return drt;
    }

    public <T> ScheduledTask<T> submit( String name, Callable<T> callable )
    {
        return schedule( name, callable, new RunNowSchedule() );
    }

    public <T> ScheduledTask<T> schedule( String name, Callable<T> callable, Schedule schedule )
    {
        DefaultScheduledTask<T> dct = new DefaultScheduledTask<T>(
            name,
            callable.getClass().getName(),
            this,
            callable,
            schedule );

        addToTasksMap( dct );

        dct.start();

        return dct;
    }

    public Map<String, List<ScheduledTask<?>>> getActiveTasks()
    {
        Map<String, List<ScheduledTask<?>>> result = null;

        // create a "snapshots" of active tasks
        synchronized ( tasksMap )
        {
            result = new HashMap<String, List<ScheduledTask<?>>>( tasksMap.size() );

            List<ScheduledTask<?>> tasks = null;

            for ( String cls : tasksMap.keySet() )
            {
                tasks = new ArrayList<ScheduledTask<?>>();

                for ( ScheduledTask<?> task : tasksMap.get( cls ) )
                {
                    if ( task.getTaskState().isActiveOrSubmitted() )
                    {
                        tasks.add( task );
                    }
                }

                if ( tasks.size() > 0 )
                {
                    result.put( cls, tasks );
                }
            }
        }
        return result;
    }

    public ScheduledTask<?> getTaskById( String id )
        throws NoSuchTaskException
    {
        if ( StringUtils.isEmpty( id ) )
        {
            throw new IllegalArgumentException( "The Tasks cannot have null IDs!" );
        }

        Collection<List<ScheduledTask<?>>> activeTasks = getActiveTasks().values();

        for ( List<ScheduledTask<?>> tasks : activeTasks )
        {
            for ( ScheduledTask<?> task : tasks )
            {
                if ( task.getId().equals( id ) )
                {
                    return task;
                }
            }
        }

        throw new NoSuchTaskException( id );
    }

}
