//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4-2 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2013.06.21 at 10:05:41 AM CEST 
//


package org.kisst.caas._2_0.template;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for XMLStoreObjectOperation.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="XMLStoreObjectOperation">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="overwrite"/>
 *     &lt;enumeration value="append"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "XMLStoreObjectOperation")
@XmlEnum
public enum XMLStoreObjectOperation {

    @XmlEnumValue("overwrite")
    OVERWRITE("overwrite"),
    @XmlEnumValue("append")
    APPEND("append");
    private final String value;

    XMLStoreObjectOperation(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static XMLStoreObjectOperation fromValue(String v) {
        for (XMLStoreObjectOperation c: XMLStoreObjectOperation.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}