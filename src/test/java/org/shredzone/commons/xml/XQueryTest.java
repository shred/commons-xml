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

import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link XQuery}.
 *
 * @author Richard "Shred" Körber
 */
public class XQueryTest {

    private XQuery xq;

    /**
     * Sets up a new {@link XQuery} instance.
     */
    @Before
    public void init() throws IOException {
        xq = XQuery.parse(XQueryTest.class.getResourceAsStream("/test.xml"));
    }

    /**
     * Test {@link Reader} based parser.
     */
    @Test
    public void parserReaderTest() throws IOException {
        try (Reader r = new InputStreamReader(XQueryTest.class.getResourceAsStream("/test.xml"))) {
            XQuery result = XQuery.parse(r);
            assertThat(result, is(notNullValue()));
        }
    }

    /**
     * Test {@link String} based parser.
     */
    @Test
    public void parserStringTest() throws IOException {
        XQuery result = XQuery.parse("<test><foo>bar</foo></test>");
        assertThat(result, is(notNullValue()));
    }

    /**
     * Test parser error on bad XML.
     */
    @Test(expected = IOException.class)
    public void parserFailTest() throws IOException {
        XQuery.parse(":-(");
    }

    /**
     * Does the {@link XQuery#stream()} of the root {@link XQuery} return the root
     * element?
     */
    @Test
    public void streamTest() throws IOException {
        List<String> tags = xq.stream().map(XQuery::name).collect(toList());

        assertThat(tags, contains("catalog"));
    }

    /**
     * Does the {@link XQuery#stream()} of a subelement return the child elements of that
     * element?
     */
    @Test
    public void subStreamTest() throws IOException {
        XQuery book7 = xq.get("//book[@id='bk7']");
        List<String> tags = book7.stream().map(XQuery::name).collect(toList());

        assertThat(tags, contains("author", "title", "original", "published", "description"));
    }

    /**
     * Does {@link XQuery#select(String)} return the correct elements? Does
     * {@link XQuery#text()} return the text content?
     */
    @Test
    public void selectTest() throws IOException {
        List<String> titles = xq.select("//book/original").map(XQuery::text).collect(toList());

        assertThat(titles, contains("Le Lotus bleu", "L'Île noire",
                        "Le Secret de la Licorne", "Objectif Lune", "Tintin au Tibet"));
    }

    /**
     * Does {@link XQuery#get(String)} return a single element?
     */
    @Test
    public void getTest() throws IOException {
        XQuery book11 = xq.get("//book[@id='bk11']");
        assertThat(book11.attr().get("id"), is("bk11"));
    }

    /**
     * Does {@link XQuery#get(String)} throw an exception if there is no such element?
     */
    @Test(expected = IllegalArgumentException.class)
    public void getNoneFailTest() throws IOException {
        xq.get("//book[@id='bk9']");
    }

    /**
     * Does {@link XQuery#get(String)} throw an exception if multiple elements are
     * found?
     */
    @Test(expected = IllegalArgumentException.class)
    public void getMultipleFailTest() throws IOException {
        xq.get("//book");
    }

    /**
     * Does {@link XQuery#previousSibling()} and {@link XQuery#nextSibling()} return
     * the correct siblings?
     */
    @Test
    public void siblingTest() throws IOException {
        XQuery book7 = xq.get("//book[@id='bk7']");

        XQuery book5 = book7.previousSibling().get();
        assertThat(book5.attr().get("id"), is("bk5"));
        assertThat(book5.previousSibling().isPresent(), is(false));

        XQuery book11 = book7.nextSibling().get();
        assertThat(book11.attr().get("id"), is("bk11"));

        XQuery book16 = book11.nextSibling().get();
        assertThat(book16.attr().get("id"), is("bk16"));

        XQuery book20 = book16.nextSibling().get();
        assertThat(book20.attr().get("id"), is("bk20"));
        assertThat(book20.nextSibling().isPresent(), is(false));
    }

    /**
     * Does {@link XQuery#exists(String)} return a correct answer.
     */
    @Test
    public void existsTest() throws IOException {
        assertThat(xq.exists("//book/original"), is(true));
        assertThat(xq.exists("//catfood/brand"), is(false));
    }

    /**
     * Does {@link XQuery#select(String)} fail on bad XPath?
     */
    @Test(expected = IllegalArgumentException.class)
    public void selectFailTest() throws IOException {
        xq.select(":-(");
    }

    /**
     * Does {@link XQuery#value(String)} return the text contents of the matching
     * elements?
     */
    @Test
    public void valueTest() throws IOException {
        List<String> titles = xq.value("/catalog/book/title").collect(toList());

        assertThat(titles, contains("The Blue Lotus","The Black Island",
                        "The Secret of the Unicorn", "Destination Moon",
                        "Tintin in Tibet"));
    }

