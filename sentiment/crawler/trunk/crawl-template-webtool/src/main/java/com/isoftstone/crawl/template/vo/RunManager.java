package com.isoftstone.crawl.template.vo;

import java.util.Date;

public class RunManager {
	/**
	 * 主键
	 */
	private String id;

	/**
	 * 主机名
	 */
	private String hostname;

	/**
	 * 主机IP.
	 */
	private String hostIp;

	/**
	 * 端口号
	 */
	private Integer port;

	/**
	 * 用户名
	 */
	private String username;

	/**
	 * 密码
	 */
	private String password;

	/**
	 * 命令
	 */
	private String command;

	/**
	 * 脚本位置
	 */
	private String commandPath;

	/**
	 * 状态（1表示启动，2表示暂停）
	 */
	private String status;

	/**
	 * 添加任务时间
	 */
	private Date createTime;

	/**
	 * 启动任务时间
	 */
	private Date startTime;

	/**
	 * 停止任务时间
	 */
	private Date stopTime;

	/**
	 * 修改任务时间
	 */
	private Date updateTime;

	/**
	 * 操作员
	 */
	private String operator;

	/**
	 * 密钥
	 */
	private String passPhrase;

	/**
	 * 密钥地址
	 */
	private String keyPath;

	/**
	 * 时间间隔（定时任务）
	 */
	private Integer period;

	/**
	 * 命令简介
	 */
	private String brief;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getHostname() {
		return hostname;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public String getHostIp() {
		return hostIp;
	}

	public void setHostIp(String hostIp) {
		this.hostIp = hostIp;
	}

	public String getUsername() {
		return username;
	}

	public Integer getPort() {
		return port;
	}

	public void setPort(Integer port) {
		this.port = port;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getCommand() {
		return command;
	}

	public void setCommand(String command) {
		this.command = command;
	}

	public String getCommandPath() {
		return commandPath;
	}

	public void setCommandPath(String commandPath) {
		this.commandPath = commandPath;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	public Date getStartTime() {
		return startTime;
	}

	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	public Date getStopTime() {
		return stopTime;
	}

	public void setStopTime(Date stopTime) {
		this.stopTime = stopTime;
	}

	public Date getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(Date updateTime) {
		this.updateTime = updateTime;
	}

	public String getOperator() {
		return operator;
	}

	public void setOperator(String operator) {
		this.operator = operator;
	}

	public String getPassPhrase() {
		return passPhrase;
	}

	public void setPassPhrase(String passPhrase) {
		this.passPhrase = passPhrase;
	}

	public String getKeyPath() {
		return keyPath;
	}

	public void setKeyPath(String keyPath) {
		this.keyPath = keyPath;
	}

	public Integer getPeriod() {
		return period;
	}

	public void setPeriod(Integer period) {
		this.period = period;
	}

	public String getBrief() {
		return brief;
	}

	public void setBrief(String brief) {
		this.brief = brief;
	}

}