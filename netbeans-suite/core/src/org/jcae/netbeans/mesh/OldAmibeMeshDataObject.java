/*
 * Project Info:  http://jcae.sourceforge.net
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 *
 * (C) Copyright 2005, by EADS CRC
 */

package org.jcae.netbeans.mesh;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.IOException;
import java.io.InputStream;
import org.jcae.mesh.bora.ds.BModel;
import org.jcae.mesh.bora.xmldata.BModelReader;
import org.jcae.netbeans.Utilities;
import org.openide.ErrorManager;
import org.openide.cookies.SaveCookie;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObjectExistsException;
import org.openide.loaders.MultiDataObject;
import org.openide.loaders.MultiFileLoader;
import org.openide.nodes.Node;

/**
 *
 * @author botrel
 * @deprecated Used only to maintain compatibility with old meshes
 */
public class OldAmibeMeshDataObject extends MultiDataObject implements SaveCookie, PropertyChangeListener
{

	public OldAmibeMeshDataObject(FileObject arg0, MultiFileLoader arg1) throws DataObjectExistsException
	{
		super(arg0, arg1);
	}

	protected Node createNodeDelegate()
	{
		return new OldAmibeMeshNode(this);
	}

	private Mesh mesh;

	public Mesh getMesh()
	{
		if(mesh==null)
		{
			mesh=initMesh();
			mesh.addPropertyChangeListener(this);
		}
		return mesh;
	}

	protected Mesh initMesh()
	{
		InputStream in=null;
		Mesh toReturn;
		try
		{
			in=getPrimaryFile().getInputStream();
			XMLDecoder decoder=new XMLDecoder(in);
			toReturn=(Mesh)decoder.readObject();
		}
		catch (Exception ex)
		{
			ErrorManager.getDefault().log(ex.getMessage());
			String name=Utilities.getFreeName(
				getPrimaryFile().getParent(),
				"amibe",".dir");
			toReturn = new Mesh(name);
		}
		finally
		{
			if(in!=null)
				try
				{
					in.close();
				}
				catch (IOException ex)
				{
					ErrorManager.getDefault().notify(ex);
				}
		}
		return toReturn;
	}

	public void save() throws IOException
	{
		FileLock l = null;
		XMLEncoder encoder = null;
		try
		{
			FileObject out = getPrimaryFile();
			l = out.lock();
			encoder = new XMLEncoder(out.getOutputStream(l));
			encoder.writeObject(mesh);
			setModified(false);
		}
		catch(IOException ex)
		{
			ErrorManager.getDefault().notify(ex);
		}
		finally
		{
			if(encoder!=null)
				encoder.close();
			if(l!=null)
				l.releaseLock();
		}
	}

	public void propertyChange(PropertyChangeEvent evt)
	{
		setModified(true);
	}
}
