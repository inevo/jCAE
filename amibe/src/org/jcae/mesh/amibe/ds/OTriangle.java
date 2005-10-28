/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finit element mesher, Plugin architecture.

    Copyright (C) 2004,2005
                  Jerome Robert <jeromerobert@users.sourceforge.net>

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

import java.util.Random;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Stack;
import org.jcae.mesh.amibe.metrics.Metric3D;
import org.apache.log4j.Logger;

/*
 * This class is derived from Jonathan R. Shewchuk's work
 * on Triangle, see
 *       http://www.cs.cmu.edu/~quake/triangle.html
 * His data structure is very compact, and similar ideas were
 * developed here, but due to Java constraints, this version is a
 * little bit less efficient than its C counterpart.
 *
 * Geometrical primitives and basic routines have been written from
 * scratch, but are in many cases very similar to those defined by
 * Shewchuk since data structures are almost equivalent and there
 * are few ways to achieve the same operations.
 *
 * Other ideas come from Bamg, written by Frederic Hecht
 *       http://www-rocq1.inria.fr/gamma/cdrom/www/bamg/eng.htm
 */

/**
 * A handle to {@link Triangle} objects.
 *
 * <p>
 *   Jonathan Richard Shewchuk
 *   <a href="http://www.cs.cmu.edu/~quake/triangle.html">explains</a>
 *   why triangle-based data structures are more efficient than their
 *   edge-based counterparts.  But mesh operations make heavy use of edges,
 *   and informations about adges are not stored in this data structure in
 *   order to be compact.
 * </p>
 *
 * <p>
 *   A triangle is composed of three edges, so a triangle and a number
 *   between 0 and 2 can represent an edge.  This <code>OTriangle</code>
 *   class plays this role, it defines an <em>oriented triangle</em>, or
 *   in other words an oriented edge.  Instances of this class are tied to
 *   their underlying {@link Triangle} instances, so modifications are not
 *   local to this class!
 * </p>
 *
 * <p>
 *   The main goal of this class is to ease mesh traversal.
 *   Consider the <code>ot</code> {@link OTriangle} with a null orientation of
 *   {@link Triangle} <code>t</code>below.
 * </p>
 * <pre>
 *                        V2
 *     V5 _________________,________________, V3
 *        \    &lt;----      / \     &lt;----     /
 *         \     1       /   \      1      /
 *          \   t3    -.//  /\\\   t0   _,/
 *           \      0 ///1   0\\\2    0 //   t.vertex = { V0, V1, V2 }
 *            \      //V   t   \\V     //   t0.vertex = { V2, V1, V3 }
 *             \     /           \     /    t2.vertex = { V0, V4, V1 }
 *              \   /      2      \   /     t3.vertex = { V5, V0, V2 }
 *               \ /     ----&gt;     \ /
 *             V0 +-----------------+ V1
 *                 \     &lt;----     /
 *                  \      1      /
 *                   \    t2   _,/
 *                    \       0//
 * </pre>
 * The following methods can be applied to <code>ot</code>:
 * <pre>
 *    ot.nextOTri();        // Moves (t,0) to (t,1)
 *    ot.prevOTri();        // Moves (t,0) to (t,2)
 *    ot.symOTri();         // Moves (t,0) to (t0,2)
 *    ot.nextOTriOrigin();  // Moves (t,0) to (t2,1)
 *    ot.prevOTriOrigin();  // Moves (t,0) to (t0,0)
 *    ot.nextOTriDest();    // Moves (t,0) to (t0,1)
 *    ot.prevOTriDest();    // Moves (t,0) to (t3,0)
 *    ot.nextOTriApex();    // Moves (t,0) to (t3,1)
 *    ot.prevOTriApex();    // Moves (t,0) to (t2,0)
 * </pre>
 */
public class OTriangle
{
	private static Logger logger = Logger.getLogger(OTriangle.class);
	
	private static final int [] next3 = { 1, 2, 0 };
	private static final int [] prev3 = { 2, 0, 1 };
	private double [] tempD = new double[3];
	private double [] tempD1 = new double[3];
	private double [] tempD2 = new double[3];
	private static final OTriangle otVoid = new OTriangle();
	
