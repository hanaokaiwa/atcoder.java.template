package testbase;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Properties;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DynamicTest;

@DisplayNameGeneration(CustomDisplayNameGenerator.class)
public abstract class TestBase {

	/** システム入力を保存 */
	private static InputStream systemIn = System.in;
	/** システム出力を保存 */
	private static PrintStream systemOut = System.out;

	/** カスタマイズ入力 */
	protected static StandardInputSnatcher in = new StandardInputSnatcher();
	/** カスタマイズ出力 */
	protected static ByteArrayOutputStream out = new ByteArrayOutputStream();
	private static PrintStream mySystemOut;

	/** Zipファイルから入力用 */
	private static ZipFile zip = null;

	/** システムの改行コード */
	protected static final String LF = System.lineSeparator();

	/** プロパティーファイル */
	private static final String PROPERTIES_FILE = "external.properties";
	/** 外部フォルダーを使用するかどうかのキー */
	private static final String USE_EXTERNAL_KEY = "USE_EXTERNAL";
	/** 外部フォルダーのキー */
	private static final String EXTERNAL_FOLDER_KEY = "EXTERNAL_FOLDER";
	/** プロパティーファイル読み込み用 */
	private static final Properties prop = new Properties();
	/** 外部フォルダーを使用するかどうか */
	private static boolean USE_EXTERNAL = false;
	/** 外部フォルダー */
	private static String EXTERNAL_FOLDER = "";
	/** 外部の入力ファイルのフォルダー */
	private static final String IN_FOLDER = "in";
	/** 外部の出力ファイルのフォルダー */
	private static final String OUT_FOLDER = "out";
	/** ZIPファイルの拡張子 */
	private static final String ZIP_EXTENSION = ".zip";
	/** ZIPファイルのパス分割符号 */
	private static final String ZIP_FILE_SEPARATOR = "/";

