/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2006, by EADS CRC
    Copyright (C) 2007,2008,2009, by EADS France

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


package org.jcae.mesh.bora.ds;

import org.jcae.mesh.cad.CADShapeEnum;

import java.util.Collection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * Here is an example with 2 connected faces.
 *       +----------+---------+
 *       |          |E1       |
 *       |          |         |
 *       |   F1     |    F2   |
 *       |          |         |
 *       +----------+---------+
 * Let us define 3 constraints:
 *   c1 = (F1, h1)
 *   c2 = (F2, h2)
 *   c3 = (E1, h3)
 * where h1, h2 and h3 are three known hypothesis.
 * We have to consider the following cases:
 *   .------------------------------------------------.
 *   |   Submesh    |  Expected result                | 
 *   |--------------+---------------------------------|
 *   | S = c1+c2    | A single mesh over F1 and F2,   |
 *   |              | E1 tessellation is removed when |
 *   |              | optimizing.                     |
 *   |--------------+---------------------------------|
 *   | S = c1+c2+c3 | A simple mesh, E1 tessellation  |
 *   |              | is preserved.                   |
 *   |--------------+---------------------------------|
 *   | S1 = c1      | Two independent meshes, E1 has  |
 *   | S2 = c2      | two distinct tessellations      |
 *   |--------------+---------------------------------|
 *   | S1 = c1+c3   | Two consistent meshes           |
 *   | S2 = c2+c3   |                                 |
 *   `------------------------------------------------'
 */
public class BSubMesh
{
	private static final Logger LOGGER=Logger.getLogger(BSubMesh.class.getName());
	//   Model
	private final BModel model;
	//   Unique identitier
	private final int id;
	//   List of user defined constraints
	private final Collection<Constraint> constraints = new ArrayList<Constraint>();

	/**
	 * Creates a root mesh.
	 */
	public BSubMesh(BModel m, int id)
	{
		model = m;
		this.id = id;
	}
	
	public int getId()
	{
		return id;
	}

	public Collection<Constraint> getConstraints()
	{
		return constraints;
	}

	/**
	 * Add a constraint to current submesh
	 *
	 * @param cons  constraint to add
	 */
	public void add(Constraint cons)
	{
		if (LOGGER.isLoggable(Level.FINE))
			LOGGER.fine("Add constraint "+cons+" to submesh "+id);
		model.addConstraint(cons);
		constraints.add(cons);
		// Add this Constraint to the CAD cell
		BCADGraphCell cell = cons.getGraphCell();
		cell.addSubMeshConstraint(this, cons);
	}

	/**
	 * Prints discretizations to a submesh.
	 * Some cells may appear twice with opposite orientations but the same discretization.
	 * This is due to the use of shapesExplorer on the root of the CAD. Another solution 
	 * would have been to use the CADGraphCell in the user constraints related to the 
	 * submesh. This solution would ensure that the CAD object with the right orientation 
	 * is printed, but CAD objects related to more than one constraint on this submesh 
	 * would appear that many times.
	 */
	public void printSubmeshDiscretizations()
	{
		BCADGraphCell root = model.getGraph().getRootCell();
		StringBuilder indent = new StringBuilder();

		for (Iterator<CADShapeEnum> itcse = CADShapeEnum.iterator(CADShapeEnum.VERTEX, CADShapeEnum.COMPOUND); itcse.hasNext(); )
		{
			CADShapeEnum cse = itcse.next();
			String tab = indent.toString();
			/* returns only one of the two shapes of the same orientation, but not
			   necessarily the good one. */
			// for (Iterator its = root.uniqueShapesExplorer(cse); its.hasNext(); )
			/* may returns the two shapes of the same orientation */
 		        for (Iterator<BCADGraphCell> its = root.shapesExplorer(cse); its.hasNext(); )
			{
				BCADGraphCell cell = its.next();
				for (Iterator<BDiscretization> itd = cell.discretizationIterator(); itd.hasNext(); )
				{
					BDiscretization discr = itd.next();
					if (discr.contains(this) && discr.isSubmeshChild(this))
					{
						System.out.println(tab+"Shape "+cell);
						System.out.println(tab+"    + "+discr);
					}
				}
			}
			indent.append("  ");
		}
	}

	// Sample test
	public static void main(String args[])
	{
		String file = "brep/2cubes.brep";

		BModel model = new BModel(file, "out");
		BCADGraphCell root = model.getGraph().getRootCell();
		Iterator<BCADGraphCell> its = root.shapesExplorer(CADShapeEnum.SOLID);
		BCADGraphCell [] solids = new BCADGraphCell[2];
		solids[0] = its.next();
		solids[1] = its.next();
		BCADGraphCell f0 = model.getGraph().getById(7);

		Hypothesis h1 = new Hypothesis();
		h1.setElement("T4");
		h1.setLength(0.3);

		Hypothesis h2 = new Hypothesis();
		h2.setElement("T4");
		h2.setLength(0.1);

		Hypothesis h3 = new Hypothesis();
		h3.setElement("T3");

		Constraint c0 = new Constraint(f0, h3);
		Constraint c1 = new Constraint(solids[0], h1);
		Constraint c2 = new Constraint(solids[1], h2);

		BSubMesh submesh0 = model.newMesh();
		submesh0.add(c1);
		submesh0.add(c0);
		BSubMesh submesh1 = model.newMesh();
		submesh1.add(c2);
		submesh1.add(c0);

		model.printAllHypothesis();
		model.compute();
		model.printConstraints();
		model.printConstraints(submesh0);
	}
}
