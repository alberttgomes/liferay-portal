/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.upgrades.lab.agent.remote.model.impl;

import com.liferay.petra.lang.HashUtil;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.model.CacheModel;
import com.liferay.portal.kernel.model.MVCCModel;
import com.liferay.upgrades.lab.agent.remote.model.UpgradeRun;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import java.util.Date;

/**
 * The cache model class for representing UpgradeRun in entity cache.
 *
 * @author Albert Gomes Cabral
 * @generated
 */
public class UpgradeRunCacheModel
	implements CacheModel<UpgradeRun>, Externalizable, MVCCModel {

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}

		if (!(object instanceof UpgradeRunCacheModel)) {
			return false;
		}

		UpgradeRunCacheModel upgradeRunCacheModel =
			(UpgradeRunCacheModel)object;

		if ((upgradeRunId == upgradeRunCacheModel.upgradeRunId) &&
			(mvccVersion == upgradeRunCacheModel.mvccVersion)) {

			return true;
		}

		return false;
	}

	@Override
	public int hashCode() {
		int hashCode = HashUtil.hash(0, upgradeRunId);

		return HashUtil.hash(hashCode, mvccVersion);
	}

	@Override
	public long getMvccVersion() {
		return mvccVersion;
	}

	@Override
	public void setMvccVersion(long mvccVersion) {
		this.mvccVersion = mvccVersion;
	}

	@Override
	public String toString() {
		StringBundler sb = new StringBundler(55);

		sb.append("{mvccVersion=");
		sb.append(mvccVersion);
		sb.append(", uuid=");
		sb.append(uuid);
		sb.append(", externalReferenceCode=");
		sb.append(externalReferenceCode);
		sb.append(", upgradeRunId=");
		sb.append(upgradeRunId);
		sb.append(", companyId=");
		sb.append(companyId);
		sb.append(", userId=");
		sb.append(userId);
		sb.append(", userName=");
		sb.append(userName);
		sb.append(", createDate=");
		sb.append(createDate);
		sb.append(", modifiedDate=");
		sb.append(modifiedDate);
		sb.append(", branch=");
		sb.append(branch);
		sb.append(", credentialKeyReference=");
		sb.append(credentialKeyReference);
		sb.append(", customerName=");
		sb.append(customerName);
		sb.append(", dbTargetType=");
		sb.append(dbTargetType);
		sb.append(", dbTargetVersion=");
		sb.append(dbTargetVersion);
		sb.append(", endDate=");
		sb.append(endDate);
		sb.append(", nodeVersion=");
		sb.append(nodeVersion);
		sb.append(", pullRequestURL=");
		sb.append(pullRequestURL);
		sb.append(", repositoryURL=");
		sb.append(repositoryURL);
		sb.append(", resultBranch=");
		sb.append(resultBranch);
		sb.append(", searchVersion=");
		sb.append(searchVersion);
		sb.append(", startDate=");
		sb.append(startDate);
		sb.append(", targetRelease=");
		sb.append(targetRelease);
		sb.append(", upgradeSourceVersion=");
		sb.append(upgradeSourceVersion);
		sb.append(", upgradeTargetJavaVersion=");
		sb.append(upgradeTargetJavaVersion);
		sb.append(", workspacePath=");
		sb.append(workspacePath);
		sb.append(", status=");
		sb.append(status);
		sb.append(", statusMessage=");
		sb.append(statusMessage);
		sb.append("}");

		return sb.toString();
	}

	@Override
	public UpgradeRun toEntityModel() {
		UpgradeRunImpl upgradeRunImpl = new UpgradeRunImpl();

		upgradeRunImpl.setMvccVersion(mvccVersion);

		if (uuid == null) {
			upgradeRunImpl.setUuid("");
		}
		else {
			upgradeRunImpl.setUuid(uuid);
		}

		if (externalReferenceCode == null) {
			upgradeRunImpl.setExternalReferenceCode("");
		}
		else {
			upgradeRunImpl.setExternalReferenceCode(externalReferenceCode);
		}

		upgradeRunImpl.setUpgradeRunId(upgradeRunId);
		upgradeRunImpl.setCompanyId(companyId);
		upgradeRunImpl.setUserId(userId);

		if (userName == null) {
			upgradeRunImpl.setUserName("");
		}
		else {
			upgradeRunImpl.setUserName(userName);
		}

		if (createDate == Long.MIN_VALUE) {
			upgradeRunImpl.setCreateDate(null);
		}
		else {
			upgradeRunImpl.setCreateDate(new Date(createDate));
		}

		if (modifiedDate == Long.MIN_VALUE) {
			upgradeRunImpl.setModifiedDate(null);
		}
		else {
			upgradeRunImpl.setModifiedDate(new Date(modifiedDate));
		}

		if (branch == null) {
			upgradeRunImpl.setBranch("");
		}
		else {
			upgradeRunImpl.setBranch(branch);
		}

		if (credentialKeyReference == null) {
			upgradeRunImpl.setCredentialKeyReference("");
		}
		else {
			upgradeRunImpl.setCredentialKeyReference(credentialKeyReference);
		}

		if (customerName == null) {
			upgradeRunImpl.setCustomerName("");
		}
		else {
			upgradeRunImpl.setCustomerName(customerName);
		}

		if (dbTargetType == null) {
			upgradeRunImpl.setDbTargetType("");
		}
		else {
			upgradeRunImpl.setDbTargetType(dbTargetType);
		}

		if (dbTargetVersion == null) {
			upgradeRunImpl.setDbTargetVersion("");
		}
		else {
			upgradeRunImpl.setDbTargetVersion(dbTargetVersion);
		}

		if (endDate == Long.MIN_VALUE) {
			upgradeRunImpl.setEndDate(null);
		}
		else {
			upgradeRunImpl.setEndDate(new Date(endDate));
		}

		if (nodeVersion == null) {
			upgradeRunImpl.setNodeVersion("");
		}
		else {
			upgradeRunImpl.setNodeVersion(nodeVersion);
		}

		if (pullRequestURL == null) {
			upgradeRunImpl.setPullRequestURL("");
		}
		else {
			upgradeRunImpl.setPullRequestURL(pullRequestURL);
		}

		if (repositoryURL == null) {
			upgradeRunImpl.setRepositoryURL("");
		}
		else {
			upgradeRunImpl.setRepositoryURL(repositoryURL);
		}

		if (resultBranch == null) {
			upgradeRunImpl.setResultBranch("");
		}
		else {
			upgradeRunImpl.setResultBranch(resultBranch);
		}

		if (searchVersion == null) {
			upgradeRunImpl.setSearchVersion("");
		}
		else {
			upgradeRunImpl.setSearchVersion(searchVersion);
		}

		if (startDate == Long.MIN_VALUE) {
			upgradeRunImpl.setStartDate(null);
		}
		else {
			upgradeRunImpl.setStartDate(new Date(startDate));
		}

		if (targetRelease == null) {
			upgradeRunImpl.setTargetRelease("");
		}
		else {
			upgradeRunImpl.setTargetRelease(targetRelease);
		}

		if (upgradeSourceVersion == null) {
			upgradeRunImpl.setUpgradeSourceVersion("");
		}
		else {
			upgradeRunImpl.setUpgradeSourceVersion(upgradeSourceVersion);
		}

		if (upgradeTargetJavaVersion == null) {
			upgradeRunImpl.setUpgradeTargetJavaVersion("");
		}
		else {
			upgradeRunImpl.setUpgradeTargetJavaVersion(
				upgradeTargetJavaVersion);
		}

		if (workspacePath == null) {
			upgradeRunImpl.setWorkspacePath("");
		}
		else {
			upgradeRunImpl.setWorkspacePath(workspacePath);
		}

		upgradeRunImpl.setStatus(status);
		upgradeRunImpl.setStatusMessage(statusMessage);

		upgradeRunImpl.resetOriginalValues();

		return upgradeRunImpl;
	}

	@Override
	public void readExternal(ObjectInput objectInput)
		throws ClassNotFoundException, IOException {

		mvccVersion = objectInput.readLong();
		uuid = objectInput.readUTF();
		externalReferenceCode = objectInput.readUTF();

		upgradeRunId = objectInput.readLong();

		companyId = objectInput.readLong();

		userId = objectInput.readLong();
		userName = objectInput.readUTF();
		createDate = objectInput.readLong();
		modifiedDate = objectInput.readLong();
		branch = objectInput.readUTF();
		credentialKeyReference = objectInput.readUTF();
		customerName = objectInput.readUTF();
		dbTargetType = objectInput.readUTF();
		dbTargetVersion = objectInput.readUTF();
		endDate = objectInput.readLong();
		nodeVersion = objectInput.readUTF();
		pullRequestURL = objectInput.readUTF();
		repositoryURL = objectInput.readUTF();
		resultBranch = objectInput.readUTF();
		searchVersion = objectInput.readUTF();
		startDate = objectInput.readLong();
		targetRelease = objectInput.readUTF();
		upgradeSourceVersion = objectInput.readUTF();
		upgradeTargetJavaVersion = objectInput.readUTF();
		workspacePath = objectInput.readUTF();

		status = objectInput.readInt();
		statusMessage = (String)objectInput.readObject();
	}

	@Override
	public void writeExternal(ObjectOutput objectOutput) throws IOException {
		objectOutput.writeLong(mvccVersion);

		if (uuid == null) {
			objectOutput.writeUTF("");
		}
		else {
			objectOutput.writeUTF(uuid);
		}

		if (externalReferenceCode == null) {
			objectOutput.writeUTF("");
		}
		else {
			objectOutput.writeUTF(externalReferenceCode);
		}

		objectOutput.writeLong(upgradeRunId);

		objectOutput.writeLong(companyId);

		objectOutput.writeLong(userId);

		if (userName == null) {
			objectOutput.writeUTF("");
		}
		else {
			objectOutput.writeUTF(userName);
		}

		objectOutput.writeLong(createDate);
		objectOutput.writeLong(modifiedDate);

		if (branch == null) {
			objectOutput.writeUTF("");
		}
		else {
			objectOutput.writeUTF(branch);
		}

		if (credentialKeyReference == null) {
			objectOutput.writeUTF("");
		}
		else {
			objectOutput.writeUTF(credentialKeyReference);
		}

		if (customerName == null) {
			objectOutput.writeUTF("");
		}
		else {
			objectOutput.writeUTF(customerName);
		}

		if (dbTargetType == null) {
			objectOutput.writeUTF("");
		}
		else {
			objectOutput.writeUTF(dbTargetType);
		}

		if (dbTargetVersion == null) {
			objectOutput.writeUTF("");
		}
		else {
			objectOutput.writeUTF(dbTargetVersion);
		}

		objectOutput.writeLong(endDate);

		if (nodeVersion == null) {
			objectOutput.writeUTF("");
		}
		else {
			objectOutput.writeUTF(nodeVersion);
		}

		if (pullRequestURL == null) {
			objectOutput.writeUTF("");
		}
		else {
			objectOutput.writeUTF(pullRequestURL);
		}

		if (repositoryURL == null) {
			objectOutput.writeUTF("");
		}
		else {
			objectOutput.writeUTF(repositoryURL);
		}

		if (resultBranch == null) {
			objectOutput.writeUTF("");
		}
		else {
			objectOutput.writeUTF(resultBranch);
		}

		if (searchVersion == null) {
			objectOutput.writeUTF("");
		}
		else {
			objectOutput.writeUTF(searchVersion);
		}

		objectOutput.writeLong(startDate);

		if (targetRelease == null) {
			objectOutput.writeUTF("");
		}
		else {
			objectOutput.writeUTF(targetRelease);
		}

		if (upgradeSourceVersion == null) {
			objectOutput.writeUTF("");
		}
		else {
			objectOutput.writeUTF(upgradeSourceVersion);
		}

		if (upgradeTargetJavaVersion == null) {
			objectOutput.writeUTF("");
		}
		else {
			objectOutput.writeUTF(upgradeTargetJavaVersion);
		}

		if (workspacePath == null) {
			objectOutput.writeUTF("");
		}
		else {
			objectOutput.writeUTF(workspacePath);
		}

		objectOutput.writeInt(status);

		if (statusMessage == null) {
			objectOutput.writeObject("");
		}
		else {
			objectOutput.writeObject(statusMessage);
		}
	}

	public long mvccVersion;
	public String uuid;
	public String externalReferenceCode;
	public long upgradeRunId;
	public long companyId;
	public long userId;
	public String userName;
	public long createDate;
	public long modifiedDate;
	public String branch;
	public String credentialKeyReference;
	public String customerName;
	public String dbTargetType;
	public String dbTargetVersion;
	public long endDate;
	public String nodeVersion;
	public String pullRequestURL;
	public String repositoryURL;
	public String resultBranch;
	public String searchVersion;
	public long startDate;
	public String targetRelease;
	public String upgradeSourceVersion;
	public String upgradeTargetJavaVersion;
	public String workspacePath;
	public int status;
	public String statusMessage;

}
// LIFERAY-SERVICE-BUILDER-HASH:-32876054