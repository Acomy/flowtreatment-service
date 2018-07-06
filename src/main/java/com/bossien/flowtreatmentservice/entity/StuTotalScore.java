package com.bossien.flowtreatmentservice.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.persistence.Entity;
import java.io.Serializable;

@Data
@Document(collection = "stu_total_score")
public class StuTotalScore implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 *
	 */
	@Id
	private String id;
	/**
	 *
	 */
	@Indexed
	private Long userid;
	/**
	 *
	 */
	private Integer personScore;
	/**
	 * 个人贡献分
	 */
	private Integer personContrib;
	/**
	 *
	 */
	private String departName;
	/**
	 *
	 */
	private Integer departScore;
	/**
	 * 单位全国排名
	 */
	private Integer provinceRank;
	/**
	 * 个人全国排名
	 */
	private Integer personRank;
	/**
	 *
	 */
	private Long duration;
	/**
	 *
	 */
	private Integer upstate;
	/**
	 *
	 */
	private Long departid;

	private String userName;
	/**
	 * 当前单位的用户人数
	 */
	private Long  currentUnitPersonCount;

	/***
	 * 0:学员成绩无效，1：学员成绩有效
	 */
	private Integer isState;

	private String nickName;
	private String telephone;
	private Long day ;

	public Long getDay() {
		return day;
	}

	public void setDay(Long day) {
		this.day = day;
	}

	public String getNickName() {
		return nickName;
	}

	public void setNickName(String nickName) {
		this.nickName = nickName;
	}

	public String getTelephone() {
		return telephone;
	}

	public void setTelephone(String telephone) {
		this.telephone = telephone;
	}

	public Integer getIsState() {
		return isState;
	}

	public void setIsState(Integer isState) {
		this.isState = isState;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public void setId(String id){
		this.id=id;
	}
	public String getId(){
		return this.id;
	}

	public void setUserid(Long userid){
		this.userid=userid;
	}
	public Long getUserid(){
		return this.userid;
	}

	public void setPersonScore(Integer personScore){
		this.personScore=personScore;
	}
	public Integer getPersonScore(){
		return this.personScore;
	}

	public void setPersonContrib(Integer personContrib){
		this.personContrib=personContrib;
	}
	public Integer getPersonContrib(){
		return this.personContrib;
	}

	public void setDepartName(String departName){
		this.departName=departName;
	}
	public String getDepartName(){
		return this.departName;
	}

	public void setDepartScore(Integer departScore){
		this.departScore=departScore;
	}
	public Integer getDepartScore(){
		return this.departScore;
	}

	public void setProvinceRank(Integer provinceRank){
		this.provinceRank=provinceRank;
	}
	public Integer getProvinceRank(){
		return this.provinceRank;
	}

	public void setPersonRank(Integer personRank){
		this.personRank=personRank;
	}
	public Integer getPersonRank(){
		return this.personRank;
	}

	public void setDuration(Long duration){
		this.duration=duration;
	}
	public Long getDuration(){
		return this.duration;
	}

	public void setUpstate(Integer upstate){
		this.upstate=upstate;
	}
	public Integer getUpstate(){
		return this.upstate;
	}

	public void setDepartid(Long departid){
		this.departid=departid;
	}
	public Long getDepartid(){
		return this.departid;
	}

	public Long getCurrentUnitPersonCount() {
		return currentUnitPersonCount;
	}

	public void setCurrentUnitPersonCount(Long currentUnitPersonCount) {
		this.currentUnitPersonCount = currentUnitPersonCount;
	}

	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append("StuTotalScore[");
		sb.append("id=");
		sb.append(id);
		sb.append(",userid=");
		sb.append(userid);
		sb.append(",personScore=");
		sb.append(personScore);
		sb.append(",personContrib=");
		sb.append(personContrib);
		sb.append(",departName=");
		sb.append(departName);
		sb.append(",departScore=");
		sb.append(departScore);
		sb.append(",provinceRank=");
		sb.append(provinceRank);
		sb.append(",personRank=");
		sb.append(personRank);
		sb.append(",duration=");
		sb.append(duration);
		sb.append(",upstate=");
		sb.append(upstate);
		sb.append(",departid=");
		sb.append(departid);
		sb.append(",currentUnitPersonCount=");
		sb.append(currentUnitPersonCount);
		sb.append(",isState=");
		sb.append(isState);
		sb.append(",telephone=");
		sb.append(telephone);
		sb.append(",nickName=");
		sb.append(nickName);
		sb.append("]");
		return sb.toString();
	}
}