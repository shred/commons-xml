# Usage

In Java, parsing XML files can be quite tedious. You have to deal with `DocumentBuilderFactory` and `DocumentBuilder` before you have read a single line of XML yet, and then need to wade through endless `NodeList` instances (which aren't `Iterable`). Other languages like Groovy offer simpler ways to deal with XML files.

The commons-xml's `XQuery` class offers a solution to read XML files very easily. Its main goal is being simple, so it does not offer more sophisticated things like validation or namespaces. However, very often you only need to process simply structured XML files. This is where `XQuery` comes in handy.

`XQuery` requires Java 8 and makes massive use of the `Stream` API. Besides of Java 8 there are no more dependencies, so it's very lightweight.

But let's see some examples...

## Reading Text

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

The first line parses the XML file and returns an `XQuery` object pointing to the root node of the document.

`XQuery.text()` accepts an XPath expression and returns the text part of the addressed node as String.

## Streaming Node Values

`XQuery` always uses [XPath](http://www.w3.org/TR/xpath) expressions for selecting nodes from the document. Multiple nodes can be streamed using Java 8's `Stream` API.

This is my bookshelf inventory:

```xml
<books>
  <book id="book1">
    <author>Douglas Adams</author>
    <title>The Hitchhiker's Guide to the Galaxy</title>
    <price>7.99</price>
  </book>
  <book id="book2">
    <author>Robert Shea, Robert Anton Wilson</author>
    <title>Illuminatus!</title>
    <price>20.00</price>
  </book>
</books>
```

This program prints out all the titles on my bookshelf:

```java
XQuery xq = XQuery.parse(new FileReader("bookshelf.xml"));
xq.value("/books/book/title")
    .forEach(System.out::println);
```

`XQuery.value()` returns a `Stream` of the selected nodes' texts. It's easy to process the streams, for example filtering all the books with their titles having at least two words.

```java
XQuery xq = XQuery.parse(new FileReader("bookshelf.xml"));
xq.value("/books/book/title")
    .filter(title -> title.indexOf(" ") >= 0)
    .forEach(System.out::println);
```

Or say I'd like to find out the amount of money I have spent on my bookshelf:

```java
XQuery xq = XQuery.parse(new FileReader("bookshelf.xml"));
BigDecimal sum = xq.value("/books/book/price")
    .map(BigDecimal::new)
    .reduce(BigDecimal.ZERO, BigDecimal::add);
System.out.println(sum);
```

## Streaming Nodes

With `XQuery.select()`, it is possible to select sections of the document tree for further processing. These sections are also represented by `XQuery` instances, so all the methods can be applied on them as well.

To stay with my bookshelf example, this example collects all the tag names that are used inside the `book` containers.

```java
XQuery xq = XQuery.parse(new FileReader("bookshelf.xml"));

Set<String> tags = new HashSet<>();
xq.select("/books/book").forEach(book -> {
    tags.addAll(book.stream().map(XQuery::name).collect(Collectors.toSet()));
});

tags.forEach(System.out::println); // author price title
```

Three new `XQuery` calls are used here.

`xq.select("/books/book")` selects all book elements and returns a stream of `XQuery` instances that represent each book tree section.

`book.stream()` now streams all children elements of the book element, as `XQuery` instances. Comments and texts are skipped.

`XQuery.name()` returns the element's tag name. All tag names are collected into a set, which is printed out in a final step.

If you expect _exactly one_ node, you can also use `XQuery.get()`. It throws an exception if there is none, or more than one node matching the XPath expression.

```java
XQuery xq = XQuery.parse(new FileReader("bookshelf.xml"));
XQuery book1 = xq.get("/books/book[@id='book1']");
```

Use `XQuery.exists()` to find out if there is at least one match:

```java
XQuery xq = XQuery.parse(new FileReader("bookshelf.xml"));
if (xq.exists("/books/book[@id='book2']")) {
  System.out.println("Yes, we have a book2!");
}
```

## Reading Text

There are two different ways of reading text content. Usually only the text of the node's immediate children is returned, which is the desired way in most of the cases. The other way is to return the text of the entire tree section, recursively.

Let me give an example. This is my shopping list. It contains a list with a title ("Groceries") and some shopping items.

```xml
<shoppinglist>
  <list>
    Groceries
    <item>Apple</item>
    <item>Lollipop</item>
  </list>
</shoppinglist>
```

Now run this code:

```java
XQuery xq = XQuery.parse(new FileReader("shoppinglist.xml"));

XQuery list = xq.select("/shoppinglist/list").findFirst().get();

String title = list.text();
String everything = list.allText();

System.out.println(title.trim()); // Groceries
System.out.println(everything.trim()); // Groceries Apple Lollipop
```

`list` is an `XQuery` instance representing the first list tree section.

When invoking `list.text()`, only the text of the immediate children nodes is returned. The result is "Groceries" (with a number of leading and trailing whitespace characters that were also present in the XML file).

When invoking `list.allText()`, the text of the entire tree section is returned. This is "Groceries Apple Lollipop", again with a number of leading, trailing and intermediate whitespace characters.

## Attributes

If the root element has attributes, they can be read using `XQuery.attr()`. It returns a map of the attribute name and value. Note that the XML specifications forbid the use of multiple attributes with the same name, so a map is fine. The original sequence of attributes is lost however.

This is a chart of the [average temperatures in Germany](http://de.wikipedia.org/wiki/Zeitreihe_der_Lufttemperatur_in_Deutschland#Durchschnitt_.282001_bis_2013.29) between 2001 and 2013:

```xml
<temperatures>
  <temperature month="January"   value="0.5"/>
  <temperature month="February"  value="0.9"/>
  <temperature month="March"     value="4.2"/>
  <temperature month="April"     value="9.1"/>
  <temperature month="May"       value="13.2"/>
  <temperature month="June"      value="16.4"/>
  <temperature month="July"      value="18.4"/>
  <temperature month="August"    value="17.8"/>
  <temperature month="September" value="13.8"/>
  <temperature month="October"   value="9.4"/>
  <temperature month="November"  value="5.1"/>
  <temperature month="December"  value="1.3"/>
</temperatures>
```

The following code example computes the average temperature of all months containing the letter "r":

```java
XQuery xq = XQuery.parse(new FileReader("temperatures.xml"));

double avg = xq.select("//temperature[contains(`month,'r')]")
    .map(node -> node.attr().get("value"))
    .collect(Collectors.averagingDouble(Double::parseDouble));

System.out.println(avg);
```

## Navigating

Use `XQuery.nextSibling()` and `XQuery.previousSibling()` to find the sibling elements of your current node.

```java
XQuery xq = XQuery.parse(new FileReader("bookshelf.xml"));
XQuery book1 = xq.get("/books/book[@id='book1']");
XQuery book2 = book1.nextSibling().get();
System.out.println(book2.attr().get("id"); // book2

if (!book1.previousSibling().isPresent()) {
  System.out.println("There is no book before book1");
}
```

`XQuery.root()` returns the root element of your node, in case you have lost it. With `XQuery.isRoot()`, you can find out if your current node is a root node.

That was easy, wasn't it?
