/*
 *        JacORB - a free Java ORB
 *
 *   Copyright (C) 1999-2012 Gerald Brose / The JacORB Team.
 *
 *   This library is free software; you can redistribute it and/or
 *   modify it under the terms of the GNU Library General Public
 *   License as published by the Free Software Foundation; either
 *   version 2 of the License, or (at your option) any later version.
 *
 *   This library is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *   Library General Public License for more details.
 *
 *   You should have received a copy of the GNU Library General Public
 *   License along with this library; if not, write to the Free
 *   Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 */
package org.jacorb.ir.gui.typesystem;


/**
 * This class was generated by a SmartGuide.
 *
 */

import java.lang.reflect.Method;
import javax.swing.tree.DefaultMutableTreeNode;

public abstract class TypeSystemNode
    extends ModelParticipant
{
    protected DefaultMutableTreeNode treeNode;
    protected String name = "";
    protected String absoluteName = "";

    /**
     */
    public TypeSystemNode ( ) {
    }

    protected TypeSystemNode ( DefaultMutableTreeNode treeNode) {
        this();
        this.treeNode = treeNode;
    }

    /**
     * @param name java.lang.String
     */

    public TypeSystemNode ( String name) {
    this();
    this.name = name;
    }

    /**
     * Returns an array of strings: the node types that can be
     * added to this node. (IRModule, for example, returns
     * "module", "interface", etc.)
     * @return java.util.Enumeration
     */
    public String[] allowedToAdd ( )
    {
        return null;
    }

    /**
     * @return int
     * @param other org.jacorb.ir.gui.typesystem.ModelParticipant
     */
    public int compareTo(ModelParticipant other) {
    return this.toString().compareTo(other.toString());
    }

    /**
     * @return java.lang.String
     */

    public  String description() {
    return  getInstanceNodeTypeName() + " " + getAbsoluteName();
    }

    /**
     * @return java.lang.String
     */
    public String getAbsoluteName() {
    return absoluteName;
    }

    /**
     * @return java.lang.String
     */

    public String getInstanceNodeTypeName ( )
    {
	// For the textual representation of a node, nodeTypeName can depend
        // on the state, e.g. for IRAttribute, where it may need to be
        // "readonly attribute" instead of "attribute".  Simulate dynamic
        // lookup of a static method:
    Method nodeTypeNameMethod;
    String nodeTypeName = "";
    try {
            nodeTypeNameMethod = getClass().getMethod("nodeTypeName", new Class[0]);
            nodeTypeName = (String) nodeTypeNameMethod.invoke(null, new Object[0]) ;
    }
    catch (Exception e) {
            e.printStackTrace();
    }
    return nodeTypeName;
    }

    /**
     * @return java.lang.String
     */

    public String getName ( )
    {
        return name;
    }

    /**
     * Adds a new child to a node. Only called by TypeSystem.insertChild(...). Here 
     * it doesn't do anything except throw an exception, if newChild is not permitted,
     * hence it must be overridden by subclasses, e.g. to call the corresponding method
     * on IR. Throws IllegalChildException for illegal child type. 
     * @param newChild TypeSystemNode
     */
    protected void insertChild ( TypeSystemNode newChild)
        throws IllegalChildException
    {
    String[] allowedTypes = allowedToAdd();
    int i;
    for (i = 0;
             i <allowedTypes.length && !allowedTypes[i].equals(TypeSystemNode.nodeTypeName());
             i++);

    if (!allowedTypes[i].equals(TypeSystemNode.nodeTypeName())) {
            throw new IllegalChildException();
    }
    // The actual insertion of DefaultMutableTreeNode is done by TypeSystem, which
    // also deals with corresponding events from the TreeModel. Subclasses
    // need to override this method (including a call to super.addChild(), and
    // call, for example, the corresponding method on the InterfaceRepository.
    }

    /**
     * Returns the name of the type of the Node, e.g. the IDL identifier "Module"
     * @return java.lang.String
     */

    public static String nodeTypeName ( ) {
    // static methods cannot be abstract
    return null;
    }

    /**
     */
    protected void setAbsoluteName(String absoluteName ) {
    this.absoluteName = absoluteName;
    }

    /**
     * called by subclasses of TypeSystemNode, therefore protected
     * @param name java.lang.String
     */

    protected void setName( String name) {
    this.name = name;
    }

    /**
     * Supposed to return an IDL-similar, complete textual representation,
     * but only of the node itself.
     * @return java.lang.String
     */
    public String toString ( ) {
    return getInstanceNodeTypeName() + " " + getName();
    }
}
