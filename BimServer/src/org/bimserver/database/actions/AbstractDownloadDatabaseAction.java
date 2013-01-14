package org.bimserver.database.actions;

/******************************************************************************
 * Copyright (C) 2009-2013  BIMserver.org
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *****************************************************************************/

import org.bimserver.GeometryGenerator;
import org.bimserver.database.BimserverDatabaseException;
import org.bimserver.database.DatabaseSession;
import org.bimserver.emf.IfcModelInterface;
import org.bimserver.models.ifc2x3tc1.Bounds;
import org.bimserver.models.ifc2x3tc1.GeometryInstance;
import org.bimserver.models.ifc2x3tc1.IfcProduct;
import org.bimserver.models.log.AccessMethod;
import org.bimserver.models.store.ConcreteRevision;
import org.bimserver.models.store.Project;
import org.bimserver.models.store.Revision;
import org.bimserver.models.store.SerializerPluginConfiguration;
import org.bimserver.plugins.PluginManager;

public abstract class AbstractDownloadDatabaseAction<T> extends BimDatabaseAction<T> {

	public AbstractDownloadDatabaseAction(DatabaseSession databaseSession, AccessMethod accessMethod) {
		super(databaseSession, accessMethod);
	}
	
	protected void checkGeometry(SerializerPluginConfiguration serializerPluginConfiguration, PluginManager pluginManager, IfcModelInterface model, Project project, ConcreteRevision concreteRevision, Revision revision) throws BimserverDatabaseException {
		if (serializerPluginConfiguration.isNeedsGeometry()) {
			if (!revision.isHasGeometry()) {
				setProgress("Generating geometry...", -1);
				new GeometryGenerator().generateGeometry(pluginManager, getDatabaseSession(), model, project.getId(), concreteRevision.getId(), revision, false, null);
			} else {
				for (IfcProduct ifcProduct : model.getAllWithSubTypes(IfcProduct.class)) {
					GeometryInstance geometryInstance = ifcProduct.getGeometryInstance();
					if (geometryInstance != null) {
						geometryInstance.load();
					}
					Bounds bounds = ifcProduct.getBounds();
					if (bounds != null) {
						bounds.load();
						bounds.getMin().load();
						bounds.getMax().load();
					}
				}
			}
		}
	}
	
	protected int findHighestStopRid(Project project, ConcreteRevision subRevision) {
		int highestStopId = Integer.MIN_VALUE;
		for (ConcreteRevision concreteRevision : project.getConcreteRevisions()) {
			// The id must at least be lower or te same as the version we are querying
			if (concreteRevision.getId() <= subRevision.getId()) {
				if (concreteRevision.isClear() && concreteRevision.getId() > highestStopId) {
					highestStopId = concreteRevision.getId();
				}
			}
		}
		return highestStopId;
	}
}