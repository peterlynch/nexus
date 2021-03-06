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
package org.sonatype.nexus.mock.pages;

import java.lang.reflect.Constructor;
import java.util.LinkedHashMap;
import java.util.Map;

import org.sonatype.nexus.mock.components.Button;
import org.sonatype.nexus.mock.components.Checkbox;
import org.sonatype.nexus.mock.components.Combobox;
import org.sonatype.nexus.mock.components.Component;
import org.sonatype.nexus.mock.components.TextField;
import org.sonatype.nexus.mock.components.Window;

import com.thoughtworks.selenium.Selenium;

public class SchedulesConfigFormTab
    extends Component
{

    private Button saveButton;

    private TextField name;

    private Combobox taskType;

    private Checkbox enabled;

    private Combobox recurrence;

    private Button cancelButton;

    public SchedulesConfigFormTab( Selenium selenium )
    {
        super( selenium, "window.Ext.getCmp('schedule-config-forms')" );

        enabled = new Checkbox( selenium, getExpression() + ".find('name', 'enabled')[0]" );
        name = new TextField( selenium, getExpression() + ".find('name', 'name')[0]" );
        taskType = new Combobox( selenium, getExpression() + ".find('name', 'typeId')[0]" );
        recurrence = new Combobox( selenium, getExpression() + ".find('name', 'schedule')[0]" );

        saveButton = new Button( selenium, "window.Ext.getCmp('savebutton')" );
        cancelButton = new Button( selenium, "window.Ext.getCmp('cancelbutton')" );
    }

    public SchedulesConfigFormTab populate( boolean enable, String name, String taskType, String recurrence )
    {
        this.enabled.check( enable );
        this.name.type( name );
        this.taskType.setValue( taskType );
        this.recurrence.setValue( recurrence );

        return this;
    }

    public SchedulesConfigFormTab save()
    {
        saveButton.click();

        new Window( selenium ).waitFor();

        return this;
    }

    public Button getSaveButton()
    {
        return saveButton;
    }

    public TextField getName()
    {
        return name;
    }

    public Combobox getTaskType()
    {
        return taskType;
    }

    public Checkbox getEnabled()
    {
        return enabled;
    }

    public Combobox getRecurrence()
    {
        return recurrence;
    }

    public Button getCancelButton()
    {
        return cancelButton;
    }

    public void cancel()
    {
        this.cancelButton.click();
    }

    private Map<String, Component> settings = new LinkedHashMap<String, Component>();

    @SuppressWarnings( "unchecked" )
    public <E extends Component> E getSetting( String fieldName, Class<E> clazz )
        throws Exception
    {
        if ( settings.containsKey( fieldName ) )
        {
            return (E) settings.get( fieldName );
        }

        Constructor<E> constructor = clazz.getConstructor( Selenium.class, String.class );
        E comp =
            constructor.newInstance( selenium, expression + ".find('name', 'serviceProperties_" + fieldName + "')[0]" );
        settings.put( fieldName, comp );

        return comp;
    }

}
