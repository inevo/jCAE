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

import org.jcae.mesh.cad.CADShape;
import org.jcae.mesh.cad.CADGeomSurface;
import org.jcae.opencascade.jni.BRep_Tool;
import org.jcae.opencascade.jni.TopoDS_Shape;
import org.jcae.opencascade.jni.TopoDS_Face;
import org.jcae.opencascade.jni.TopAbs_Orientation;

public class OCCShape implements CADShape
{
	protected TopoDS_Shape myShape = null;
	
	public OCCShape()
	{
	}
	
	public void setShape(Object o)
	{
		myShape = (TopoDS_Shape) o;
	}
	
	public Object getShape()
	{
		return myShape;
	}
	
	public CADGeomSurface getGeomSurface()
	{
		assert myShape instanceof TopoDS_Face;
		OCCGeomSurface surface = new OCCGeomSurface();
		surface.setSurface(BRep_Tool.surface((TopoDS_Face) myShape));
		return (CADGeomSurface) surface;
	}
	
	public CADShape reversed()
	{
		OCCShape s = new OCCShape();
		s.setShape(myShape.reversed());
		return (CADShape) s;
	}
	
	public int orientation()
	{
		return myShape.orientation();
	}
	
	public boolean isOrientationForward()
	{
		return myShape.orientation() == TopAbs_Orientation.FORWARD;
	}
	
	public boolean equals(Object o)
	{
		OCCShape that = (OCCShape) o;
		return myShape.equals(that.myShape);
	}
	
	public boolean isSame(Object o)
	{
		OCCShape that = (OCCShape) o;
		return myShape.isSame(that.myShape);
	}
	
	public int hashCode()
	{
		return myShape.hashCode();
	}
	
}
