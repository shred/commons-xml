/*
 * Shredzone Commons
 *
 * Copyright (C) 2014 Richard "Shred" Körber
 *   http://commons.shredzone.org
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Library General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.shredzone.commons.xml;

import static java.util.stream.Collectors.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.WillClose;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Helps to easily read content from XML sources.
 * <p>
 * A main goal of {@link XQuery} is to keep XML reading as simple as possible. For this
 * reason, sophisticated XML features like validation or namespaces are not supported.
 * <p>
 * Performance was not a goal as well. If you need to parse large documents, you better
 * use the old-fashioned Java ways.
 *
 * @author Richard "Shred" Körber
 */
@ParametersAreNonnullByDefault
public class XQuery {

    private final Node node;
    private final XPathFactory xpf = XPathFactory.newInstance();
    private Optional<XQuery> parent = null;
    private Map<String, String> attrMap = null;

    /**
     * Private constructor for a {@link Node} element.
     */
    private XQuery(Node node) {
        this.node = node;
    }

    /**
     * Parses an XML source and returns an {@link XQuery} object representing the root of
     * the document.
     *
     * @param in
     *            {@link InputSource} of the XML document
     * @return {@link XQuery} representing the root of the parsed document
     * @throws IOException
     *             if the XML source could not be read or parsed for any reason
     */
    public static @Nonnull XQuery parse(@WillClose InputSource in) throws IOException {
        try {
            DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            return new XQuery(db.parse(in));
        } catch (ParserConfigurationException|SAXException ex) {
            throw new IOException("Could not parse XML", ex);
        }
    }

    /**
     * Parses an XML source and returns an {@link XQuery} object representing the root of
     * the document.
     *
     * @param in
     *            {@link InputStream} of the XML document
     * @return {@link XQuery} representing the root of the parsed document
     * @throws IOException
     *             if the XML source could not be read or parsed for any reason
     */
    public static @Nonnull XQuery parse(@WillClose InputStream in) throws IOException {
        return parse(new InputSource(in));
    }

    /**
     * Parses an XML source and returns an {@link XQuery} object representing the root of
     * the document.
     *
     * @param r
     *            {@link Reader} providing the XML document
     * @return {@link XQuery} representing the root of the parsed document
     * @throws IOException
     *             if the XML source could not be read or parsed for any reason
     */
    public static @Nonnull XQuery parse(@WillClose Reader r) throws IOException {
        return parse(new InputSource(r));
    }

    /**
     * Parses an XML source and returns an {@link XQuery} object representing the root of
     * the document.
     *
     * @param xml
     *            String containing the XML document
     * @return {@link XQuery} representing the root of the parsed document
     * @throws IOException
     *             if the XML source could not be read or parsed for any reason
     */
    public static @Nonnull XQuery parse(String xml) throws IOException {
        return parse(new StringReader(xml));
    }

    /**
     * Streams all children of this element. Children elements are represented by
     * {@link XQuery} objects as well.
     *
     * @return {@link Stream} of children
     */
    public @Nonnull Stream<XQuery> stream() {
        return new NodeListSpliterator(node.getChildNodes()).stream()
                        .filter(it -> it instanceof Element)
                        .map(XQuery::new);
    }

    /**
     * Returns the next sibling of this element.
     *
     * @return Next sibling element
     * @since 1.1
     */
    public @Nonnull Optional<XQuery> nextSibling() {
        return findElement(Node::getNextSibling);
    }

    /**
     * Returns the previous sibling of this element.
     *
     * @return Previous sibling element
     * @since 1.1
     */
    public @Nonnull Optional<XQuery> previousSibling() {
        return findElement(Node::getPreviousSibling);
    }

    /**
     * Selects elements based on the XPath expression that is applied to the tree
     * represented by this {@link XQuery}.
     *
     * @param xpath
     *            XPath expression
     * @return Stream of selected nodes as {@link XQuery} object
     */
    public @Nonnull Stream<XQuery> select(String xpath) {
        return new NodeListSpliterator(evaluate(xpath)).stream().map(XQuery::new);
    }

    /**
     * Gets a single element based on the XPath expression that is applied to the tree
     * represented by this {@link XQuery}. Exactly one element is expected to match the
     * XPath expression, otherwise an exception is thrown.
     *
     * @param xpath
     *            XPath expression
     * @return Selected node
     * @since 1.1
     */
    public @Nonnull XQuery get(String xpath) {
        NodeList nl = evaluate(xpath);
        if (nl.getLength() == 1) {
            return new XQuery(nl.item(0));
        } else if (nl.getLength() == 0) {
            throw new IllegalArgumentException("XPath '" + xpath
                + "' does not match any elements");
        } else {
            throw new IllegalArgumentException("XPath '" + xpath + "' matches "
                + nl.getLength() + " elements");
        }
    }

