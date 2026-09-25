/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.upgrades.lab.agent.remote.rest.client.serdes.v1_0;

import com.liferay.upgrades.lab.agent.remote.rest.client.dto.v1_0.UpgradeRun;
import com.liferay.upgrades.lab.agent.remote.rest.client.json.BaseJSONParser;

import jakarta.annotation.Generated;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/**
 * @author Albert Gomes Cabral
 * @generated
 */
@Generated("")
public class UpgradeRunSerDes {

	public static UpgradeRun toDTO(String json) {
		UpgradeRunJSONParser upgradeRunJSONParser = new UpgradeRunJSONParser();

		return upgradeRunJSONParser.parseToDTO(json);
	}

	public static UpgradeRun[] toDTOs(String json) {
		UpgradeRunJSONParser upgradeRunJSONParser = new UpgradeRunJSONParser();

		return upgradeRunJSONParser.parseToDTOs(json);
	}

	public static String toJSON(UpgradeRun upgradeRun) {
		if (upgradeRun == null) {
			return "null";
		}

		StringBuilder sb = new StringBuilder();

		sb.append("{");

		if (upgradeRun.getBranch() != null) {
			if (sb.length() > 1) {
				sb.append(", ");
			}

			sb.append("\"branch\": ");

			sb.append("\"");

			sb.append(_escape(upgradeRun.getBranch()));

			sb.append("\"");
		}

		if (upgradeRun.getCredentialKeyReference() != null) {
			if (sb.length() > 1) {
				sb.append(", ");
			}

			sb.append("\"credentialKeyReference\": ");

			sb.append("\"");

			sb.append(_escape(upgradeRun.getCredentialKeyReference()));

			sb.append("\"");
		}

		if (upgradeRun.getCustomerName() != null) {
			if (sb.length() > 1) {
				sb.append(", ");
			}

			sb.append("\"customerName\": ");

			sb.append("\"");

			sb.append(_escape(upgradeRun.getCustomerName()));

			sb.append("\"");
		}

		if (upgradeRun.getDbTargetType() != null) {
			if (sb.length() > 1) {
				sb.append(", ");
			}

			sb.append("\"dbTargetType\": ");

			sb.append("\"");

			sb.append(_escape(upgradeRun.getDbTargetType()));

			sb.append("\"");
		}

		if (upgradeRun.getDbTargetVersion() != null) {
			if (sb.length() > 1) {
				sb.append(", ");
			}

			sb.append("\"dbTargetVersion\": ");

			sb.append("\"");

			sb.append(_escape(upgradeRun.getDbTargetVersion()));

			sb.append("\"");
		}

		if (upgradeRun.getExternalReferenceCode() != null) {
			if (sb.length() > 1) {
				sb.append(", ");
			}

			sb.append("\"externalReferenceCode\": ");

			sb.append("\"");

			sb.append(_escape(upgradeRun.getExternalReferenceCode()));

			sb.append("\"");
		}

		if (upgradeRun.getNodeVersion() != null) {
			if (sb.length() > 1) {
				sb.append(", ");
			}

			sb.append("\"nodeVersion\": ");

			sb.append("\"");

			sb.append(_escape(upgradeRun.getNodeVersion()));

			sb.append("\"");
		}

		if (upgradeRun.getPullRequestURL() != null) {
			if (sb.length() > 1) {
				sb.append(", ");
			}

			sb.append("\"pullRequestURL\": ");

			sb.append("\"");

			sb.append(_escape(upgradeRun.getPullRequestURL()));

			sb.append("\"");
		}

		if (upgradeRun.getRepositoryURL() != null) {
			if (sb.length() > 1) {
				sb.append(", ");
			}

			sb.append("\"repositoryURL\": ");

			sb.append("\"");

			sb.append(_escape(upgradeRun.getRepositoryURL()));

			sb.append("\"");
		}

		if (upgradeRun.getResultBranch() != null) {
			if (sb.length() > 1) {
				sb.append(", ");
			}

			sb.append("\"resultBranch\": ");

			sb.append("\"");

			sb.append(_escape(upgradeRun.getResultBranch()));

			sb.append("\"");
		}

		if (upgradeRun.getSearchVersion() != null) {
			if (sb.length() > 1) {
				sb.append(", ");
			}

			sb.append("\"searchVersion\": ");

			sb.append("\"");

			sb.append(_escape(upgradeRun.getSearchVersion()));

			sb.append("\"");
		}

		if (upgradeRun.getStatus() != null) {
			if (sb.length() > 1) {
				sb.append(", ");
			}

			sb.append("\"status\": ");

			sb.append("\"");
			sb.append(upgradeRun.getStatus());
			sb.append("\"");
		}

		if (upgradeRun.getStatusMessage() != null) {
			if (sb.length() > 1) {
				sb.append(", ");
			}

			sb.append("\"statusMessage\": ");

			sb.append("\"");

			sb.append(_escape(upgradeRun.getStatusMessage()));

			sb.append("\"");
		}

		if (upgradeRun.getTargetRelease() != null) {
			if (sb.length() > 1) {
				sb.append(", ");
			}

			sb.append("\"targetRelease\": ");

			sb.append("\"");

			sb.append(_escape(upgradeRun.getTargetRelease()));

			sb.append("\"");
		}

		if (upgradeRun.getUpgradeRunId() != null) {
			if (sb.length() > 1) {
				sb.append(", ");
			}

			sb.append("\"upgradeRunId\": ");

			sb.append(upgradeRun.getUpgradeRunId());
		}

		if (upgradeRun.getUpgradeSourceVersion() != null) {
			if (sb.length() > 1) {
				sb.append(", ");
			}

			sb.append("\"upgradeSourceVersion\": ");

			sb.append("\"");

			sb.append(_escape(upgradeRun.getUpgradeSourceVersion()));

			sb.append("\"");
		}

		if (upgradeRun.getUpgradeTargetJavaVersion() != null) {
			if (sb.length() > 1) {
				sb.append(", ");
			}

			sb.append("\"upgradeTargetJavaVersion\": ");

			sb.append("\"");

			sb.append(_escape(upgradeRun.getUpgradeTargetJavaVersion()));

			sb.append("\"");
		}

		if (upgradeRun.getWorkspacePath() != null) {
			if (sb.length() > 1) {
				sb.append(", ");
			}

			sb.append("\"workspacePath\": ");

			sb.append("\"");

			sb.append(_escape(upgradeRun.getWorkspacePath()));

			sb.append("\"");
		}

		sb.append("}");

		return sb.toString();
	}

