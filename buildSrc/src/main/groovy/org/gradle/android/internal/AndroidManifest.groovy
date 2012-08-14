package org.gradle.android.internal

import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

class AndroidManifest {
    String packageName
    Integer versionCode
    String versionName
    Element manifestXml

    void load(File sourceFile) {
        def builderFactory = DocumentBuilderFactory.newInstance()
        builderFactory.setNamespaceAware(true)
        manifestXml = builderFactory.newDocumentBuilder().parse(sourceFile).documentElement

        packageName = manifestXml.getAttribute("package")
        versionCode = manifestXml.getAttributeNS("http://schemas.android.com/apk/res/android", "versionCode").toInteger()
        versionName = manifestXml.getAttributeNS("http://schemas.android.com/apk/res/android", "versionName")
    }

    void save(File destFile) {
        manifestXml.setAttribute("package", packageName)
        manifestXml.getAttributeNodeNS("http://schemas.android.com/apk/res/android", "versionCode").setValue(versionCode.toString())
        manifestXml.getAttributeNodeNS("http://schemas.android.com/apk/res/android", "versionName").setValue(versionName)

        TransformerFactory.newInstance().newTransformer().transform(new DOMSource(manifestXml), new StreamResult(destFile))
    }
}

