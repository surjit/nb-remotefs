/* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
/*
/* Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
/*
/* The contents of this file are subject to the terms of either the GNU
/* General Public License Version 2 only ("GPL") or the Common
/* Development and Distribution License("CDDL") (collectively, the
/* "License"). You may not use this file except in compliance with the
/* License. You can obtain a copy of the License at
/* http://www.netbeans.org/cddl-gplv2.html
/* or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
/* specific language governing permissions and limitations under the
/* License.  When distributing the software, include this License Header
/* Notice in each file and include the License file at
/* nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
/* particular file as subject to the "Classpath" exception as provided
/* by Sun in the GPL Version 2 section of the License file that
/* accompanied this code. If applicable, add the following below the
/* License Header, with the fields enclosed by brackets [] replaced by
/* your own identifying information:
/* "Portions Copyrighted [year] [name of copyright owner]"
/*
/* Contributor(s):
 *
 * The Original Software is RemoteFS. The Initial Developer of the Original
/* Software is Libor Martinek. Portions created by Libor Martinek are
 * Copyright (C) 2000. All Rights Reserved.
/*
/* If you wish your version of this file to be governed by only the CDDL
/* or only the GPL Version 2, indicate your decision by adding
/* "[Contributor] elects to include this software in this distribution
/* under the [CDDL or GPL Version 2] license." If you do not indicate a
/* single choice of license, a recipient has the option to distribute
/* your version of this file under either the CDDL, the GPL Version 2 or
/* to extend the choice of license to its licensees as provided above.
/* However, if you add GPL Version 2 code and therefore, elected the GPL
/* Version 2 license, then the option applies only if the new code is
/* made subject to such option by the copyright holder.
 *
 * Contributor(s): Libor Martinek.
 */
package org.netbeans.modules.remotefs.ftp.client;

import org.netbeans.modules.remotefs.api.RemoteFileName;

/**
 *
 * @author  lmartinek
 * @version
 */
public class FTPFileName implements RemoteFileName {

    public static final String ROOT_FOLDER = "/";
    private String name;
    private String directory;

    /** Creates new FTPFileName
     * @param directory
     * @param name  */
    protected FTPFileName(String directory, String name) {
        this.name = name;
        this.directory = directory;
    }

    /** Get the name. Only last name is returned, not whole path
     * @return  name of this object */
    public String getName() {
        return name;
    }

    /** Set new name. Used for renaming. Only name is chnaged, path remains.
     * @param newname  new name */
    public void setName(String newname) {
        name = newname;
    }

    /** Get full name (with whole path).
     * @return  full name*/
    public String getFullName() {
        String path = (directory.equals("/") ? "" : directory) + (name.equals("/") ? "" : "/") + name;
        return path;
    }

    /** Get directory of this filename
     * @return directory of this filename */
    public String getDirectory() {
        return directory;
    }

    /** Create new name object under this name object.
     * @param name name of new name object
     * @return created name object */
    public RemoteFileName createNew(String name) {
        return new FTPFileName(getFullName(), name);
    }

    /** Get root
     * @return root */
    public static RemoteFileName getRoot() {
        return new FTPFileName("", "/");
    }

    @Override
    public String toString() {
        return "[ DIR:" + getDirectory() + " FILE: " + getName() + " ]";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof FTPFileName)) {
            return false;
        }
        FTPFileName iObj = (FTPFileName) obj;
        return name == null ? iObj.name == null : name.equals(iObj.name) &&
                directory == null ? iObj.directory == null : directory.equals(iObj.directory);
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 53 * hash + (this.name != null ? this.name.hashCode() : 0);
        hash = 53 * hash + (this.directory != null ? this.directory.hashCode() : 0);
        return hash;
    }
}
