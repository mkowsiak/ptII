/* A CompositeEntity is a cluster in a clustered graph.

 Copyright (c) 1997-2000 The Regents of the University of California.
 All rights reserved.
 Permission is hereby granted, without written agreement and without
 license or royalty fees, to use, copy, modify, and distribute this
 software and its documentation for any purpose, provided that the above
 copyright notice and the following two paragraphs appear in all copies
 of this software.

 IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY
 FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES
 ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF
 THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF
 SUCH DAMAGE.

 THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
 PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY OF
 CALIFORNIA HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES,
 ENHANCEMENTS, OR MODIFICATIONS.

                                        PT_COPYRIGHT_VERSION_2
                                        COPYRIGHTENDKEY

@ProposedRating Green (eal@eecs.berkeley.edu)
@AcceptedRating Yellow (neuendor@eecs.berkeley.edu)
Review changeRequest / changeListener code.
Also need to review exportMoML's export of indexed links.
*/

package ptolemy.kernel;

import ptolemy.kernel.event.*;
import ptolemy.kernel.util.*;

import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

//////////////////////////////////////////////////////////////////////////
//// CompositeEntity
/**
A CompositeEntity is a cluster in a clustered graph.
I.e., it is a non-atomic entity, in that
it can contain other entities and relations.  It supports transparent ports,
where, in effect, the port of a contained entity is represented by a port
of the container entity. Methods that "deeply" traverse the topology
see right through transparent ports.
It may be opaque, in which case its ports are opaque and methods
that "deeply" traverse the topology do not see through them.
For instance, deepEntityList() returns the opaque entities
directly or indirectly contained by this entity.
<p>
To add an entity or relation to this composite, call its setContainer() method
with this composite as an argument.  To remove it, call its setContainer()
method with a null argument (or another container). The entity must be
an instance of ComponentEntity and the relation of ComponentRelation or
an exception is thrown.  Derived classes may further constrain these
to subclasses.  To do that, they should override the protected methods
_addEntity() and _addRelation() and the public member newRelation().
<p>
A CompositeEntity may be contained by another CompositeEntity.
To set that up, call the setContainer() method of the inside entity.
Derived classes may further constrain the container to be
a subclass of CompositeEntity.  To do this, they should override
setContainer() to throw an exception.  Recursive containment
structures, where an entity directly or indirectly contains itself,
are disallowed, and an exception is thrown on an attempt to set up
such a structure.
<p>
A CompositeEntity can contain instances of ComponentPort.  By default
these ports will be transparent, although subclasses of CompositeEntity
can make them opaque by overriding the isOpaque() method to return
<i>true</i>. Derived classes may further constrain the ports to a
subclass of ComponentPort.
To do this, they should override the public method newPort() to create
a port of the appropriate subclass, and the protected method _addPort()
to throw an exception if its argument is a port that is not of the
appropriate subclass.

@author John S. Davis II, Edward A. Lee
@version $Id$
*/
public class CompositeEntity extends ComponentEntity {

    /** Construct an entity in the default workspace with an empty string
     *  as its name. Add the entity to the workspace directory.
     *  Increment the version number of the workspace.
     */
    public CompositeEntity() {
        super();
    }

    /** Construct an entity in the specified workspace with an empty
     *  string as a name. You can then change the name with setName().
     *  If the workspace argument is null, then use the default workspace.
     *  Add the entity to the workspace directory.
     *  Increment the version number of the workspace.
     *  @param workspace The workspace that will list the entity.
     */
    public CompositeEntity(Workspace workspace) {
	super(workspace);
    }

    /** Create an object with a name and a container.
     *  The container argument must not be null, or a
     *  NullPointerException will be thrown.  This entity will use the
     *  workspace of the container for synchronization and version counts.
     *  If the name argument is null, then the name is set to the empty string.
     *  Increment the version of the workspace.
     *  @param container The container entity.
     *  @param name The name of the entity.
     *  @exception IllegalActionException If the container is incompatible
     *   with this entity.
     *  @exception NameDuplicationException If the name coincides with
     *   an entity already in the container.
     */
    public CompositeEntity(CompositeEntity container, String name)
            throws IllegalActionException, NameDuplicationException {
        super(container, name);
    }

    ///////////////////////////////////////////////////////////////////
    ////                         public methods                    ////

