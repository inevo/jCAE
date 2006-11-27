/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2004,2005,2006, by EADS CRC

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

import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.OTriangle;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.InitialTriangulationException;
import org.jcae.mesh.amibe.metrics.Metric2D;
import org.jcae.mesh.cad.*;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Stack;
import org.apache.log4j.Logger;

/**
 * Mesh data structure for paramterized surfaces.
 * Connectivity between triangles and vertices is inherited from {@link Mesh},
 * and a {@link QuadTree} instance added in order to speed up finding the
 * nearest {@link Vertex2D} <code>V</code> from any given point <code>V0</code>.
 * This gives a {@link Triangle} having <code>V</code> in its vertices,
 * and we can loop around <code>V</code> to find the {@link Triangle}
 * containing <code>V0</code>.
 */


public class Mesh2D extends Mesh
{
	private static Logger logger=Logger.getLogger(Mesh2D.class);
	
	//  Topological face on which mesh is applied
	private CADShape face;
	
	//  The geometrical surface describing the topological face, stored for
	//  efficiebcy reason
	private CADGeomSurface surface;
	
	//  Stack of methods to compute geometrical values
	private Stack compGeomStack = new Stack();
	
	/**
	 * Structure to fasten search of nearest vertices.
	 */
	public QuadTree quadtree = null;
	
	/**
	 * Sole constructor.
	 */
	public Mesh2D()
	{
		super();
	}

	/**
	 * Creates an empty mesh bounded to the topological surface.
	 * This constructor also initializes tolerance values.  If length
	 * criterion is null, {@link Metric2D#setLength} is called with
	 * the diagonal length of face bounding box as argument.
	 * If property <code>org.jcae.mesh.amibe.ds.Mesh.epsilon</code> is
	 * not set, epsilon is computed as being the maximal value between
	 * length criterion by 100 and diagonal length by 1000.
	 *
	 * @param f   topological surface
	 */
	public Mesh2D(CADFace f)
	{
		super();
		face = f;
		surface = f.getGeomSurface();
		double [] bb = f.boundingBox();
		double diagonal = Math.sqrt(
		    (bb[0] - bb[3]) * (bb[0] - bb[3]) +
		    (bb[1] - bb[4]) * (bb[1] - bb[4]) +
		    (bb[2] - bb[5]) * (bb[2] - bb[5]));
		if (Metric2D.getLength() == 0.0)
			Metric2D.setLength(diagonal);
		String absEpsilonProp = System.getProperty("org.jcae.mesh.amibe.ds.Mesh.epsilon");
		if (absEpsilonProp == null)
		{
			absEpsilonProp = "-1.0";
			System.setProperty("org.jcae.mesh.amibe.ds.Mesh.epsilon", absEpsilonProp);
		}
		Double absEpsilon = new Double(absEpsilonProp);
		epsilon = absEpsilon.doubleValue();
		if (epsilon < 0)
			epsilon = Math.max(diagonal/1000.0, Metric2D.getLength() / 100.0);
		String accumulateEpsilonProp = System.getProperty("org.jcae.mesh.amibe.ds.Mesh.cumulativeEpsilon");
		if (accumulateEpsilonProp == null)
		{
			accumulateEpsilonProp = "false";
			System.setProperty("org.jcae.mesh.amibe.ds.Mesh.cumulativeEpsilon", accumulateEpsilonProp);
		}
		accumulateEpsilon = accumulateEpsilonProp.equals("true");
		logger.debug("Bounding box diagonal: "+diagonal);
		logger.debug("Epsilon: "+epsilon);
	}
	
	public Mesh2D(CADEdge e)
	{
		super();
		face = e;
	}
	
	/**
	 * Returns the topological face.
	 *
	 * @return the topological face.
	 */
	public CADShape getGeometry()
	{
		return face;
	}
	
	/**
	 * Returns the geometrical surface.
	 *
	 * @return the geometrical surface.
	 */
	public CADGeomSurface getGeomSurface()
	{
		return surface;
	}
	
	/**
	 * Initialize a QuadTree with a given bounding box.
	 * @param umin  bottom-left U coordinate.
	 * @param umax  top-right U coordinate.
	 * @param vmin  bottom-left V coordinate.
	 * @param vmax  top-right V coordinate.
	 */
	public void initQuadTree(double umin, double umax, double vmin, double vmax)
	{
		quadtree = new QuadTree(umin, umax, vmin, vmax);
		quadtree.setCompGeom(compGeom());
		Vertex2D.outer = Vertex2D.valueOf((umin+umax)*0.5, (vmin+vmax)*0.5);
	}
	
	/**
	 * Returns the quadtree associated with this mesh.
	 *
	 * @return the quadtree associated with this mesh.
	 */
	public QuadTree getQuadTree()
	{
		return quadtree;
	}
	
	/**
	 * Sets the quadtree associated with this mesh.
	 *
	 * @param q  the quadtree associated with this mesh.
	 */
	public void setQuadTree(QuadTree q)
	{
		quadtree = q;
		quadtree.setCompGeom(compGeom());
	}
	
