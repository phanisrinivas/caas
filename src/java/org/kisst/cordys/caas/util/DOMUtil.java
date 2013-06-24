package org.kisst.cordys.caas.util;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.kisst.cordys.caas.exception.CaasRuntimeException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Util class with helpers to support the usage of JAXB
 * 
 * @author pgussow
 */
public class DOMUtil
{
    /**
     * This method converts the JDom Element to a org.w3c.Element
     * 
     * @param xml The JDOm xml node to convert.
     * @return The converted element.
     */
    public static Element convert(XmlNode xml)
    {
        Element retVal = null;

        if (xml != null)
        {
            String xmlstr = xml.toString();

            try
            {
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                Document doc = dBuilder.parse(new ByteArrayInputStream(xmlstr.getBytes()));
                retVal = doc.getDocumentElement();
            }
            catch (Exception e)
            {
                // Should not happen. Xml is valid XML.
                throw new CaasRuntimeException(e);
            }
        }

        return retVal;
    }

    /**
     * Pretty format.
     * 
     * @param input The input
     * @param indent The indent
     * @return The string
     */
    public static String prettyFormat(Node input, int indent)
    {
        try
        {
            Source xmlInput = new DOMSource(input);
            StringWriter stringWriter = new StringWriter();
            StreamResult xmlOutput = new StreamResult(stringWriter);
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setAttribute("indent-number", indent);
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.transform(xmlInput, xmlOutput);

            return xmlOutput.getWriter().toString();
        }
        catch (Exception e)
        {
            throw new CaasRuntimeException(e);
        }
    }

    /**
     * Pretty format.
     * 
     * @param input The input
     * @return The string
     */
    public static String prettyFormat(Node input)
    {
        return prettyFormat(input, 2);
    }

    /**
     * Write the element to string
     * 
     * @param input The input
     * @param indent The indent
     * @return The string
     */
    public static String dumpXml(Node node)
    {
        try
        {
            Source xmlInput = new DOMSource(node);
            StringWriter stringWriter = new StringWriter();
            StreamResult xmlOutput = new StreamResult(stringWriter);
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.transform(xmlInput, xmlOutput);

            return xmlOutput.getWriter().toString();
        }
        catch (Exception e)
        {
            throw new CaasRuntimeException(e);
        }
    }

    /**
     * Converts the w3c element into a JDOM element.
     * 
     * @param node The node to convert.
     * @return The xml node
     */
    public static XmlNode convert(Element node)
    {
        String tmp = dumpXml(node);

        return new XmlNode(tmp);
    }
}