    /** Add a change listener.  In this base class, 
     *  add the listener to the list of change listeners in
     *  this entity.  
     *  Each listener will be notified of the execution of each 
     *  change request that is executed via the requestChange() method
     *  at this level of the hierarchy.  Note that in this base class
     *  implementation, only the toplevel composite entity executes changes,
     *  so it probably doesn't make sense to add change listeners to 
     *  composites that are not at the toplevel.
     *  If the listener is already in the list, do not add it again.
     *  @param listener The listener to add.
     */
    public void addChangeListener(ChangeListener listener) {
	if (_changeListeners == null) {
	    _changeListeners = new LinkedList();
	} else {
	    if (_changeListeners.contains(listener)) {
		return;
	    }
	}
	_changeListeners.add(listener);
    }

    /** Allow or disallow connections that are created using the connect()
     *  method to cross levels of the hierarchy.
     *  The default is that such connections are disallowed.
     *  Generally it is a bad idea to allow level-crossing
     *  connections, since it breaks modularity.  This loss of modularity
     *  means, among other things, that this composite cannot be cloned.
     *  Nonetheless, this capability is provided for the benefit of users
     *  that feel they just must have it, and who are willing to sacrifice
     *  clonability and modularity.
     *  @param boole True to allow level-crossing connections.
     */
    public void allowLevelCrossingConnect(boolean boole) {
        _levelCrossingConnectAllowed = boole;
    }

    /** Clone the object into the specified workspace. The new object is
     *  <i>not</i> added to the directory of that workspace (you must do this
     *  yourself if you want it there).
     *  NOTE: This will not work if there are level-crossing transitions.
     *  The result is an entity with clones of the ports of the original
     *  entity, the contained entities, and the contained relations.
     *  The ports of the returned entity are not connected to anything.
     *  The connections of the relations are duplicated in the new entity,
     *  unless they cross levels, in which case an exception is thrown.
     *  @param ws The workspace for the cloned object.
     *  @exception CloneNotSupportedException If the entity contains
     *   level crossing transitions so that its connections cannot be cloned,
     *   or if one of the attributes cannot be cloned.
     *  @return A new CompositeEntity.
     */
    public Object clone(Workspace ws) throws CloneNotSupportedException {
        CompositeEntity newentity = (CompositeEntity)super.clone(ws);

        newentity._containedEntities = new NamedList(newentity);
        newentity._containedRelations = new NamedList(newentity);

        // Clone the contained relations.
        Iterator relations = relationList().iterator();
        while (relations.hasNext()) {
            ComponentRelation relation =
                (ComponentRelation)relations.next();
            ComponentRelation newrelation =
                (ComponentRelation)relation.clone(ws);
            // Assume that since we are dealing with clones,
            // exceptions won't occur normally.  If they do, throw a
            // CloneNotSupportedException.
            try {
                newrelation.setContainer(newentity);
            } catch (KernelException ex) {
                throw new CloneNotSupportedException(
                        "Failed to clone a CompositeEntity: " +
                        ex.getMessage());
            }
        }

        // Clone the contained entities.
        Iterator entities = entityList().iterator();
        while (entities.hasNext()) {
            ComponentEntity entity =
                (ComponentEntity)entities.next();
            ComponentEntity newsubentity = (ComponentEntity)entity.clone(ws);
            // Assume that since we are dealing with clones,
            // exceptions won't occur normally.  If they do, throw a
            // CloneNotSupportedException.
            try {
                newsubentity.setContainer(newentity);
            } catch (KernelException ex) {
                throw new CloneNotSupportedException(
                        "Failed to clone a CompositeEntity: " +
                        ex.getMessage());
            }

            // Clone the links of the ports of the cloned entities.
            Iterator ports = entity.portList().iterator();
            while (ports.hasNext()) {
                ComponentPort port = (ComponentPort)ports.next();
                Enumeration lrels = port.linkedRelations();
                while (lrels.hasMoreElements()) {
                    ComponentRelation rel =
                        (ComponentRelation)lrels.nextElement();
                    if (rel.getContainer() != this) {
                        throw new CloneNotSupportedException(
                                "Cannot clone a CompositeEntity with level " +
                                "crossing transitions.");
                    }
                    ComponentRelation newrel =
                        newentity.getRelation(rel.getName());
                    Port newport =
                        newsubentity.getPort(port.getName());
                    try {
                        newport.link(newrel);
                    } catch (IllegalActionException ex) {
                        throw new CloneNotSupportedException(
                                "Failed to clone a CompositeEntity: " +
                                ex.getMessage());
                    }
                }
            }
        }

        // Clone the inside links from the ports of this entity.
        Iterator ports = portList().iterator();
        while (ports.hasNext()) {
            ComponentPort port = (ComponentPort)ports.next();
            relations = port.insideRelationList().iterator();
            while (relations.hasNext()) {
                Relation relation = (Relation)relations.next();
                ComponentRelation newrel =
                    newentity.getRelation(relation.getName());
                Port newport =
                    newentity.getPort(port.getName());
                try {
                    newport.link(newrel);
                } catch (IllegalActionException ex) {
                    throw new CloneNotSupportedException(
                            "Failed to clone a CompositeEntity: " +
                            ex.getMessage());
                }
            }
        }

        return newentity;
    }

