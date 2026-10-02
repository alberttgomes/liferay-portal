/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.util;

import com.liferay.portal.kernel.util.FileUtil;
import com.liferay.portal.kernel.util.MimeTypesUtil;
import com.liferay.portal.kernel.util.StringUtil;

/**
 * @author Jürgen Kappler
 */
public class BrowserExecutableContentTypeUtil {

	public static boolean isBrowserExecutableContentType(String contentType) {
		if (contentType == null) {
			return false;
		}

		String lowerCaseContentType = StringUtil.toLowerCase(contentType);

		if (lowerCaseContentType.startsWith("application/javascript") ||
			lowerCaseContentType.startsWith("application/xhtml+xml") ||
			lowerCaseContentType.startsWith("image/svg+xml") ||
			lowerCaseContentType.startsWith("text/html") ||
			lowerCaseContentType.startsWith("text/javascript")) {

			return true;
		}

		return false;
	}

	public static boolean isBrowserExecutableContentType(
		String contentType, String fileName) {

		if (isBrowserExecutableContentType(contentType) ||
			isBrowserExecutableContentType(
				MimeTypesUtil.getExtensionContentType(
					FileUtil.getExtension(fileName)))) {

			return true;
		}

		return false;
	}

}