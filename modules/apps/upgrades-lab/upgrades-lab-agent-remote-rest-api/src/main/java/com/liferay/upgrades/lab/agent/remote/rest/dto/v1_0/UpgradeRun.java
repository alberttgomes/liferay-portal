/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.upgrades.lab.agent.remote.rest.dto.v1_0;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import com.liferay.petra.function.UnsafeSupplier;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.vulcan.graphql.annotation.GraphQLField;
import com.liferay.portal.vulcan.graphql.annotation.GraphQLName;
import com.liferay.portal.vulcan.util.ObjectMapperUtil;

import jakarta.annotation.Generated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import jakarta.xml.bind.annotation.XmlRootElement;

import java.io.Serializable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

/**
 * @author Albert Gomes Cabral
 * @generated
 */
@Generated("")
@GraphQLName(
	description = "One upgrade run as the runner knows it. The result branch and the pull request URL stay empty until the run reaches publishing.",
	value = "UpgradeRun"
)
@io.swagger.v3.oas.annotations.media.Schema(
	description = "One upgrade run as the runner knows it. The result branch and the pull request URL stay empty until the run reaches publishing.",
	requiredProperties = {
		"branch", "customerName", "dbTargetType", "dbTargetVersion",
		"nodeVersion", "repositoryURL", "searchVersion", "targetRelease",
		"upgradeSourceVersion", "upgradeTargetJavaVersion"
	}
)
@JsonFilter("Liferay.Vulcan")
@XmlRootElement(name = "UpgradeRun")
public class UpgradeRun implements Serializable {

	public static UpgradeRun toDTO(String json) {
		return ObjectMapperUtil.readValue(UpgradeRun.class, json);
	}

	public static UpgradeRun unsafeToDTO(String json) {
		return ObjectMapperUtil.unsafeReadValue(UpgradeRun.class, json);
	}

	@io.swagger.v3.oas.annotations.media.Schema(
		description = "The branch of the repository to upgrade."
	)
	public String getBranch() {
		if (_branchSupplier != null) {
			branch = _branchSupplier.get();

			_branchSupplier = null;
		}

		return branch;
	}

	public void setBranch(String branch) {
		this.branch = branch;

		_branchSupplier = null;
	}

	@JsonIgnore
	public void setBranch(
		UnsafeSupplier<String, Exception> branchUnsafeSupplier) {

		_branchSupplier = () -> {
			try {
				return branchUnsafeSupplier.get();
			}
			catch (RuntimeException runtimeException) {
				throw runtimeException;
			}
			catch (Exception exception) {
				throw new RuntimeException(exception);
			}
		};
	}

	@GraphQLField(description = "The branch of the repository to upgrade.")
	@JsonProperty(access = JsonProperty.Access.READ_WRITE)
	@NotEmpty
	protected String branch;

	@JsonIgnore
	private Supplier<String> _branchSupplier;

	@io.swagger.v3.oas.annotations.media.Schema(
		description = "The key reference of the private key the runner reaches the repository with. Leave it out when the repository needs no credential."
	)
	public String getCredentialKeyReference() {
		if (_credentialKeyReferenceSupplier != null) {
			credentialKeyReference = _credentialKeyReferenceSupplier.get();

			_credentialKeyReferenceSupplier = null;
		}

		return credentialKeyReference;
	}

	public void setCredentialKeyReference(String credentialKeyReference) {
		this.credentialKeyReference = credentialKeyReference;

		_credentialKeyReferenceSupplier = null;
	}

	@JsonIgnore
	public void setCredentialKeyReference(
		UnsafeSupplier<String, Exception>
			credentialKeyReferenceUnsafeSupplier) {

		_credentialKeyReferenceSupplier = () -> {
			try {
				return credentialKeyReferenceUnsafeSupplier.get();
			}
			catch (RuntimeException runtimeException) {
				throw runtimeException;
			}
			catch (Exception exception) {
				throw new RuntimeException(exception);
			}
		};
	}

	@GraphQLField(
		description = "The key reference of the private key the runner reaches the repository with. Leave it out when the repository needs no credential."
	)
	@JsonProperty(access = JsonProperty.Access.READ_WRITE)
	protected String credentialKeyReference;

	@JsonIgnore
	private Supplier<String> _credentialKeyReferenceSupplier;

