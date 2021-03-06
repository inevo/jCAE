/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2004,2005, by EADS CRC
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

package org.jcae.mesh.amibe.patch;

/**
 * This class implements a 'long long' type for exact geometrical
 * computations.  As Java has no unsigned types, this is quite tricky,
 * but fortunately integer coordinates are in the range [0;2^30] and
 * atomic long values are in [-2^60;2^60].  When LongLong have to be
 * added, they must be of opposite sign so that this range is not
 * bypassed.
 */
class LongLong
{
	private long hi, lo;
	// 2^31 - 1
	private static final long pow31m1 = 0x7fffffffL;
	// 2^62 - 1
	private static final long pow62m1 = 0x3fffffffffffffffL;
	
	LongLong(long l1, long l2)
	{
		assert l1 >= 0L : l1;
		assert l1 <= 1L << 61 : l1;
		// l1 = l1H31 * 2^31 + l1L31, 0 <= l1L31 < 2^31
		long l1H31 = l1 >> 31;
		long l1L31 = l1 & pow31m1;
		boolean minus = false;
		assert l2 >= -(1L << 61) : l2;
		assert l2 <= 1L << 61 : l2;
		if (l2 < 0L)
		{
			minus = true;
			l2 = -l2;
		}
		// l2 = l2H31 * 2^31 + l2L31, 0 <= l2L31 < 2^31
		long l2H31 = l2 >> 31;
		long l2L31 = l2 & pow31m1;
		// l1 * l2 = l1H31*l2H31 * 2^62 +
		//           (l1H31*l2L31+l1L31*l2H31) * 2^31 +
		//           l1L31 * l2L31
		//         = hi * 2^62 + lo, 0 <= lo < 2^62
		long res = l1H31 * l2L31 + l1L31 * l2H31;
		hi = (res >> 31) + l1H31 * l2H31;
		lo = ((res & pow31m1) << 31) + l1L31 * l2L31;
		if (lo >= (1L << 62))
		{
			hi++;
			lo &= pow62m1;
		}
		if (minus)
		{
			hi = - hi;
			if (lo > 0L)
			{
				hi--;
				lo = (- lo) & pow62m1;
			}
		}
		assert (lo >= 0L && lo < (1L << 62)) : lo;
	}
	
	protected final void add(LongLong that)
	{
		hi += that.hi;
		lo += that.lo;
		if (lo >= (1L << 62))
		{
			hi++;
			lo &= pow62m1;
		}
	}
	
	public final boolean isNegative()
	{
		return (hi < 0L);
	}
	
	public boolean isPositive()
	{
		return (hi > 0L);
	}
	
	@Override
	public final String toString()
	{
		return "hi: "+hi+" lo: "+lo;
	}
	
}
