/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2005, by EADS CRC

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

package org.jcae.mesh.amibe.validation;

import org.jcae.mesh.amibe.ds.MFace3D;
import org.jcae.mesh.amibe.ds.MNode3D;
import java.util.Iterator;

/**
 * Compute maximal angle by triangle.
 * This class implements the {@link QualityProcedure#quality(Object)}
 * method to compute maximal angles of triangles.
 */
public class MaxAngleFace extends QualityProcedure
{
	public MaxAngleFace()
	{
		setType(QualityProcedure.FACE);
	}
	
	public float quality(Object o)
	{
		if (!(o instanceof MFace3D))
			throw new IllegalArgumentException();
		MFace3D f = (MFace3D) o;
		
		Iterator itn = f.getNodesIterator();
		MNode3D n1 = (MNode3D) itn.next();
		MNode3D n2 = (MNode3D) itn.next();
		MNode3D n3 = (MNode3D) itn.next();
		double a1 = Math.abs(n1.angle(n2, n3));
		double a2 = Math.abs(n2.angle(n3, n1));
		double a3 = Math.abs(n3.angle(n1, n2));
		if (a2 > a1)
			a1 = a2;
		if (a3 > a1)
			a1 = a3;
		return (float) a1;
	}
	
}