	/**
	 * Bootstraps node instertion by creating the first triangle.
	 * This initial triangle is counter-clockwise oriented, and
	 * outer triangles are constructed.
	 *
	 * @param v0  first vertex.
	 * @param v1  second vertex.
	 * @param v2  third vertex.
	 */
	public void bootstrap(Vertex2D v0, Vertex2D v1, Vertex2D v2)
	{
		assert quadtree != null;
		assert v0.onLeft(this, v1, v2) != 0L;
		if (v0.onLeft(this, v1, v2) < 0L)
		{
			Vertex2D temp = v2;
			v2 = v1;
			v1 = temp;
		}
		Triangle first = new Triangle(v0, v1, v2);
		Triangle adj0 = new Triangle(Vertex2D.outer, v2, v1);
		Triangle adj1 = new Triangle(Vertex2D.outer, v0, v2);
		Triangle adj2 = new Triangle(Vertex2D.outer, v1, v0);
		OTriangle2D ot = new OTriangle2D(first, 0);
		OTriangle2D oa0 = new OTriangle2D(adj0, 0);
		OTriangle2D oa1 = new OTriangle2D(adj1, 0);
		OTriangle2D oa2 = new OTriangle2D(adj2, 0);
		ot.glue(oa0);
		ot.nextOTri();
		ot.glue(oa1);
		ot.nextOTri();
		ot.glue(oa2);
		oa0.nextOTri();
		oa2.prevOTri();
		oa0.glue(oa2);
		oa0.nextOTri();
		oa1.nextOTri();
		oa0.glue(oa1);
		oa1.nextOTri();
		oa2.prevOTri();
		oa2.glue(oa1);
		
		Vertex2D.outer.setLink(adj0);
		v0.setLink(first);
		v1.setLink(first);
		v2.setLink(first);
		
		add(first);
		add(adj0);
		add(adj1);
		add(adj2);
		quadtree.add(v0);
		quadtree.add(v1);
		quadtree.add(v2);
	}
	
	/**
	 * Enforces an edge between tow points.
	 * This routine is used to build constrained Delaunay meshes.
	 * Intersections between existing mesh segments and the new
	 * segment are computed, then edges are swapped so that the
	 * new edge is part of the mesh.
	 *
	 * @param start    start point.
	 * @param end      end point.
	 * @param maxIter  maximal number of iterations.
	 * @return a handle to the newly created edge.
	 * @throws InitialTriangulationException  if the boundary edge cannot
	 *         be enforced.
	 */
	public OTriangle2D forceBoundaryEdge(Vertex2D start, Vertex2D end, int maxIter)
		throws InitialTriangulationException
	{
		assert (start != end);
		Triangle t = (Triangle) start.getLink();
		OTriangle2D s = new OTriangle2D(t, 0);
		if (s.origin() != start)
			s.nextOTri();
		if (s.origin() != start)
			s.nextOTri();
		assert s.origin() == start : ""+start+" does not belong to "+t;
		Vertex2D dest = (Vertex2D) s.destination();
		int i = 0;
		while (true)
		{
			Vertex2D d = (Vertex2D) s.destination();
			if (d == end)
				return s;
			else if (d != Vertex2D.outer && start.onLeft(this, end, d) > 0L)
				break;
			s.nextOTriOrigin();
			i++;
			if ((Vertex2D) s.destination() == dest || i > maxIter)
				throw new InitialTriangulationException();
		}
		s.prevOTriOrigin();
		dest = (Vertex2D) s.destination();
		i = 0;
		while (true)
		{
			Vertex2D d = (Vertex2D) s.destination();
			if (d == end)
				return s;
			else if (d != Vertex2D.outer && start.onLeft(this, end, d) < 0L)
				break;
			s.prevOTriOrigin();
			i++;
			if (s.destination() == dest || i > maxIter)
				throw new InitialTriangulationException();
		}
		//  s has 'start' as its origin point, its destination point
		//  is to the right side of (start,end) and its apex is to the
		//  left side.
		i = 0;
		while (true)
		{
			int inter = s.forceBoundaryEdge(this, end);
			logger.debug("Intersectionss: "+inter);
			//  s is modified by forceBoundaryEdge, it now has 'end'
			//  as its origin point, its destination point is to the
			//  right side of (end,start) and its apex is to the left
			//  side.  This algorithm can be called iteratively after
			//  exchanging 'start' and 'end', it is known to finish.
			if (s.destination() == start)
				return s;
			i++;
			if (i > maxIter)
				throw new InitialTriangulationException();
			Vertex2D temp = start;
			start = end;
			end = temp;
		}
	}
	
