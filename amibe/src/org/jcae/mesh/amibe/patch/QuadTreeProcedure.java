/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2004,2005, by EADS CRC

    This library is free software; you can redistribute it and/or
    modify it under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either
    version 2.1 of the License, or (at your option) any later version.

    This library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public
    License along with this library; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA */

package org.jcae.mesh.amibe.patch;

/**
 * Procedure to apply on all quadtree elements.
 */
public interface QuadTreeProcedure
{
	/**
	 * Perform an action on the given quadtree cell.
	 * This method is called by {@link QuadTree#walk(QuadTreeProcedure)} on
	 * all cells of the quadtree recursively.
	 *
	 * @param o  quadtree cell.
	 * @param s  cell size.
	 * @param i  first coordinate of the bottom left corner of this cell.
	 * @param j  second coordinate of the bottom left corner of this cell.
	 * @return <code>-1</code> if <code>walk</code> processing must
	 * abort now, <code>1</code> if node chikldren have to be ignored, and
	 * <code>0</code> to process node children recursively.
	 */
	public int action(Object o, int s, int i, int j);
}