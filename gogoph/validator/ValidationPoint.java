package gogoph.validator;

public class ValidationPoint {
	
	public ValidationPoint(String category, String suject, String comment,
			int level, String resolution) {
		super();
		this.category = category;
		this.suject = suject;
		this.comment = comment;
		this.level = level;
		this.resolution = resolution;
	}
	private String category;
	private String suject;
	private String comment;
	private int level;
	private String resolution;
	
	public String getCategory() {
		return category;
	}
	public void setCategory(String category) {
		this.category = category;
	}
	public String getSuject() {
		return suject;
	}
	public void setSuject(String suject) {
		this.suject = suject;
	}
	public String getComment() {
		return comment;
	}
	public void setComment(String comment) {
		this.comment = comment;
	}
	public int getLevel() {
		return level;
	}
	public void setLevel(int level) {
		this.level = level;
	}
	public String getResolution() {
		return resolution;
	}
	public void setResolution(String resolution) {
		this.resolution = resolution;
	}
}