	/**
	 * Sets metrics dimension.
	 * Metrics operations can be performed either on 2D or 3D Euclidien
	 * spaces.  The latter is the normal case, but the former can
	 * also be used, e.g. when retrieving boundary edges of a
	 * constrained mesh.  Argument is either 2 or 3, other values
	 *
	 * @param i  metrics dimension.
	 * @throws IllegalArgumentException  If argument is neither 2 nor 3,
	 *         this exception is raised.
	 */
	public void pushCompGeom(int i)
	{
		if (i == 2)
			compGeomStack.push(new Calculus2D(this));
		else if (i == 3)
			compGeomStack.push(new Calculus3D(this));
		else
			throw new java.lang.IllegalArgumentException("pushCompGeom argument must be either 2 or 3, current value is: "+i);
		if (quadtree != null)
		{
			quadtree.setCompGeom(compGeom());
			quadtree.clearAllMetrics();
		}
	}
	
	/**
	 * Resets metrics dimension.
	 *
	 * @return metrics dimension.
	 * @throws IllegalArgumentException  If argument is neither 2 nor 3,
	 *         this exception is raised.
	 */
	public Calculus popCompGeom()
	{
		//  Metrics are always reset by pushCompGeom.
		//  Only reset them here when there is a change.
		Object ret = compGeomStack.pop();
		if (!compGeomStack.empty() && !ret.getClass().equals(compGeomStack.peek().getClass()) && quadtree != null)
		{
			quadtree.setCompGeom(compGeom());
			quadtree.clearAllMetrics();
		}
		return (Calculus) ret;
	}
	
	/**
	 * Resets metrics dimension.
	 * Checks that the found metrics dimension is identical to the one
	 * expected.
	 *
	 * @param i  expected metrics dimension.
	 * @return metrics dimension.
	 * @throws RuntimeException  If argument is different from
	 *         metrics dimension.
	 */
	public Calculus popCompGeom(int i)
		throws RuntimeException
	{
		Object ret = compGeomStack.pop();
		if (compGeomStack.size() > 0 && !ret.getClass().equals(compGeomStack.peek().getClass()) && quadtree != null)
			quadtree.clearAllMetrics();
		if (i == 2)
		{
			if (!(ret instanceof Calculus2D))
				throw new java.lang.RuntimeException("Internal error.  Expected value: 2, found: 3");
		}
		else if (i == 3)
		{
			if (!(ret instanceof Calculus3D))
				throw new java.lang.RuntimeException("Internal error.  Expected value: 3, found: 2");
		}
		else
			throw new java.lang.IllegalArgumentException("pushCompGeom argument must be either 2 or 3, current value is: "+i);
		return (Calculus) ret;
	}
	
	/**
	 * Returns metrics dimension.
	 *
	 * @return metrics dimension.
	 */
	public Calculus compGeom()
	{
		if (compGeomStack.empty())
			return null;
		return (Calculus) compGeomStack.peek();
	}
	
	/**
	 * Remove degenerted edges.
	 * Degenerated wdges are present in 2D mesh, and have to be
	 * removed in the 2D -&gt; 3D transformation.  Triangles and
	 * vertices must then be updated too.
	 */
	public void removeDegeneratedEdges()
	{
		logger.debug("Removing degenerated edges");
		OTriangle2D ot = new OTriangle2D();
		HashSet removedTriangles = new HashSet();
		for (Iterator it = triangleList.iterator(); it.hasNext(); )
		{
			Triangle t = (Triangle) it.next();
			if (removedTriangles.contains(t))
				continue;
			if (t.isOuter())
				continue;
			ot.bind(t);
			for (int i = 0; i < 3; i++)
			{
				ot.nextOTri();
				if (!ot.hasAttributes(OTriangle.BOUNDARY))
					continue;
				int ref1 = ot.origin().getRef();
				int ref2 = ot.destination().getRef();
				if (ref1 != 0 && ref2 != 0 && ref1 == ref2)
				{
					if (logger.isDebugEnabled())
						logger.debug("  Collapsing "+ot);
					removedTriangles.add(ot.getTri());
					ot.removeDegenerated(this);
					break;
				}
			}
		}
		for (Iterator it = removedTriangles.iterator(); it.hasNext(); )
		{
			Triangle t = (Triangle) it.next();
			triangleList.remove(t);
		}
	}
	
	public boolean isValid(boolean constrained)
	{
		if (!super.isValid(constrained))
			return false;
		for (Iterator it = triangleList.iterator(); it.hasNext(); )
		{
			Triangle t = (Triangle) it.next();
			// We can not rely on t.isOuter() here, attributes
			// may not have been set yet.
			if (t.vertex[0] == Vertex2D.outer || t.vertex[1] == Vertex2D.outer || t.vertex[2] == Vertex2D.outer)
					continue;
			Vertex2D tv0 = (Vertex2D) t.vertex[0];
			Vertex2D tv1 = (Vertex2D) t.vertex[1];
			Vertex2D tv2 = (Vertex2D) t.vertex[2];
			double l = tv0.onLeft(this, tv1, tv2);
			if (l <= 0L)
			{
				logger.debug("Wrong orientation: "+l+" "+t);
				return false;
			}
		}
		return true;
	}
	
}