	static {
		InputStream is = TestBase.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE);
		try {
			if (null != is) {
				prop.load(is);
				USE_EXTERNAL = Boolean.parseBoolean((String) prop.getOrDefault(USE_EXTERNAL_KEY, "false"));
				EXTERNAL_FOLDER = (String) prop.getOrDefault(EXTERNAL_FOLDER_KEY, "");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		System.setIn(in);
		System.setOut(mySystemOut = new PrintStream(out));
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterAll
	static void tearDownAfterClass() throws Exception {
		System.setOut(systemOut);
		System.setIn(systemIn);
		mySystemOut.close();
		out.close();
		if (null != zip) {
			zip.close();
		}
	}

	/**
	 * カスタマイズ入力と出力をクリアする
	 *
	 * @throws IOException
	 */
	@BeforeEach
	void clearInAndOut() throws IOException {
		out.reset();
		in.clear();
	}

	/**
	 * カスタマイズ入力と出力をクリアする（例外を発生させない）
	 */
	void clearInAndOutWithoutException() {
		try {
			clearInAndOut();
			System.gc();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * テストケースを実行した結果をチェック
	 *
	 * @param expected 予定される結果
	 */
	protected void assertResultIs(String expected) {
		assertEquals(replaceLineSeparator(expected + LF), replaceLineSeparator(out.toString()));
	}

	/**
	 * テストケースを実行した結果をチェック
	 *
	 * @param expecteds 予定される結果の一覧
	 */
	protected void assertResultIn(String... expecteds) {
		String actualResult = replaceLineSeparator(out.toString());
		assertTrue(Arrays.stream(expecteds).filter(s -> replaceLineSeparator(s + LF).equals(actualResult)).count() > 0,
				"result is " + actualResult);
	}

	/**
	 * テストケースを実行した結果をチェック
	 *
	 * @param regexp 予定される結果の正規表現
	 */
	protected void assertResultMatches(String regexp) {
		assertTrue(replaceLineSeparator(out.toString()).matches(replaceLineSeparator(regexp + LF)),
				"result is " + out.toString());
	}

	/**
	 * テストケースを実行した結果をチェック
	 *
	 * @param expected 予定される結果
	 */
	protected void assertResultIs(double expected) {
		assertEquals(expected, Double.parseDouble(out.toString()));
	}

	/**
	 * テストケースを実行した結果をチェック
	 *
	 * @param expected  予定される結果
	 * @param tolerance 誤差範囲
	 */
	protected void assertResultIsAbout(double expected, double tolerance) {
		assertTrue(Math.abs(Double.parseDouble(out.toString()) - expected) < tolerance,
				"number is " + out.toString() + ", expected is " + expected);
	}

	/**
	 * テスト対象のメソッドを実行
	 */
	protected void execute() {
		try {
			// テストクラス名から末尾の「Test」を取ったクラス名のクラスを取得し、mainメソッドを実行
			Class<?> clazz = Class.forName(this.getClass().getName().replaceFirst("Test$", ""));
			Method method = clazz.getDeclaredMethod("main", String[].class);
			method.invoke(null, (Object) null);
		} catch (ClassNotFoundException | SecurityException | IllegalArgumentException | NoSuchMethodException
				| IllegalAccessException | InvocationTargetException e) {
			e.printStackTrace();
			fail(e);
		}
	}

	/**
	 * テストを実施する
	 *
	 * @param input    入力文字列
	 * @param expected 予想される実行結果
	 */
	protected void check(String input, String expected) {
		in.input(input);
		execute();
		assertResultIs(expected);
	}

	/**
	 * テストを実施する
	 *
	 * @param input    入力文字列を保存するファイル
	 * @param expected 予想される実行結果
	 */
	protected void check(File input, String expected) {
		try (InputStream inputIs = new FileInputStream(input)) {
			check(inputIs, expected);
		} catch (IOException e) {
			e.printStackTrace();
			fail(e);
		}
	}

	/**
	 * テストを実施する
	 *
	 * @param input    入力文字列を保存するファイル
	 * @param expected 予想される実行結果を保存するファイル
	 */
	protected void check(File input, File expected) {
		check(input, expected, (inputIs, expectedIs) -> check(inputIs, expectedIs));
	}

	/**
	 * テストを実施する
	 *
	 * @param input    入力文字列を保存するファイル
	 * @param expected 予想される実行結果を保存するファイル
	 */
	protected void check(File input, File expected, InputStreamChecker checker) {
		try (InputStream inputIs = new FileInputStream(input); InputStream expectedIs = new FileInputStream(expected)) {
			checker.check(inputIs, expectedIs);
		} catch (IOException e) {
			e.printStackTrace();
			fail(e);
		}
	}

	/**
	 * テストを実施する
	 *
	 * @param inputIs    入力文字列を保存するInputStream
	 * @param expectedIs 予想される実行結果を保存するInputStream
	 */
	private void check(InputStream inputIs, InputStream expectedIs) {
		try (ByteArrayOutputStream inputBaos = new ByteArrayOutputStream();
				ByteArrayOutputStream expectedBaos = new ByteArrayOutputStream()) {
			byte[] buffer = new byte[8192];
			int length = 0;
			while (-1 != (length = inputIs.read(buffer))) {
				inputBaos.write(buffer, 0, length);
			}
			in.bytes = inputBaos.toByteArray();
			while (-1 != (length = expectedIs.read(buffer))) {
				expectedBaos.write(buffer, 0, length);
			}
			execute();
			assertEquals(replaceLineSeparator(expectedBaos.toString()), replaceLineSeparator(out.toString()));
		} catch (IOException e) {
			e.printStackTrace();
			fail(e);
		}
	}

	/**
	 * テストを実施する
	 *
	 * @param inputIs  入力文字列を保存するInputStream
	 * @param expected 予想される実行結果
	 */
	protected void check(InputStream inputIs, String expected) {
		try (ByteArrayOutputStream inputBaos = new ByteArrayOutputStream();
				ByteArrayOutputStream expectedBaos = new ByteArrayOutputStream()) {
			byte[] buffer = new byte[8192];
			int length = 0;
			while (-1 != (length = inputIs.read(buffer))) {
				inputBaos.write(buffer, 0, length);
			}
			in.bytes = inputBaos.toByteArray();
			execute();
			assertResultIs(expected);
		} catch (IOException e) {
			e.printStackTrace();
			fail(e);
		}
	}

	/**
	 * テストを実施する
	 *
	 * @param inputIs    入力文字列を保存するInputStream
	 * @param expectedIs 予想される実行結果を保存するInputStream
	 * @param tolerance  誤差範囲
	 */
	protected void checkResultIsAbout(InputStream inputIs, InputStream expectedIs, double tolerance) {
		try (ByteArrayOutputStream inputBaos = new ByteArrayOutputStream();
				Scanner expectedScanner = new Scanner(expectedIs)) {
			byte[] buffer = new byte[8192];
			int length = 0;
			while (-1 != (length = inputIs.read(buffer))) {
				inputBaos.write(buffer, 0, length);
			}
			in.bytes = inputBaos.toByteArray();
			execute();
			assertResultIsAbout(expectedScanner.nextDouble(), tolerance);
		} catch (IOException e) {
			e.printStackTrace();
			fail(e);
		}
	}

	/**
	 * テストを実施する
	 *
	 * @param input    入力文字列
	 * @param expected 予想される実行結果の一覧
	 */
	protected void checkResultIn(String input, String... expected) {
		in.input(input);
		execute();
		assertResultIn(expected);
	}

	/**
	 * テストを実施する
	 *
	 * @param input     入力文字列
	 * @param expected  予想される実行結果
	 * @param tolerance 誤差範囲
	 */
	protected void checkResultIsAbout(String input, double expected, double tolerance) {
		in.input(input);
		execute();
		assertResultIsAbout(expected, tolerance);
	}

	/**
	 * テストを実施する
	 *
	 * @param input          入力文字列
	 * @param expectedRegexp 予想される実行結果の正規表現
	 */
	protected void checkResultMatches(String input, String expectedRegexp) {
		in.input(input);
		execute();
		assertResultMatches(expectedRegexp);
	}

	/**
	 * 想定結果がが空のテストを実施する
	 *
	 * @param input 入力文字列
	 */
	protected void checkResultIsEmpty(String input) {
		in.input(input);
		execute();
		assertEquals("", out.toString());
	}

	/**
	 * テストを実施する
	 *
	 * @param input    入力文字列
	 * @param expected 予想される実行結果
	 */
	protected void check(String input, double expected) {
		in.input(input);
		execute();
		assertResultIs(expected);
	}

	/**
	 * テストケースを実行した結果をチェック
	 *
	 * @param expected  予定した結果
	 * @param number    実行結果の数字形式の文字列
	 * @param tolerance 誤差範囲
	 */
	protected void assertNumberIsAbout(double expected, String number, double tolerance) {
		assertTrue(Math.abs(Double.parseDouble(number) - expected) < tolerance,
				"number is " + number + ", expected is " + expected);
	}

	/**
	 * テストケースを実行した結果をチェック
	 *
	 * @param expected  予定した結果
	 * @param number    実行結果の数字
	 * @param tolerance 誤差範囲
	 */
	protected void assertNumberIsAbout(double expected, double number, double tolerance) {
		assertTrue(Math.abs(number - expected) < tolerance, "number is " + number + ", expected is " + expected);
	}

	/**
	 * 入力文字列の改行コードをすべてLFに置き換える
	 *
	 * @param string 入力文字列
	 * @return 入力文字列の改行コードをすべてLFに置き換えた文字列
	 */
	String replaceLineSeparator(String string) {
		return string.replaceAll("\\R", LF);
	}

	/**
	 * 外部のテストケースを読み込み、動的テストを作成する
	 *
	 * @param path 外部のテストケースのパス
	 * @return 作成された動的テストの一覧
	 */
	protected Collection<DynamicTest> checkExternal(String path) {
		return checkExternal(path, this::check, "");
	}

	/**
	 * 外部のテストケースを読み込み、動的テストを作成する
	 *
	 * @param path      外部のテストケースのパス
	 * @param tolerance 誤差範囲
	 * @return 作成された動的テストの一覧
	 */
	protected Collection<DynamicTest> checkExternal(String path, double tolerance) {
		return checkExternal(path, (inputIs, expectedIs) -> checkResultIsAbout(inputIs, expectedIs, tolerance), "");
	}

	/**
	 * 外部のテストケースを読み込み、動的テストを作成する
	 *
	 * @param path    外部のテストケースのパス
	 * @param checker テストの実行方法
	 * @return 作成された動的テストの一覧
	 */
	protected Collection<DynamicTest> checkExternal(String path, InputStreamChecker checker) {
		return checkExternal(path, checker, "");
	}

	/**
	 * 外部のテストケースを読み込み、動的テストを作成する
	 *
	 * @param path     外部のテストケースのパス
	 * @param testcase 対象テストケース名（空の場合ではすべてのテストケース）
	 * @return 作成された動的テストの一覧
	 */
	protected Collection<DynamicTest> checkExternal(String path, String testcase) {
		return checkExternal(path, this::check, testcase);
	}

	/**
	 * 外部のテストケースを読み込み、動的テストを作成する
	 *
	 * @param path      外部のテストケースのパス
	 * @param tolerance 誤差範囲
	 * @param testcase  対象テストケース名（空の場合ではすべてのテストケース）
	 * @return 作成された動的テストの一覧
	 */
	protected Collection<DynamicTest> checkExternal(String path, double tolerance, String testcase) {
		return checkExternal(path, (inputIs, expectedIs) -> checkResultIsAbout(inputIs, expectedIs, tolerance),
				testcase);
	}

	/**
	 * 外部のテストケースを読み込み、動的テストを作成する
	 *
	 * @param path     外部のテストケースのパス
	 * @param checker  テストの実行方法
	 * @param testcase 対象テストケース名（空の場合ではすべてのテストケース）
	 * @return 作成された動的テストの一覧
	 */
	protected Collection<DynamicTest> checkExternal(String path, InputStreamChecker checker, String testcase) {
		assertNotNull(path);
		assertNotNull(checker);
		assertNotNull(testcase);
		if ((null != path) && (!path.isBlank())) {
			// パスの分割符号をシステム標準のものに置き換える
			path = path.replaceAll("[\\\\/]", Matcher.quoteReplacement(File.separator));
			File baseFolder = new File(EXTERNAL_FOLDER);
			if (USE_EXTERNAL && baseFolder.exists() && baseFolder.isDirectory()) {
				Collection<DynamicTest> collection = checkExternalFolder(path, checker, testcase);
				if (!collection.isEmpty()) {
					return collection;
				}
			}
			int lastSeparator = path.lastIndexOf(File.separatorChar);
			if (lastSeparator > 0) {
				String path1 = path.substring(0, lastSeparator), path2 = path.substring(lastSeparator + 1);
				Collection<DynamicTest> collection = checkExternalZip(path1, path2, checker, testcase);
				if (!collection.isEmpty()) {
					return collection;
				}
			}
			Collection<DynamicTest> collection = checkExternalZip(path, "", checker, testcase);
			if (!collection.isEmpty()) {
				return collection;
			}
		}
		return Collections.<DynamicTest>emptyList();
	}

	/**
	 * フォルダーにある外部のテストケースを読み込み、動的テストを作成する
	 *
	 * @param path     外部のテストケースのパス
	 * @param checker  テストの実行方法
	 * @param testcase 対象テストケース名（空の場合ではすべてのテストケース）
	 * @return 作成された動的テストの一覧
	 */
	private Collection<DynamicTest> checkExternalFolder(String path, InputStreamChecker checker, String testcase) {
		File folder = Paths.get(EXTERNAL_FOLDER, path).toFile();
		if (folder.exists() && folder.isDirectory()) {
			File inFolder = Paths.get(folder.getAbsolutePath(), IN_FOLDER).toFile();
			File outFolder = Paths.get(folder.getAbsolutePath(), OUT_FOLDER).toFile();
			if (inFolder.exists() && inFolder.isDirectory() && outFolder.exists() && outFolder.isDirectory()) {
				File[] inFiles = inFolder.listFiles();
				Arrays.sort(inFiles);
				if (null != inFiles) {
					return Arrays.stream(inFiles).filter(File::isFile)
							.filter(inFile -> testcase.isEmpty() || inFile.getName().equals(testcase)).map(inFile -> {
								File outFile = Paths
										.get(outFolder.getAbsolutePath(),
												inFile.getName().replaceAll("\\." + IN_FOLDER + "$", "." + OUT_FOLDER))
										.toFile();
								return new File[] { inFile, outFile };
							}).filter(files -> files[1].exists() && files[1].isFile()).map(files -> DynamicTest
									.dynamicTest(files[0].getName().replaceAll("\\." + IN_FOLDER + "$", ""), () -> {
										clearInAndOutWithoutException();
										check(files[0], files[1], checker);
									}))
							.collect(Collectors.toList());
				}
			}
		}
		return Collections.<DynamicTest>emptyList();
	}

	/**
	 * Zipファイルにある外部のテストケースを読み込み、動的テストを作成する
	 *
	 * @param path     外部のテストケースのパス
	 * @param prefix   Zipファイル内部の先頭フォルダー
	 * @param checker  テストの実行方法
	 * @param testcase 対象テストケース名（空の場合ではすべてのテストケース）
	 * @return 作成された動的テストの一覧
	 */
	private Collection<DynamicTest> checkExternalZip(String path, String prefix, InputStreamChecker checker,
			String testcase) {
		int lastSeparator = path.lastIndexOf(File.separatorChar);
		File baseFolder = (lastSeparator > 0) ? Paths.get(EXTERNAL_FOLDER, path.substring(0, lastSeparator)).toFile()
				: new File(EXTERNAL_FOLDER);
		String zipFileName = path.substring(lastSeparator + 1) + ZIP_EXTENSION;
		// zipファイルのパターンを使ってzipファイルを探す
		Pattern pattern = Pattern.compile(zipFileName, Pattern.CASE_INSENSITIVE);
		File[] zipFiles = baseFolder.listFiles(file -> pattern.matcher(file.getName()).matches());
		if (zipFiles != null) {
			for (File zipFile : zipFiles) {
				if (zipFile.isFile() && (zipFile.length() > 0L)) {
					Collection<DynamicTest> collection = checkExternalZip(zipFile, prefix, checker, testcase);
					if (!collection.isEmpty()) {
						return collection;
					}
				}
			}
		}
		return Collections.<DynamicTest>emptyList();
	}

	/**
	 * Zipファイルにある外部のテストケースを読み込み、動的テストを作成する
	 *
	 * @param zipFile  Zipファイル
	 * @param prefix   Zipファイル内部の先頭フォルダー
	 * @param checker  テストの実行方法
	 * @param testcase 対象テストケース名（空の場合ではすべてのテストケース）
	 * @return 作成された動的テストの一覧
	 */
	private Collection<DynamicTest> checkExternalZip(File zipFile, String prefix, InputStreamChecker checker,
			String testcase) {
		try {
			// 後続テストを実施するため、ここではクローズしない
			zip = new ZipFile(zipFile);
			prefix = (!prefix.isEmpty()) ? prefix + ZIP_FILE_SEPARATOR : prefix;
			String inPath = prefix + IN_FOLDER + ZIP_FILE_SEPARATOR, outPath = prefix + OUT_FOLDER + ZIP_FILE_SEPARATOR;
			ZipEntry inEntry = zip.getEntry(inPath), outEntry = zip.getEntry(outPath);
			if ((null != inEntry) && inEntry.isDirectory() && (null != outEntry) && (outEntry.isDirectory())) {
				Collection<DynamicTest> collection = zip.stream()
						.filter(entry -> entry.getName().startsWith(inPath) && (!entry.isDirectory()))
						.filter(entry -> testcase.isEmpty() || entry.getName().equals(inPath + testcase))
						.sorted((x, y) -> x.getName().compareTo(y.getName())).map(entry -> {
							ZipEntry outFileEntry = zip.getEntry(entry.getName().replaceAll(IN_FOLDER, OUT_FOLDER));
							if (null != outFileEntry) {
								return new ZipEntry[] { entry, outFileEntry };
							} else {
								return new ZipEntry[] { entry,
										zip.getEntry(entry.getName().replaceFirst(IN_FOLDER, OUT_FOLDER)
												.replaceAll("\\." + IN_FOLDER + "$", "." + OUT_FOLDER)) };
							}
						}).filter(entries -> (null != entries[1]) && (!entries[1].isDirectory())).map(entries -> {
							String fileName = entries[0].getName().replace(inPath, "");
							return DynamicTest.dynamicTest(fileName.replaceAll("\\." + IN_FOLDER + "$", ""), () -> {
								clearInAndOutWithoutException();
								try {
									checker.check(zip.getInputStream(entries[0]), zip.getInputStream(entries[1]));
								} catch (IOException e) {
									e.printStackTrace();
								}
							});
						}).collect(Collectors.toList());
				if (!collection.isEmpty()) {
					return collection;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return Collections.<DynamicTest>emptyList();
	}

	/**
	 * 標準入力を代替するクラス
	 */
	protected static class StandardInputSnatcher extends InputStream implements InputSnatcher {

		/** データを保存するバッファー */
		private StringBuilder buffer = new StringBuilder();

		/** バッファーから変換されたバイトの配列 */
		private byte[] bytes = null;

		/** バイトから入力する用のInputStream */
		private ByteArrayInputStream inputStream = null;

		/**
		 * 文字列を入力する。
		 *
		 * @param str 入力文字列
		 */
		@Override
		public void input(String str) {
			clearIfInputStreamExists();
			assertNull(bytes, "bytes should not be null.");
			buffer.append(str).append(LF);
		}

		/**
		 * 数字を入力する。
		 *
		 * @param num 入力数字
		 */
		@Override
		public void input(Number num) {
			clearIfInputStreamExists();
			assertNull(bytes, "bytes should not be null.");
			buffer.append(num).append(LF);
		}

		/**
		 * 1文字を読み取る
		 */
		@Override
		public synchronized int read() throws IOException {
			if (null == inputStream) {
				initInputStream();
			}
			return inputStream.read();
		}

		/**
		 * inputStreamを作成する
		 */
		private void initInputStream() {
			if (null == bytes) {
				bytes = buffer.toString().getBytes();
			}
			inputStream = new ByteArrayInputStream(bytes);
		}

		/**
		 * inputStreamが存在する場合、バッファーをクリアする
		 */
		private void clearIfInputStreamExists() {
			if (null != inputStream) {
				clear();
			}
		}

		/**
		 * 未使用のバッファーをクリアする
		 */
		public void clear() {
			buffer.setLength(0);
			bytes = null;
			closeInputStream();
		}

		/**
		 * inputStreamをクローズする
		 */
		private void closeInputStream() {
			if (null != inputStream) {
				try {
					inputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				inputStream = null;
			}
		}

		/**
		 * クローズ
		 */
		@Override
		public void close() throws IOException {
			if (null != inputStream) {
				inputStream.close();
			}
			if (null != bytes) {
				bytes = null;
			}
		}
	}

	/**
	 * InputStreamをテストするメソッドを定義
	 */
	protected static interface InputStreamChecker {
		void check(InputStream inputIs, InputStream expectedIs);
	}

	/**
	 * 標準入力を代替するクラスの共通インターフェース
	 */
	protected static interface InputSnatcher {
		public void input(String str);

		public void input(Number num);
	}

	/**
	 * 対話型プログラム用のInputStream
	 */
	protected static class InterpreterInputSnatcher extends PipedInputStream implements InputSnatcher {

		private final PipedOutputStream pos;
		private final PipedInputStream pis;

		/**
		 * コンストラクター
		 *
		 * @throws IOException
		 */
		public InterpreterInputSnatcher() throws IOException {
			pos = new PipedOutputStream();
			pis = new PipedInputStream(pos);
		}

		/**
		 * 文字列を入力する。
		 *
		 * @param str 入力文字列
		 */
		@Override
		public void input(String str) {
			try {
				pos.write((str + LF).getBytes());
				pos.flush();
			} catch (IOException e) {
				e.printStackTrace();
				fail();
			}
		}

		/**
		 * 数字を入力する。
		 *
		 * @param num 入力数字
		 */
		@Override
		public void input(Number num) {
			try {
				pos.write((num.toString() + LF).getBytes());
				pos.flush();
			} catch (IOException e) {
				e.printStackTrace();
				fail();
			}
		}

		/**
		 * 1文字を読み取る
		 */
		@Override
		public synchronized int read() throws IOException {
			return pis.read();
		}
	}
}
