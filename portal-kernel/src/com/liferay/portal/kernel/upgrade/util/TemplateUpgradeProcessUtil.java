/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.kernel.upgrade.util;

import com.liferay.petra.string.CharPool;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Albert Gomes Cabral
 */
public class TemplateUpgradeProcessUtil {

	public static List<String> getMessageIds(
		String template, List<TemplateReplacement> templateReplacements) {

		List<String> messageIds = new ArrayList<>();

		if (Validator.isNull(template)) {
			return messageIds;
		}

		for (TemplateReplacement templateReplacement : templateReplacements) {
			if (!templateReplacement.hasMessage()) {
				continue;
			}

			if (Validator.isNotNull(
					templateReplacement.getDeprecatedClassName())) {

				if (_hasDeprecatedClassUsage(template, templateReplacement)) {
					messageIds.add(templateReplacement.getId());
				}

				continue;
			}

			Pattern pattern = templateReplacement.getPattern();

			Matcher matcher = pattern.matcher(template);

			if (!matcher.find()) {
				continue;
			}

			messageIds.add(templateReplacement.getId());
		}

		return messageIds;
	}

	public static String replace(
		boolean squareBracketSyntax, String template,
		List<TemplateReplacement> templateReplacements) {

		if (Validator.isNull(template)) {
			return template;
		}

		for (TemplateReplacement templateReplacement : templateReplacements) {
			if (templateReplacement.hasMessage()) {
				continue;
			}

			if (Validator.isNull(
					templateReplacement.getDeprecatedClassName())) {

				template = _replacePattern(
					squareBracketSyntax, template, templateReplacement);
			}
			else {
				template = _replaceDeprecatedClass(
					squareBracketSyntax, template, templateReplacement);
			}
		}

		return template;
	}

	private static String _addNewAssigns(
		String[] newAssigns, boolean squareBracketSyntax, String template) {

		if (ArrayUtil.isEmpty(newAssigns)) {
			return template;
		}

		StringBundler sb = new StringBundler(newAssigns.length * 4);

		for (String newAssign : newAssigns) {
			String variableName = StringUtil.trim(
				newAssign.substring(0, newAssign.indexOf(CharPool.EQUAL)));

			Pattern pattern = Pattern.compile(
				StringBundler.concat(
					"(?<![\\w.])", Pattern.quote(variableName), "\\s*=(?!=)"));

			Matcher matcher = pattern.matcher(template);

			if (matcher.find()) {
				continue;
			}

			if (squareBracketSyntax) {
				sb.append("[#assign ");
				sb.append(newAssign);
				sb.append(" /]");
			}
			else {
				sb.append("<#assign ");
				sb.append(newAssign);
				sb.append(" />");
			}

			sb.append(StringPool.NEW_LINE);
		}

		if (sb.index() == 0) {
			return template;
		}

		Matcher matcher = _ftlDirectivePattern.matcher(template);

		if (matcher.lookingAt()) {
			return StringBundler.concat(
				template.substring(0, matcher.end()), sb,
				template.substring(matcher.end()));
		}

		return sb + template;
	}

	private static List<String> _getDeprecatedClassVariableNames(
		String template, TemplateReplacement templateReplacement) {

		List<String> variableNames = new ArrayList<>();

		Pattern pattern = templateReplacement.getDeprecatedClassPattern();

		Matcher matcher = pattern.matcher(template);

		while (matcher.find()) {
			variableNames.add(matcher.group(1));
		}

		return variableNames;
	}

	private static Pattern _getMethodCallPattern(
		String[] methodNames, String variableName) {

		return Pattern.compile(
			StringBundler.concat(
				"(?<![\\w.])", Pattern.quote(variableName), "(\\s*\\.\\s*(?:",
				StringUtil.merge(methodNames, "|"), ")\\s*\\()"));
	}

	private static boolean _hasDeprecatedClassUsage(
		String template, TemplateReplacement templateReplacement) {

		List<String> variableNames = _getDeprecatedClassVariableNames(
			template, templateReplacement);

		if (variableNames.isEmpty()) {
			return false;
		}

		String[] methodNames = templateReplacement.getMethodNames();

		if (ArrayUtil.isEmpty(methodNames)) {
			return true;
		}

		for (String variableName : variableNames) {
			Pattern pattern = _getMethodCallPattern(methodNames, variableName);

			Matcher matcher = pattern.matcher(template);

			if (matcher.find()) {
				return true;
			}
		}

		return false;
	}

	private static String _replaceDeprecatedClass(
		boolean squareBracketSyntax, String template,
		TemplateReplacement templateReplacement) {

		List<String> variableNames = _getDeprecatedClassVariableNames(
			template, templateReplacement);

		if (variableNames.isEmpty()) {
			return template;
		}

		String[] methodNames = templateReplacement.getMethodNames();

		if (ArrayUtil.isNotEmpty(methodNames)) {
			String newTemplate = template;

			for (String variableName : variableNames) {
				Pattern pattern = _getMethodCallPattern(
					methodNames, variableName);

				Matcher matcher = pattern.matcher(newTemplate);

				newTemplate = matcher.replaceAll(
					Matcher.quoteReplacement(templateReplacement.getTo()) +
						"$1");
			}

			if (newTemplate.equals(template)) {
				return template;
			}

			return _addNewAssigns(
				templateReplacement.getNewAssigns(), squareBracketSyntax,
				newTemplate);
		}

		String contextVariable = templateReplacement.getContextVariable();

		if (Validator.isNull(contextVariable)) {
			return template;
		}

		Pattern deprecatedClassPattern =
			templateReplacement.getDeprecatedClassPattern();

		Matcher deprecatedClassMatcher = deprecatedClassPattern.matcher(
			template);

		template = deprecatedClassMatcher.replaceAll(StringPool.BLANK);

		for (String variableName : variableNames) {
			if (variableName.equals(contextVariable)) {
				continue;
			}

			Pattern pattern = Pattern.compile(
				StringBundler.concat(
					"(?<![\\w.])", Pattern.quote(variableName), "(?!\\w)"));

			Matcher matcher = pattern.matcher(template);

			template = matcher.replaceAll(
				Matcher.quoteReplacement(contextVariable));
		}

		Matcher emptyAssignMatcher = _emptyAssignPattern.matcher(template);

		return emptyAssignMatcher.replaceAll(StringPool.BLANK);
	}

	private static String _replacePattern(
		boolean squareBracketSyntax, String template,
		TemplateReplacement templateReplacement) {

		Pattern pattern = templateReplacement.getPattern();

		Matcher matcher = pattern.matcher(template);

		if (!matcher.find()) {
			return template;
		}

		String newTemplate = matcher.replaceAll(
			templateReplacement.getReplacement());

		if (newTemplate.equals(template)) {
			return template;
		}

		return _addNewAssigns(
			templateReplacement.getNewAssigns(), squareBracketSyntax,
			newTemplate);
	}

	private static final Pattern _emptyAssignPattern = Pattern.compile(
		"<#assign\\s*/?>|\\[#assign\\s*/?\\]");
	private static final Pattern _ftlDirectivePattern = Pattern.compile(
		"\\s*[<\\[]#ftl\\b[^>\\]]*[>\\]]\\s*");

}