    /**
     * Does {@link XQuery#allValue(String)} return the text contents of the matching
     * elements, recursively?
     */
    @Test
    public void allValueTest() throws IOException {
        List<String> dates = xq.value("/catalog/book/published").map(String::trim).collect(toList());
        List<String> allDates = xq.allValue("/catalog/book/published").map(String::trim).collect(toList());

        assertThat(dates, contains("", "", "", "", ""));
        assertThat(allDates, contains("1936\n        1946", "1938\n        1943\n        1966",
                        "1943", "1953", "1960"));
    }

    /**
     * Is {@link XQuery#value(String)} non-recursive, i.e. does it only return the text
     * of the immediate children?
     */
    @Test
    public void valueNonRecursiveTest() throws IOException {
        List<String> dates = xq.value("/catalog/book[@id='bk7']/published")
                        .map(String::trim)
                        .collect(toList());

        assertThat(dates, contains(""));
    }

    /**
     * Does {@link XQuery#select(String)} and {@link XQuery#allText()} return the text
     * contents of the entire subtree?
     */
    @Test
    public void value3Test() throws IOException {
        String dates = xq.select("/catalog/book[@id='bk7']/published")
                        .map(XQuery::allText)
                        .findFirst()
                        .get();

        assertThat(dates, is("\n        1938\n        1943\n        1966\n      "));
    }

    /**
     * Does {@link XQuery#attr()} return a map of attributes?
     */
    @Test
    public void attrTest() throws IOException {
        List<String> ids = xq.select("/catalog/book")
                .map(book -> book.attr().get("id"))
                .collect(toList());

        assertThat(ids, contains("bk5", "bk7", "bk11", "bk16", "bk20"));
    }

    /**
     * Does {@link XQuery#attr()} return a map of multiple attributes?
     */
    @Test
    public void attrManyTest() throws IOException {
        List<XQuery> albums = xq.select("/catalog/book[@id='bk7']/published/album")
                .collect(toList());

        assertThat(albums.get(0).attr().size(), is(1));
        assertThat(albums.get(0).attr().get("type"), is("bw"));

        assertThat(albums.get(1).attr().size(), is(1));
        assertThat(albums.get(1).attr().get("type"), is("color"));

        assertThat(albums.get(2).attr().size(), is(2));
        assertThat(albums.get(2).attr().get("type"), is("color"));
        assertThat(albums.get(2).attr().get("republished"), is("yes"));
    }

    /**
     * Does {@link XQuery#attr()} return an empty map of attributes when there are none?
     */
    @Test
    public void attrEmptyTest() throws IOException {
        xq.select("/catalog/book/author")
                .forEach(author -> assertThat(author.attr().keySet(), is(empty())));
        assertThat(xq.attr().keySet(), is(empty()));
    }

    /**
     * Are comments ignored?
     */
    @Test
    public void noCommentTest() throws IOException {
        String text = xq.text("/catalog/book[@id='bk16']/description");

        assertThat(text, is("Tintin's friend Professor Calculus has been secretly commissioned\n"
            + "      \n"
            + "      by the Syldavian government to build a rocket ship that will fly from the\n"
            + "      Earth to the Moon."));
    }

    /**
     * Are CDATA elements read as text?
     */
    @Test
    public void cdataTest() throws IOException {
        String text = xq.text("/catalog/book[@id='bk20']/description");

        assertThat(text, is("While on holiday at a resort in the French Alps with Snowy,\n"
            + "      Captain Haddock, & Professor Calculus, <Tintin> reads about a plane crash in\n"
            + "      the Gosain Than Massif in the Himalayas of Tibet."));
    }

    /**
     * Does {@link XQuery#parent()} return the correct parent node?
     */
    @Test
    public void parentTest() throws IOException {
        XQuery title = xq.get("/catalog/book[@id='bk11']/title");

        XQuery parent = title.parent().get();
        assertThat(parent.name(), is("book"));

        // make sure parent is cached
        XQuery parent2 = title.parent().get();
        assertThat(parent2, is(sameInstance(parent)));
    }

    /**
     * Does {@link XQuery#parent()} return empty on root?
     */
    @Test
    public void parentOfRootTest() throws IOException {
        assertThat(xq.parent().isPresent(), is(false));
    }

    /**
     * Does {@link XQuery#parent()} return the correct parent node?
     */
    @Test
    public void rootTest() throws IOException {
        XQuery title = xq.get("/catalog/book[@id='bk11']/title");
        assertThat(title.isRoot(), is(false));
        assertThat(title.name(), is("title"));

        XQuery root = title.root();
        assertThat(root.isRoot(), is(true));
        assertThat(root.name(), is("#document"));
    }

    /**
     * Does {@link XQuery#parent()} return empty on root?
     */
    @Test
    public void rootOfRootTest() throws IOException {
        assertThat(xq.root(), is(sameInstance(xq)));
        assertThat(xq.isRoot(), is(true));
    }

}
