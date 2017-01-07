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

import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A {@link Spliterator} for {@link NodeList}. Used internally by {@link XQuery}.
 *
 * @author Richard "Shred" Körber
 */
public class NodeListSpliterator implements Spliterator<Node> {

    private final NodeList list;
    private int pos;
    private int end;

    /**
     * Creates a new {@link NodeListSpliterator}.
     *
     * @param list
     *            {@link NodeList} to create a {@link NodeListSpliterator} of
     */
    public NodeListSpliterator(NodeList list) {
        this(list, 0, list.getLength());
    }

    /**
     * Creates a new {@link NodeListSpliterator} with limited range. Used internally for
     * splitting.
     *
     * @param list
     *            {@link NodeList}
     * @param pos
     *            Beginning of range, inclusive
     * @param end
     *            Ending of range, exlusive
     */
    private NodeListSpliterator(NodeList list, int pos, int end) {
        this.list = list;
        this.pos = pos;
        this.end = end;
    }

    /**
     * Creates a new {@link Stream} of {@link Node} for this spliterator.
     * <p>
     * This is a convenience call. It just invokes
     * {@link StreamSupport#stream(Spliterator, boolean)}.
     *
     * @return {@link Stream} of nodes
     */
    public Stream<Node> stream() {
        return StreamSupport.stream(this, false);
    }

    @Override
    public boolean tryAdvance(Consumer<? super Node> action) {
        if (pos < end) {
            action.accept(list.item(pos));
            pos++;
            return true;
        }

        return false;
    }

    @Override
    public Spliterator<Node> trySplit() {
        int remain = end - pos;
        if (remain > 1) {
            int half = remain >> 1;
            int split = pos + half;
            Spliterator<Node> result = new NodeListSpliterator(list, split, end);
            end = split;
            return result;
        }
        return null;
    }

    @Override
    public long estimateSize() {
        return ((long) end) - ((long) pos);
    }

    @Override
    public int characteristics() {
        return ORDERED | DISTINCT | SIZED | NONNULL | SUBSIZED;
    }

}
