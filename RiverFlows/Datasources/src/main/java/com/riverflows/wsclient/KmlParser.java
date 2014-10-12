/**
 * 
 */
package com.riverflows.wsclient;

import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

/**
 * Lightweight SAX parser for Keyhole Markup Language files
 * @author robin
 *
 */
abstract class KmlParser implements ContentHandler {
	private static final Log LOG = LogFactory.getLog(KmlParser.class);
	
	public class Placemark {
		String name;
		String description;
		double latitude;
		double longitude;
		Integer altitude;
	}
	
	
	public static final String EN_PLACEMARK = "Placemark";
	public static final String EN_PLACE_NAME = "name";
	public static final String EN_PLACE_DESCRIPTION = "description";
	public static final String EN_POINT_COORDINATES = "coordinates";
	
	private Placemark currentPlacemark;
	private String currentAncestorElement;
	private String currentElement;
	private String curStr;
	
	@Override
	public void startDocument() throws SAXException {
	}
	 
	 @Override
	public void startElement(String uri, String localName, String qName,
			Attributes atts) throws SAXException {
		if(qName.equals(EN_PLACEMARK)) {
			currentPlacemark = new Placemark();
			currentElement = qName;
			currentAncestorElement = qName;
			return;
		}
		
		if(currentAncestorElement == null) {
			return;
		}
		
		if(!currentAncestorElement.equals(EN_PLACEMARK)) {
			//not in a placemark element- return
			return;
		}
		
		if(qName.equals(EN_PLACE_NAME)
			|| qName.equals(EN_PLACE_DESCRIPTION)
			|| qName.equals(EN_POINT_COORDINATES)) {
			currentElement = qName;
		}
	}
	
	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		if(currentElement == null) {
			return;
		}
		
		if(currentElement.equals(EN_PLACE_DESCRIPTION)
				|| currentElement.equals(EN_PLACE_NAME)
				|| currentElement.equals(EN_POINT_COORDINATES)) {
			
			if(curStr == null) {
				curStr = new String(ch,start,length);
			} else {
				curStr = curStr + new String(ch,start,length);
			}
		}
	}
	
	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		if(currentAncestorElement == null) {
			return;
		}
		
		if(!currentAncestorElement.equals(EN_PLACEMARK)) {
			//not in a placemark element- return
			return;
		}
		
		if(qName.equals(EN_PLACEMARK)) {
			try {
				handlePlacemark(currentPlacemark);
			} catch(Exception e) {
				LOG.error("unhandled exception in handlePlacemark()",e);
			}
			currentPlacemark = null;
			currentElement = null;
			currentAncestorElement = null;
			return;
		}

		if(qName.equals(EN_PLACE_NAME)) {
			currentPlacemark.name = curStr;
		} else if(qName.equals(EN_PLACE_DESCRIPTION)) {
			currentPlacemark.description = curStr;
		} else if(qName.equals(EN_POINT_COORDINATES)) {
			if(curStr != null) {
				StringTokenizer tokens = new StringTokenizer(curStr, ",");
				try {
					String longStr = tokens.nextToken().trim();
					String latStr = tokens.nextToken().trim();
					String altStr = null;
					
					if(tokens.hasMoreTokens()) {
						altStr = tokens.nextToken().trim();
					}
					
					currentPlacemark.longitude = Double.parseDouble(longStr);
					currentPlacemark.latitude = Double.parseDouble(latStr);
					if(altStr != null) {
						currentPlacemark.altitude = Integer.parseInt(altStr);
					}
				} catch(NoSuchElementException nse) {
					LOG.error("invalid coordinate string: " + curStr, nse);
				} catch(NumberFormatException nfe) {
					LOG.error("invalid coordinate string: " + curStr, nfe);
				}
			} else {
				LOG.warn("missing coordinates");
			}
		}
		
		curStr = null;
		currentElement = null;
	}
	
	@Override
	public void endDocument() throws SAXException {
	}
	
	@Override
	public void startPrefixMapping(String prefix, String uri)
			throws SAXException {
	}
	
	@Override
	public void endPrefixMapping(String prefix) throws SAXException {
	}
	
	@Override
	public void ignorableWhitespace(char[] ch, int start, int length)
			throws SAXException {
	}
	
	@Override
	public void processingInstruction(String target, String data)
			throws SAXException {
	}
	
	@Override
	public void setDocumentLocator(Locator locator) {
	}
	
	@Override
	public void skippedEntity(String name) throws SAXException {
	}
	
	public abstract void handlePlacemark(Placemark placemark);
 }