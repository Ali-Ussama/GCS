package data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

public class Surveyor {

	@SerializedName("SurveyorId")
	@Expose
	private int surveyorId;
	@SerializedName("SurveyorName")
	@Expose
	private String surveyorName;
	@SerializedName("UserName")
	@Expose
	private String userName;
	@SerializedName("Password")
	@Expose
	private String password;
	@SerializedName("GlobalID")
	@Expose
	private String globalID;
	@SerializedName("HashPassword")
	@Expose
	private String hashPassword;
	@SerializedName("IsAdmin")
	@Expose
	private boolean isAdmin;
	@SerializedName("DeviceID")
	@Expose
	private String deviceID;

	public int getSurveyorId() {
		return surveyorId;
	}

	public void setSurveyorId(int surveyorId) {
		this.surveyorId = surveyorId;
	}

	public String getSurveyorName() {
		return surveyorName;
	}

	public void setSurveyorName(String surveyorName) {
		this.surveyorName = surveyorName;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getGlobalID() {
		return globalID;
	}

	public void setGlobalID(String globalID) {
		this.globalID = globalID;
	}

	public String getHashPassword() {
		return hashPassword;
	}

	public void setHashPassword(String hashPassword) {
		this.hashPassword = hashPassword;
	}

	public boolean isIsAdmin() {
		return isAdmin;
	}

	public void setIsAdmin(boolean isAdmin) {
		this.isAdmin = isAdmin;
	}

	public String getDeviceID() {
		return deviceID;
	}

	public void setDeviceID(String deviceID) {
		this.deviceID = deviceID;
	}

}