	@io.swagger.v3.oas.annotations.media.Schema(
		description = "The short customer identifier the agent names reports and artifacts with."
	)
	public String getCustomerName() {
		if (_customerNameSupplier != null) {
			customerName = _customerNameSupplier.get();

			_customerNameSupplier = null;
		}

		return customerName;
	}

	public void setCustomerName(String customerName) {
		this.customerName = customerName;

		_customerNameSupplier = null;
	}

	@JsonIgnore
	public void setCustomerName(
		UnsafeSupplier<String, Exception> customerNameUnsafeSupplier) {

		_customerNameSupplier = () -> {
			try {
				return customerNameUnsafeSupplier.get();
			}
			catch (RuntimeException runtimeException) {
				throw runtimeException;
			}
			catch (Exception exception) {
				throw new RuntimeException(exception);
			}
		};
	}

	@GraphQLField(
		description = "The short customer identifier the agent names reports and artifacts with."
	)
	@JsonProperty(access = JsonProperty.Access.READ_WRITE)
	@NotEmpty
	protected String customerName;

	@JsonIgnore
	private Supplier<String> _customerNameSupplier;

	@io.swagger.v3.oas.annotations.media.Schema(
		description = "The database type the upgraded workspace runs against, such as mysql or postgresql."
	)
	public String getDbTargetType() {
		if (_dbTargetTypeSupplier != null) {
			dbTargetType = _dbTargetTypeSupplier.get();

			_dbTargetTypeSupplier = null;
		}

		return dbTargetType;
	}

	public void setDbTargetType(String dbTargetType) {
		this.dbTargetType = dbTargetType;

		_dbTargetTypeSupplier = null;
	}

	@JsonIgnore
	public void setDbTargetType(
		UnsafeSupplier<String, Exception> dbTargetTypeUnsafeSupplier) {

		_dbTargetTypeSupplier = () -> {
			try {
				return dbTargetTypeUnsafeSupplier.get();
			}
			catch (RuntimeException runtimeException) {
				throw runtimeException;
			}
			catch (Exception exception) {
				throw new RuntimeException(exception);
			}
		};
	}

	@GraphQLField(
		description = "The database type the upgraded workspace runs against, such as mysql or postgresql."
	)
	@JsonProperty(access = JsonProperty.Access.READ_WRITE)
	@NotEmpty
	protected String dbTargetType;

	@JsonIgnore
	private Supplier<String> _dbTargetTypeSupplier;

	@io.swagger.v3.oas.annotations.media.Schema(
		description = "The database version the upgraded workspace runs against, such as 8.0."
	)
	public String getDbTargetVersion() {
		if (_dbTargetVersionSupplier != null) {
			dbTargetVersion = _dbTargetVersionSupplier.get();

			_dbTargetVersionSupplier = null;
		}

		return dbTargetVersion;
	}

	public void setDbTargetVersion(String dbTargetVersion) {
		this.dbTargetVersion = dbTargetVersion;

		_dbTargetVersionSupplier = null;
	}

	@JsonIgnore
	public void setDbTargetVersion(
		UnsafeSupplier<String, Exception> dbTargetVersionUnsafeSupplier) {

		_dbTargetVersionSupplier = () -> {
			try {
				return dbTargetVersionUnsafeSupplier.get();
			}
			catch (RuntimeException runtimeException) {
				throw runtimeException;
			}
			catch (Exception exception) {
				throw new RuntimeException(exception);
			}
		};
	}

	@GraphQLField(
		description = "The database version the upgraded workspace runs against, such as 8.0."
	)
	@JsonProperty(access = JsonProperty.Access.READ_WRITE)
	@NotEmpty
	protected String dbTargetVersion;

	@JsonIgnore
	private Supplier<String> _dbTargetVersionSupplier;

	@io.swagger.v3.oas.annotations.media.Schema(
		description = "The identifier the runner knows the run by. Read only."
	)
	public String getExternalReferenceCode() {
		if (_externalReferenceCodeSupplier != null) {
			externalReferenceCode = _externalReferenceCodeSupplier.get();

			_externalReferenceCodeSupplier = null;
		}

		return externalReferenceCode;
	}

	public void setExternalReferenceCode(String externalReferenceCode) {
		this.externalReferenceCode = externalReferenceCode;

		_externalReferenceCodeSupplier = null;
	}