	/**
	 * Numeric constants for edge attributes.  Set if edge is on
	 * boundary.
	 * @see #setAttributes
	 * @see #hasAttributes
	 */
	public static final int BOUNDARY = 1 << 0;
	/**
	 * Numeric constants for edge attributes.  Set if edge is outer.
	 * (Ie. one of its end point is {@link Vertex#outer})
	 * @see #setAttributes
	 * @see #hasAttributes
	 */
	public static final int OUTER    = 1 << 1;
	/**
	 * Numeric constants for edge attributes.  Set if edge had been
	 * swapped.
	 * @see #setAttributes
	 * @see #hasAttributes
	 */
	public static final int SWAPPED  = 1 << 2;
	/**
	 * Numeric constants for edge attributes.  Set if edge had been
	 * marked (for any operation).
	 * @see #setAttributes
	 * @see #hasAttributes
	 */
	public static final int MARKED   = 1 << 3;
	/**
	 * Numeric constants for edge attributes.  Set if edge is the inner
	 * edge of a quadrangle.
	 * @see #setAttributes
	 * @see #hasAttributes
	 */
	public static final int QUAD     = 1 << 4;
	/**
	 * Numeric constants for edge attributes.  Set if edge is non
	 * manifold.
	 * @see #setAttributes
	 * @see #hasAttributes
	 */
	public static final int NONMANIFOLD = 1 << 5;
	
	//  Complex algorithms require several OTriangle, they are
	//  allocated here to prevent allocation/deallocation overhead.
	private static OTriangle [] work = new OTriangle[4];
	static {
		for (int i = 0; i < 4; i++)
			work[i] = new OTriangle();
	}
	
	private static final Triangle dummy = new Triangle();
	
	/*
	 * Vertices can be accessed through
	 *        origin = tri.vertex[next3[orientation]]
	 *   destination = tri.vertex[prev3[orientation]]
	 *          apex = tri.vertex[orientation]
	 * Adjacent triangle is tri.adj[orientation].tri and its orientation
	 * is ((tri.adjPos >> (2*orientation)) & 3)
	 */
	protected Triangle tri;
	protected int orientation;
	protected int attributes;
	
	/**
	 * Sole constructor.
	 */
	public OTriangle()
	{
		tri = null;
		orientation = 0;
		attributes = 0;
	}
	
	/**
	 * Create an object to handle data about a triangle.
	 *
	 * @param t  geometrical triangle.
	 * @param o  a number between 0 and 2 determining an edge.
	 */
	public OTriangle(Triangle t, int o)
	{
		tri = t;
		orientation = o;
		attributes = (tri.adjPos >> (8*(1+orientation))) & 0xff;
	}
	
	/**
	 * Return the triangle tied to this object.
	 *
	 * @return the triangle tied to this object.
	 */
	public final Triangle getTri()
	{
		return tri;
	}
	
	/**
	 * Set the triangle tied to this object, and resets orientation.
	 *
	 * @param t  the triangle tied to this object.
	 */
	public final void bind(Triangle t)
	{
		tri = t;
		orientation = 0;
		attributes = (tri.adjPos >> 8) & 0xff;
	}
	
	public final void bind(Triangle t, int o)
	{
		tri = t;
		orientation = o;
		pullAttributes();
	}
	
	/**
	 * Check if some attributes of this oriented triangle are set.
	 *
	 * @param attr  the attributes to check
	 * @return <code>true</code> if this OTriangle has all these
	 * attributes set, <code>false</code> otherwise.
	 */
	public final boolean hasAttributes(int attr)
	{
		return (attributes & attr) == attr;
	}
	
	/**
	 * Set attributes of this oriented triangle.
	 *
	 * @param attr  the attribute of this oriented triangle.
	 */
	public final void setAttributes(int attr)
	{
		attributes |= attr;
		pushAttributes();
	}
	
	/**
	 * Reset attributes of this oriented triangle.
	 *
	 * @param attr   the attributes of this oriented triangle to clear out.
	 */
	public final void clearAttributes(int attr)
	{
		attributes &= ~attr;
		pushAttributes();
	}
	
	// Adjust tri.adjPos after attributes is modified.
	public final void pushAttributes()
	{
		tri.adjPos &= ~(0xff << (8*(1+orientation)));
		tri.adjPos |= ((attributes & 0xff) << (8*(1+orientation)));
	}
	
	// Adjust attributes after tri.adjPos is modified.
	public final void pullAttributes()
	{
		attributes = (tri.adjPos >> (8*(1+orientation))) & 0xff;
	}
	
	/**
	 * Copy an <code>OTriangle</code> into another <code>OTriangle</code>.
	 *
	 * @param src   <code>OTriangle</code> being duplicated
	 * @param dest  already allocated <code>OTriangle</code> where data are
	 *              copied
	 */
	public static final void copyOTri(OTriangle src, OTriangle dest)
	{
		dest.tri = src.tri;
		dest.orientation = src.orientation;
		dest.attributes = src.attributes;
	}
	
	//  These geometrical primitives have 2 signatures:
	//      fct(this, that)   applies fct to 'this' and stores result
	//                        in an already allocated object 'that'.
	//      fct() transforms current object.
	//  This is definitely not an OO approach, but it is much more
	//  efficient by preventing useless memory allocations.
	//  They do not return any value to make clear that calling
	//  these routines requires extra care.
	