    /** Create a new relation and use it to connect two ports.
     *  It creates a new relation using newRelation() with an automatically
     *  generated name and uses it to link the specified ports.
     *  The order of the ports determines the order in which the
     *  links to the relation are established, but otherwise has no
     *  importance.
     *  The name is of the form "_R<i>i</i>" where <i>i</i> is an integer.
     *  Level-crossing connections are not permitted unless
     *  allowLevelCrossingConnect() has been called with a <i>true</i>
     *  argument.  Note that is rarely a good idea to permit level crossing
     *  connections, since they break modularity and cloning.
     *  A reference to the newly created relation is returned.
     *  To remove the relation, call its setContainer() method with a null
     *  argument. This method is write-synchronized on the workspace
     *  and increments its version number.
     *  @param port1 The first port to connect.
     *  @param port2 The second port to connect.
     *  @exception IllegalActionException If one of the arguments is null, or
     *   if a disallowed level-crossing connection would result.
     */
    public ComponentRelation connect(ComponentPort port1, ComponentPort port2)
            throws IllegalActionException {
        try {
            return connect(port1, port2, uniqueName("_R"));
        } catch (NameDuplicationException ex) {
            // This exception should not be thrown.
            throw new InternalErrorException(
                    "Internal error in ComponentRelation connect() method!"
                    + ex.getMessage());
        }
    }

    /** Create a new relation with the specified name and use it to
     *  connect two ports. Level-crossing connections are not permitted
     *  unless allowLevelCrossingConnect() has been called with a true
     *  argument.   Note that is rarely a good idea to permit level crossing
     *  connections, since they break modularity and cloning.
     *  A reference to the newly created alias relation is returned.
     *  To remove the relation, call its setContainer() method with a null
     *  argument. This method is write-synchronized on the workspace
     *  and increments its version number.
     *  @param port1 The first port to connect.
     *  @param port2 The second port to connect.
     *  @param relationname The name of the new relation.
     *  @exception IllegalActionException If one of the arguments is null, or
     *   if a disallowed level-crossing connection would result, or if the two
     *   ports are not in the same workspace as this entity.
     *  @exception NameDuplicationException If there is already a relation with
     *   the specified name in this entity.
     */
    public ComponentRelation connect(ComponentPort port1, ComponentPort port2,
            String relationname)
            throws IllegalActionException, NameDuplicationException {
        if (port1 == null || port2 == null) {
            throw new IllegalActionException(this,
                    "Attempt to connect null port.");
        }
        if (port1.workspace() != port2.workspace() ||
                port1.workspace() != _workspace) {
            throw new IllegalActionException(port1, port2,
                    "Cannot connect ports because workspaces are different.");
        }
        try {
            _workspace.getWriteAccess();
            ComponentRelation ar = newRelation(relationname);
            if (_levelCrossingConnectAllowed) {
                port1.liberalLink(ar);
            } else {
                port1.link(ar);
            }
            // Have to catch the exception to restore the original state.
            try {
                if (_levelCrossingConnectAllowed) {
                    port2.liberalLink(ar);
                } else {
                    port2.link(ar);
                }
            } catch (IllegalActionException ex) {
                port1.unlink(ar);
                throw ex;
            }
            return ar;
        } finally {
            _workspace.doneWriting();
        }
    }

