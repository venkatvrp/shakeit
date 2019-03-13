package org.shakeit.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

@XmlAccessorType(XmlAccessType.FIELD)
public class Servers
{
    private Server[] server;

    public Server[] getServer ()
    {
        return server;
    }

    public void setServer (Server[] server)
    {
        this.server = server;
    }

    @Override
    public String toString()
    {
        return "ClassPojo [server = "+server+"]";
    }
}
