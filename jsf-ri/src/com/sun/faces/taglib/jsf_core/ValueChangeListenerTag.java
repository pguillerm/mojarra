/*
 * $Id: ValueChangeListenerTag.java,v 1.25 2006/12/12 19:28:17 rlubke Exp $
 */

/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 * 
 * You can obtain a copy of the License at
 * https://javaserverfaces.dev.java.net/CDDL.html or
 * legal/CDDLv1.0.txt. 
 * See the License for the specific language governing
 * permission and limitations under the License.
 * 
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at legal/CDDLv1.0.txt.    
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * 
 * [Name of File] [ver.__] [Date]
 * 
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.faces.taglib.jsf_core;


import javax.el.ELException;
import javax.el.ValueExpression;
import javax.faces.component.EditableValueHolder;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.event.ValueChangeListener;
import javax.faces.webapp.UIComponentClassicTagBase;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.faces.util.MessageUtils;
import com.sun.faces.util.Util;

/**
 * <p>Tag implementation that creates a {@link ValueChangeListener} instance
 * and registers it on the {@link UIComponent} associated with our most
 * immediate surrounding instance of a tag whose implementation class
 * is a subclass of {@link UIComponentClassicTagBase}.  This tag creates no output to the
 * page currently being created.</p>
 * <p/>
 * <p>This class may be used directly to implement a generic event handler
 * registration tag (based on the fully qualified Java class name specified
 * by the <code>type</code> attribute), or as a base class for tag instances
 * that support specific {@link ValueChangeListener} subclasses.</p>
 * <p/>
 * <p>Subclasses of this class must implement the
 * <code>createValueChangeListener()</code> method, which creates and returns a
 * {@link ValueChangeListener} instance.  Any configuration properties that
 * are required by this {@link ValueChangeListener} instance must have been
 * set by the <code>createValueChangeListener()</code> method.  Generally,
 * this occurs by copying corresponding attribute values on the tag
 * instance.</p>
 * <p/>
 * <p>This tag creates no output to the page currently being created.  It
 * is used solely for the side effect of {@link ValueChangeListener}
 * creation.</p>
 */

public class ValueChangeListenerTag extends TagSupport {

    // ------------------------------------------------------------- Attributes

    private static final long serialVersionUID = -212845116876281363L;
    private static final Logger logger =
         Util.getLogger(Util.FACES_LOGGER + Util.TAGLIB_LOGGER);


    /**
     * <p>The fully qualified class name of the {@link ValueChangeListener}
     * instance to be created.</p>
     */
    private ValueExpression type = null;

    /**
     * <p>The value expression used to create a listener instance and it
     * is also used to wire up this listener to an {@link
     * ValueChangeListener} property of a JavaBean class.</p>
     */
    private ValueExpression binding = null;

    /**
     * <p>Set the fully qualified class name of the
     * {@link ValueChangeListener} instance to be created.
     *
     * @param type The new class name
     */
    public void setType(ValueExpression type) {

        this.type = type;

    }

    /*
     * <p>Set the value binding expression  for this listener.</p>
     *
     * @param binding The new value binding expression
     */
    public void setBinding(ValueExpression binding) {
        this.binding = binding;
    }

    // --------------------------------------------------------- Public Methods


    /**
     * <p>Create a new instance of the specified {@link ValueChangeListener}
     * class, and register it with the {@link UIComponent} instance associated
     * with our most immediately surrounding {@link UIComponentClassicTagBase} instance, if
     * the {@link UIComponent} instance was created by this execution of the
     * containing JSP page.</p>
     *
     * @throws JspException if a JSP error occurs
     */
    public int doStartTag() throws JspException {

        ValueChangeListener handler = null;

        // Locate our parent UIComponentTag
        UIComponentClassicTagBase tag =
             UIComponentClassicTagBase.getParentUIComponentClassicTagBase(pageContext);
        if (tag == null) {
            //  Object[] params = {this.getClass().getName()};
            // PENDING(rogerk): do something with params
            throw new JspException(
                 MessageUtils.getExceptionMessageString(
                      MessageUtils.NOT_NESTED_IN_FACES_TAG_ERROR_MESSAGE_ID));
        }

        // Nothing to do unless this tag created a component
        if (!tag.getCreated()) {
            return (SKIP_BODY);
        }

        UIComponent component = tag.getComponentInstance();
        if (component == null) {
            throw new JspException(
                 MessageUtils.getExceptionMessageString(MessageUtils.NULL_COMPONENT_ERROR_MESSAGE_ID));
        }
        if (!(component instanceof EditableValueHolder)) {
            Object[] params = {"valueChangeListener", "javax.faces.component.EditableValueHolder"};
            throw new JspException(
                 MessageUtils.getExceptionMessageString(
                      MessageUtils.NOT_NESTED_IN_TYPE_TAG_ERROR_MESSAGE_ID, params));
        }

        // If "binding" is set, use it to create a listener instance.
        FacesContext context = FacesContext.getCurrentInstance();
        if (binding != null) {
            try {
                handler = (ValueChangeListener) binding.getValue(context.getELContext());
                if (handler != null) {
                    // we ignore the type in this case, even though
                    // it may have been set.
                    ((EditableValueHolder) component).addValueChangeListener(handler);
                    return (SKIP_BODY);
                }
            } catch (ELException e) {
                throw new JspException(e);
            }
        }
        // If "type" is set, use it to create the listener
        // instance.  If "type" and "binding" are both set, store the 
        // listener instance in the value of the property represented by
        // the value binding expression.    
        if (type != null) {
            handler = createValueChangeListener(context);
            if (handler != null && binding != null) {
                try {
                    binding.setValue(context.getELContext(), handler);
                } catch (ELException e) {
                    throw new JspException(e);
                }
            }
        }

        // We need to cast here because addValueChangeListener
        // method does not apply to all components (it is not a method on
        // UIComponent/UIComponentBase).
        if (handler != null) {
            ((EditableValueHolder) component).addValueChangeListener(handler);
        } else {
            if (logger.isLoggable(Level.FINE)) {
                if (binding == null && type == null) {
                    logger.fine("'handler' was not created because both 'binding' and 'type' were null.");
                } else {
                    logger.fine("'handler' was not created.");
                }
            }
        }


        return (SKIP_BODY);

    }


    /**
     * <p>Release references to any acquired resources.
     */
    public void release() {

        this.type = null;

    }

    // ------------------------------------------------------ Protected Methods


    /**
     * <p>Create and return a new {@link ValueChangeListener} to be registered
     * on our surrounding {@link UIComponent}.</p>
     * @param context the <code>FacesContext</code> for the current request
     * @return a new <code>ValueChangeListener</code> instance.
     * @throws JspException if a new instance cannot be created
     */
    protected ValueChangeListener createValueChangeListener(FacesContext context)
         throws JspException {

        try {
            String className = type.getValue(context.getELContext()).toString();

            Class clazz = Util.loadClass(className, this);
            return ((ValueChangeListener) clazz.newInstance());
        } catch (Exception e) {
            throw new JspException(e);
        }

    }

}