    /** List the opaque entities that are directly or indirectly
     *  contained by this entity.  The list will be empty if there
     *  are no such contained entities.
     *  This method is read-synchronized on the workspace.
     *  @return A list of opaque ComponentEntity objects.
     */
    public List deepEntityList() {
        try {
            _workspace.getReadAccess();
            LinkedList result = new LinkedList();

            Iterator entities = _containedEntities.elementList().iterator();

            while (entities.hasNext()) {
                ComponentEntity entity = (ComponentEntity)entities.next();
                if (entity.isOpaque()) {
                    result.add(entity);
                } else {
                    result.addAll(((CompositeEntity)entity).deepEntityList());
                }
            }
            return result;
        } finally {
            _workspace.doneReading();
        }
    }

    /** Enumerate the opaque entities that are directly or indirectly
     *  contained by this entity.  The enumeration will be empty if there
     *  are no such contained entities.
     *  This method is read-synchronized on the workspace.
     *  @deprecated Use deepEntityList() instead.
     *  @return An enumeration of opaque ComponentEntity objects.
     */
    public Enumeration deepGetEntities() {
        return Collections.enumeration(deepEntityList());
    }

    /** List the contained entities in the order they were added
     *  (using their setContainer() method).
     *  The returned list is static in the sense
     *  that it is not affected by any subsequent additions or removals
     *  of entities.
     *  This method is read-synchronized on the workspace.
     *  @return An unmodifiable list of ComponentEntity objects.
     */
    public List entityList() {
        try {
            _workspace.getReadAccess();
            // Copy the list so we can create a static enumeration.
            NamedList entitiesCopy = new NamedList(_containedEntities);
            return entitiesCopy.elementList();
        } finally {
            _workspace.doneReading();
        }
    }

    /** Get the attribute with the given name. The name may be compound,
     *  with fields separated by periods, in which case the attribute
     *  returned is contained by a (deeply) contained attribute, port,
     *  relation, or entity.
     *  If the name contains one or more periods, then it is assumed
     *  to be the relative name of an attribute contained by one of
     *  the contained attributes, ports, entities or relations.
     *  This method is read-synchronized on the workspace.
     *  @param name The name of the desired attribute.
     *  @return The requested attribute if it is found, null otherwise.
     */
    public Attribute getAttribute(String name) {
        try {
            _workspace.getReadAccess();
            // Check attributes and ports first.
            Attribute result = super.getAttribute(name);
            if (result == null) {
                // Check entities first.
                String[] subnames = _splitName(name);
                if (subnames[1] != null) {
                    ComponentEntity entity = getEntity(subnames[0]);
                    if (entity != null) {
                        result = entity.getAttribute(subnames[1]);
                    }
                    if (result == null) {
                        // Check relations.
                        ComponentRelation relation = getRelation(subnames[0]);
                        if (relation != null) {
                            result = relation.getAttribute(subnames[1]);
                        }
                    }
                }
            }
            return result;
        } finally {
            _workspace.doneReading();
        }
    }

    /** Get a contained entity by name. The name may be compound,
     *  with fields separated by periods, in which case the entity
     *  returned is contained by a (deeply) contained entity.
     *  This method is read-synchronized on the workspace.
     *  @param name The name of the desired entity.
     *  @return An entity with the specified name, or null if none exists.
     */
    public ComponentEntity getEntity(String name) {
        try {
            _workspace.getReadAccess();
            String[] subnames = _splitName(name);
            if (subnames[1] == null) {
                return (ComponentEntity)_containedEntities.get(name);
            } else {
                Object match = _containedEntities.get(subnames[0]);
                if (match == null) {
                    return null;
                } else {
                    if (match instanceof CompositeEntity) {
                        return ((CompositeEntity)match).getEntity(subnames[1]);
                    } else {
                        return null;
                    }
                }
            }
        } finally {
            _workspace.doneReading();
        }
    }

    /** Enumerate the contained entities in the order they were added
     *  (using their setContainer() method).
     *  The returned enumeration is static in the sense
     *  that it is not affected by any subsequent additions or removals
     *  of entities.
     *  This method is read-synchronized on the workspace.
     *  @deprecated Use entityList() instead.
     *  @return An enumeration of ComponentEntity objects.
     */
    public Enumeration getEntities() {
        return Collections.enumeration(entityList());
    }

