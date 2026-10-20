/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.kernel.upgrade.util;

import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.Validator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Albert Gomes Cabral
 */
public class TemplateReplacement {

	public TemplateReplacement(
		String contextVariable, String deprecatedClassName, String from,
		boolean hasMessage, String id, String[] methodNames,
		String[] newAssigns, String to) {

		_contextVariable = contextVariable;
		_deprecatedClassName = deprecatedClassName;
		_hasMessage = hasMessage;
		_id = id;
		_methodNames = methodNames;
		_newAssigns = newAssigns;
		_to = to;

		if (Validator.isNotNull(deprecatedClassName)) {
			String deprecatedClassNameRegex = Pattern.quote(
				deprecatedClassName);

			_deprecatedClassPattern = Pattern.compile(
				StringBundler.concat(
					"(?<![\\w.])([A-Za-z_]\\w*)\\s*=\\s*(?:(?:serviceLocator",
					"\\s*\\.\\s*findService|utilLocator\\s*\\.\\s*findUtil)",
					"\\s*\\(\\s*\"", deprecatedClassNameRegex,
					"\"\\s*\\)|staticUtil\\s*\\[\\s*\"",
					deprecatedClassNameRegex, "\"\\s*\\])"));
		}
		else {
			_deprecatedClassPattern = null;
		}

		if (Validator.isNull(from)) {
			_pattern = null;
			_replacement = null;
		}
		else if (from.startsWith(_REGEX_PREFIX)) {
			_pattern = Pattern.compile(from.substring(_REGEX_PREFIX.length()));
			_replacement = to;
		}
		else {
			_pattern = Pattern.compile(_getLiteralRegex(from));
			_replacement = Matcher.quoteReplacement(GetterUtil.getString(to));
		}
	}

	public String getContextVariable() {
		return _contextVariable;
	}

	public String getDeprecatedClassName() {
		return _deprecatedClassName;
	}

	public Pattern getDeprecatedClassPattern() {
		return _deprecatedClassPattern;
	}

	public String getId() {
		return _id;
	}

	public String[] getMethodNames() {
		return _methodNames;
	}

	public String[] getNewAssigns() {
		return _newAssigns;
	}

	public Pattern getPattern() {
		return _pattern;
	}

	public String getReplacement() {
		return _replacement;
	}

	public String getTo() {
		return _to;
	}

	public boolean hasMessage() {
		return _hasMessage;
	}

	private String _getLiteralRegex(String from) {
		StringBundler sb = new StringBundler(3);

		if (_isWordCharacter(from.charAt(0))) {
			sb.append("(?<![\\w.])");
		}

		sb.append(Pattern.quote(from));

		if (_isWordCharacter(from.charAt(from.length() - 1))) {
			sb.append("(?!\\w)");
		}

		return sb.toString();
	}

	private boolean _isWordCharacter(char c) {
		if (Character.isLetterOrDigit(c) || (c == '_')) {
			return true;
		}

		return false;
	}

	private static final String _REGEX_PREFIX = "regex:";

	private final String _contextVariable;
	private final String _deprecatedClassName;
	private final Pattern _deprecatedClassPattern;
	private final boolean _hasMessage;
	private final String _id;
	private final String[] _methodNames;
	private final String[] _newAssigns;
	private final Pattern _pattern;
	private final String _replacement;
	private final String _to;

}