    /**
     * Checks if there is at least one element matching the XPath expression.
     *
     * @param xpath
     *            XPath expression
     * @return {@code true} if there is at least one element, {@code false} if there is
     *         none.
     * @since 1.1
     */
    public boolean exists(String xpath) {
        return select(xpath).findAny().isPresent();
    }

    /**
     * Selects values based on the XPath expression that is applied to the tree
     * represented by this {@link XQuery}.
     *
     * @param xpath
     *            XPath expression
     * @return Stream of strings containing the node values
     */
    public @Nonnull Stream<String> value(String xpath) {
        return select(xpath).map(XQuery::text);
    }

    /**
     * Selects values based on the XPath expression that is applied to the tree
     * represented by this {@link XQuery}. In contrast to {@link #value(String)}, this
     * method reads the element texts recursively, using {@link #allText()}.
     *
     * @param xpath
     *            XPath expression
     * @return Stream of strings containing the node values
     */
    public @Nonnull Stream<String> allValue(String xpath) {
        return select(xpath).map(XQuery::allText);
    }

    /**
     * Returns the text selected by the XPath expression.
     *
     * @param xpath
     *            XPath expression
     * @return Text selected by the expression
     */
    public @Nonnull String text(String xpath) {
        return value(xpath).collect(joining());
    }

    /**
     * @return this {@link XQuery} node's tag name.
     */
    public @Nonnull String name() {
        return node.getNodeName();
    }

    /**
     * @return this {@link XQuery} node's text content, non recursively.
     */
    public @Nonnull String text() {
        return new NodeListSpliterator(node.getChildNodes()).stream()
                        .filter(it -> it instanceof Text)
                        .map(it -> ((Text) it).getNodeValue())
                        .collect(joining());
    }

    /**
     * @return this {@link XQuery} node's text content, recursively.
     */
    public @Nonnull String allText() {
        return node.getTextContent();
    }

    /**
     * @return a map of this node's attributes.
     */
    public @Nonnull Map<String, String> attr() {
        if (attrMap == null) {
            NamedNodeMap nnm = node.getAttributes();
            if (nnm != null) {
                attrMap = Collections.unmodifiableMap(
                        IntStream.range(0, nnm.getLength())
                            .mapToObj(nnm::item)
                            .collect(toMap(Node::getNodeName, Node::getNodeValue)));
            } else {
                attrMap = Collections.emptyMap();
            }
        }
        return attrMap;
    }

    /**
     * Returns the parent node of this node, as {@link XQuery} object. A root node
     * returns an empty optional instead.
     *
     * @return parent node
     */
    public @Nonnull Optional<XQuery> parent() {
        if (parent == null) {
            Node p = node.getParentNode();
            if (p != null) {
                parent = Optional.of(new XQuery(p));
            } else {
                parent = Optional.empty();
            }
        }
        return parent;
    }

    /**
     * Checks if this is a root node.
     *
     * @return {@code true} if this is a root node, {@code false} if there's a parent.
     * @since 1.1
     */
    public boolean isRoot() {
        return node.getParentNode() == null;
    }

    /**
     * Returns the root node of this node, as {@link XQuery} object. A root node returns
     * itself.
     *
     * @return root node
     * @since 1.1
     */
    public @Nonnull XQuery root() {
        if (isRoot()) {
            return this;
        } else {
            return new XQuery(node.getOwnerDocument());
        }
    }

    /**
     * Evaluates the XPath expression and returns a list of nodes.
     *
     * @param xpath
     *            XPath expression
     * @return {@link NodeList} matching the expression
     * @throws IllegalArgumentException
     *             if the XPath expression was invalid
     */
    private @Nonnull NodeList evaluate(String xpath) {
        try {
            XPathExpression expr = xpf.newXPath().compile(xpath);
            return (NodeList) expr.evaluate(node, XPathConstants.NODESET);
        } catch (XPathExpressionException ex) {
            throw new IllegalArgumentException("Invalid XPath '" + xpath + "'", ex);
        }
    }

    /**
     * Finds an Element node by applying the iterator function until another Element was
     * found.
     *
     * @param iterator
     *            Iterator to apply
     * @return node that was found
     */
    private @Nonnull Optional<XQuery> findElement(Function<Node, Node> iterator) {
        Node it = node;
        do {
            it = iterator.apply(it);
        } while (it != null && !(it instanceof Element));
        return Optional.ofNullable(it).map(XQuery::new);
    }

}
