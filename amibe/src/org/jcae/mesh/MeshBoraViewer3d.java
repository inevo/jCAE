/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2007, by EADS France

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


package org.jcae.mesh;

import org.jcae.mesh.bora.ds.BModel;
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.xmldata.MeshWriter;
import org.jcae.mesh.bora.xmldata.Storage;
import org.jcae.mesh.bora.xmldata.BModelReader;

import java.io.File;
import javax.swing.JFrame;
import javax.swing.WindowConstants;
import org.jcae.viewer3d.fe.amibe.AmibeProvider;
import org.jcae.viewer3d.fe.ViewableFE;
import org.jcae.viewer3d.View;

/**
 * This class MeshBoraView allows to view a mesh generated by bora.
 */
public class MeshBoraViewer3d
{
	public static void main(String args[])
	{
		if (args.length != 2)
		{
			System.out.println("Usage : MeshBoraView boraDir tempDir");
			System.exit(0);
		}
		String boraDir = args[0];
		String tmpDir = args[1];
		BModel model = BModelReader.readObject(boraDir);
		model = BModelReader.readObject(boraDir);
		Mesh m = Storage.readAllFaces(model.getGraph().getRootCell());
		try
		{
			MeshWriter.writeObject3D(m, tmpDir, "jcae3d", ".", "temp.brep");
		}
		catch (java.io.IOException ex)
		{
			ex.printStackTrace();
			System.exit(1);
		}

		JFrame feFrame=new JFrame("jCAE Demo");
		feFrame.setSize(800,600);
		feFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

		View bgView=new View(feFrame);
		try
		{
			AmibeProvider ap = new AmibeProvider(new File(tmpDir));
			bgView.add(new ViewableFE(ap));
			bgView.fitAll();
			feFrame.getContentPane().add(bgView);
			feFrame.setVisible(true);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}
}