    /** Get a contained port by name. The name may be compound,
     *  with fields separated by periods, in which case the port returned is
     *  contained by a (deeply) contained entity.
     *  This method is read-synchronized on the workspace.
     *  @param name The name of the desired port.
     *  @return A port with the specified name, or null if none exists.
     */
    public Port getPort(String name) {
        try {
            _workspace.getReadAccess();
            String[] subnames = _splitName(name);
            if (subnames[1] == null) {
                return super.getPort(name);
            } else {
                ComponentEntity match =
                    (ComponentEntity)_containedEntities.get(subnames[0]);
                if (match == null) {
                    return null;
                } else {
                    return match.getPort(subnames[1]);
                }
            }
        } finally {
            _workspace.doneReading();
        }
    }

    /** Get a contained relation by name. The name may be compound,
     *  with fields separated by periods, in which case the relation
     *  returned is contained by a (deeply) contained entity.
     *  This method is read-synchronized on the workspace.
     *  @param name The name of the desired relation.
     *  @return A relation with the specified name, or null if none exists.
     */
    public ComponentRelation getRelation(String name) {
        try {
            _workspace.getReadAccess();
            String[] subnames = _splitName(name);
            if (subnames[1] == null) {
                return (ComponentRelation)_containedRelations.get(name);
            } else {
                Object match = _containedEntities.get(subnames[0]);
                if (match == null) {
                    return null;
                } else {
                    if (match instanceof CompositeEntity) {
                        return ((CompositeEntity)match)
                            .getRelation(subnames[1]);
                    } else {
                        return null;
                    }
                }
            }
        } finally {
            _workspace.doneReading();
        }
    }

    /** Enumerate the relations contained by this entity.
     *  The returned enumeration is static in the sense
     *  that it is not affected by any subsequent additions or removals
     *  of relations.
     *  This method is read-synchronized on the workspace.
     *  @deprecated Use relationList() instead.
     *  @return An enumeration of ComponentRelation objects.
     */
    public Enumeration getRelations() {
        return Collections.enumeration(relationList());
    }

    /** Return false since CompositeEntities are not atomic.
     *  Note that this will return false even if there are no contained
     *  entities or relations.  Derived classes may not override this.
     *  To hide the contents of the entity, they should override isOpaque().
     *  @return False.
     */
    public final boolean isAtomic() {
	return false;
    }

    /** Return false.  Derived classes may return true in order to hide
     *  their components behind opaque ports.
     *  @return True if the entity is opaque.
     *  @see ptolemy.kernel.CompositeEntity
     */
    public boolean isOpaque() {
	return false;
    }

    /** Create a new relation with the specified name, add it to the
     *  relation list, and return it. Derived classes can override
     *  this to create domain-specific subclasses of ComponentRelation.
     *  This method is write-synchronized on the workspace and increments
     *  its version number.
     *  @param name The name of the new relation.
     *  @return The new relation.
     *  @exception IllegalActionException If name argument is null.
     *  @exception NameDuplicationException If name collides with a name
     *   already in the container.
     */
    public ComponentRelation newRelation(String name)
            throws IllegalActionException, NameDuplicationException {
        try {
            _workspace.getWriteAccess();
            ComponentRelation rel = new ComponentRelation(this, name);
            return rel;
        } finally {
            _workspace.doneWriting();
        }
    }

    /** Notify all change listeners registered with this composite 
     *  that the given request has completed
     *  without throwing an exception.  The notification is deferred to
     *  mutation itself, which performs the actual notification.
     *  If no change listeners have been added, then do nothing.
     *  This method is public so that subclasses can easily defer the 
     *  actual execution of the change request to another class.  For 
     *  example, an instance of CompositeActor defers to a Manager to 
     *  execute the change request at an appropriate time.
     */
    public void notifyChangeListeners(ChangeRequest request) {
	if (_changeListeners != null) {
	    Iterator listeners = _changeListeners.iterator();
	    while(listeners.hasNext()) {
		ChangeListener listener = (ChangeListener)listeners.next();
		request.notify(listener);
	    }
	}
    }

    /** Return the number of contained entities.
     *  This method is read-synchronized on the workspace.
     *  @return The number of entities.
     */
    public int numEntities() {
        try {
            _workspace.getReadAccess();
            return _containedEntities.size();
        } finally {
            _workspace.doneReading();
        }
    }

    /** Return the number of contained relations.
     *  This method is read-synchronized on the workspace.
     *  @return The number of relations.
     */
    public int numRelations() {
        try {
            _workspace.getReadAccess();
            return _containedRelations.size();
        } finally {
            _workspace.doneReading();
        }
    }