	@JsonIgnore
	public void setExternalReferenceCode(
		UnsafeSupplier<String, Exception> externalReferenceCodeUnsafeSupplier) {

		_externalReferenceCodeSupplier = () -> {
			try {
				return externalReferenceCodeUnsafeSupplier.get();
			}
			catch (RuntimeException runtimeException) {
				throw runtimeException;
			}
			catch (Exception exception) {
				throw new RuntimeException(exception);
			}
		};
	}

	@GraphQLField(
		description = "The identifier the runner knows the run by. Read only."
	)
	@JsonProperty(access = JsonProperty.Access.READ_ONLY)
	protected String externalReferenceCode;

	@JsonIgnore
	private Supplier<String> _externalReferenceCodeSupplier;

	@io.swagger.v3.oas.annotations.media.Schema(
		description = "The concrete Node.js patch version for the upgraded workspace, such as 20.18.0. The agent feeds it into a Docker image tag verbatim, so a range does not resolve."
	)
	public String getNodeVersion() {
		if (_nodeVersionSupplier != null) {
			nodeVersion = _nodeVersionSupplier.get();

			_nodeVersionSupplier = null;
		}

		return nodeVersion;
	}

	public void setNodeVersion(String nodeVersion) {
		this.nodeVersion = nodeVersion;

		_nodeVersionSupplier = null;
	}

	@JsonIgnore
	public void setNodeVersion(
		UnsafeSupplier<String, Exception> nodeVersionUnsafeSupplier) {

		_nodeVersionSupplier = () -> {
			try {
				return nodeVersionUnsafeSupplier.get();
			}
			catch (RuntimeException runtimeException) {
				throw runtimeException;
			}
			catch (Exception exception) {
				throw new RuntimeException(exception);
			}
		};
	}

	@GraphQLField(
		description = "The concrete Node.js patch version for the upgraded workspace, such as 20.18.0. The agent feeds it into a Docker image tag verbatim, so a range does not resolve."
	)
	@JsonProperty(access = JsonProperty.Access.READ_WRITE)
	@NotEmpty
	protected String nodeVersion;

	@JsonIgnore
	private Supplier<String> _nodeVersionSupplier;

	@io.swagger.v3.oas.annotations.media.Schema(
		description = "The pull request the run published its result to. Read only."
	)
	public String getPullRequestURL() {
		if (_pullRequestURLSupplier != null) {
			pullRequestURL = _pullRequestURLSupplier.get();

			_pullRequestURLSupplier = null;
		}

		return pullRequestURL;
	}

	public void setPullRequestURL(String pullRequestURL) {
		this.pullRequestURL = pullRequestURL;

		_pullRequestURLSupplier = null;
	}

	@JsonIgnore
	public void setPullRequestURL(
		UnsafeSupplier<String, Exception> pullRequestURLUnsafeSupplier) {

		_pullRequestURLSupplier = () -> {
			try {
				return pullRequestURLUnsafeSupplier.get();
			}
			catch (RuntimeException runtimeException) {
				throw runtimeException;
			}
			catch (Exception exception) {
				throw new RuntimeException(exception);
			}
		};
	}

	@GraphQLField(
		description = "The pull request the run published its result to. Read only."
	)
	@JsonProperty(access = JsonProperty.Access.READ_ONLY)
	protected String pullRequestURL;

	@JsonIgnore
	private Supplier<String> _pullRequestURLSupplier;

	@io.swagger.v3.oas.annotations.media.Schema(
		description = "The URL of the repository to upgrade. An SSH URL needs a credential key reference. An HTTPS or file URL of a repository Git can reach anonymously needs none."
	)
	public String getRepositoryURL() {
		if (_repositoryURLSupplier != null) {
			repositoryURL = _repositoryURLSupplier.get();

			_repositoryURLSupplier = null;
		}

		return repositoryURL;
	}

	public void setRepositoryURL(String repositoryURL) {
		this.repositoryURL = repositoryURL;

		_repositoryURLSupplier = null;
	}

	@JsonIgnore
	public void setRepositoryURL(
		UnsafeSupplier<String, Exception> repositoryURLUnsafeSupplier) {

		_repositoryURLSupplier = () -> {
			try {
				return repositoryURLUnsafeSupplier.get();
			}
			catch (RuntimeException runtimeException) {
				throw runtimeException;
			}
			catch (Exception exception) {
				throw new RuntimeException(exception);
			}
		};
	}