	/**
	 * Copy an <code>OTriangle</code> and move to its symmetric edge.
	 *
	 * @param o     source <code>OTriangle</code>
	 * @param that  already allocated <code>OTriangle</code> where data are
	 *              copied
	 */
	public static final void symOTri(OTriangle o, OTriangle that)
	{
		that.tri = (Triangle) o.tri.getAdj(o.orientation);
		that.orientation = ((o.tri.adjPos >> (2*o.orientation)) & 3);
		that.attributes = (that.tri.adjPos >> (8*(1+that.orientation))) & 0xff;
	}
	
	/**
	 * Move to the symmetric edge.
	 */
	public final void symOTri()
	{
		int neworient = ((tri.adjPos >> (2*orientation)) & 3);
		tri = (Triangle) tri.getAdj(orientation);
		orientation = neworient;
		attributes = (tri.adjPos >> (8*(1+orientation))) & 0xff;
	}
	
	/**
	 * Copy an <code>OTriangle</code> and move it to the counterclockwaise
	 * following edge.
	 *
	 * @param o     source <code>OTriangle</code>
	 * @param that  already allocated <code>OTriangle</code> where data are
	 *              copied
	 */
	public static final void nextOTri(OTriangle o, OTriangle that)
	{
		that.tri = o.tri;
		that.orientation = next3[o.orientation];
		that.attributes = (that.tri.adjPos >> (8*(1+that.orientation))) & 0xff;
	}
	
	/**
	 * Move to the counterclockwaise following edge.
	 */
	public final void nextOTri()
	{
		orientation = next3[orientation];
		attributes = (tri.adjPos >> (8*(1+orientation))) & 0xff;
	}
	
	/**
	 * Copy an <code>OTriangle</code> and move it to the counterclockwaise
	 * previous edge.
	 *
	 * @param o     source <code>OTriangle</code>
	 * @param that  already allocated <code>OTriangle</code> where data are
	 *              copied
	 */
	public static final void prevOTri(OTriangle o, OTriangle that)
	{
		that.tri = o.tri;
		that.orientation = prev3[o.orientation];
		that.attributes = (that.tri.adjPos >> (8*(1+that.orientation))) & 0xff;
	}
	
	/**
	 * Move to the counterclockwaise previous edge.
	 */
	public final void prevOTri()
	{
		orientation = prev3[orientation];
		attributes = (tri.adjPos >> (8*(1+orientation))) & 0xff;
	}
	
	/**
	 * Copy an <code>OTriangle</code> and move it to the counterclockwaise
	 * following edge which has the same origin.
	 *
	 * @param o     source <code>OTriangle</code>
	 * @param that  already allocated <code>OTriangle</code> where data are
	 *              copied
	 */
	public static final void nextOTriOrigin(OTriangle o, OTriangle that)
	{
		prevOTri(o, that);
		that.symOTri();
	}
	
	/**
	 * Move counterclockwaise to the following edge with the same origin.
	 */
	public final void nextOTriOrigin()
	{
		prevOTri();
		symOTri();
	}
	
	/**
	 * Copy an <code>OTriangle</code> and move it to the counterclockwaise
	 * previous edge which has the same origin.
	 *
	 * @param o     source <code>OTriangle</code>
	 * @param that  already allocated <code>OTriangle</code> where data are
	 *              copied
	 */
	public static final void prevOTriOrigin(OTriangle o, OTriangle that)
	{
		symOTri(o, that);
		that.nextOTri();
	}
	
	/**
	 * Move counterclockwaise to the previous edge with the same origin.
	 */
	public final void prevOTriOrigin()
	{
		symOTri();
		nextOTri();
	}
	
	/**
	 * Copy an <code>OTriangle</code> and move it to the counterclockwaise
	 * following edge which has the same destination.
	 *
	 * @param o     source <code>OTriangle</code>
	 * @param that  already allocated <code>OTriangle</code> where data are
	 *              copied
	 */
	public static final void nextOTriDest(OTriangle o, OTriangle that)
	{
		symOTri(o, that);
		that.prevOTri();
	}
	
	/**
	 * Move counterclockwaise to the following edge with the same
	 * destination.
	 */
	public final void nextOTriDest()
	{
		symOTri();
		prevOTri();
	}
	
	/**
	 * Copy an <code>OTriangle</code> and move it to the counterclockwaise
	 * previous edge which has the same destination.
	 *
	 * @param o     source <code>OTriangle</code>
	 * @param that  already allocated <code>OTriangle</code> where data are
	 *              copied
	 */
	public static final void prevOTriDest(OTriangle o, OTriangle that)
	{
		nextOTri(o, that);
		that.symOTri();
	}
	
