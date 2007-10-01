/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2006, by EADS CRC

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

package org.jcae.mesh.amibe.ds;

import org.jcae.mesh.amibe.traits.Traits;
import org.jcae.mesh.amibe.traits.MeshTraitsBuilder;

public class AbstractMesh
{
	/**
	 * User-defined traits builder.
	 */
	protected final MeshTraitsBuilder traitsBuilder;

	/**
	 * User-defined traits
	 */
	protected final Traits traits;

	public AbstractMesh(MeshTraitsBuilder builder)
	{
		traitsBuilder = builder;
		if (builder != null)
			traits = builder.createTraits();
		else
			traits = null;
	}
}