	@GraphQLField(
		description = "The URL of the repository to upgrade. An SSH URL needs a credential key reference. An HTTPS or file URL of a repository Git can reach anonymously needs none."
	)
	@JsonProperty(access = JsonProperty.Access.READ_WRITE)
	@NotEmpty
	protected String repositoryURL;

	@JsonIgnore
	private Supplier<String> _repositoryURLSupplier;

	@io.swagger.v3.oas.annotations.media.Schema(
		description = "The branch the run pushed its result to. Read only."
	)
	public String getResultBranch() {
		if (_resultBranchSupplier != null) {
			resultBranch = _resultBranchSupplier.get();

			_resultBranchSupplier = null;
		}

		return resultBranch;
	}

	public void setResultBranch(String resultBranch) {
		this.resultBranch = resultBranch;

		_resultBranchSupplier = null;
	}

	@JsonIgnore
	public void setResultBranch(
		UnsafeSupplier<String, Exception> resultBranchUnsafeSupplier) {

		_resultBranchSupplier = () -> {
			try {
				return resultBranchUnsafeSupplier.get();
			}
			catch (RuntimeException runtimeException) {
				throw runtimeException;
			}
			catch (Exception exception) {
				throw new RuntimeException(exception);
			}
		};
	}

	@GraphQLField(
		description = "The branch the run pushed its result to. Read only."
	)
	@JsonProperty(access = JsonProperty.Access.READ_ONLY)
	protected String resultBranch;

	@JsonIgnore
	private Supplier<String> _resultBranchSupplier;

	@io.swagger.v3.oas.annotations.media.Schema(
		description = "The concrete search engine version for the upgraded workspace, such as 8.17.4. The agent feeds it into a Docker image tag verbatim, so a range does not resolve."
	)
	public String getSearchVersion() {
		if (_searchVersionSupplier != null) {
			searchVersion = _searchVersionSupplier.get();

			_searchVersionSupplier = null;
		}

		return searchVersion;
	}

	public void setSearchVersion(String searchVersion) {
		this.searchVersion = searchVersion;

		_searchVersionSupplier = null;
	}

	@JsonIgnore
	public void setSearchVersion(
		UnsafeSupplier<String, Exception> searchVersionUnsafeSupplier) {

		_searchVersionSupplier = () -> {
			try {
				return searchVersionUnsafeSupplier.get();
			}
			catch (RuntimeException runtimeException) {
				throw runtimeException;
			}
			catch (Exception exception) {
				throw new RuntimeException(exception);
			}
		};
	}

	@GraphQLField(
		description = "The concrete search engine version for the upgraded workspace, such as 8.17.4. The agent feeds it into a Docker image tag verbatim, so a range does not resolve."
	)
	@JsonProperty(access = JsonProperty.Access.READ_WRITE)
	@NotEmpty
	protected String searchVersion;

	@JsonIgnore
	private Supplier<String> _searchVersionSupplier;

	@io.swagger.v3.oas.annotations.media.Schema(
		description = "The status the run is in. Read only."
	)
	@JsonGetter("status")
	@Valid
	public Status getStatus() {
		if (_statusSupplier != null) {
			status = _statusSupplier.get();

			_statusSupplier = null;
		}

		return status;
	}

	@JsonIgnore
	public String getStatusAsString() {
		Status status = getStatus();

		if (status == null) {
			return null;
		}

		return status.toString();
	}

	public void setStatus(Status status) {
		this.status = status;

		_statusSupplier = null;
	}

	@JsonIgnore
	public void setStatus(
		UnsafeSupplier<Status, Exception> statusUnsafeSupplier) {

		_statusSupplier = () -> {
			try {
				return statusUnsafeSupplier.get();
			}
			catch (RuntimeException runtimeException) {
				throw runtimeException;
			}
			catch (Exception exception) {
				throw new RuntimeException(exception);
			}
		};
	}

	@GraphQLField(description = "The status the run is in. Read only.")
	@JsonProperty(access = JsonProperty.Access.READ_ONLY)
	protected Status status;

	@JsonIgnore
	private Supplier<Status> _statusSupplier;

