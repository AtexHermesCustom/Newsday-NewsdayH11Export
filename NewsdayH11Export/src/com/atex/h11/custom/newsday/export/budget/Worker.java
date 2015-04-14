package com.atex.h11.custom.newsday.export.budget;

import com.atex.h11.custom.newsday.export.common.CommonWorker;
import com.atex.h11.custom.newsday.export.common.ExportException;
import com.atex.h11.custom.newsday.export.common.Main;
import com.atex.h11.custom.newsday.export.common.QueueItem;
import com.atex.h11.custom.newsday.export.common.XSLTMessageReceiver;
import com.unisys.media.cr.adapter.ncm.common.data.pk.NCMObjectPK;
import com.unisys.media.cr.adapter.ncm.common.data.values.NCMObjectBuildProperties;
import com.unisys.media.cr.adapter.ncm.model.data.values.NCMObjectValueClient;
import com.unisys.media.ncm.cfg.model.values.UserHermesCfgValueClient;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.text.ParseException;
import java.util.AbstractQueue;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import net.sf.saxon.Controller;
import net.sf.saxon.event.Receiver;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Worker extends CommonWorker {

    private static final String loggerName = Worker.class.getName();
    private static final Logger logger = Logger.getLogger(loggerName);      	
	
    private Templates cachedXSLT = null;
    private URL destObitURL = null;
    private URL destPhotoURL = null;
    
    @Override
    public void init(AbstractQueue<QueueItem> inQ, Properties props)
    		throws RuntimeException {
    	logger.entering(getClass().getName(), "init");

    	try {
    		this.inQ = inQ;
    		this.props = props;    		
    		
    		// Prepare an XPath
    		xpf = XPathFactory.newInstance();
    		xp = xpf.newXPath();    		
    		
	        // Prepare a transfomer.
	        tf = TransformerFactory.newInstance();    		
    		
	        File xsl = loadStylesheetFile(props, "transformFinalStylesheet", true);
	        cachedXSLT = tf.newTemplates(new StreamSource(xsl)); 
	        destObitURL = loadURL(props, "destinationURL");
	        destPhotoURL = loadURL(props, "destinationPhotoURL");
    		
		} catch (Exception e) {
			throw new RuntimeException(e);
	    }    	
		
		logger.exiting(getClass().getName(), "init");
    }
    
    @Override
    public void run()
    		throws RuntimeException {
    	logger.entering(getClass().getName(), "run");

    	try {
    		QueueItem item = dequeue(inQ);
    		Document doc = item.getDocument();
    		
    		if (isReady(doc)) {
    			processDoc(doc);
    		}
    		
		} catch (Exception e) {
			// throw new RuntimeException(e);
			logger.log(Level.SEVERE, "Error encountered", e);
	    }    	
		
		logger.exiting(getClass().getName(), "run");
    }
    
    private boolean isReady(Document doc) {
    	return true;	// no checks for now
    }    
    
    private void processDoc(Document doc)
			throws XPathExpressionException, UnsupportedEncodingException, IOException,
			TransformerConfigurationException, TransformerException, ParseException, ExportException {
    	logger.entering(getClass().getName(), "processDoc");
    	
    	String fileName = getProcessingInstructionData("processing-instruction('file-name')",
    			doc.getDocumentElement(), true);    	
    	logger.info("Exporting: " + fileName);
    	
    	// transform to final 
		DOMSource source = new DOMSource(doc);
		DOMResult result = new DOMResult();		
		
		Transformer t = cachedXSLT.newTransformer();
		// to receive messages from XSLT
		Controller controller = (Controller) t;
		Receiver receiver = new XSLTMessageReceiver(logger);
		controller.setMessageEmitter(receiver);
		// parameters read from properties file
		for (String prop : props.stringPropertyNames()) {	
		    if (prop.startsWith("transform.final.param."))
		        t.setParameter(prop.replaceFirst("transform.final.param.", ""), props.getProperty(prop));
		}
		t.setOutputProperty(OutputKeys.METHOD, "xml");
		t.setOutputProperty(OutputKeys.INDENT, "no");
		t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		t.setOutputProperty(OutputKeys.ENCODING, getEncoding());		
		t.transform(source, result);
		
		write(destObitURL, fileName, (Document) result.getNode());
				
		logger.exiting(getClass().getName(), "processDoc");
    }
    
}
