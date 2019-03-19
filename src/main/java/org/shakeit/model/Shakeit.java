package org.shakeit.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="shakeit")
public class Shakeit
{

    private Servers servers;
    private Applications applications;

    public Servers getServers ()
    {
        return servers;
    }
    @XmlElement
    public void setServers (Servers servers)
    {
        this.servers = servers;
    }
   
    public Applications getApplications ()
    {
        return applications;
    }
    @XmlElement
    public void setApplications (Applications applications)
    {
        this.applications = applications;
    }

    @Override
    public String toString()
    {
        return "ClassPojo [servers = "+servers+", applications = "+applications+"]";
    }
}
			
			