	/**
	 * Move counterclockwaise to the previous edge with the same
	 * destination.
	 */
	public final void prevOTriDest()
	{
		nextOTri();
		symOTri();
	}
	
	/**
	 * Copy an <code>OTriangle</code> and move it to the counterclockwaise
	 * following edge which has the same apex.
	 *
	 * @param o     source <code>OTriangle</code>
	 * @param that  already allocated <code>OTriangle</code> where data are
	 *              copied
	 */
	public static final void nextOTriApex(OTriangle o, OTriangle that)
	{
		nextOTri(o, that);
		that.symOTri();
		that.nextOTri();
	}
	
	/**
	 * Move counterclockwaise to the following edge with the same apex.
	 */
	public final void nextOTriApex()
	{
		nextOTri();
		symOTri();
		nextOTri();
	}
	
	/**
	 * Copy an <code>OTriangle</code> and move it to the counterclockwaise
	 * previous edge which has the same apex.
	 *
	 * @param o     source <code>OTriangle</code>
	 * @param that  already allocated <code>OTriangle</code> where data are
	 *              copied
	 */
	public static final void prevOTriApex(OTriangle o, OTriangle that)
	{
		prevOTri(o, that);
		that.symOTri();
		that.prevOTri();
	}
	
	/**
	 * Move counterclockwaise to the previous edge with the same apex.
	 */
	public final void prevOTriApex()
	{
		prevOTri();
		symOTri();
		prevOTri();
	}
	
	/**
	 * Returns the start vertex of this edge.
	 *
	 * @return the start vertex of this edge.
	 */
	public final Vertex origin()
	{
		return tri.vertex[next3[orientation]];
	}
	
	/**
	 * Returns the end vertex of this edge.
	 *
	 * @return the end vertex of this edge.
	 */
	public final Vertex destination()
	{
		return tri.vertex[prev3[orientation]];
	}
	
	/**
	 * Returns the apex of this edge.
	 *
	 * @return the apex of this edge.
	 */
	public final Vertex apex()
	{
		return tri.vertex[orientation];
	}
	
	//  The following 3 methods change the underlying triangle.
	//  So they also modify all OTriangle bound to this one.
	/**
	 * Sets the start vertex of this edge.
	 *
	 * @param v  the start vertex of this edge.
	 */
	public final void setOrigin(Vertex v)
	{
		tri.vertex[next3[orientation]] = v;
	}
	
	/**
	 * Sets the end vertex of this edge.
	 *
	 * @param v  the end vertex of this edge.
	 */
	public final void setDestination(Vertex v)
	{
		tri.vertex[prev3[orientation]] = v;
	}
	
	/**
	 * Sets the apex of this edge.
	 *
	 * @param v  the apex of this edge.
	 */
	public final void setApex(Vertex v)
	{
		tri.vertex[orientation] = v;
	}
	
	/**
	 * Sets adjacency relations between two triangles.
	 *
	 * @param sym  the triangle bond to this one.
	 */
	public final void glue(OTriangle sym)
	{
		assert !(hasAttributes(NONMANIFOLD) || sym.hasAttributes(NONMANIFOLD)) : this+"\n"+sym;
		tri.glue1(orientation, sym.tri, sym.orientation);
		sym.tri.glue1(sym.orientation, tri, orientation);
	}
	
	/**
	 * Sets adjacency relation for a triangle.
	 *
	 * @param link  the triangle bond to this one of this edge is manifold, or an Object otherwise.
	 */
	public final Object getAdj()
	{
		return tri.getAdj(orientation);
	}
	
	public final void setAdj(Object link)
	{
		tri.setAdj(orientation, link);
	}
	
	protected Iterator getOTriangleAroundApexIterator()
	{
		final OTriangle ot = this;
		return new Iterator()
		{
			private Vertex first = ot.origin();
			private boolean lookAhead = false;
			private boolean init = true;
			private boolean loop = false;
			public boolean hasNext()
			{
				if (!lookAhead)
				{
					next();
					lookAhead = true;
				}
				return !(loop && ot.origin() == first);
			}
			public Object next()
			{
				if (lookAhead)
				{
					lookAhead = false;
					return ot;
				}
				if (init)
				{
					init = false;
					return ot;
				}
				lookAhead = false;
				loop = true;
				if (ot.hasAttributes(OUTER))
				{
					// Loop clockwise to another boundary
					// and start again from there.
					ot.prevOTri();
					ot.nextOTriDest();
					while (true)
					{
						ot.prevOTri();
						if (ot.hasAttributes(BOUNDARY))
							break;
						ot.nextOTriDest();
					}
				}
				else
					ot.prevOTriDest();
				ot.nextOTri();
				return ot;
			}
			public void remove()
			{
			}
		};
	}
	
