# atcoder.java.template
Template of Atcoder Java Answers

## 外部テストケースの使い方
1. src/test/resources/external.properties.sample をコピーし、src/test/resources/external.properties ファイルを作成する

1. src/test/resources/external.properties を以下のように編集する
```
# 外部フォルダーを使用するかどうか
USE_EXTERNAL=true
# 外部フォルダーを指定する
EXTERNAL_FOLDER=外部フォルダーのパス
```

1. [Atcoderのテストケース](https://www.dropbox.com/sh/nx3tnilzqz7df8a/AAAYlTq2tiEHl5hsESw6-yfLa?dl=0) からテストケースをzip形式でダウンロードし、外部フォルダーに配置する。
例えば、ABC051のテストケースはABC051.zipとして、ダウンロードされる。

1. 該当のテストファイルに以下のようなコードを追加する
```
	@TestFactory
	Collection<DynamicTest> external() {
		return checkExternal("ABC051/A");
	}
```
ここでの`ABC051/A`の最後の`A`はA問題を意味する

1. 該当のテストクラスをJUnitで実行する
Eclipseでは以下の画像のようになれば、設定が正しいことが確認できる。

![ABC051のA問題のJUnitの実行結果](./junit.png)
