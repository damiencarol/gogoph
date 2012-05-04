package gogoph.crawler;

public class CrawlerSiteNode {
	public CrawlerSiteNode(String type, String username, String selector) {
		super();
		this.type = type;
		this.username = username;
		this.selector = selector;
	}

	private String type;
	private String username;
	private String selector;

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getSelector() {
		return selector;
	}

	public void setSelector(String selector) {
		this.selector = selector;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((selector == null) ? 0 : selector.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		/*result = prime * result
				+ ((username == null) ? 0 : username.hashCode());*/
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CrawlerSiteNode other = (CrawlerSiteNode) obj;
		if (selector == null) {
			if (other.selector != null)
				return false;
		} else if (!selector.equals(other.selector))
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		/*if (username == null) {
			if (other.username != null)
				return false;
		} else if (!username.equals(other.username))
			return false;*/
		return true;
	}
}
