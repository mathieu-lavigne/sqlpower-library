/*
 * Copyright (c) 2009, SQL Power Group Inc.
 *
 * This file is part of SQL Power Library.
 *
 * SQL Power Library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * SQL Power Library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */

package ca.sqlpower.swingui;

import org.apache.log4j.Logger;


public class StubSwingWorkerRegistry implements SwingWorkerRegistry {

	private static final Logger logger = Logger
			.getLogger(StubSwingWorkerRegistry.class);
	
	public void registerSwingWorker(SPSwingWorker worker) {
		logger.debug("Registering Swing Worker: " + worker);
	}

	public void removeSwingWorker(SPSwingWorker worker) {
		logger.debug("Removing Swing Worker: " + worker);
	}

}
