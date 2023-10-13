/**
 * SPDX-FileCopyrightText: (c) 2023 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.source.formatter.check;

import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.petra.string.StringUtil;
import com.liferay.source.formatter.check.util.JavaSourceUtil;
import com.liferay.source.formatter.parser.JavaClass;
import com.liferay.source.formatter.parser.JavaClassParser;
import com.liferay.source.formatter.parser.JavaMethod;
import com.liferay.source.formatter.parser.JavaTerm;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Albert Gomes Cabral
 */
public class UpgradeJavaFetchCommerceAccountGroupByExternalReferenceCodeCheck
	extends BaseUpgradeCheck {

	@Override
	protected String format(
			String fileName, String absolutePath, String content)
		throws Exception {

		String newContent = content;

		JavaClass javaClass = JavaClassParser.parseJavaClass(fileName, content);

		for (JavaTerm childJavaTerm : javaClass.getChildJavaTerms()) {
			if (!childJavaTerm.isJavaMethod()) {
				continue;
			}

			JavaMethod javaMethod = (JavaMethod)childJavaTerm;

			String javaMethodContent = javaMethod.getContent();

			String newJavaMethodContent = javaMethodContent;

			Matcher matcher =
				_fetchCommerceAccountGroupByExternalReferenceCodePattern.
					matcher(javaMethodContent);

			while (matcher.find()) {
				String methodCall = JavaSourceUtil.getMethodCall(
					javaMethodContent, matcher.start());

				if (!hasClassOrVariableName(
						"AccountGroupLocalService", content, javaMethodContent,
						methodCall)) {

					continue;
				}

				String newMethodCall = StringUtil.replace(
					methodCall,
					"fetchCommerceAccountGroupByExternalReferenceCode",
					"fetchAccountGroupByExternalReferenceCode");

				List<String> parameterList = JavaSourceUtil.getParameterList(
					methodCall);

				String message = StringBundler.concat(
					"Unable to format method ",
					"fetchCommerceAccountGroupByExternalReferenceCode from ",
					"CommerceAccountGroupLocalService, Fill the new changes ",
					"manually, see TICKET.");

				String[] parameterTypes = {"long", "String"};

				if (hasValidParameters(
						2, fileName, javaMethodContent, message, parameterList,
						parameterTypes)) {

					String newParameters = StringBundler.concat(
						parameterList.get(1), StringPool.COMMA_AND_SPACE,
						parameterList.get(0));

					newMethodCall = StringUtil.replace(
						newMethodCall, JavaSourceUtil.getParameters(methodCall),
						newParameters);
				}

				newJavaMethodContent = StringUtil.replace(
					javaMethodContent, methodCall, newMethodCall);
			}

			newContent = StringUtil.replace(
				newContent, javaMethodContent, newJavaMethodContent);
		}

		return newContent;
	}

	private static final Pattern
		_fetchCommerceAccountGroupByExternalReferenceCodePattern =
			Pattern.compile("\\w*\\s*?\\.\\s*?fetchCommerceAccount" +
				"GroupByExternalReferenceCode\\(\\s*.+,\\s*.+\\)");

}