	public Iterator getOTriangleAroundOriginIterator()
	{
		final OTriangle ot = this;
		return new Iterator()
		{
			private Vertex first = ot.destination();
			private boolean lookAhead = false;
			private boolean init = true;
			private int state = 0;
			public boolean hasNext()
			{
				if (init)
					return true;
				if (!lookAhead)
				{
					next();
					lookAhead = true;
				}
				return !(state > 0 && ot.destination() == first);
			}
			public Object next()
			{
				if (init)
				{
					init = false;
					return ot;
				}
				if (lookAhead)
				{
					lookAhead = false;
					return ot;
				}
				lookAhead = false;
				if (state == 0)
					state = 1;
				if (ot.hasAttributes(OUTER) && state == 1)
				{
					// Loop clockwise to another boundary
					// and start again from there.
					state = 2;
					ot.prevOTriOrigin();
					while (true)
					{
						if (ot.hasAttributes(OUTER))
							break;
						ot.prevOTriOrigin();
					}
				}
				else
				{
					ot.nextOTriOrigin();
				}
				return ot;
			}
			public void remove()
			{
			}
		};
	}
	
	/**
	 * Checks whether an edge can be swapped.
	 *
	 * @return <code>false</code> if edge is a boundary or outside the mesh,
	 * <code>true</code> otherwise.
	 */
	public final boolean isMutable()
	{
		return !(hasAttributes(BOUNDARY) || hasAttributes(OUTER));
	}
	
	/**
	 * Checks whether an edge is Delaunay.
	 *
	 * As apical vertices are already computed by calling routines,
	 * they are passed as parameters for efficiency reasons.
	 *
	 * @param apex2  apex of the symmetric edge
	 * @return <code>true</code> if edge is Delaunay, <code>false</code>
	 * otherwise.
	 */
	public final boolean isDelaunay(Vertex apex2)
	{
		if (apex2.isPseudoIsotropic())
			return isDelaunay_isotropic(apex2);
		return isDelaunay_anisotropic(apex2);
	}
	
	private final boolean isDelaunay_isotropic(Vertex apex2)
	{
		assert Vertex.outer != origin();
		assert Vertex.outer != destination();
		assert Vertex.outer != apex();
		Vertex vA = origin();
		Vertex vB = destination();
		Vertex v1 = apex();
		long tp1 = vA.onLeft(vB, v1);
		long tp2 = vB.onLeft(vA, apex2);
		long tp3 = apex2.onLeft(vB, v1);
		long tp4 = v1.onLeft(vA, apex2);
		if (Math.abs(tp3) + Math.abs(tp4) < Math.abs(tp1)+Math.abs(tp2) )
			return true;
		if (tp1 > 0L && tp2 > 0L)
		{
			if (tp3 <= 0L || tp4 <= 0L)
				return true;
		}
		return !apex2.inCircleTest2(this);
	}
	
	private final boolean isDelaunay_anisotropic(Vertex apex2)
	{
		assert Vertex.outer != origin();
		assert Vertex.outer != destination();
		assert Vertex.outer != apex();
		if (apex2 == Vertex.outer)
			return true;
		return !apex2.inCircleTest3(this);
	}
	
	/**
	 * Check whether an edge can be contracted.
	 * @return <code>true</code> if this edge can be contracted, <code>flase</code> otherwise.
	 */
	public final boolean canContract(Vertex n)
	{
		if (n.mesh.getType() == Mesh.MESH_3D && !checkInversion(n))
				return false;
		
		//  Topology check
		//  TODO: normally this check could be removed, but the
		//        following test triggers an error:
		//    * mesh Scie_shell.brep with deflexion=0.2 aboslute
		//    * decimate with length=6
		ArrayList link = origin().getNeighboursNodes();
		link.retainAll(destination().getNeighboursNodes());
		return link.size() < 3;
	}
	