    /** List the relations contained by this entity.
     *  The returned list is static in the sense
     *  that it is not affected by any subsequent additions or removals
     *  of relations.
     *  This method is read-synchronized on the workspace.
     *  @return An unmodifiable list of ComponentRelation objects.
     */
    public List relationList() {
        try {
            _workspace.getReadAccess();
            // Copy the list so we can create a static enumeration.
            NamedList relationsCopy = new NamedList(_containedRelations);
            return relationsCopy.elementList();
        } finally {
            _workspace.doneReading();
        }
    }

    /** Remove all contained entities and unlink them from all relations.
     *  This is done by setting their containers to null.
     *  This method is read-synchronized on the workspace
     *  and increments its version number.
     */
    public void removeAllEntities() {
        try {
            _workspace.getReadAccess();
            Iterator entities = entityList().iterator();

            while (entities.hasNext()) {
                ComponentEntity entity = (ComponentEntity)entities.next();
                try {
                    entity.setContainer(null);
                } catch (KernelException ex) {
                    // This exception should not be thrown.
                    throw new InternalErrorException(
                            "Internal error in ComponentRelation "
                            + "removeAllEntities() method!"
                            + ex.getMessage());
                }
            }
        } finally {
            _workspace.doneReading();
        }
    }

    /** Remove all contained relations and unlink them from everything.
     *  This is done by setting their containers to null.
     *  This method is write-synchronized on the workspace
     *  and increments its version number.
     */
    public void removeAllRelations() {
        try {
            _workspace.getWriteAccess();
            Iterator relations = relationList().iterator();

            while (relations.hasNext()) {
                ComponentRelation relation =
                    (ComponentRelation)relations.next();
                try {
                    relation.setContainer(null);
                } catch (KernelException ex) {
                    // This exception should not be thrown.
                    throw new InternalErrorException(
                            "Internal error in ComponentRelation "
                            + "removeAllRelations() method!"
                            + ex.getMessage());
                }
            }
        } finally {
            _workspace.doneWriting();
        }
    }

    /** Remove a change listener. If the specified listener is not
     *  on the list, do nothing.
     *  @param listener The listener to remove.
     */
    public void removeChangeListener(ChangeListener listener) {
        if (_changeListeners == null) {
            return;
        }
        _changeListeners.remove(listener);
    }

    /** Request that given change request be executed.   In this base 
     *  class defer the change request to the container of this entity. 
     *  If the entity has no container, then execute the request immediately.
     *  In other words, the request will get passed up to the toplevel 
     *  composite entity and then executed.  Subclasses should override
     *  this to queue the change request and execute at an appropriate time.
     *  An exception is thrown by this method if the change 
     *  request fails.
     *  @param change The requested change.
     *  @exception ChangeFailedException If the model is idle and the
     *  change request fails.
     */
    public void requestChange(ChangeRequest change) 
	throws ChangeFailedException {
	// If the model is idle (i.e., initialize() has not yet been
	// invoked), then process the change request right now.
	CompositeEntity container = (CompositeEntity) getContainer();
	if(container == null) {
	    change.execute();
	    notifyChangeListeners(change);
	} else {
	    container.requestChange(change);
	}
    }

    /** Return a name that is guaranteed to not be the name of
     *  any contained attribute, port, entity, or relation.
     *  @param prefix A prefix for the name.
     *  @return A unique name.
     */
    public String uniqueName(String prefix) {
        String candidate = prefix + _uniqueNameIndex++;
        while(getAttribute(candidate) != null
                || getPort(candidate) != null
                || getEntity(candidate) != null
                || getRelation(candidate) != null) {
            candidate = prefix + _uniqueNameIndex++;
        }
        return candidate;
    }

    ///////////////////////////////////////////////////////////////////
    ////                         protected methods                 ////