	public static Map<String, Object> toMap(String json) {
		UpgradeRunJSONParser upgradeRunJSONParser = new UpgradeRunJSONParser();

		return upgradeRunJSONParser.parseToMap(json);
	}

	public static Map<String, String> toMap(UpgradeRun upgradeRun) {
		if (upgradeRun == null) {
			return null;
		}

		Map<String, String> map = new TreeMap<>();

		if (upgradeRun.getBranch() == null) {
			map.put("branch", null);
		}
		else {
			map.put("branch", String.valueOf(upgradeRun.getBranch()));
		}

		if (upgradeRun.getCredentialKeyReference() == null) {
			map.put("credentialKeyReference", null);
		}
		else {
			map.put(
				"credentialKeyReference",
				String.valueOf(upgradeRun.getCredentialKeyReference()));
		}

		if (upgradeRun.getCustomerName() == null) {
			map.put("customerName", null);
		}
		else {
			map.put(
				"customerName", String.valueOf(upgradeRun.getCustomerName()));
		}

		if (upgradeRun.getDbTargetType() == null) {
			map.put("dbTargetType", null);
		}
		else {
			map.put(
				"dbTargetType", String.valueOf(upgradeRun.getDbTargetType()));
		}

		if (upgradeRun.getDbTargetVersion() == null) {
			map.put("dbTargetVersion", null);
		}
		else {
			map.put(
				"dbTargetVersion",
				String.valueOf(upgradeRun.getDbTargetVersion()));
		}

		if (upgradeRun.getExternalReferenceCode() == null) {
			map.put("externalReferenceCode", null);
		}
		else {
			map.put(
				"externalReferenceCode",
				String.valueOf(upgradeRun.getExternalReferenceCode()));
		}

		if (upgradeRun.getNodeVersion() == null) {
			map.put("nodeVersion", null);
		}
		else {
			map.put("nodeVersion", String.valueOf(upgradeRun.getNodeVersion()));
		}

		if (upgradeRun.getPullRequestURL() == null) {
			map.put("pullRequestURL", null);
		}
		else {
			map.put(
				"pullRequestURL",
				String.valueOf(upgradeRun.getPullRequestURL()));
		}

		if (upgradeRun.getRepositoryURL() == null) {
			map.put("repositoryURL", null);
		}
		else {
			map.put(
				"repositoryURL", String.valueOf(upgradeRun.getRepositoryURL()));
		}

		if (upgradeRun.getResultBranch() == null) {
			map.put("resultBranch", null);
		}
		else {
			map.put(
				"resultBranch", String.valueOf(upgradeRun.getResultBranch()));
		}

		if (upgradeRun.getSearchVersion() == null) {
			map.put("searchVersion", null);
		}
		else {
			map.put(
				"searchVersion", String.valueOf(upgradeRun.getSearchVersion()));
		}

		if (upgradeRun.getStatus() == null) {
			map.put("status", null);
		}
		else {
			map.put("status", String.valueOf(upgradeRun.getStatus()));
		}

		if (upgradeRun.getStatusMessage() == null) {
			map.put("statusMessage", null);
		}
		else {
			map.put(
				"statusMessage", String.valueOf(upgradeRun.getStatusMessage()));
		}

		if (upgradeRun.getTargetRelease() == null) {
			map.put("targetRelease", null);
		}
		else {
			map.put(
				"targetRelease", String.valueOf(upgradeRun.getTargetRelease()));
		}

		if (upgradeRun.getUpgradeRunId() == null) {
			map.put("upgradeRunId", null);
		}
		else {
			map.put(
				"upgradeRunId", String.valueOf(upgradeRun.getUpgradeRunId()));
		}

		if (upgradeRun.getUpgradeSourceVersion() == null) {
			map.put("upgradeSourceVersion", null);
		}
		else {
			map.put(
				"upgradeSourceVersion",
				String.valueOf(upgradeRun.getUpgradeSourceVersion()));
		}

		if (upgradeRun.getUpgradeTargetJavaVersion() == null) {
			map.put("upgradeTargetJavaVersion", null);
		}
		else {
			map.put(
				"upgradeTargetJavaVersion",
				String.valueOf(upgradeRun.getUpgradeTargetJavaVersion()));
		}

		if (upgradeRun.getWorkspacePath() == null) {
			map.put("workspacePath", null);
		}
		else {
			map.put(
				"workspacePath", String.valueOf(upgradeRun.getWorkspacePath()));
		}

		return map;
	}

