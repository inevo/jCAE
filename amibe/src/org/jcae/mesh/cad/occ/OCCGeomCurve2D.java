/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finit element mesher, Plugin architecture.

    Copyright (C) 2004 Jerome Robert <jeromerobert@users.sourceforge.net>

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

package org.jcae.mesh.cad.occ;

import org.jcae.mesh.cad.CADGeomCurve2D;
import org.jcae.mesh.cad.CADEdge;
import org.jcae.mesh.cad.CADFace;
import org.jcae.opencascade.jni.BRep_Tool;
import org.jcae.opencascade.jni.Geom2d_Curve;
import org.jcae.opencascade.jni.Geom2dAdaptor_Curve;
import org.jcae.opencascade.jni.TopoDS_Edge;
import org.jcae.opencascade.jni.TopoDS_Face;

public class OCCGeomCurve2D implements CADGeomCurve2D
{
	protected Geom2dAdaptor_Curve myCurve = null;
	protected double [] range = new double[2];
	
	public OCCGeomCurve2D(CADEdge E, CADFace F)
	{
		OCCEdge occEdge=(OCCEdge)E;
		OCCFace occFace=(OCCFace)F;
		Geom2d_Curve curve = BRep_Tool.curveOnSurface(
			(TopoDS_Edge) occEdge.getShape(),
			(TopoDS_Face) occFace.getShape(), range);
		if (curve == null)
			throw new RuntimeException();
		myCurve = new Geom2dAdaptor_Curve(curve);
	}
	
	public double [] value(double p)
	{
		assert myCurve != null;
		return myCurve.value((float) p);
	}
	
}
