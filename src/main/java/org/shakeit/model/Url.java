package org.shakeit.model;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlAttribute;

public class Url
{
    private String id;

    private String type;

    private String value;    
    
    private String application;

    public String getId ()
    {
        return id;
    }

    @XmlAttribute
    public void setId (String id)
    {
        this.id = id;
    }

    public String getType ()
    {
        return type;
    }

    @XmlAttribute
    public void setType (String type)
    {
        this.type = type;
    }

    public String getValue ()
    {
        return value;
    }

    @XmlAttribute
    public void setValue (String value)
    {
        this.value = value;
    }
    
    public String getApplication ()
    {
        return application;
    }

    @XmlAttribute
    public void setApplication (String application)
    {
        this.application = application;
    } 

    @Override
    public String toString()
    {
        return "ClassPojo [id = "+id+", type = "+type+", value = "+value+"]";
    }
}