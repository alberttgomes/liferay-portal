/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.upgrades.lab.agent.remote.rest.client.dto.v1_0;

import com.liferay.upgrades.lab.agent.remote.rest.client.function.UnsafeSupplier;
import com.liferay.upgrades.lab.agent.remote.rest.client.serdes.v1_0.UpgradeRunSerDes;

import jakarta.annotation.Generated;

import java.io.Serializable;

import java.util.Objects;

/**
 * @author Albert Gomes Cabral
 * @generated
 */
@Generated("")
public class UpgradeRun implements Cloneable, Serializable {

	public static UpgradeRun toDTO(String json) {
		return UpgradeRunSerDes.toDTO(json);
	}

	public String getBranch() {
		return branch;
	}

	public void setBranch(String branch) {
		this.branch = branch;
	}

	public void setBranch(
		UnsafeSupplier<String, Exception> branchUnsafeSupplier) {

		try {
			branch = branchUnsafeSupplier.get();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected String branch;

	public String getCredentialKeyReference() {
		return credentialKeyReference;
	}

	public void setCredentialKeyReference(String credentialKeyReference) {
		this.credentialKeyReference = credentialKeyReference;
	}

	public void setCredentialKeyReference(
		UnsafeSupplier<String, Exception>
			credentialKeyReferenceUnsafeSupplier) {

		try {
			credentialKeyReference = credentialKeyReferenceUnsafeSupplier.get();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected String credentialKeyReference;

	public String getCustomerName() {
		return customerName;
	}

	public void setCustomerName(String customerName) {
		this.customerName = customerName;
	}

	public void setCustomerName(
		UnsafeSupplier<String, Exception> customerNameUnsafeSupplier) {

		try {
			customerName = customerNameUnsafeSupplier.get();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected String customerName;

	public String getDbTargetType() {
		return dbTargetType;
	}

	public void setDbTargetType(String dbTargetType) {
		this.dbTargetType = dbTargetType;
	}

	public void setDbTargetType(
		UnsafeSupplier<String, Exception> dbTargetTypeUnsafeSupplier) {

		try {
			dbTargetType = dbTargetTypeUnsafeSupplier.get();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected String dbTargetType;

	public String getDbTargetVersion() {
		return dbTargetVersion;
	}

	public void setDbTargetVersion(String dbTargetVersion) {
		this.dbTargetVersion = dbTargetVersion;
	}

	public void setDbTargetVersion(
		UnsafeSupplier<String, Exception> dbTargetVersionUnsafeSupplier) {

		try {
			dbTargetVersion = dbTargetVersionUnsafeSupplier.get();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected String dbTargetVersion;

	public String getExternalReferenceCode() {
		return externalReferenceCode;
	}

	public void setExternalReferenceCode(String externalReferenceCode) {
		this.externalReferenceCode = externalReferenceCode;
	}

	public void setExternalReferenceCode(
		UnsafeSupplier<String, Exception> externalReferenceCodeUnsafeSupplier) {

		try {
			externalReferenceCode = externalReferenceCodeUnsafeSupplier.get();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected String externalReferenceCode;

	public String getNodeVersion() {
		return nodeVersion;
	}

	public void setNodeVersion(String nodeVersion) {
		this.nodeVersion = nodeVersion;
	}

	public void setNodeVersion(
		UnsafeSupplier<String, Exception> nodeVersionUnsafeSupplier) {

		try {
			nodeVersion = nodeVersionUnsafeSupplier.get();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected String nodeVersion;

	public String getPullRequestURL() {
		return pullRequestURL;
	}

	public void setPullRequestURL(String pullRequestURL) {
		this.pullRequestURL = pullRequestURL;
	}

	public void setPullRequestURL(
		UnsafeSupplier<String, Exception> pullRequestURLUnsafeSupplier) {

		try {
			pullRequestURL = pullRequestURLUnsafeSupplier.get();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected String pullRequestURL;

	public String getRepositoryURL() {
		return repositoryURL;
	}

	public void setRepositoryURL(String repositoryURL) {
		this.repositoryURL = repositoryURL;
	}

	public void setRepositoryURL(
		UnsafeSupplier<String, Exception> repositoryURLUnsafeSupplier) {

		try {
			repositoryURL = repositoryURLUnsafeSupplier.get();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected String repositoryURL;

	public String getResultBranch() {
		return resultBranch;
	}

	public void setResultBranch(String resultBranch) {
		this.resultBranch = resultBranch;
	}

	public void setResultBranch(
		UnsafeSupplier<String, Exception> resultBranchUnsafeSupplier) {

		try {
			resultBranch = resultBranchUnsafeSupplier.get();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected String resultBranch;

	public String getSearchVersion() {
		return searchVersion;
	}

	public void setSearchVersion(String searchVersion) {
		this.searchVersion = searchVersion;
	}

	public void setSearchVersion(
		UnsafeSupplier<String, Exception> searchVersionUnsafeSupplier) {

		try {
			searchVersion = searchVersionUnsafeSupplier.get();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected String searchVersion;

	public Status getStatus() {
		return status;
	}

	public String getStatusAsString() {
		if (status == null) {
			return null;
		}

		return status.toString();
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public void setStatus(
		UnsafeSupplier<Status, Exception> statusUnsafeSupplier) {

		try {
			status = statusUnsafeSupplier.get();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected Status status;

	public String getStatusMessage() {
		return statusMessage;
	}

	public void setStatusMessage(String statusMessage) {
		this.statusMessage = statusMessage;
	}

	public void setStatusMessage(
		UnsafeSupplier<String, Exception> statusMessageUnsafeSupplier) {

		try {
			statusMessage = statusMessageUnsafeSupplier.get();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected String statusMessage;

	public String getTargetRelease() {
		return targetRelease;
	}

	public void setTargetRelease(String targetRelease) {
		this.targetRelease = targetRelease;
	}

	public void setTargetRelease(
		UnsafeSupplier<String, Exception> targetReleaseUnsafeSupplier) {

		try {
			targetRelease = targetReleaseUnsafeSupplier.get();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected String targetRelease;

	public Long getUpgradeRunId() {
		return upgradeRunId;
	}

	public void setUpgradeRunId(Long upgradeRunId) {
		this.upgradeRunId = upgradeRunId;
	}

	public void setUpgradeRunId(
		UnsafeSupplier<Long, Exception> upgradeRunIdUnsafeSupplier) {

		try {
			upgradeRunId = upgradeRunIdUnsafeSupplier.get();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected Long upgradeRunId;

	public String getUpgradeSourceVersion() {
		return upgradeSourceVersion;
	}

	public void setUpgradeSourceVersion(String upgradeSourceVersion) {
		this.upgradeSourceVersion = upgradeSourceVersion;
	}

	public void setUpgradeSourceVersion(
		UnsafeSupplier<String, Exception> upgradeSourceVersionUnsafeSupplier) {

		try {
			upgradeSourceVersion = upgradeSourceVersionUnsafeSupplier.get();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected String upgradeSourceVersion;

	public String getUpgradeTargetJavaVersion() {
		return upgradeTargetJavaVersion;
	}

	public void setUpgradeTargetJavaVersion(String upgradeTargetJavaVersion) {
		this.upgradeTargetJavaVersion = upgradeTargetJavaVersion;
	}

	public void setUpgradeTargetJavaVersion(
		UnsafeSupplier<String, Exception>
			upgradeTargetJavaVersionUnsafeSupplier) {

		try {
			upgradeTargetJavaVersion =
				upgradeTargetJavaVersionUnsafeSupplier.get();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected String upgradeTargetJavaVersion;

	public String getWorkspacePath() {
		return workspacePath;
	}

	public void setWorkspacePath(String workspacePath) {
		this.workspacePath = workspacePath;
	}

	public void setWorkspacePath(
		UnsafeSupplier<String, Exception> workspacePathUnsafeSupplier) {

		try {
			workspacePath = workspacePathUnsafeSupplier.get();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected String workspacePath;

	@Override
	public UpgradeRun clone() throws CloneNotSupportedException {
		return (UpgradeRun)super.clone();
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}

		if (!(object instanceof UpgradeRun)) {
			return false;
		}

		UpgradeRun upgradeRun = (UpgradeRun)object;

		return Objects.equals(toString(), upgradeRun.toString());
	}

	@Override
	public int hashCode() {
		String string = toString();

		return string.hashCode();
	}

	public String toString() {
		return UpgradeRunSerDes.toJSON(this);
	}

	public static enum Status {

		BLOCKED("blocked"), CANCELLED("cancelled"), CLONING("cloning"),
		FAILED("failed"), PROVISIONING("provisioning"),
		PUBLISHING("publishing"), QUEUED("queued"), RUNNING("running"),
		SUCCESSFUL("successful"), TIMED_OUT("timed-out"),
		VALIDATING("validating");

		public static Status create(String value) {
			for (Status status : values()) {
				if (Objects.equals(status.getValue(), value) ||
					Objects.equals(status.name(), value)) {

					return status;
				}
			}

			return null;
		}

		public String getValue() {
			return _value;
		}

		@Override
		public String toString() {
			return _value;
		}

		private Status(String value) {
			_value = value;
		}

		private final String _value;

	}

}
// LIFERAY-REST-BUILDER-HASH:-1757813640