	public static class UpgradeRunJSONParser
		extends BaseJSONParser<UpgradeRun> {

		@Override
		protected UpgradeRun createDTO() {
			return new UpgradeRun();
		}

		@Override
		protected UpgradeRun[] createDTOArray(int size) {
			return new UpgradeRun[size];
		}

		@Override
		protected boolean parseMaps(String jsonParserFieldName) {
			if (Objects.equals(jsonParserFieldName, "branch")) {
				return false;
			}
			else if (Objects.equals(
						jsonParserFieldName, "credentialKeyReference")) {

				return false;
			}
			else if (Objects.equals(jsonParserFieldName, "customerName")) {
				return false;
			}
			else if (Objects.equals(jsonParserFieldName, "dbTargetType")) {
				return false;
			}
			else if (Objects.equals(jsonParserFieldName, "dbTargetVersion")) {
				return false;
			}
			else if (Objects.equals(
						jsonParserFieldName, "externalReferenceCode")) {

				return false;
			}
			else if (Objects.equals(jsonParserFieldName, "nodeVersion")) {
				return false;
			}
			else if (Objects.equals(jsonParserFieldName, "pullRequestURL")) {
				return false;
			}
			else if (Objects.equals(jsonParserFieldName, "repositoryURL")) {
				return false;
			}
			else if (Objects.equals(jsonParserFieldName, "resultBranch")) {
				return false;
			}
			else if (Objects.equals(jsonParserFieldName, "searchVersion")) {
				return false;
			}
			else if (Objects.equals(jsonParserFieldName, "status")) {
				return false;
			}
			else if (Objects.equals(jsonParserFieldName, "statusMessage")) {
				return false;
			}
			else if (Objects.equals(jsonParserFieldName, "targetRelease")) {
				return false;
			}
			else if (Objects.equals(jsonParserFieldName, "upgradeRunId")) {
				return false;
			}
			else if (Objects.equals(
						jsonParserFieldName, "upgradeSourceVersion")) {

				return false;
			}
			else if (Objects.equals(
						jsonParserFieldName, "upgradeTargetJavaVersion")) {

				return false;
			}
			else if (Objects.equals(jsonParserFieldName, "workspacePath")) {
				return false;
			}

			return false;
		}

		@Override
		protected void setField(
			UpgradeRun upgradeRun, String jsonParserFieldName,
			Object jsonParserFieldValue) {

			if (Objects.equals(jsonParserFieldName, "branch")) {
				if (jsonParserFieldValue != null) {
					upgradeRun.setBranch((String)jsonParserFieldValue);
				}
			}
			else if (Objects.equals(
						jsonParserFieldName, "credentialKeyReference")) {

				if (jsonParserFieldValue != null) {
					upgradeRun.setCredentialKeyReference(
						(String)jsonParserFieldValue);
				}
			}
			else if (Objects.equals(jsonParserFieldName, "customerName")) {
				if (jsonParserFieldValue != null) {
					upgradeRun.setCustomerName((String)jsonParserFieldValue);
				}
			}
			else if (Objects.equals(jsonParserFieldName, "dbTargetType")) {
				if (jsonParserFieldValue != null) {
					upgradeRun.setDbTargetType((String)jsonParserFieldValue);
				}
			}
			else if (Objects.equals(jsonParserFieldName, "dbTargetVersion")) {
				if (jsonParserFieldValue != null) {
					upgradeRun.setDbTargetVersion((String)jsonParserFieldValue);
				}
			}
			else if (Objects.equals(
						jsonParserFieldName, "externalReferenceCode")) {

				if (jsonParserFieldValue != null) {
					upgradeRun.setExternalReferenceCode(
						(String)jsonParserFieldValue);
				}
			}
			else if (Objects.equals(jsonParserFieldName, "nodeVersion")) {
				if (jsonParserFieldValue != null) {
					upgradeRun.setNodeVersion((String)jsonParserFieldValue);
				}
			}
			else if (Objects.equals(jsonParserFieldName, "pullRequestURL")) {
				if (jsonParserFieldValue != null) {
					upgradeRun.setPullRequestURL((String)jsonParserFieldValue);
				}
			}
			else if (Objects.equals(jsonParserFieldName, "repositoryURL")) {
				if (jsonParserFieldValue != null) {
					upgradeRun.setRepositoryURL((String)jsonParserFieldValue);
				}
			}
			else if (Objects.equals(jsonParserFieldName, "resultBranch")) {
				if (jsonParserFieldValue != null) {
					upgradeRun.setResultBranch((String)jsonParserFieldValue);
				}
			}
			else if (Objects.equals(jsonParserFieldName, "searchVersion")) {
				if (jsonParserFieldValue != null) {
					upgradeRun.setSearchVersion((String)jsonParserFieldValue);
				}
			}
			else if (Objects.equals(jsonParserFieldName, "status")) {
				if (jsonParserFieldValue != null) {
					upgradeRun.setStatus(
						UpgradeRun.Status.create((String)jsonParserFieldValue));
				}
			}
			else if (Objects.equals(jsonParserFieldName, "statusMessage")) {
				if (jsonParserFieldValue != null) {
					upgradeRun.setStatusMessage((String)jsonParserFieldValue);
				}
			}
			else if (Objects.equals(jsonParserFieldName, "targetRelease")) {
				if (jsonParserFieldValue != null) {
					upgradeRun.setTargetRelease((String)jsonParserFieldValue);
				}
			}
			else if (Objects.equals(jsonParserFieldName, "upgradeRunId")) {
				if (jsonParserFieldValue != null) {
					upgradeRun.setUpgradeRunId(
						Long.valueOf((String)jsonParserFieldValue));
				}
			}
			else if (Objects.equals(
						jsonParserFieldName, "upgradeSourceVersion")) {

				if (jsonParserFieldValue != null) {
					upgradeRun.setUpgradeSourceVersion(
						(String)jsonParserFieldValue);
				}
			}
			else if (Objects.equals(
						jsonParserFieldName, "upgradeTargetJavaVersion")) {

				if (jsonParserFieldValue != null) {
					upgradeRun.setUpgradeTargetJavaVersion(
						(String)jsonParserFieldValue);
				}
			}
			else if (Objects.equals(jsonParserFieldName, "workspacePath")) {
				if (jsonParserFieldValue != null) {
					upgradeRun.setWorkspacePath((String)jsonParserFieldValue);
				}
			}
		}

	}