	@io.swagger.v3.oas.annotations.media.Schema(
		description = "What the run was doing when it last reported. Read only."
	)
	public String getStatusMessage() {
		if (_statusMessageSupplier != null) {
			statusMessage = _statusMessageSupplier.get();

			_statusMessageSupplier = null;
		}

		return statusMessage;
	}

	public void setStatusMessage(String statusMessage) {
		this.statusMessage = statusMessage;

		_statusMessageSupplier = null;
	}

	@JsonIgnore
	public void setStatusMessage(
		UnsafeSupplier<String, Exception> statusMessageUnsafeSupplier) {

		_statusMessageSupplier = () -> {
			try {
				return statusMessageUnsafeSupplier.get();
			}
			catch (RuntimeException runtimeException) {
				throw runtimeException;
			}
			catch (Exception exception) {
				throw new RuntimeException(exception);
			}
		};
	}

	@GraphQLField(
		description = "What the run was doing when it last reported. Read only."
	)
	@JsonProperty(access = JsonProperty.Access.READ_ONLY)
	protected String statusMessage;

	@JsonIgnore
	private Supplier<String> _statusMessageSupplier;

	@io.swagger.v3.oas.annotations.media.Schema(
		description = "The release to upgrade the repository to."
	)
	public String getTargetRelease() {
		if (_targetReleaseSupplier != null) {
			targetRelease = _targetReleaseSupplier.get();

			_targetReleaseSupplier = null;
		}

		return targetRelease;
	}

	public void setTargetRelease(String targetRelease) {
		this.targetRelease = targetRelease;

		_targetReleaseSupplier = null;
	}

	@JsonIgnore
	public void setTargetRelease(
		UnsafeSupplier<String, Exception> targetReleaseUnsafeSupplier) {

		_targetReleaseSupplier = () -> {
			try {
				return targetReleaseUnsafeSupplier.get();
			}
			catch (RuntimeException runtimeException) {
				throw runtimeException;
			}
			catch (Exception exception) {
				throw new RuntimeException(exception);
			}
		};
	}

	@GraphQLField(description = "The release to upgrade the repository to.")
	@JsonProperty(access = JsonProperty.Access.READ_WRITE)
	@NotEmpty
	protected String targetRelease;

	@JsonIgnore
	private Supplier<String> _targetReleaseSupplier;

	@io.swagger.v3.oas.annotations.media.Schema(
		description = "The primary key of the run the portal stores."
	)
	public Long getUpgradeRunId() {
		if (_upgradeRunIdSupplier != null) {
			upgradeRunId = _upgradeRunIdSupplier.get();

			_upgradeRunIdSupplier = null;
		}

		return upgradeRunId;
	}

	public void setUpgradeRunId(Long upgradeRunId) {
		this.upgradeRunId = upgradeRunId;

		_upgradeRunIdSupplier = null;
	}

	@JsonIgnore
	public void setUpgradeRunId(
		UnsafeSupplier<Long, Exception> upgradeRunIdUnsafeSupplier) {

		_upgradeRunIdSupplier = () -> {
			try {
				return upgradeRunIdUnsafeSupplier.get();
			}
			catch (RuntimeException runtimeException) {
				throw runtimeException;
			}
			catch (Exception exception) {
				throw new RuntimeException(exception);
			}
		};
	}

	@GraphQLField(description = "The primary key of the run the portal stores.")
	@JsonProperty(access = JsonProperty.Access.READ_WRITE)
	protected Long upgradeRunId;

	@JsonIgnore
	private Supplier<Long> _upgradeRunIdSupplier;

	@io.swagger.v3.oas.annotations.media.Schema(
		description = "The Liferay version the repository currently targets, such as 7.4.13-u92."
	)
	public String getUpgradeSourceVersion() {
		if (_upgradeSourceVersionSupplier != null) {
			upgradeSourceVersion = _upgradeSourceVersionSupplier.get();

			_upgradeSourceVersionSupplier = null;
		}

		return upgradeSourceVersion;
	}

	public void setUpgradeSourceVersion(String upgradeSourceVersion) {
		this.upgradeSourceVersion = upgradeSourceVersion;

		_upgradeSourceVersionSupplier = null;
	}