	/**
	 * Swaps an edge.
	 *
	 * This routine swaps an edge (od) to (na), updates
	 * adjacency relations and backward links between vertices and
	 * triangles.  Current object is transformed from (oda) to (ona)
	 * and not (nao), because this helps turning around o, e.g.
	 * at the end of {@link #split3}.
	 *
	 * @param a  apex of the current edge
	 * @param n  apex of the symmetric edge
	 * @return a handle to (ona) oriented triangle.
	 * otherwise.
	 */
	public final OTriangle swapOTriangle(Vertex a, Vertex n)
	{
		Vertex o = origin();
		Vertex d = destination();
		/*
		 *            d                    d
		 *            .                    .
		 *           /|\                  / \
		 *       a1 / | \ a4         a1  /   \ a4
		 *         /  |  \              /     \
		 *      a +   |   + n        a +-------+ n
		 *         \  |  /              \     /
		 *       a2 \ | / a3         a2  \   / a3
		 *           \|/                  \ /
		 *            '                    '
		 *            o                    o
		 *                                 .
		 *            | switch            /|\
		 *            |   adj              |  switch
		 *           \|/                   | vertices
		 *            '
		 *            d                    n
		 *            .                    .
		 *           /|\                  / \
		 *       a2 / | \ a1         a1  /   \ a4
		 *         /  |  \              /     \
		 *      a +   |   + n   is   d +-------+ o
		 *         \  |  /              \     /
		 *       a3 \ | / a4         a2  \   / a3
		 *           \|/                  \ /
		 *            '                    '
		 *            o                    a
		 */
		// this = (oda)
		symOTri(this, work[0]);         // (don)
		//  Clear SWAPPED flag for all edges of the 2 triangles
		clearAttributes(SWAPPED);
		work[0].clearAttributes(SWAPPED);
		
		nextOTri(this, work[1]);        // (dao)
		work[1].clearAttributes(SWAPPED);
		int attr1 = work[1].attributes;
		work[1].symOTri();              // a1 = (ad*)
		work[1].clearAttributes(SWAPPED);
		prevOTri(this, work[2]);        // (aod)
		work[2].clearAttributes(SWAPPED);
		int attr2 = work[2].attributes;
		nextOTri();                     // (dao)
		work[2].symOTri();              // a2 = (oa*)
		work[2].clearAttributes(SWAPPED);
		glue(work[2]);                  // a2 and (dao)
		nextOTri(work[0], work[2]);     // (ond)
		nextOTri();                     // (aod)
		work[2].clearAttributes(SWAPPED);
		int attr3 = work[2].attributes;
		work[2].symOTri();             // a3 = (no*)
		work[2].clearAttributes(SWAPPED);
		work[2].glue(this);            // a3 and (aod)
		//  Reset 'this' to (oda)
		nextOTri();                     // (oda)
		prevOTri(work[0], work[2]);     // (ndo)
		work[2].clearAttributes(SWAPPED);
		int attr4 = work[2].attributes;
		work[0].nextOTri();             // (ond)
		work[2].symOTri();              // a4 = (dn*)
		work[2].clearAttributes(SWAPPED);
		work[2].glue(work[0]);          // a4 and (ond)
		work[0].nextOTri();             // (ndo)
		work[0].glue(work[1]);          // a1 and (ndo)
		work[0].nextOTri();             // (don)
		//  Adjust vertices
		setOrigin(n);
		setDestination(a);
		setApex(o);
		work[0].setOrigin(a);
		work[0].setDestination(n);
		work[0].setApex(d);
		//  Fix links to triangles
		n.setLink(tri);
		a.setLink(tri);
		o.setLink(tri);
		d.setLink(work[0].tri);
		//  Fix attributes
		tri.adjPos &= 0xff;
		tri.adjPos |= ((attr2 & 0xff) << (8*(1+next3[orientation])));
		tri.adjPos |= ((attr3 & 0xff) << (8*(1+prev3[orientation])));
		work[0].tri.adjPos &= 0xff;
		work[0].tri.adjPos |= ((attr4 & 0xff) << (8*(1+next3[work[0].orientation])));
		work[0].tri.adjPos |= ((attr1 & 0xff) << (8*(1+prev3[work[0].orientation])));
		//  Mark new edge
		setAttributes(SWAPPED);
		work[0].setAttributes(SWAPPED);

		//  Eventually change 'this' to (ona) to ease moving around o.
		prevOTri();                     // (ona)
		return this;
	}
	
	public double [] getTempVector()
	{
		return tempD;
	}
	
