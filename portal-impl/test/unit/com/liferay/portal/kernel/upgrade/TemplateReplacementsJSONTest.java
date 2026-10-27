/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.kernel.upgrade;

import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.json.JSONFactoryImpl;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.test.ReflectionTestUtil;
import com.liferay.portal.kernel.upgrade.util.TemplateReplacement;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.test.rule.LiferayUnitTestRule;

import java.io.IOException;

import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Albert Gomes Cabral
 */
public class TemplateReplacementsJSONTest {

	@ClassRule
	@Rule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@BeforeClass
	public static void setUpClass() throws Exception {
		ReflectionTestUtil.setFieldValue(
			JSONFactoryUtil.class, "_jsonFactory", new JSONFactoryImpl());

		_portalPath = Paths.get(System.getProperty("user.dir"));

		_templateReplacementsPaths = _getTemplateReplacementsPaths();
	}

	@Test
	public void testIds() throws Exception {
		for (Path path : _templateReplacementsPaths) {
			JSONArray jsonArray = _getJSONArray(path);

			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject jsonObject = jsonArray.getJSONObject(i);

				String id = jsonObject.getString("id");

				Assert.assertTrue(
					"Missing ID in " + path, id.startsWith("FTL-"));
			}
		}
	}

	@Test
	public void testTo() throws Exception {
		for (Path path : _templateReplacementsPaths) {
			JSONArray jsonArray = _getJSONArray(path);

			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject jsonObject = jsonArray.getJSONObject(i);

				String to = jsonObject.getString("to");

				if (to.contains(StringPool.DOLLAR)) {
					continue;
				}

				TemplateReplacement templateReplacement =
					new TemplateReplacement(
						jsonObject.getString("contextVariable"),
						jsonObject.getString("deprecatedClassName"),
						jsonObject.getString("from"),
						jsonObject.getBoolean("hasMessage"),
						jsonObject.getString("id"),
						JSONUtil.toStringArray(
							jsonObject.getJSONArray("methodNames")),
						JSONUtil.toStringArray(
							jsonObject.getJSONArray("newAssigns")),
						to);

				Pattern pattern = templateReplacement.getPattern();

				if (pattern == null) {
					continue;
				}

				Matcher matcher = pattern.matcher(to);

				Assert.assertFalse(
					StringBundler.concat(
						"Pattern ", jsonObject.getString("id"), " in ", path,
						" matches its own replacement"),
					matcher.find());
			}
		}
	}

	private static List<Path> _getTemplateReplacementsPaths() throws Exception {
		List<Path> paths = new ArrayList<>();

		for (String modulesDirName : _MODULES_DIR_NAMES) {
			Path modulesPath = _portalPath.resolve(modulesDirName);

			if (Files.notExists(modulesPath)) {
				continue;
			}

			Files.walkFileTree(
				modulesPath,
				new SimpleFileVisitor<Path>() {

					@Override
					public FileVisitResult preVisitDirectory(
						Path dirPath, BasicFileAttributes basicFileAttributes) {

						String dirName = String.valueOf(dirPath.getFileName());

						if (dirName.startsWith(StringPool.PERIOD) ||
							Objects.equals(dirName, "build") ||
							Objects.equals(dirName, "classes") ||
							Objects.equals(dirName, "node_modules")) {

							return FileVisitResult.SKIP_SUBTREE;
						}

						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult visitFile(
							Path path, BasicFileAttributes basicFileAttributes)
						throws IOException {

						String pathString = StringUtil.replace(
							String.valueOf(path), '\\', '/');

						if (pathString.endsWith(
								"/dependencies/template-replacements.json") &&
							pathString.contains("/src/main/resources/") &&
							pathString.contains("/upgrade/")) {

							paths.add(path);
						}

						return FileVisitResult.CONTINUE;
					}

				});
		}

		return paths;
	}

	private JSONArray _getJSONArray(Path path) throws Exception {
		return JSONFactoryUtil.createJSONArray(
			new String(Files.readAllBytes(path), StandardCharsets.UTF_8));
	}

	private static final String[] _MODULES_DIR_NAMES = {
		"modules/apps", "modules/dxp/apps"
	};

	private static Path _portalPath;
	private static List<Path> _templateReplacementsPaths;

}