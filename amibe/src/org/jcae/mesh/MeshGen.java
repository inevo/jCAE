/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finit element mesher, Plugin architecture.

    Copyright (C) 2003 Jerome Robert <jeromerobert@users.sourceforge.net>

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

/**
* \package org.jcae.mesh Surface mesh generator. 
* \page page1 Surface mesh generator. 
* 
* \section Introduction
* This application allows to mesh a geometric shape provided from CAO.
* Here, the geometric object is provided by OpenCascade.
* This mesh generator is a surfacic one.
* \n
* \section general Data structure 
* \n
* To realise a surface mesh generator, three main data type are needed:
* - Geometric data that describe the geometric shape. These data are provided by CAO environment.
* - Meshing specifications that describe geometric constraints to apply to the mesh and tesselation length information.
* - the mesh, that includes all the elements forming the discretisation.
* \n Links between these three classes describe the necessary data to build the mesh.
* \n
* \section meshing Data structure dedicated to the mesh definition
* \n
* In order to build a mesh, the different kinds of geometry which could be handled (vertices, lines, faces and volumes),
* require to manage four basic entities :
* - nodes,
* - edges,
* - faces,
* - volumes.
* \n Each of these entities are used to build specific meshes like Vertexfield meshes for the nodes, Line meshes for the edges
* and so on . 
* \n These entities are also used to define the different levels of meshes entities definition: an edge is defined by a couple
* of nodes, a face is composed of n edges (depending of its shape) and a volume could de defined from n faces.
*/

package org.jcae.mesh;

import java.io.File;
import javax.media.j3d.BranchGroup;
import org.apache.log4j.Logger;
import org.jcae.mesh.algos.ComputeEdgesConnectivity;
import org.jcae.mesh.mesher.algos1d.UniformLength;
import org.jcae.mesh.mesher.algos2d.*;
import org.jcae.mesh.mesher.algos3d.Fuse;
import org.jcae.mesh.mesher.ds.*;
import org.jcae.mesh.mesher.metrics.Metric2D;
import org.jcae.mesh.mesher.metrics.Metric3D;
import org.jcae.mesh.mesher.InitialTriangulationException;
import org.jcae.mesh.xmldata.*;
import org.jcae.mesh.cad.*;
import org.jcae.opencascade.jni.*;
import org.jcae.view3d.Viewer;
import org.jcae.view3d.XMLBranchGroup;

/**
 * This class MeshGen allows to load a file, construct the mesh structure and read mesh hypothesis.
 * Then starts meshing operation.
 * This class allows to set all explicit constraints desired by the user, and to set all implicit constraints linked to 
 * mesher requirement.
 * The main idea of mesh generation is to sub-structure the mesh linked to the geometric shape into several sub-meshes 
 * according to specifications and geometry decomposition (see mesh.MeshMesh.initMesh()).
 */
public class MeshGen
{
	private static Logger logger=Logger.getLogger(MeshGen.class);

	/**
	 * Convert a step or an IGES file to a brep file
	 * @param brepname the name of the input file
	 * @return the name of the created brep file
	 */
	public static String convertToBRep(String name)
	{
		TopoDS_Shape shape;
		String outputName;
		if (name.endsWith(".step"))
		{
			STEPControl_Reader aReader = new STEPControl_Reader();
			aReader.readFile(name);
			aReader.nbRootsForTransfer();
			aReader.transferRoots();
			shape = aReader.oneShape();
			outputName=name.substring(0, name.length()-".step".length())+".brep";
		}
		else if (name.endsWith(".igs"))
		{
			IGESControl_Reader aReader = new IGESControl_Reader();
			aReader.readFile(name);
			aReader.clear();
			aReader.transferRoots(false);
			shape = aReader.oneShape();
			outputName=name.substring(0, name.length()-".igs".length())+".brep";
		}
		else
			throw new IllegalArgumentException("Cannot get the type of file from is name");
		BRepTools.write(shape, outputName);	
		return outputName;
	}
		
