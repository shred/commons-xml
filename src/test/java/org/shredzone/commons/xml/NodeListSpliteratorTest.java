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

import static java.util.Arrays.copyOfRange;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;
import java.util.Spliterator;
import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Unit tests for the {@link NodeListSpliterator}.
 *
 * @author Richard "Shred" Körber
 */
public class NodeListSpliteratorTest {

    private static final int NODES = 11; // prime number, so splitting isn't too easy
    private static final int HALF = NODES / 2;

    private Node[] nodes;
    private NodeList nodeList;

    /**
     * Sets up an array of nodes and a {@link NodeList} returning that array.
     */
    @Before
    public void init() {
        nodes = new Node[NODES];
        IntStream.range(0, nodes.length).forEach(ix -> nodes[ix] = mock(Node.class));

        nodeList = new NodeList() {
            @Override
            public Node item(int index) {
                return nodes[index];
            }
            @Override
            public int getLength() {
                return nodes.length;
            }
        };
    }

    /**
     * Is {@link NodeListSpliterator#characteristics()} returning the correct
     * characteristics?
     */
    @Test
    public void characteristicsTest() {
        NodeListSpliterator spliterator = new NodeListSpliterator(nodeList);
        assertThat(spliterator.characteristics(), is(Spliterator.ORDERED
                        | Spliterator.DISTINCT | Spliterator.SIZED | Spliterator.NONNULL
                        | Spliterator.SUBSIZED));
    }

    /**
     * Does the {@link NodeListSpliterator#tryAdvance(java.util.function.Consumer)}
     * return all {@link NodeList} elements, in correct order?
     */
    @Test
    public void simpleTest() {
        NodeListSpliterator spliterator = new NodeListSpliterator(nodeList);
        assertThat(spliterator.estimateSize(), is((long) nodes.length));

        List<Node> result = consume(spliterator);

        assertThat(result, contains(nodes));
        assertThat(spliterator.estimateSize(), is(0L));
    }

    /**
     * Does {@link NodeListSpliterator#trySplit()} split up correctly, leaving two
     * spliterators containing half of the {@link NodeList} each?
     */
    @Test
    public void splitTest() {
        NodeListSpliterator s1 = new NodeListSpliterator(nodeList);
        assertThat(s1.estimateSize(), is((long) nodes.length));

        Spliterator<Node> s2 = s1.trySplit();
        assertThat(s1.estimateSize(), is((long) HALF));
        assertThat(s2.estimateSize(), is((long) (NODES - HALF)));

        List<Node> r1 = consume(s1);
        assertThat(s1.estimateSize(), is(0L));
        assertThat(r1, contains(copyOfRange(nodes, 0, HALF)));

        List<Node> r2 = consume(s2);
        assertThat(s2.estimateSize(), is(0L));
        assertThat(r2, contains(copyOfRange(nodes, HALF, NODES)));
    }

    /**
     * Does {@link NodeListSpliterator#trySplit()} split up correctly to the limits?
     */
    @Test
    public void fullSplitTest() {
        List<Spliterator<Node>> spliterators = new ArrayList<>();
        spliterators.add(new NodeListSpliterator(nodeList));

        // Split all spliterators until they contain a single element
        while (spliterators.size() != NODES) {
            List<Spliterator<Node>> newSpliterators = new ArrayList<>();
            for (Spliterator<Node> sp1 : spliterators) {
                newSpliterators.add(sp1);
                Spliterator<Node> sp2 = sp1.trySplit();
                if (sp2 != null) {
                    newSpliterators.add(sp2);
                }
            }
            spliterators = newSpliterators;
        }

        // Make sure all spliterators cannot be split any further
        for (Spliterator<Node> sp : spliterators) {
            assertThat(sp.estimateSize(), is(1L));
            assertThat(sp.trySplit(), is(nullValue()));
        }

        // Consume the content of each spliterator, make sure they are in correct order
        for (int ix = 0; ix < NODES; ix++) {
            Node refNode = nodes[ix];
            Spliterator<Node> sp = spliterators.get(ix);
            sp.tryAdvance(it -> assertThat(it, is(refNode)));
            assertThat(sp.estimateSize(), is(0L));
        }
    }

    /**
     * Does the {@link NodeListSpliterator#stream()} method return a stream containing
     * all remaining elements?
     */
    @Test
    public void streamTest() {
        NodeListSpliterator spliterator = new NodeListSpliterator(nodeList);
        List<Node> result = spliterator.stream().collect(toList());
        assertThat(result, contains(nodes));
    }

    /**
     * Consumes the {@link Spliterator} and returns a list of all {@link Node} retrieved.
     *
     * @param spliterator
     *            {@link Spliterator} to consume
     * @return List of all {@link Node} elements of that {@link Spliterator}
     */
    private List<Node> consume(Spliterator<Node> spliterator) {
        List<Node> result = new ArrayList<>();
        spliterator.forEachRemaining(result::add);
        return result;
    }

}