	private final boolean checkInversion(Vertex n)
	{
		Vertex o = origin();
		Vertex d = destination();
		nextOTri(this, work[0]);
		prevOTri(this, work[1]);
		//  If both adjacent edges are on a boundary, do not contract
		if (work[0].hasAttributes(BOUNDARY) && work[1].hasAttributes(BOUNDARY))
				return false;
		symOTri(this, work[1]);
		symOTri(this, work[0]);
		work[0].prevOTri();
		work[1].nextOTri();
		if (work[0].hasAttributes(BOUNDARY) && work[1].hasAttributes(BOUNDARY))
				return false;
		//  Loop around o to check that triangles will not be inverted
		nextOTri(this, work[0]);
		symOTri(this, work[1]);
		double [] v1 = new double[3];
		double [] xn = n.getUV();
		double [] xo = o.getUV();
		do
		{
			work[0].nextOTriApex();
			if (work[0].tri != tri && work[0].tri != work[1].tri && !work[0].hasAttributes(OUTER))
			{
				work[0].computeNormal3DT();
				double [] nu = work[0].getTempVector();
				double [] x1 = work[0].origin().getUV();
				for (int i = 0; i < 3; i++)
					v1[i] = xn[i] - x1[i];
				if (Metric3D.prodSca(v1, nu) >= 0.0)
					return false;
			}
		}
		while (work[0].destination() != d);
		//  Loop around d to check that triangles will not be inverted
		work[0].prevOTri();
		xo = d.getUV();
		do
		{
			work[0].nextOTriApex();
			if (work[0].tri != tri && work[0].tri != work[1].tri && !work[0].hasAttributes(OUTER))
			{
				work[0].computeNormal3DT();
				double [] nu = work[0].getTempVector();
				double [] x1 = work[0].origin().getUV();
				for (int i = 0; i < 3; i++)
					v1[i] = xn[i] - x1[i];
				if (Metric3D.prodSca(v1, nu) >= 0.0)
					return false;
			}
		}
		while (work[0].destination() != o);
		return true;
	}
	
	// Warning: this vectore is not normalized, it has the same length as
	// this.
	public void computeNormal3DT()
	{
		double [] p0 = origin().getUV();
		double [] p1 = destination().getUV();
		double [] p2 = apex().getUV();
		tempD1[0] = p1[0] - p0[0];
		tempD1[1] = p1[1] - p0[1];
		tempD1[2] = p1[2] - p0[2];
		tempD[0] = p2[0] - p0[0];
		tempD[1] = p2[1] - p0[1];
		tempD[2] = p2[2] - p0[2];
		Metric3D.prodVect3D(tempD1, tempD, tempD2);
		double norm = Metric3D.norm(tempD2);
		if (norm != 0.0)
		{
			tempD2[0] /= norm;
			tempD2[1] /= norm;
			tempD2[2] /= norm;
		}
		Metric3D.prodVect3D(tempD1, tempD2, tempD);
	}
	
	public void computeNormal3D()
	{
		double [] p0 = origin().getUV();
		double [] p1 = destination().getUV();
		double [] p2 = apex().getUV();
		tempD1[0] = p1[0] - p0[0];
		tempD1[1] = p1[1] - p0[1];
		tempD1[2] = p1[2] - p0[2];
		tempD2[0] = p2[0] - p0[0];
		tempD2[1] = p2[1] - p0[1];
		tempD2[2] = p2[2] - p0[2];
		Metric3D.prodVect3D(tempD1, tempD2, tempD);
		double norm = Metric3D.norm(tempD);
		if (norm != 0.0)
		{
			tempD[0] /= norm;
			tempD[1] /= norm;
			tempD[2] /= norm;
		}
	}
	
	/**
	 * Contract an edge.
	 * TODO: Attributes are not checked.
	 * @param n the resulting vertex
	 */
	public final void contract(Vertex n)
	{
		Vertex o = origin();
		Vertex d = destination();
		logger.debug("contract ("+o+" "+d+")\ninto "+n);
		/*
		 *           V1                       V1
		 *  V3+-------+-------+ V4   V3 +------+------+ V4
		 *     \ t3  / \ t4  /           \  t3 | t4  / 
		 *      \   /   \   /              \   |   /
		 *       \ / t1  \ /                 \ | /  
		 *      o +-------+ d   ------>      n +
		 *       / \ t2  / \                 / | \
		 *      /   \   /   \              /   |   \
		 *     / t5  \ / t6  \           /  t5 | t6  \
		 *    +-------+-------+         +------+------+
		 *  V5        V2       V6     V5       V2      V6
		 */
		// this = (odV1)
		
		//  Replace o by n in all incident triangles
		//  NOTE: if t5 is outer, it will not be updated by this loop
		copyOTri(this, work[0]);
		for (Iterator it = work[0].getOTriangleAroundOriginIterator(); it.hasNext(); )
		{
			work[0] = (OTriangle) it.next();
			work[0].setOrigin(n);
			work[0].nextOTriOrigin();
			if (work[0].destination() == n)
				break;
		}
		//  Replace d by n in all incident triangles
		//  NOTE: if t4 is outer, it will not be updated by this loop
		symOTri(this, work[0]);
		for (Iterator it = work[0].getOTriangleAroundOriginIterator(); it.hasNext(); )
		{
			work[0] = (OTriangle) it.next();
			work[0].setOrigin(n);
		}
		//  Update adjacency links.  For clarity, o and d are
		//  written instead of n.
		if (!hasAttributes(OUTER))
		{
			nextOTri();             // (dV1o)
			int attr4 = attributes;
			symOTri(this, work[0]); // (V1dV4)
			//  See NOTE above
			work[0].setDestination(n);
			nextOTri();             // (V1od)
			int attr3 = attributes;
			symOTri(this, work[1]); // (oV1V3)
			work[0].glue(work[1]);
			work[0].attributes |= attr3;
			work[1].attributes |= attr4;
			work[0].pushAttributes();
			work[1].pushAttributes();
			Triangle t34 = work[1].tri;
			if (t34.isOuter())
				t34 = work[0].tri;
			assert !t34.isOuter() : work[0]+"\n"+work[1];
			work[1].destination().setLink(t34);
			n.setLink(t34);
			nextOTri();             // (odV1)
		}
		symOTri();                      // (doV2)
		if (!hasAttributes(OUTER))
		{
			nextOTri();             // (oV2d)
			int attr5 = attributes;
			symOTri(this, work[0]); // (V2oV5)
			//  See NOTE above
			work[0].setDestination(n);
			nextOTri();             // (V2do)
			int attr6 = attributes;
			symOTri(this, work[1]); // (dV2V6)
			work[0].glue(work[1]);
			work[0].attributes |= attr6;
			work[1].attributes |= attr5;
			work[0].pushAttributes();
			work[1].pushAttributes();
			Triangle t56 = work[0].tri;
			if (t56.isOuter())
				t56 = work[1].tri;
			assert !t56.isOuter();
			work[0].origin().setLink(t56);
			n.setLink(t56);
			nextOTri();             // (doV2)
		}
		clearAttributes(MARKED);
		pushAttributes();
		symOTri();
		clearAttributes(MARKED);
		pushAttributes();
	}
	
