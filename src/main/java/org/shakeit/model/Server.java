package org.shakeit.model;

import javax.xml.bind.annotation.XmlAttribute;

public class Server
{
    private String application;

    private String id;

    private String env;

    private Url[] url;

    public String getApplication ()
    {
        return application;
    }

    @XmlAttribute
    public void setApplication (String application)
    {
        this.application = application;
    }

    public String getId ()
    {
        return id;
    }

    @XmlAttribute
    public void setId (String id)
    {
        this.id = id;
    }

    public String getEnv ()
    {
        return env;
    }

    @XmlAttribute
    public void setEnv (String env)
    {
        this.env = env;
    }

    public Url[] getUrl ()
    {
        return url;
    }

    public void setUrl (Url[] url)
    {
        this.url = url;
    }

    @Override
    public String toString()
    {
        return "ClassPojo [application = "+application+", id = "+id+", env = "+env+", url = "+url+"]";
    }
}
		
