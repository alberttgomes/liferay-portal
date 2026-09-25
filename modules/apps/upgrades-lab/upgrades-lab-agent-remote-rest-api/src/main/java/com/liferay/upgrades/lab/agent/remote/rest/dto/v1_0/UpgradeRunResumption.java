/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.upgrades.lab.agent.remote.rest.dto.v1_0;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.liferay.petra.function.UnsafeSupplier;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.vulcan.graphql.annotation.GraphQLField;
import com.liferay.portal.vulcan.graphql.annotation.GraphQLName;
import com.liferay.portal.vulcan.util.ObjectMapperUtil;

import jakarta.annotation.Generated;

import jakarta.validation.Valid;

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
	description = "What a resumed run changes from the finished run it continues. Both properties are optional.",
	value = "UpgradeRunResumption"
)
@io.swagger.v3.oas.annotations.media.Schema(
	description = "What a resumed run changes from the finished run it continues. Both properties are optional."
)
@JsonFilter("Liferay.Vulcan")
@XmlRootElement(name = "UpgradeRunResumption")
public class UpgradeRunResumption implements Serializable {

	public static UpgradeRunResumption toDTO(String json) {
		return ObjectMapperUtil.readValue(UpgradeRunResumption.class, json);
	}

	public static UpgradeRunResumption unsafeToDTO(String json) {
		return ObjectMapperUtil.unsafeReadValue(
			UpgradeRunResumption.class, json);
	}

	@io.swagger.v3.oas.annotations.media.Schema(
		description = "The phase the agent starts at, 1 to 4. Leave it out to start at the phase the finished run stopped in. Every phase before it is taken as complete on the result branch."
	)
	public Integer getFirstPhase() {
		if (_firstPhaseSupplier != null) {
			firstPhase = _firstPhaseSupplier.get();

			_firstPhaseSupplier = null;
		}

		return firstPhase;
	}

	public void setFirstPhase(Integer firstPhase) {
		this.firstPhase = firstPhase;

		_firstPhaseSupplier = null;
	}

	@JsonIgnore
	public void setFirstPhase(
		UnsafeSupplier<Integer, Exception> firstPhaseUnsafeSupplier) {

		_firstPhaseSupplier = () -> {
			try {
				return firstPhaseUnsafeSupplier.get();
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
		description = "The phase the agent starts at, 1 to 4. Leave it out to start at the phase the finished run stopped in. Every phase before it is taken as complete on the result branch."
	)
	@JsonProperty(access = JsonProperty.Access.READ_WRITE)
	protected Integer firstPhase;

	@JsonIgnore
	private Supplier<Integer> _firstPhaseSupplier;

	@io.swagger.v3.oas.annotations.media.Schema(
		description = "Workspace settings to add to or replace among the finished run's settings, keyed as in upgrade-run.properties, such as customer.name. A null value removes the key."
	)
	@Valid
	public Map<String, String> getSettings() {
		if (_settingsSupplier != null) {
			settings = _settingsSupplier.get();

			_settingsSupplier = null;
		}

		return settings;
	}

	public void setSettings(Map<String, String> settings) {
		this.settings = settings;

		_settingsSupplier = null;
	}

	@JsonIgnore
	public void setSettings(
		UnsafeSupplier<Map<String, String>, Exception> settingsUnsafeSupplier) {

		_settingsSupplier = () -> {
			try {
				return settingsUnsafeSupplier.get();
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
		description = "Workspace settings to add to or replace among the finished run's settings, keyed as in upgrade-run.properties, such as customer.name. A null value removes the key."
	)
	@JsonProperty(access = JsonProperty.Access.READ_WRITE)
	protected Map<String, String> settings;

	@JsonIgnore
	private Supplier<Map<String, String>> _settingsSupplier;

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}

		if (!(object instanceof UpgradeRunResumption)) {
			return false;
		}

		UpgradeRunResumption upgradeRunResumption =
			(UpgradeRunResumption)object;

		return Objects.equals(toString(), upgradeRunResumption.toString());
	}

	@Override
	public int hashCode() {
		String string = toString();

		return string.hashCode();
	}

	public String toString() {
		StringBundler sb = new StringBundler();

		sb.append("{");

		Integer firstPhase = getFirstPhase();

		if (firstPhase != null) {
			if (sb.length() > 1) {
				sb.append(", ");
			}

			sb.append("\"firstPhase\": ");

			sb.append(firstPhase);
		}

		Map<String, String> settings = getSettings();

		if (settings != null) {
			if (sb.length() > 1) {
				sb.append(", ");
			}

			sb.append("\"settings\": ");

			sb.append(_toJSON(settings));
		}

		sb.append("}");

		return sb.toString();
	}

	@io.swagger.v3.oas.annotations.media.Schema(
		accessMode = io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY,
		defaultValue = "com.liferay.upgrades.lab.agent.remote.rest.dto.v1_0.UpgradeRunResumption",
		name = "x-class-name"
	)
	public String xClassName;

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
// LIFERAY-REST-BUILDER-HASH:16566038