	@JsonIgnore
	public void setUpgradeSourceVersion(
		UnsafeSupplier<String, Exception> upgradeSourceVersionUnsafeSupplier) {

		_upgradeSourceVersionSupplier = () -> {
			try {
				return upgradeSourceVersionUnsafeSupplier.get();
			}
			catch (RuntimeException runtimeException) {
				throw runtimeException;
			}
			catch (Exception exception) {
				throw new RuntimeException(exception);
			}
		};
	}

	@GraphQLField(
		description = "The Liferay version the repository currently targets, such as 7.4.13-u92."
	)
	@JsonProperty(access = JsonProperty.Access.READ_WRITE)
	@NotEmpty
	protected String upgradeSourceVersion;

	@JsonIgnore
	private Supplier<String> _upgradeSourceVersionSupplier;

	@io.swagger.v3.oas.annotations.media.Schema(
		description = "The Java version the upgraded workspace builds with, such as 21."
	)
	public String getUpgradeTargetJavaVersion() {
		if (_upgradeTargetJavaVersionSupplier != null) {
			upgradeTargetJavaVersion = _upgradeTargetJavaVersionSupplier.get();

			_upgradeTargetJavaVersionSupplier = null;
		}

		return upgradeTargetJavaVersion;
	}

	public void setUpgradeTargetJavaVersion(String upgradeTargetJavaVersion) {
		this.upgradeTargetJavaVersion = upgradeTargetJavaVersion;

		_upgradeTargetJavaVersionSupplier = null;
	}

	@JsonIgnore
	public void setUpgradeTargetJavaVersion(
		UnsafeSupplier<String, Exception>
			upgradeTargetJavaVersionUnsafeSupplier) {

		_upgradeTargetJavaVersionSupplier = () -> {
			try {
				return upgradeTargetJavaVersionUnsafeSupplier.get();
			}
			catch (RuntimeException runtimeException) {
				throw runtimeException;
			}
			catch (Exception exception) {
				throw new RuntimeException(exception);
			}
		};
	}

	@GraphQLField(
		description = "The Java version the upgraded workspace builds with, such as 21."
	)
	@JsonProperty(access = JsonProperty.Access.READ_WRITE)
	@NotEmpty
	protected String upgradeTargetJavaVersion;

	@JsonIgnore
	private Supplier<String> _upgradeTargetJavaVersionSupplier;

	@io.swagger.v3.oas.annotations.media.Schema(
		description = "The directory on the runner host where the run cloned and upgraded the repository. Read only."
	)
	public String getWorkspacePath() {
		if (_workspacePathSupplier != null) {
			workspacePath = _workspacePathSupplier.get();

			_workspacePathSupplier = null;
		}

		return workspacePath;
	}

	public void setWorkspacePath(String workspacePath) {
		this.workspacePath = workspacePath;

		_workspacePathSupplier = null;
	}

	@JsonIgnore
	public void setWorkspacePath(
		UnsafeSupplier<String, Exception> workspacePathUnsafeSupplier) {

		_workspacePathSupplier = () -> {
			try {
				return workspacePathUnsafeSupplier.get();
			}
			catch (RuntimeException runtimeException) {
				throw runtimeException;
			}
			catch (Exception exception) {
				throw new RuntimeException(exception);
			}
		};
	}

	@GraphQLField(
		description = "The directory on the runner host where the run cloned and upgraded the repository. Read only."
	)
	@JsonProperty(access = JsonProperty.Access.READ_ONLY)
	protected String workspacePath;

	@JsonIgnore
	private Supplier<String> _workspacePathSupplier;

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
		StringBundler sb = new StringBundler();

		sb.append("{");

		String branch = getBranch();

		if (branch != null) {
			if (sb.length() > 1) {
				sb.append(", ");
			}

			sb.append("\"branch\": ");

			sb.append("\"");

			sb.append(_escape(branch));

			sb.append("\"");
		}

		String credentialKeyReference = getCredentialKeyReference();

		if (credentialKeyReference != null) {
			if (sb.length() > 1) {
				sb.append(", ");
			}

			sb.append("\"credentialKeyReference\": ");

			sb.append("\"");

			sb.append(_escape(credentialKeyReference));

			sb.append("\"");
		}

		String customerName = getCustomerName();

		if (customerName != null) {
			if (sb.length() > 1) {
				sb.append(", ");
			}

			sb.append("\"customerName\": ");

			sb.append("\"");

			sb.append(_escape(customerName));

			sb.append("\"");
		}