    /** Add an entity to this container. This method should not be used
     *  directly.  Call the setContainer() method of the entity instead.
     *  This method does not set
     *  the container of the entity to point to this composite entity.
     *  It assumes that the entity is in the same workspace as this
     *  container, but does not check.  The caller should check.
     *  Derived classes may override this method to constrain the
     *  the entity to a subclass of ComponentEntity.
     *  This method is <i>not</i> synchronized on the workspace, so the
     *  caller should be.
     *  @param entity Entity to contain.
     *  @exception IllegalActionException If the entity has no name, or the
     *   action would result in a recursive containment structure.
     *  @exception NameDuplicationException If the name collides with a name
     *  already in the entity.
     */
    protected void _addEntity(ComponentEntity entity)
            throws IllegalActionException, NameDuplicationException {
        if (entity.deepContains(this)) {
            throw new IllegalActionException(entity, this,
                    "Attempt to construct recursive containment.");
        }
        _containedEntities.append(entity);
    }

    /** Add a relation to this container. This method should not be used
     *  directly.  Call the setContainer() method of the relation instead.
     *  This method does not set
     *  the container of the relation to refer to this container.
     *  This method is <i>not</i> synchronized on the workspace, so the
     *  caller should be.
     *  @param relation Relation to contain.
     *  @exception IllegalActionException If the relation has no name.
     *  @exception NameDuplicationException If the name collides with a name
     *   already on the contained relations list.
     */
    protected void _addRelation(ComponentRelation relation)
            throws IllegalActionException, NameDuplicationException {
        _containedRelations.append(relation);
    }

    /** Return a description of the object.  The level of detail depends
     *  on the argument, which is an or-ing of the static final constants
     *  defined in the NamedObj class.  Lines are indented according to
     *  to the level argument using the protected method _getIndentPrefix().
     *  Zero, one or two brackets can be specified to surround the returned
     *  description.  If one is specified it is the the leading bracket.
     *  This is used by derived classes that will append to the description.
     *  Those derived classes are responsible for the closing bracket.
     *  An argument other than 0, 1, or 2 is taken to be equivalent to 0.
     *  This method is read-synchronized on the workspace.
     *  @param detail The level of detail.
     *  @param indent The amount of indenting.
     *  @param bracket The number of surrounding brackets (0, 1, or 2).
     *  @return A description of the object.
     */
    protected String _description(int detail, int indent, int bracket) {
        try {
            _workspace.getReadAccess();
            String result;
            if (bracket == 1 || bracket == 2) {
                result = super._description(detail, indent, 1);
            } else {
                result = super._description(detail, indent, 0);
            }
            if ((detail & CONTENTS) != 0) {
                if (result.trim().length() > 0) {
                    result += " ";
                }
                result += "entities {\n";
                Iterator entities = entityList().iterator();
                while (entities.hasNext()) {
                    ComponentEntity entity =
                        (ComponentEntity)entities.next();
                    result +=
                        entity._description(detail, indent+1, 2) + "\n";
                }
                result += _getIndentPrefix(indent) + "} relations {\n";
                Iterator relations = relationList().iterator();
                while (relations.hasNext()) {
                    Relation relation = (Relation)relations.next();
                    result += relation._description(detail, indent+1, 2) + "\n";
                }
                result += _getIndentPrefix(indent) + "}";
            }
            if (bracket == 2) result += "}";
            return result;
        } finally {
            _workspace.doneReading();
        }
    }

