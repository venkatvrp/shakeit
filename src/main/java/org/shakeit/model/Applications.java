package org.shakeit.model;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

@XmlAccessorType(XmlAccessType.FIELD)
public class Applications
{
	private List<Url> url;    

    public List<Url> getUrl() {
		return url;
	}

	public void setUrl(List<Url> url) {
		this.url = url;
	}

	@Override
    public String toString()
    {
        return "ClassPojo [url = "+url+"]";
    }
}