	private static String _escape(Object object) {
		String string = String.valueOf(object);

		for (String[] strings : BaseJSONParser.JSON_ESCAPE_STRINGS) {
			string = string.replace(strings[0], strings[1]);
		}

		return string;
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
			sb.append(entry.getKey());
			sb.append("\": ");

			Object value = entry.getValue();

			sb.append(_toJSON(value));

			if (iterator.hasNext()) {
				sb.append(", ");
			}
		}

		sb.append("}");

		return sb.toString();
	}

	private static String _toJSON(Object value) {
		if (value == null) {
			return "null";
		}

		if (value instanceof Collection) {
			Collection<?> collection = (Collection<?>)value;

			return _toJSON(collection.toArray());
		}

		if (value instanceof Map) {
			return _toJSON((Map)value);
		}

		Class<?> clazz = value.getClass();

		if (clazz.isArray()) {
			StringBuilder sb = new StringBuilder("[");

			Object[] values = (Object[])value;

			for (int i = 0; i < values.length; i++) {
				sb.append(_toJSON(values[i]));

				if ((i + 1) < values.length) {
					sb.append(", ");
				}
			}

			sb.append("]");

			return sb.toString();
		}

		if (value instanceof String) {
			return "\"" + _escape(value) + "\"";
		}

		return String.valueOf(value);
	}

}
// LIFERAY-REST-BUILDER-HASH:-614904441