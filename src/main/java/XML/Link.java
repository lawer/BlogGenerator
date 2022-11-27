package XML;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class Link {
    public Link(String href) {
        this.href = href;
    }
    @JacksonXmlProperty(isAttribute = true)
    private String href;

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }
}
