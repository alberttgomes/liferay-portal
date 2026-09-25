/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.upgrades.lab.agent.remote.model;

import com.liferay.exportimport.kernel.lar.StagedModelType;
import com.liferay.portal.kernel.model.ModelWrapper;
import com.liferay.portal.kernel.model.wrapper.BaseModelWrapper;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * This class is a wrapper for {@link UpgradeRun}.
 * </p>
 *
 * @author Albert Gomes Cabral
 * @see UpgradeRun
 * @generated
 */
public class UpgradeRunWrapper
	extends BaseModelWrapper<UpgradeRun>
	implements ModelWrapper<UpgradeRun>, UpgradeRun {

	public UpgradeRunWrapper(UpgradeRun upgradeRun) {
		super(upgradeRun);
	}

	@Override
	public Map<String, Object> getModelAttributes() {
		Map<String, Object> attributes = new HashMap<String, Object>();

		attributes.put("mvccVersion", getMvccVersion());
		attributes.put("uuid", getUuid());
		attributes.put("externalReferenceCode", getExternalReferenceCode());
		attributes.put("upgradeRunId", getUpgradeRunId());
		attributes.put("companyId", getCompanyId());
		attributes.put("userId", getUserId());
		attributes.put("userName", getUserName());
		attributes.put("createDate", getCreateDate());
		attributes.put("modifiedDate", getModifiedDate());
		attributes.put("branch", getBranch());
		attributes.put("credentialKeyReference", getCredentialKeyReference());
		attributes.put("customerName", getCustomerName());
		attributes.put("dbTargetType", getDbTargetType());
		attributes.put("dbTargetVersion", getDbTargetVersion());
		attributes.put("endDate", getEndDate());
		attributes.put("nodeVersion", getNodeVersion());
		attributes.put("pullRequestURL", getPullRequestURL());
		attributes.put("repositoryURL", getRepositoryURL());
		attributes.put("resultBranch", getResultBranch());
		attributes.put("searchVersion", getSearchVersion());
		attributes.put("startDate", getStartDate());
		attributes.put("targetRelease", getTargetRelease());
		attributes.put("upgradeSourceVersion", getUpgradeSourceVersion());
		attributes.put(
			"upgradeTargetJavaVersion", getUpgradeTargetJavaVersion());
		attributes.put("workspacePath", getWorkspacePath());
		attributes.put("status", getStatus());
		attributes.put("statusMessage", getStatusMessage());

		return attributes;
	}

	@Override
	public void setModelAttributes(Map<String, Object> attributes) {
		Long mvccVersion = (Long)attributes.get("mvccVersion");

		if (mvccVersion != null) {
			setMvccVersion(mvccVersion);
		}

		String uuid = (String)attributes.get("uuid");

		if (uuid != null) {
			setUuid(uuid);
		}

		String externalReferenceCode = (String)attributes.get(
			"externalReferenceCode");

		if (externalReferenceCode != null) {
			setExternalReferenceCode(externalReferenceCode);
		}

		Long upgradeRunId = (Long)attributes.get("upgradeRunId");

		if (upgradeRunId != null) {
			setUpgradeRunId(upgradeRunId);
		}

		Long companyId = (Long)attributes.get("companyId");

		if (companyId != null) {
			setCompanyId(companyId);
		}

		Long userId = (Long)attributes.get("userId");

		if (userId != null) {
			setUserId(userId);
		}

		String userName = (String)attributes.get("userName");

		if (userName != null) {
			setUserName(userName);
		}

		Date createDate = (Date)attributes.get("createDate");

		if (createDate != null) {
			setCreateDate(createDate);
		}

		Date modifiedDate = (Date)attributes.get("modifiedDate");

		if (modifiedDate != null) {
			setModifiedDate(modifiedDate);
		}

		String branch = (String)attributes.get("branch");

		if (branch != null) {
			setBranch(branch);
		}

		String credentialKeyReference = (String)attributes.get(
			"credentialKeyReference");

		if (credentialKeyReference != null) {
			setCredentialKeyReference(credentialKeyReference);
		}

		String customerName = (String)attributes.get("customerName");

		if (customerName != null) {
			setCustomerName(customerName);
		}

		String dbTargetType = (String)attributes.get("dbTargetType");

		if (dbTargetType != null) {
			setDbTargetType(dbTargetType);
		}

		String dbTargetVersion = (String)attributes.get("dbTargetVersion");

		if (dbTargetVersion != null) {
			setDbTargetVersion(dbTargetVersion);
		}

		Date endDate = (Date)attributes.get("endDate");

		if (endDate != null) {
			setEndDate(endDate);
		}

		String nodeVersion = (String)attributes.get("nodeVersion");

		if (nodeVersion != null) {
			setNodeVersion(nodeVersion);
		}

		String pullRequestURL = (String)attributes.get("pullRequestURL");

		if (pullRequestURL != null) {
			setPullRequestURL(pullRequestURL);
		}

		String repositoryURL = (String)attributes.get("repositoryURL");

		if (repositoryURL != null) {
			setRepositoryURL(repositoryURL);
		}

		String resultBranch = (String)attributes.get("resultBranch");

		if (resultBranch != null) {
			setResultBranch(resultBranch);
		}

		String searchVersion = (String)attributes.get("searchVersion");

		if (searchVersion != null) {
			setSearchVersion(searchVersion);
		}

		Date startDate = (Date)attributes.get("startDate");

		if (startDate != null) {
			setStartDate(startDate);
		}

		String targetRelease = (String)attributes.get("targetRelease");

		if (targetRelease != null) {
			setTargetRelease(targetRelease);
		}

		String upgradeSourceVersion = (String)attributes.get(
			"upgradeSourceVersion");

		if (upgradeSourceVersion != null) {
			setUpgradeSourceVersion(upgradeSourceVersion);
		}

		String upgradeTargetJavaVersion = (String)attributes.get(
			"upgradeTargetJavaVersion");

		if (upgradeTargetJavaVersion != null) {
			setUpgradeTargetJavaVersion(upgradeTargetJavaVersion);
		}

		String workspacePath = (String)attributes.get("workspacePath");

		if (workspacePath != null) {
			setWorkspacePath(workspacePath);
		}

		Integer status = (Integer)attributes.get("status");

		if (status != null) {
			setStatus(status);
		}

		String statusMessage = (String)attributes.get("statusMessage");

		if (statusMessage != null) {
			setStatusMessage(statusMessage);
		}
	}

	@Override
	public UpgradeRun cloneWithOriginalValues() {
		return wrap(model.cloneWithOriginalValues());
	}

	/**
	 * Returns the branch of this upgrade run.
	 *
	 * @return the branch of this upgrade run
	 */
	@Override
	public String getBranch() {
		return model.getBranch();
	}

	/**
	 * Returns the company ID of this upgrade run.
	 *
	 * @return the company ID of this upgrade run
	 */
	@Override
	public long getCompanyId() {
		return model.getCompanyId();
	}

	/**
	 * Returns the create date of this upgrade run.
	 *
	 * @return the create date of this upgrade run
	 */
	@Override
	public Date getCreateDate() {
		return model.getCreateDate();
	}

	/**
	 * Returns the credential key reference of this upgrade run.
	 *
	 * @return the credential key reference of this upgrade run
	 */
	@Override
	public String getCredentialKeyReference() {
		return model.getCredentialKeyReference();
	}

	/**
	 * Returns the customer name of this upgrade run.
	 *
	 * @return the customer name of this upgrade run
	 */
	@Override
	public String getCustomerName() {
		return model.getCustomerName();
	}

	/**
	 * Returns the db target type of this upgrade run.
	 *
	 * @return the db target type of this upgrade run
	 */
	@Override
	public String getDbTargetType() {
		return model.getDbTargetType();
	}

	/**
	 * Returns the db target version of this upgrade run.
	 *
	 * @return the db target version of this upgrade run
	 */
	@Override
	public String getDbTargetVersion() {
		return model.getDbTargetVersion();
	}

	/**
	 * Returns the end date of this upgrade run.
	 *
	 * @return the end date of this upgrade run
	 */
	@Override
	public Date getEndDate() {
		return model.getEndDate();
	}

	/**
	 * Returns the external reference code of this upgrade run.
	 *
	 * @return the external reference code of this upgrade run
	 */
	@Override
	public String getExternalReferenceCode() {
		return model.getExternalReferenceCode();
	}

	/**
	 * Returns the modified date of this upgrade run.
	 *
	 * @return the modified date of this upgrade run
	 */
	@Override
	public Date getModifiedDate() {
		return model.getModifiedDate();
	}

	/**
	 * Returns the mvcc version of this upgrade run.
	 *
	 * @return the mvcc version of this upgrade run
	 */
	@Override
	public long getMvccVersion() {
		return model.getMvccVersion();
	}

	/**
	 * Returns the node version of this upgrade run.
	 *
	 * @return the node version of this upgrade run
	 */
	@Override
	public String getNodeVersion() {
		return model.getNodeVersion();
	}

	/**
	 * Returns the primary key of this upgrade run.
	 *
	 * @return the primary key of this upgrade run
	 */
	@Override
	public long getPrimaryKey() {
		return model.getPrimaryKey();
	}

	/**
	 * Returns the pull request url of this upgrade run.
	 *
	 * @return the pull request url of this upgrade run
	 */
	@Override
	public String getPullRequestURL() {
		return model.getPullRequestURL();
	}

	/**
	 * Returns the repository url of this upgrade run.
	 *
	 * @return the repository url of this upgrade run
	 */
	@Override
	public String getRepositoryURL() {
		return model.getRepositoryURL();
	}

	/**
	 * Returns the result branch of this upgrade run.
	 *
	 * @return the result branch of this upgrade run
	 */
	@Override
	public String getResultBranch() {
		return model.getResultBranch();
	}

	/**
	 * Returns the search version of this upgrade run.
	 *
	 * @return the search version of this upgrade run
	 */
	@Override
	public String getSearchVersion() {
		return model.getSearchVersion();
	}

	/**
	 * Returns the start date of this upgrade run.
	 *
	 * @return the start date of this upgrade run
	 */
	@Override
	public Date getStartDate() {
		return model.getStartDate();
	}

	/**
	 * Returns the status of this upgrade run.
	 *
	 * @return the status of this upgrade run
	 */
	@Override
	public int getStatus() {
		return model.getStatus();
	}

	/**
	 * Returns the status message of this upgrade run.
	 *
	 * @return the status message of this upgrade run
	 */
	@Override
	public String getStatusMessage() {
		return model.getStatusMessage();
	}

	/**
	 * Returns the target release of this upgrade run.
	 *
	 * @return the target release of this upgrade run
	 */
	@Override
	public String getTargetRelease() {
		return model.getTargetRelease();
	}

	/**
	 * Returns the upgrade run ID of this upgrade run.
	 *
	 * @return the upgrade run ID of this upgrade run
	 */
	@Override
	public long getUpgradeRunId() {
		return model.getUpgradeRunId();
	}

	/**
	 * Returns the upgrade source version of this upgrade run.
	 *
	 * @return the upgrade source version of this upgrade run
	 */
	@Override
	public String getUpgradeSourceVersion() {
		return model.getUpgradeSourceVersion();
	}

	/**
	 * Returns the upgrade target java version of this upgrade run.
	 *
	 * @return the upgrade target java version of this upgrade run
	 */
	@Override
	public String getUpgradeTargetJavaVersion() {
		return model.getUpgradeTargetJavaVersion();
	}

	/**
	 * Returns the user ID of this upgrade run.
	 *
	 * @return the user ID of this upgrade run
	 */
	@Override
	public long getUserId() {
		return model.getUserId();
	}

	/**
	 * Returns the user name of this upgrade run.
	 *
	 * @return the user name of this upgrade run
	 */
	@Override
	public String getUserName() {
		return model.getUserName();
	}

	/**
	 * Returns the user uuid of this upgrade run.
	 *
	 * @return the user uuid of this upgrade run
	 */
	@Override
	public String getUserUuid() {
		return model.getUserUuid();
	}

	/**
	 * Returns the uuid of this upgrade run.
	 *
	 * @return the uuid of this upgrade run
	 */
	@Override
	public String getUuid() {
		return model.getUuid();
	}

	/**
	 * Returns the workspace path of this upgrade run.
	 *
	 * @return the workspace path of this upgrade run
	 */
	@Override
	public String getWorkspacePath() {
		return model.getWorkspacePath();
	}

	@Override
	public void persist() {
		model.persist();
	}

	/**
	 * Sets the branch of this upgrade run.
	 *
	 * @param branch the branch of this upgrade run
	 */
	@Override
	public void setBranch(String branch) {
		model.setBranch(branch);
	}

	/**
	 * Sets the company ID of this upgrade run.
	 *
	 * @param companyId the company ID of this upgrade run
	 */
	@Override
	public void setCompanyId(long companyId) {
		model.setCompanyId(companyId);
	}

	/**
	 * Sets the create date of this upgrade run.
	 *
	 * @param createDate the create date of this upgrade run
	 */
	@Override
	public void setCreateDate(Date createDate) {
		model.setCreateDate(createDate);
	}

	/**
	 * Sets the credential key reference of this upgrade run.
	 *
	 * @param credentialKeyReference the credential key reference of this upgrade run
	 */
	@Override
	public void setCredentialKeyReference(String credentialKeyReference) {
		model.setCredentialKeyReference(credentialKeyReference);
	}

	/**
	 * Sets the customer name of this upgrade run.
	 *
	 * @param customerName the customer name of this upgrade run
	 */
	@Override
	public void setCustomerName(String customerName) {
		model.setCustomerName(customerName);
	}

	/**
	 * Sets the db target type of this upgrade run.
	 *
	 * @param dbTargetType the db target type of this upgrade run
	 */
	@Override
	public void setDbTargetType(String dbTargetType) {
		model.setDbTargetType(dbTargetType);
	}

	/**
	 * Sets the db target version of this upgrade run.
	 *
	 * @param dbTargetVersion the db target version of this upgrade run
	 */
	@Override
	public void setDbTargetVersion(String dbTargetVersion) {
		model.setDbTargetVersion(dbTargetVersion);
	}

	/**
	 * Sets the end date of this upgrade run.
	 *
	 * @param endDate the end date of this upgrade run
	 */
	@Override
	public void setEndDate(Date endDate) {
		model.setEndDate(endDate);
	}

	/**
	 * Sets the external reference code of this upgrade run.
	 *
	 * @param externalReferenceCode the external reference code of this upgrade run
	 */
	@Override
	public void setExternalReferenceCode(String externalReferenceCode) {
		model.setExternalReferenceCode(externalReferenceCode);
	}

	/**
	 * Sets the modified date of this upgrade run.
	 *
	 * @param modifiedDate the modified date of this upgrade run
	 */
	@Override
	public void setModifiedDate(Date modifiedDate) {
		model.setModifiedDate(modifiedDate);
	}

	/**
	 * Sets the mvcc version of this upgrade run.
	 *
	 * @param mvccVersion the mvcc version of this upgrade run
	 */
	@Override
	public void setMvccVersion(long mvccVersion) {
		model.setMvccVersion(mvccVersion);
	}

	/**
	 * Sets the node version of this upgrade run.
	 *
	 * @param nodeVersion the node version of this upgrade run
	 */
	@Override
	public void setNodeVersion(String nodeVersion) {
		model.setNodeVersion(nodeVersion);
	}

	/**
	 * Sets the primary key of this upgrade run.
	 *
	 * @param primaryKey the primary key of this upgrade run
	 */
	@Override
	public void setPrimaryKey(long primaryKey) {
		model.setPrimaryKey(primaryKey);
	}

	/**
	 * Sets the pull request url of this upgrade run.
	 *
	 * @param pullRequestURL the pull request url of this upgrade run
	 */
	@Override
	public void setPullRequestURL(String pullRequestURL) {
		model.setPullRequestURL(pullRequestURL);
	}

	/**
	 * Sets the repository url of this upgrade run.
	 *
	 * @param repositoryURL the repository url of this upgrade run
	 */
	@Override
	public void setRepositoryURL(String repositoryURL) {
		model.setRepositoryURL(repositoryURL);
	}

	/**
	 * Sets the result branch of this upgrade run.
	 *
	 * @param resultBranch the result branch of this upgrade run
	 */
	@Override
	public void setResultBranch(String resultBranch) {
		model.setResultBranch(resultBranch);
	}

	/**
	 * Sets the search version of this upgrade run.
	 *
	 * @param searchVersion the search version of this upgrade run
	 */
	@Override
	public void setSearchVersion(String searchVersion) {
		model.setSearchVersion(searchVersion);
	}

	/**
	 * Sets the start date of this upgrade run.
	 *
	 * @param startDate the start date of this upgrade run
	 */
	@Override
	public void setStartDate(Date startDate) {
		model.setStartDate(startDate);
	}

	/**
	 * Sets the status of this upgrade run.
	 *
	 * @param status the status of this upgrade run
	 */
	@Override
	public void setStatus(int status) {
		model.setStatus(status);
	}

	/**
	 * Sets the status message of this upgrade run.
	 *
	 * @param statusMessage the status message of this upgrade run
	 */
	@Override
	public void setStatusMessage(String statusMessage) {
		model.setStatusMessage(statusMessage);
	}

	/**
	 * Sets the target release of this upgrade run.
	 *
	 * @param targetRelease the target release of this upgrade run
	 */
	@Override
	public void setTargetRelease(String targetRelease) {
		model.setTargetRelease(targetRelease);
	}

	/**
	 * Sets the upgrade run ID of this upgrade run.
	 *
	 * @param upgradeRunId the upgrade run ID of this upgrade run
	 */
	@Override
	public void setUpgradeRunId(long upgradeRunId) {
		model.setUpgradeRunId(upgradeRunId);
	}

	/**
	 * Sets the upgrade source version of this upgrade run.
	 *
	 * @param upgradeSourceVersion the upgrade source version of this upgrade run
	 */
	@Override
	public void setUpgradeSourceVersion(String upgradeSourceVersion) {
		model.setUpgradeSourceVersion(upgradeSourceVersion);
	}

	/**
	 * Sets the upgrade target java version of this upgrade run.
	 *
	 * @param upgradeTargetJavaVersion the upgrade target java version of this upgrade run
	 */
	@Override
	public void setUpgradeTargetJavaVersion(String upgradeTargetJavaVersion) {
		model.setUpgradeTargetJavaVersion(upgradeTargetJavaVersion);
	}

	/**
	 * Sets the user ID of this upgrade run.
	 *
	 * @param userId the user ID of this upgrade run
	 */
	@Override
	public void setUserId(long userId) {
		model.setUserId(userId);
	}

	/**
	 * Sets the user name of this upgrade run.
	 *
	 * @param userName the user name of this upgrade run
	 */
	@Override
	public void setUserName(String userName) {
		model.setUserName(userName);
	}

	/**
	 * Sets the user uuid of this upgrade run.
	 *
	 * @param userUuid the user uuid of this upgrade run
	 */
	@Override
	public void setUserUuid(String userUuid) {
		model.setUserUuid(userUuid);
	}

	/**
	 * Sets the uuid of this upgrade run.
	 *
	 * @param uuid the uuid of this upgrade run
	 */
	@Override
	public void setUuid(String uuid) {
		model.setUuid(uuid);
	}

	/**
	 * Sets the workspace path of this upgrade run.
	 *
	 * @param workspacePath the workspace path of this upgrade run
	 */
	@Override
	public void setWorkspacePath(String workspacePath) {
		model.setWorkspacePath(workspacePath);
	}

	@Override
	public String toXmlString() {
		return model.toXmlString();
	}

	@Override
	public StagedModelType getStagedModelType() {
		return model.getStagedModelType();
	}

	@Override
	protected UpgradeRunWrapper wrap(UpgradeRun upgradeRun) {
		return new UpgradeRunWrapper(upgradeRun);
	}

}
// LIFERAY-SERVICE-BUILDER-HASH:-1632013597