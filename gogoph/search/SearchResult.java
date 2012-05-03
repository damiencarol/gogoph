package gogoph.search;

import gogoph.GopherDirectoryEntity;

public class SearchResult {
	
	private String title;
	private GopherDirectoryEntity entity;
	private float score;


	public SearchResult(String ptitle, GopherDirectoryEntity pentity, float pscore) {
		title = ptitle;
		entity = pentity;
		score = pscore;
	}

	

	public float getScore() {
		return score;
	}

	public void setScore(float score) {
		this.score = score;
	}

	public GopherDirectoryEntity getEntity() {
		return entity;
	}

	public void setEntity(GopherDirectoryEntity entity) {
		this.entity = entity;
	}



	public String getTitle() {
		return title;
	}



	public void setTitle(String title) {
		this.title = title;
	}

}