		String dbTargetType = getDbTargetType();

		if (dbTargetType != null) {
			if (sb.length() > 1) {
				sb.append(", ");
			}

			sb.append("\"dbTargetType\": ");

			sb.append("\"");

			sb.append(_escape(dbTargetType));

			sb.append("\"");
		}

		String dbTargetVersion = getDbTargetVersion();

		if (dbTargetVersion != null) {
			if (sb.length() > 1) {
				sb.append(", ");
			}

			sb.append("\"dbTargetVersion\": ");

			sb.append("\"");

			sb.append(_escape(dbTargetVersion));

			sb.append("\"");
		}

		String externalReferenceCode = getExternalReferenceCode();

		if (externalReferenceCode != null) {
			if (sb.length() > 1) {
				sb.append(", ");
			}

			sb.append("\"externalReferenceCode\": ");

			sb.append("\"");

			sb.append(_escape(externalReferenceCode));

			sb.append("\"");
		}

		String nodeVersion = getNodeVersion();

		if (nodeVersion != null) {
			if (sb.length() > 1) {
				sb.append(", ");
			}

			sb.append("\"nodeVersion\": ");

			sb.append("\"");

			sb.append(_escape(nodeVersion));

			sb.append("\"");
		}

		String pullRequestURL = getPullRequestURL();

		if (pullRequestURL != null) {
			if (sb.length() > 1) {
				sb.append(", ");
			}

			sb.append("\"pullRequestURL\": ");

			sb.append("\"");

			sb.append(_escape(pullRequestURL));

			sb.append("\"");
		}

		String repositoryURL = getRepositoryURL();

		if (repositoryURL != null) {
			if (sb.length() > 1) {
				sb.append(", ");
			}

			sb.append("\"repositoryURL\": ");

			sb.append("\"");

			sb.append(_escape(repositoryURL));

			sb.append("\"");
		}

		String resultBranch = getResultBranch();

		if (resultBranch != null) {
			if (sb.length() > 1) {
				sb.append(", ");
			}

			sb.append("\"resultBranch\": ");

			sb.append("\"");

			sb.append(_escape(resultBranch));

			sb.append("\"");
		}

		String searchVersion = getSearchVersion();

		if (searchVersion != null) {
			if (sb.length() > 1) {
				sb.append(", ");
			}

			sb.append("\"searchVersion\": ");

			sb.append("\"");

			sb.append(_escape(searchVersion));

			sb.append("\"");
		}

		Status status = getStatus();

		if (status != null) {
			if (sb.length() > 1) {
				sb.append(", ");
			}

			sb.append("\"status\": ");

			sb.append("\"");
			sb.append(status);
			sb.append("\"");
		}

		String statusMessage = getStatusMessage();

		if (statusMessage != null) {
			if (sb.length() > 1) {
				sb.append(", ");
			}

			sb.append("\"statusMessage\": ");

			sb.append("\"");

			sb.append(_escape(statusMessage));

			sb.append("\"");
		}

		String targetRelease = getTargetRelease();

		if (targetRelease != null) {
			if (sb.length() > 1) {
				sb.append(", ");
			}

			sb.append("\"targetRelease\": ");

			sb.append("\"");

			sb.append(_escape(targetRelease));

			sb.append("\"");
		}

		Long upgradeRunId = getUpgradeRunId();

		if (upgradeRunId != null) {
			if (sb.length() > 1) {
				sb.append(", ");
			}

			sb.append("\"upgradeRunId\": ");

			sb.append(upgradeRunId);
		}

		String upgradeSourceVersion = getUpgradeSourceVersion();

		if (upgradeSourceVersion != null) {
			if (sb.length() > 1) {
				sb.append(", ");
			}

			sb.append("\"upgradeSourceVersion\": ");

			sb.append("\"");

			sb.append(_escape(upgradeSourceVersion));

			sb.append("\"");
		}

		String upgradeTargetJavaVersion = getUpgradeTargetJavaVersion();

		if (upgradeTargetJavaVersion != null) {
			if (sb.length() > 1) {
				sb.append(", ");
			}

			sb.append("\"upgradeTargetJavaVersion\": ");

			sb.append("\"");

			sb.append(_escape(upgradeTargetJavaVersion));

			sb.append("\"");
		}