	/** 
	 * Reads the file, the algorithm type and the constraint value for meshing
	 * @param brepfilename  the filename of the brep file	 
	 * @param discr  the value of the meshing constraint
	 */
	public static void load(String brepfilename, double discr, double tolerance)
	{
		//  Declare all variables here
		MMesh3D mesh3D = new MMesh3D();
		Metric3D.setLength(discr);
		Metric2D.setLength(discr);
		org.jcae.mesh.amibe.metrics.Metric3D.setLength(discr);
		org.jcae.mesh.amibe.metrics.Metric2D.setLength(discr);
		MMesh1D mesh1D;
		//     xmlDir:      absolute path name where XML files are stored
		//     xmlFile:     basename of the main XML file
		//     xmlBrepDir:  path to brep file, relative to xmlDir
		//     brepFile:    basename of the brep file
		String xmlDir = (new File(brepfilename+".jcae")).getAbsolutePath();
		String xmlFile = "jcae1d";
		String xmlBrepDir = "..";
		String brepFile = (new File(brepfilename)).getName();
		int iFace = 0;
		logger.info("Loading " + brepfilename);
		boolean testWrite2D = true;

		CADShapeBuilder factory = CADShapeBuilder.factory;
		CADShape shape = factory.newShape(brepfilename);
		CADExplorer expF = factory.newExplorer();
		
if (true) {
		//  Step 1: Compute 1D mesh
		mesh1D = new MMesh1D(shape);
		mesh1D.setMaxLength(discr);
		new UniformLength(mesh1D).compute();
		//  Store the 1D mesh onto disk
		MMesh1DWriter.writeObject(mesh1D, xmlDir, xmlFile, xmlBrepDir, brepFile);
		
		//  Step 2: Read the 1D mesh and compute 2D meshes
		mesh1D = MMesh1DReader.readObject(xmlDir, xmlFile);
		shape = mesh1D.getGeometry();
		mesh1D.setMaxLength(discr);

		//  Prepare 2D discretization
		mesh1D.duplicateEdges();
		//  Compute node labels shared by all 2D and 3D meshes
		mesh1D.updateNodeLabels();
		
		int nTryMax = 20;
		for (expF.init(shape, CADExplorer.FACE); expF.more(); expF.next())
		{
			CADFace F = (CADFace) expF.current();
			iFace++;
			SubMesh2D submesh = new SubMesh2D(F);
			int nTry = 0;
			while (nTry < nTryMax)
			{
				try
				{
					submesh.pushCompGeom(2);
					submesh.init(mesh1D);
				
					//  Those calls to InnerRefine are only needed for spheres:
					//new InnerRefine(submesh).compute();
					//new InnerRefine(submesh).compute();
					submesh.popCompGeom(2);
					
					submesh.pushCompGeom(3);
					new TargetSize(submesh).compute();
					new SmoothNodes(submesh, 20).compute();
					if (testWrite2D)
					{
						xmlFile = "jcae2d."+iFace;
						SubMesh2DWriter.writeObject(submesh, xmlDir, xmlFile, xmlBrepDir, brepFile, iFace);
					}
					else
					{
						mesh3D.addSubMesh2D(submesh, false);
						mesh3D.printInfos();
					}
					submesh.popCompGeom(3);
				}
				catch(Exception ex)
				{
					if (ex instanceof InitialTriangulationException)
					{
						logger.warn("Face "+iFace+" cannot be triangulated, trying again with a larger tolerance...");
						submesh = new SubMesh2D(F);
						submesh.scaleTolerance(10.);
						nTry++;
						continue;
					}
					logger.warn(ex.getMessage());
					ex.printStackTrace();
				}
				break;
			}
			if (nTry == nTryMax)
				logger.error("Face "+iFace+" cannot be triangulated, skipping...");
		}
}
		
		// Step 3: Read 2D meshes and compute 3D mesh
		if (testWrite2D)
		{
			try
			{
				iFace = 0;
				for (expF.init(shape, CADExplorer.FACE); expF.more(); expF.next())
				{
					CADFace F = (CADFace) expF.current();
					iFace++;
					xmlFile = "jcae2d."+iFace;
					SubMesh2D submesh = SubMesh2DReader.readObject(xmlDir, xmlFile, F);
					if (null != submesh)
					{
						logger.debug("Loading face "+iFace);
						submesh.pushCompGeom(3);
						mesh3D.addSubMesh2D(submesh, true);
						submesh.popCompGeom(3);
						mesh3D.printInfos();
					}
				}
			}
			catch(Exception ex)
			{
				logger.warn(ex.getMessage());
				ex.printStackTrace();
			}
		}
		
		xmlFile = "jcae3d";
		MMesh3DWriter.writeObject(mesh3D, xmlDir, xmlFile, xmlBrepDir);
		
		new Fuse(mesh3D, tolerance).compute();
		
		xmlFile = "jcae3d";
		// Step 4: Read 3D mesh
		try
		{
			ComputeEdgesConnectivity computeEdgesConnectivity =
				new ComputeEdgesConnectivity(xmlDir, xmlFile, xmlBrepDir);
			computeEdgesConnectivity.compute();
			
			XMLBranchGroup xbg=new XMLBranchGroup(xmlDir, xmlFile);
			xbg.parseXML();
			Viewer v=new Viewer();
			BranchGroup bg=xbg.getBranchGroup(0);
			logger.info("Bounding sphere of the mesh: "+bg.getBounds());
			v.addBranchGroup(bg);
			bg=xbg.getBranchGroup("FreeEdges");
			v.addBranchGroup(bg);
			bg=xbg.getBranchGroup("MultiEdges");
			v.addBranchGroup(bg);
			v.show();
			v.zoomTo();
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}

	/**
	 * main method, reads 2 arguments and calls mesh.MeshGen.load() method
	 * @param args  an array of String, filename, algorithm type and constraint value
	 * @see #load
	 */
	public static void main(String args[])
	{
		if (args.length<2)
		{
			System.out.println("Usage : MeshGen filename size");
			System.exit(0);
		}
		else
		{
			String filename=args[0];
			if (filename.endsWith(".step") || filename.endsWith(".igs"))
				filename=convertToBRep(filename);
			Double discr=new Double(args[1]);
			Double tolerance;
			if (args.length == 3)
				tolerance=new Double(args[2]);
			else
				tolerance=new Double(0.0);
			load(filename, discr.doubleValue(), tolerance.doubleValue());
		}
	}
}
