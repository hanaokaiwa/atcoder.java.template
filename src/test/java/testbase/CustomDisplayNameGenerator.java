package testbase;

import org.junit.jupiter.api.DisplayNameGenerator.Standard;

/**
 * テストクラスの表示名をカスタマイズ
 */
public class CustomDisplayNameGenerator extends Standard {

	/**
	 * テストクラスの表示名をパッケージ名を含めるように修正
	 *
	 * @param testClass テストクラス
	 * @return テストクラスの表示名
	 */
	@Override
	public String generateDisplayNameForClass(Class<?> testClass) {
		String name = testClass.getName();
		int lastDot = name.lastIndexOf('.');
		if (lastDot > 0) {
			int secondDot = name.lastIndexOf('.', lastDot - 1);
			return name.substring(secondDot + 1);
		}
		return name.substring(lastDot + 1);
	}
}
