package com.riverflows;

import junit.framework.TestCase;

import com.riverflows.wsclient.Utils;

public class UtilsTest extends TestCase {
	
	//private static final Log LOG = LogFactory.getLog(UtilsTest.class);
	
	public void testAbbreviate() throws Throwable {
		assertEquals("2.48", Utils.abbreviateNumber(2.48, 4));
		assertEquals("-2.48", Utils.abbreviateNumber(-2.48, 4));
		assertEquals("-71.11", Utils.abbreviateNumber(-71.1111111, 4));
		assertEquals("4112", Utils.abbreviateNumber(4112.21, 4));
		assertEquals("-4112", Utils.abbreviateNumber(-4112.21, 4));
		assertEquals("1000", Utils.abbreviateNumber(1000.000, 4));
		assertEquals("-1000", Utils.abbreviateNumber(-1000.000, 4));
		assertEquals("-999.5", Utils.abbreviateNumber(-999.598523, 4));
		assertEquals("999.5", Utils.abbreviateNumber(999.598523, 4));
		assertEquals("0", Utils.abbreviateNumber(0.0, 4));
		assertEquals("3.567E-4", Utils.abbreviateNumber(0.000356772, 4));
		assertEquals("356772", Utils.abbreviateNumber(356772, 4));
		assertEquals("-356772", Utils.abbreviateNumber(-356772, 4));
	}
}
