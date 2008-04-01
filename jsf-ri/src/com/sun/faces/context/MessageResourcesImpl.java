/*
 * $Id: MessageResourcesImpl.java,v 1.2 2002/07/24 19:15:33 jvisvanathan Exp $
 */

/*
 * Copyright 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.sun.faces.context;

import com.sun.faces.util.Util;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;

import java.util.Locale;
import java.util.HashMap;
import java.util.Iterator;
import javax.faces.FacesException;
import javax.faces.context.MessageImpl;
import javax.faces.context.Message;
import javax.faces.context.MessageResources;
import javax.faces.context.FacesContext;
import javax.faces.FacesException;
import java.text.MessageFormat;
import java.io.IOException;
import com.sun.faces.RIConstants;

import org.apache.commons.digester.Digester;
import org.apache.commons.digester.Rule;
import org.apache.commons.logging.impl.SimpleLog;

import org.mozilla.util.Assert;
import org.mozilla.util.ParameterCheck;

public class MessageResourcesImpl extends MessageResources
{
    //
    // Protected Constants
    //

    //
    // Class Variables
    //

    //
    // Instance Variables
    //
    private String messageResourceId = null;
    private Digester digester = null;
    
    // The key is a String and formed from resource + language + country + varient.  
    // The value is a MessageCatalog.
    private HashMap catalogList = null;
     
    // Attribute Instance Variables

    // Relationship Instance Variables

    //
    // Constructors and Initializers    
    //

    public MessageResourcesImpl(String messageResourceId) {
        super();
        this.messageResourceId = messageResourceId;
        digester = initConfig();
        catalogList = new HashMap();
    }

    //
    // Class methods
    //

    //
    // General Methods
    //
    
    private MessageCatalog findCatalog(Locale locale) {
       
        MessageCatalog cat = null;
 
        ParameterCheck.nonNull(locale);
        String[] name = new String[4];
        int i = 3;
        StringBuffer b = new StringBuffer(100);
        String resource = RIConstants.JSF_RESOURCE_FILENAME;
        b.append(resource);
        name[i--] = resource;

        if ( locale.getLanguage().length() > 0 ) {
            b.append('_');
            b.append(locale.getLanguage());
            name[i--] = b.toString();
        }    
      
        if( locale.getCountry().length() > 0 ) {
            b.append('_');
            b.append(locale.getCountry());
            name[i--] = b.toString();
        }    

        if (locale.getVariant().length() > 0) {
            b.append('_');
            b.append(locale.getVariant());
            name[i--] = b.toString();
        }

        for (int j = i+1; j < name.length; j++) {
            // if catalog does not exist it needs to be loaded from XML file.
            // start with variant appended to resource file name and iterate
            // until a catalog is found to match locale.
            synchronized( catalogList ) {
                cat = (MessageCatalog)catalogList.get(name[j]);
                if (cat == null) {
                    cat = loadMessages(name[j]+".xml", locale);
                    if ( cat != null ) {
                        catalogList.put(name[j], cat);
                        break;
                    }    
                } else {
                    // catalog is already loaded. 
                    break;
                }
            }    
        }    
        return cat;
    }
        
    private Digester initConfig() {
        Digester digester = new Digester();
        digester.setNamespaceAware(true);
        digester.setValidating(false);
        
        // PENDING (visvan) log level should be configurable.
        SimpleLog sLog = new SimpleLog("MessageLog");
        sLog.setLevel(SimpleLog.LOG_LEVEL_ERROR);
        digester.setLogger(sLog);
        
        digester.addObjectCreate("messages/message", 
                "com.sun.faces.context.MessageTemplate");
        digester.addSetProperties("messages/message");
        digester.addCallMethod("messages/message/detail", "setDetail", 0);
        digester.addSetNext("messages/message", "addMessage", 
                 "com.sun.faces.context.MessageTemplate");
        return digester;
    }
    
    public MessageCatalog loadMessages(String fileName, Locale locale) {
        InputStream in;
        MessageCatalog catalog = null;
        
        ParameterCheck.nonNull( fileName );
        ParameterCheck.nonNull(locale);
        
        in = this.getClass().getClassLoader().getResourceAsStream(
                fileName);
        if ( in == null ) {
            // PENDING (visvan) log error
            return null;
        }    
        
        try {
            catalog = new MessageCatalog(locale);
            digester.push( catalog );
            digester.parse(in);
            in.close();
        } catch (Throwable t) {
            return null;
           // throw new IllegalStateException(
           //         "Unable to parse file:"+t.getMessage());
        }
        return catalog;
    } 
    
     public String substituteParams(Locale locale, String msgtext, Object params[]) {
        String localizedStr = null;
        
        if (params == null || msgtext == null ) {
            return msgtext;
        }    
        StringBuffer b = new StringBuffer(100);
        MessageFormat mf = new MessageFormat(msgtext);
        if (locale != null) {
            mf.setLocale(locale);
            b.append(mf.format(params));
            localizedStr = b.toString();
        }    
        return localizedStr;
    }


    //
    // Methods from MessageResources
    // 
    public Message getMessage(FacesContext context, String messageId) {
        return getMessage(context, messageId, null);
    }    

    public Message getMessage(FacesContext context, String messageId,
                                       Object params[]) {
        if (context == null || messageId == null ) {
            throw new NullPointerException("One or more parameters could be null");
        }
        
        Locale locale = context.getLocale();
        Assert.assert_it(locale != null);
        
        MessageCatalog catalog = findCatalog(locale);
        if (catalog == null) {
            // PENDING (visvan) log error
            throw new FacesException("No message catalogs for resource " + 
                   messageResourceId);
        }
        MessageTemplate template = (MessageTemplate) catalog.get(messageId);
        if (template == null) {
            throw new FacesException("The message id '" +
                  messageId + "' was not found in the message catalog");
        }
        
        // substitute parameters
        String summary = substituteParams(locale, template.getSummary(), params);
        String detail = substituteParams(locale, template.getDetail(),params);
        return (new MessageImpl(template.getSeverity(), summary, detail));
    }  
    
    public Message getMessage(FacesContext context, String messageId,
                                       Object param0) {
        return getMessage(context, messageId, new Object[]{param0});                                       
    }                                       
    
    public Message getMessage(FacesContext context, String messageId,
                                       Object param0, Object param1) {
         return getMessage(context, messageId, new Object[]{param0, param1});                                        
    }                                       

    public Message getMessage(FacesContext context, String messageId,
                                       Object param0, Object param1,
                                       Object param2) {
         return getMessage(context, messageId, 
             new Object[]{param0, param1, param2});                                        
    }                                       

    public Message getMessage(FacesContext context, String messageId,
                                       Object param0, Object param1,
                                       Object param2, Object param3) {
         return getMessage(context, messageId, 
                 new Object[]{param0, param1, param2, param3});                                        
    }                                       

    // The testcase for this class is TestclassName.java 

} // end of class MessageResourcesImpl
