/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.upgrades.lab.agent.remote.rest.client.serdes.v1_0;

import com.liferay.upgrades.lab.agent.remote.rest.client.dto.v1_0.UpgradeRunResumption;
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
public class UpgradeRunResumptionSerDes {

	public static UpgradeRunResumption toDTO(String json) {
		UpgradeRunResumptionJSONParser upgradeRunResumptionJSONParser =
			new UpgradeRunResumptionJSONParser();

		return upgradeRunResumptionJSONParser.parseToDTO(json);
	}

	public static UpgradeRunResumption[] toDTOs(String json) {
		UpgradeRunResumptionJSONParser upgradeRunResumptionJSONParser =
			new UpgradeRunResumptionJSONParser();

		return upgradeRunResumptionJSONParser.parseToDTOs(json);
	}

	public static String toJSON(UpgradeRunResumption upgradeRunResumption) {
		if (upgradeRunResumption == null) {
			return "null";
		}

		StringBuilder sb = new StringBuilder();

		sb.append("{");

		if (upgradeRunResumption.getFirstPhase() != null) {
			if (sb.length() > 1) {
				sb.append(", ");
			}

			sb.append("\"firstPhase\": ");

			sb.append(upgradeRunResumption.getFirstPhase());
		}

		if (upgradeRunResumption.getSettings() != null) {
			if (sb.length() > 1) {
				sb.append(", ");
			}

			sb.append("\"settings\": ");

			sb.append(_toJSON(upgradeRunResumption.getSettings()));
		}

		sb.append("}");

		return sb.toString();
	}

	public static Map<String, Object> toMap(String json) {
		UpgradeRunResumptionJSONParser upgradeRunResumptionJSONParser =
			new UpgradeRunResumptionJSONParser();

		return upgradeRunResumptionJSONParser.parseToMap(json);
	}

	public static Map<String, String> toMap(
		UpgradeRunResumption upgradeRunResumption) {

		if (upgradeRunResumption == null) {
			return null;
		}

		Map<String, String> map = new TreeMap<>();

		if (upgradeRunResumption.getFirstPhase() == null) {
			map.put("firstPhase", null);
		}
		else {
			map.put(
				"firstPhase",
				String.valueOf(upgradeRunResumption.getFirstPhase()));
		}

		if (upgradeRunResumption.getSettings() == null) {
			map.put("settings", null);
		}
		else {
			map.put(
				"settings", String.valueOf(upgradeRunResumption.getSettings()));
		}

		return map;
	}

	public static class UpgradeRunResumptionJSONParser
		extends BaseJSONParser<UpgradeRunResumption> {

		@Override
		protected UpgradeRunResumption createDTO() {
			return new UpgradeRunResumption();
		}

		@Override
		protected UpgradeRunResumption[] createDTOArray(int size) {
			return new UpgradeRunResumption[size];
		}

		@Override
		protected boolean parseMaps(String jsonParserFieldName) {
			if (Objects.equals(jsonParserFieldName, "firstPhase")) {
				return false;
			}
			else if (Objects.equals(jsonParserFieldName, "settings")) {
				return true;
			}

			return false;
		}

		@Override
		protected void setField(
			UpgradeRunResumption upgradeRunResumption,
			String jsonParserFieldName, Object jsonParserFieldValue) {

			if (Objects.equals(jsonParserFieldName, "firstPhase")) {
				if (jsonParserFieldValue != null) {
					upgradeRunResumption.setFirstPhase(
						Integer.valueOf((String)jsonParserFieldValue));
				}
			}
			else if (Objects.equals(jsonParserFieldName, "settings")) {
				if (jsonParserFieldValue != null) {
					upgradeRunResumption.setSettings(
						(Map<String, String>)jsonParserFieldValue);
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
// LIFERAY-REST-BUILDER-HASH:1032888566