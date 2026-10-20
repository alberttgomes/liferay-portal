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
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.test.rule.LiferayUnitTestRule;

import java.io.IOException;

import java.math.BigInteger;

import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import java.security.MessageDigest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
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
	public void testChecksums() throws Exception {
		Set<String> relativePaths = new TreeSet<>();

		for (Path path : _templateReplacementsPaths) {
			String relativePath = StringUtil.replace(
				String.valueOf(_portalPath.relativize(path)), '\\', '/');

			relativePaths.add(relativePath);

			Assert.assertEquals(
				StringBundler.concat(
					"Released template replacements are frozen. Add the new ",
					"patterns to a new upgrade step instead of changing ",
					relativePath),
				_checksums.get(relativePath), _getChecksum(path));
		}

		Assert.assertEquals(new TreeSet<>(_checksums.keySet()), relativePaths);
	}

	@Test
	public void testIds() throws Exception {
		Map<String, Path> paths = new HashMap<>();

		for (Path path : _templateReplacementsPaths) {
			JSONArray jsonArray = _getJSONArray(path);

			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject jsonObject = jsonArray.getJSONObject(i);

				String id = jsonObject.getString("id");

				Assert.assertTrue(
					"Missing ID in " + path, id.startsWith("FTL-"));

				Path previousPath = paths.putIfAbsent(id, path);

				if (previousPath == null) {
					continue;
				}

				Assert.assertEquals(
					StringBundler.concat(
						"Pattern ", id, " is used in ", previousPath, " and ",
						path),
					previousPath, path);
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

		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(
				_portalPath.resolve(
					"portal-kernel/src/com/liferay/portal/kernel/upgrade" +
						"/dependencies"),
				"template-replacements-*.json")) {

			for (Path path : directoryStream) {
				paths.add(path);
			}
		}

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

	private String _getChecksum(Path path) throws Exception {
		MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");

		String content = new String(
			Files.readAllBytes(path), StandardCharsets.UTF_8);

		byte[] bytes = messageDigest.digest(
			StringUtil.replace(
				content, "\r\n", StringPool.NEW_LINE
			).getBytes(
				StandardCharsets.UTF_8
			));

		return String.format("%064x", new BigInteger(1, bytes));
	}

	private JSONArray _getJSONArray(Path path) throws Exception {
		return JSONFactoryUtil.createJSONArray(
			new String(Files.readAllBytes(path), StandardCharsets.UTF_8));
	}

	private static final String[] _MODULES_DIR_NAMES = {
		"modules/apps", "modules/dxp/apps"
	};

	private static final Map<String, String> _checksums = HashMapBuilder.put(
		"modules/apps/dynamic-data-mapping/dynamic-data-mapping-service/src" +
			"/main/resources/com/liferay/dynamic/data/mapping/internal" +
				"/upgrade/v7_1_2/dependencies/template-replacements.json",
		"4204178a3751377423aef6b55c0cb31cf7005d62bf94ccdc1762301227e01140"
	).put(
		"modules/apps/fragment/fragment-service/src/main/resources/com" +
			"/liferay/fragment/internal/upgrade/v4_1_1/dependencies" +
				"/template-replacements.json",
		"f34bf72f15142e0a849d0766c78e242a3d2bbe1ab852afbbcfdafe23f9ad0ca5"
	).put(
		"portal-kernel/src/com/liferay/portal/kernel/upgrade/dependencies" +
			"/template-replacements-1.json",
		"951806589ed3dbb5f0a157501b5aa3f49f09c92e85e5b320625e93d23a3bda57"
	).build();
	private static Path _portalPath;
	private static List<Path> _templateReplacementsPaths;

}