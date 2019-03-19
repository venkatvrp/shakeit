package org.shakeit.model;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

@XmlAccessorType(XmlAccessType.FIELD)
public class Servers
{
    private List<Server> server;

	public List<Server> getServer() {
		return server;
	}

	public void setServer(List<Server> server) {
		this.server = server;
	}

	@Override
    public String toString()
    {
        return "ClassPojo [server = "+server+"]";
    }
}
