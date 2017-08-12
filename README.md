# commons-xml ![build status](https://shredzone.org/badge/commons-xml.svg) ![maven central](https://maven-badges.herokuapp.com/maven-central/org.shredzone.commons/commons-xml/badge.svg)

A Java library for easy XML processing.

This software is part of the Shredzone Commons.

## Features

* Lightweight, very simple and comprehensible API
* No bells and whistles, no namespaces, no validation, just plain XML parsing
* Use XPath expressions for selecting the XML parts you want to read
* Use streaming API and lambda expressions to process the XML data

## Quick Start

A very simple use case is reading text from a configuration file. This is the XML file to be read:

```xml
<config>
  <foo1>Hello</foo1>
  <foo2>World</foo2>
</config>
```

This is the program code:

```java
XQuery xq = XQuery.parse(new FileReader("config.xml"));
String foo1 = xq.text("/config/foo1"); // Hello
String foo2 = xq.text("/config/foo2"); // World
```

That's all!

But _commons-xml_ offers a lot more possibilities. Just see the [online documentation](https://shredzone.org/maven/commons-xml/) for more examples.

## Contribute

* Fork the [Source code at GitHub](https://github.com/shred/commons-xml). Feel free to send pull requests.
* Found a bug? [File a bug report!](https://github.com/shred/commons-xml/issues)

## License

_commons-xml_ is open source software. The source code is distributed under the terms of [GNU Lesser General Public License Version 3](http://www.gnu.org/licenses/lgpl-3.0.html).
