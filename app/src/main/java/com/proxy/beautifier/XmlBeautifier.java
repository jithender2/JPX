package com.proxy.beautifier;

import java.io.StringWriter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.dom.DOMSource;
import java.io.ByteArrayInputStream;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import org.w3c.dom.Node;
import javax.xml.xpath.XPathConstants;
import org.w3c.dom.NodeList;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPath;
import org.xml.sax.InputSource;
import com.proxy.utils.BodyType;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import java.nio.charset.Charset;

/**
 * `XmlBeautifier` is a `Beautifier` implementation that formats XML content
 * into a more readable, indented format.  It uses standard Java XML libraries
 * (DOM and Transformer) for parsing and formatting.
 */
public class XmlBeautifier implements Beautifier {

	/**
	 * Checks if this beautifier accepts the given `BodyType`.
	 *
	 * @param type The `BodyType` to check.
	 * @return `true` if the `BodyType` is `BodyType.xml`, `false` otherwise.
	 */
	@Override
	public boolean accept(BodyType type) {
		return type == BodyType.xml;
	}

	@Override
	public String beautify(String s, Charset charset) {
		s = s.trim();
		String xml;
		try {
			xml = formatXML(s);
		} catch (Exception e) {

			xml = s;
		}
		return xml;
	}

	public String formatXML(String xml) throws Exception {
		// Parse the XML input
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setIgnoringElementContentWhitespace(true);
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document document = builder.parse(new InputSource(new ByteArrayInputStream(xml.getBytes("utf-8"))));

		// Remove empty text nodes
		XPath xPath = XPathFactory.newInstance().newXPath();
		NodeList nodeList = (NodeList) xPath.evaluate("//text()[normalize-space()='']", document,
				XPathConstants.NODESET);

		for (int i = 0; i < nodeList.getLength(); ++i) {
			Node node = nodeList.item(i);
			node.getParentNode().removeChild(node);
		}

		// Normalize the document
		document.normalize();

		// Setup transformer for pretty printing
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4"); // Apache-specific indent

		// Convert to string
		StringWriter stringWriter = new StringWriter();
		transformer.transform(new DOMSource(document), new StreamResult(stringWriter));

		// Ensure a newline after XML declaration
		String result = stringWriter.toString();
		if (result.startsWith("<?xml")) {
			result = result.replaceFirst("\\?>", "?>\n"); // Add a newline after XML declaration
		}

		return result;
	}

	/*
		private String formatXML(String xml) throws Exception {
			Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
					.parse(new InputSource(new ByteArrayInputStream(xml.getBytes("utf-8"))));
	
			// Remove whitespaces outside tags
			XPath xPath = XPathFactory.newInstance().newXPath();
			NodeList nodeList = (NodeList) xPath.evaluate("//text()[normalize-space()='']", document,
					XPathConstants.NODESET);
	
			for (int i = 0; i < nodeList.getLength(); ++i) {
				Node node = nodeList.item(i);
				node.getParentNode().removeChild(node);
			}
	
			// Setup pretty print options
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			transformerFactory.setAttribute("indent-number", 4);
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			if (!xml.startsWith("<?")) {
				transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			}
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
	
			// Return pretty print xml string
			StringWriter stringWriter = new StringWriter();
			transformer.transform(new DOMSource(document), new StreamResult(stringWriter));
			return stringWriter.toString();
		}*/
}