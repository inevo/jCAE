/*
 * Project Info:  http://jcae.sourceforge.net
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 *
 * (C) Copyright 2005, by EADS CRC
 */

package org.jcae.netbeans.mesh;

import java.util.ArrayList;
import java.util.Collection;
import org.jcae.mesh.xmldata.Group;
import org.jcae.mesh.xmldata.Groups;
import org.jcae.netbeans.mesh.GroupNode;
import org.openide.nodes.Children.Array;
import org.openide.nodes.Node;

/**
 * Children class. His associated node has a lookup to it.
 * @author ibarz
 */
public class GroupChildren extends Array {

	private final Groups groups;

	public GroupChildren(Groups groups)
	{
		this.groups=groups;
	}

	@Override
	protected Collection<Node> initCollection()
	{
		Group[] gps=groups.getGroups();
		ArrayList<Node> toReturn = new ArrayList<Node>(gps.length);
		for(int i=0; i<gps.length; i++)
		{
			toReturn.add(new GroupNode(gps[i], groups));
		}
		return toReturn;
	}
}