    /** Write a MoML description of the contents of this object, which
     *  in this class are the attributes, ports, contained relations,
     *  and contained entities, plus all links.  The links are written
     *  in an order that respects the ordering in ports, but not necessarily
     *  the ordering in relations.  This method is called
     *  by exportMoML().  Each description is indented according to the
     *  specified depth and terminated with a newline character.
     *  @param output The output to write to.
     *  @param depth The depth in the hierarchy, to determine indenting.
     *  @exception IOException If an I/O error occurs.
     */
    protected void _exportMoMLContents(Writer output, int depth)
            throws IOException {
        super._exportMoMLContents(output, depth);

        Iterator entities = entityList().iterator();
        while (entities.hasNext()) {
            ComponentEntity entity = (ComponentEntity)entities.next();
            entity.exportMoML(output, depth);
        }
        Iterator relations = relationList().iterator();
        while (relations.hasNext()) {
            ComponentRelation relation
                = (ComponentRelation)relations.next();
            relation.exportMoML(output, depth);
        }
        // Next write the links.  To get the ordering right,
        // we read the links from the ports, not from the relations.

        // First, produce the inside links on contained ports.
        Iterator ports = portList().iterator();
        while (ports.hasNext()) {
            ComponentPort port = (ComponentPort)ports.next();
            relations = port.insideRelationList().iterator();
            // The following variables are used to determine whether to
            // specify the index of the link explicitly, or to leave
            // it implicit.
            int index = -1;
            boolean useIndex = false;
            while (relations.hasNext()) {
                index++;
                ComponentRelation relation
                    = (ComponentRelation)relations.next();
                if (relation == null) {
                    // Gap in the links.  The next link has to use an
                    // explicit index.
                    useIndex = true;
                    continue;
                }
                // In order to support level-crossing links, consider the
                // possibility that the relation is not contained by this.
                String relationName;
                if (relation.getContainer() == this) {
                    relationName = relation.getName();
                } else {
                    relationName = relation.getFullName();
                }
                if (useIndex) {
                    useIndex = false;
                    output.write(_getIndentPrefix(depth)
                            + "<link port=\""
                            + port.getName()
                            + "\" relation=\""
                            + relationName
                            + "\" index=\""
                            + index
                            + "\"/>\n");
                } else {
                    output.write(_getIndentPrefix(depth)
                            + "<link port=\""
                            + port.getName()
                            + "\" relation=\""
                            + relationName
                            + "\"/>\n");
                }
            }
        }

        // Next, produce the links on ports contained by contained entities.
        entities = entityList().iterator();
        while (entities.hasNext()) {
            ComponentEntity entity = (ComponentEntity)entities.next();
            ports = entity.portList().iterator();
            while (ports.hasNext()) {
                ComponentPort port = (ComponentPort)ports.next();
                relations = port.linkedRelationList().iterator();
                // The following variables are used to determine whether to
                // specify the index of the link explicitly, or to leave
                // it implicit.
                int index = -1;
                boolean useIndex = false;
                while (relations.hasNext()) {
                    index++;
                    ComponentRelation relation
                            = (ComponentRelation)relations.next();
                    if (relation == null) {
                        // Gap in the links.  The next link has to use an
                        // explicit index.
                        useIndex = true;
                        continue;
                    }
                    // In order to support level-crossing links, consider the
                    // possibility that the relation is not contained by this.
                    String relationName;
                    if (relation.getContainer() == this) {
                        relationName = relation.getName();
                    } else {
                        relationName = relation.getFullName();
                    }
                    if (useIndex) {
                        useIndex = false;
                        output.write(_getIndentPrefix(depth)
                            + "<link port=\""
                            + entity.getName()
                            + "."
                            + port.getName()
                            + "\" relation=\""
                            + relationName
                            + "\" index=\""
                            + index
                            + "\"/>\n");
                    } else {
                        output.write(_getIndentPrefix(depth)
                            + "<link port=\""
                            + entity.getName()
                            + "."
                            + port.getName()
                            + "\" relation=\""
                            + relationName
                            + "\"/>\n");
                    }
                }
            }
        }
    }

    /** Remove the specified entity. This method should not be used
     *  directly.  Call the setContainer() method of the entity instead with
     *  a null argument.
     *  The entity is assumed to be contained by this composite (otherwise,
     *  nothing happens). This does not alter the entity in any way.
     *  This method is <i>not</i> synchronized on the workspace, so the
     *  caller should be.
     *  @param entity The entity to remove.
     */
    protected void _removeEntity(ComponentEntity entity) {
        _containedEntities.remove(entity);
    }

    /** Remove the specified relation. This method should not be used
     *  directly.  Call the setContainer() method of the relation instead with
     *  a null argument.
     *  The relation is assumed to be contained by this composite (otherwise,
     *  nothing happens). This does not alter the relation in any way.
     *  This method is <i>not</i> synchronized on the workspace, so the
     *  caller should be.
     *  @param relation The relation to remove.
     */
    protected void _removeRelation(ComponentRelation relation) {
        _containedRelations.remove(relation);
    }

    ///////////////////////////////////////////////////////////////////
    ////                         private variables                 ////

    // A list of change listeners.
    private List _changeListeners;

    /** @serial List of contained entities. */
    private NamedList _containedEntities = new NamedList(this);

    /** @serial List of contained ports. */
    private NamedList _containedRelations = new NamedList(this);

    /** @serial Count of automatic names generated for entities */
    private int _entitynamecount = 0;

    /** @serial Count of automatic names generated for entities */
    private int _relationnamecount = 0;

    /** @serial Flag indicating whether level-crossing connect is permitted. */
    private boolean _levelCrossingConnectAllowed = false;
}
