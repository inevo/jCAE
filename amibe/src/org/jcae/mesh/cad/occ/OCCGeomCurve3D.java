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

import org.jcae.mesh.cad.CADGeomCurve3D;
import org.jcae.mesh.cad.CADEdge;
import org.jcae.opencascade.jni.Adaptor3d_Curve;
import org.jcae.opencascade.jni.BRep_Tool;
import org.jcae.opencascade.jni.Geom_Curve;
import org.jcae.opencascade.jni.GeomAdaptor_Curve;
import org.jcae.opencascade.jni.GCPnts_UniformAbscissa;
import org.jcae.opencascade.jni.TopoDS_Edge;
import org.jcae.opencascade.jni.GCPnts_AbscissaPoint;

public class OCCGeomCurve3D implements CADGeomCurve3D
{
	protected GeomAdaptor_Curve myCurve = null;
	protected double [] range = new double[2];
	protected GCPnts_UniformAbscissa discret = null;
	
	public OCCGeomCurve3D(CADEdge E)
	{
		OCCEdge occEdge=(OCCEdge)E;
		Geom_Curve curve = BRep_Tool.curve((TopoDS_Edge) occEdge.getShape(), range);
		if (curve == null)
			throw new RuntimeException();
		myCurve = new GeomAdaptor_Curve(curve);
	}
	
	public double [] value(double p)
	{
		assert myCurve != null;
		return myCurve.value((float) p);
	}
	
	public double [] getRange()
	{
		return range;
	}
	
	public void discretize(double maxlen)
	{
		if (discret == null)
			discret = new GCPnts_UniformAbscissa();
		discret.initialize((Adaptor3d_Curve) myCurve, maxlen, range[0], range[1]);
	}
	
	public void discretize(int n)
	{
		if (discret == null)
			discret = new GCPnts_UniformAbscissa();
		discret.initialize((Adaptor3d_Curve) myCurve, n, range[0], range[1]);
	}
	
	public int nbPoints()
	{
		assert discret != null;
		return discret.nbPoints();
	}
	
	public double parameter(int index)
	{
		assert discret != null;
		return discret.parameter(index);
	}
	
	public double length()
	{
		assert myCurve != null;
		return new GCPnts_AbscissaPoint().length(myCurve);
	}
	
}