	public void invertOrientationFace(boolean markLocked)
	{
		assert markLocked == true;
		// Swap origin and destination, update adjacency relations and process
		// neighbours
		Vertex o = origin();
		Vertex d = destination();
		Stack todo = new Stack();
		HashSet seen = new HashSet();
		todo.push(tri);
		todo.push(new Integer(orientation));
		swapVertices(seen, todo);
		assert o == destination() : o+" "+d+" "+this;
	}
	
	private static void swapVertices(HashSet seen, Stack todo)
	{
		OTriangle ot = new OTriangle();
		OTriangle sym = new OTriangle();
		while (todo.size() > 0)
		{
			int o = ((Integer) todo.pop()).intValue();
			Triangle t = (Triangle) todo.pop();
			if (seen.contains(t))
				continue;
			seen.add(t);
			Vertex temp = t.vertex[next3[o]];
			t.vertex[next3[o]] = t.vertex[prev3[o]];
			t.vertex[prev3[o]] = temp;
			Object a = t.getAdj(next3[o]);
			t.setAdj(next3[o], t.getAdj(prev3[o]));
			t.setAdj(prev3[o], a);
			// Swap attributes for edges
			if (o == 0)
				t.swapAttributes12();
			else if (o == 1)
				t.swapAttributes02();
			else
				t.swapAttributes01();
			// Fix adjacent triangles
			ot.bind(t);
			for (int i = 0; i < 3; i++)
			{
				ot.nextOTri();
				if (!ot.hasAttributes(BOUNDARY))
				{
					if (!ot.hasAttributes(NONMANIFOLD))
					{
						OTriangle.symOTri(ot, sym);
						todo.push(sym.tri);
						todo.push(new Integer(sym.orientation));
						sym.tri.glue1(sym.orientation, ot.tri, ot.orientation);
					}
				}
			}
		}
	}
	
	private final String showAdj(int num)
	{
		if (!(tri.getAdj(num) instanceof Triangle))
			return "N/A";
		String r = "";
		Triangle t = (Triangle) tri.getAdj(num);
		if (t == null)
			r+= "null";
		else
			r+= t.hashCode()+"["+(((tri.adjPos & (3 << (2*num))) >> (2*num)) & 3)+"]";
		return r;
	}
	
	public String toString()
	{
		String r = "Orientation: "+orientation;
		r += "\nTri hashcode: "+tri.hashCode();
		r += "\nAdjacency: "+showAdj(0)+" "+showAdj(1)+" "+showAdj(2);
		r += "\nAttributes: "+Integer.toHexString((tri.adjPos >> 8) & 0xff)+" "+Integer.toHexString((tri.adjPos >> 16) & 0xff)+" "+Integer.toHexString((tri.adjPos >> 24) & 0xff)+" => "+Integer.toHexString(attributes);
		r += "\nVertices:";
		r += "\n  Origin: "+origin();
		r += "\n  Destination: "+destination();
		r += "\n  Apex: "+apex();
		return r;
	}

}