		String workspacePath = getWorkspacePath();

		if (workspacePath != null) {
			if (sb.length() > 1) {
				sb.append(", ");
			}

			sb.append("\"workspacePath\": ");

			sb.append("\"");

			sb.append(_escape(workspacePath));

			sb.append("\"");
		}

		sb.append("}");

		return sb.toString();
	}

	@io.swagger.v3.oas.annotations.media.Schema(
		accessMode = io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY,
		defaultValue = "com.liferay.upgrades.lab.agent.remote.rest.dto.v1_0.UpgradeRun",
		name = "x-class-name"
	)
	public String xClassName;

	@GraphQLName("Status")
	public static enum Status {

		BLOCKED("blocked"), CANCELLED("cancelled"), CLONING("cloning"),
		FAILED("failed"), PROVISIONING("provisioning"),
		PUBLISHING("publishing"), QUEUED("queued"), RUNNING("running"),
		SUCCESSFUL("successful"), TIMED_OUT("timed-out"),
		VALIDATING("validating");

		@JsonCreator
		public static Status create(String value) {
			if ((value == null) || value.equals("")) {
				return null;
			}

			for (Status status : values()) {
				if (Objects.equals(status.getValue(), value)) {
					return status;
				}
			}

			throw new IllegalArgumentException("Invalid enum value: " + value);
		}

		@JsonValue
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

	private static String _escape(Object object) {
		return StringUtil.replace(
			String.valueOf(object), _JSON_ESCAPE_STRINGS[0],
			_JSON_ESCAPE_STRINGS[1]);
	}

	private static boolean _isArray(Object value) {
		if (value == null) {
			return false;
		}

		Class<?> clazz = value.getClass();

		return clazz.isArray();
	}

	private static String _toJSON(Map<String, ?> map) {
		StringBuilder sb = new StringBuilder("{");

		@SuppressWarnings("unchecked")
		Set set = map.entrySet();

		@SuppressWarnings("unchecked")
		Iterator<Map.Entry<String, ?>> iterator = set.iterator();

		while (iterator.hasNext()) {
			Map.Entry<String, ?> entry = iterator.next();

			sb.append("\"");
			sb.append(_escape(entry.getKey()));
			sb.append("\": ");

			Object value = entry.getValue();

			if (_isArray(value)) {
				sb.append("[");

				Object[] valueArray = (Object[])value;

				for (int i = 0; i < valueArray.length; i++) {
					if (valueArray[i] instanceof Map) {
						sb.append(_toJSON((Map<String, ?>)valueArray[i]));
					}
					else if (valueArray[i] instanceof String) {
						sb.append("\"");
						sb.append(valueArray[i]);
						sb.append("\"");
					}
					else {
						sb.append(valueArray[i]);
					}

					if ((i + 1) < valueArray.length) {
						sb.append(", ");
					}
				}

				sb.append("]");
			}
			else if (value instanceof Map) {
				sb.append(_toJSON((Map<String, ?>)value));
			}
			else if (value instanceof String) {
				sb.append("\"");
				sb.append(_escape(value));
				sb.append("\"");
			}
			else {
				sb.append(value);
			}

			if (iterator.hasNext()) {
				sb.append(", ");
			}
		}

		sb.append("}");

		return sb.toString();
	}

	private static String _toJSON(Object value) {
		if (value instanceof Collection) {
			return String.valueOf(
				JSONFactoryUtil.createJSONArray((Collection<?>)value));
		}
		else if (value instanceof Map) {
			return String.valueOf(
				JSONFactoryUtil.createJSONObject((Map<?, ?>)value));
		}
		else if (value instanceof Object[]) {
			return String.valueOf(
				JSONFactoryUtil.createJSONArray(
					Arrays.asList((Object[])value)));
		}
		else if (value instanceof String) {
			return StringBundler.concat("\"", _escape(value), "\"");
		}

		return String.valueOf(value);
	}

	private static final String[][] _JSON_ESCAPE_STRINGS = {
		{"\\", "\"", "\b", "\f", "\n", "\r", "\t"},
		{"\\\\", "\\\"", "\\b", "\\f", "\\n", "\\r", "\\t"}
	};

	private Map<String, Serializable> _extendedProperties;

}
// LIFERAY-REST-BUILDER-HASH:-1891249920