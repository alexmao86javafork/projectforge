/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.rest.dto

import org.projectforge.framework.persistence.entities.AbstractHistorizableBaseDO
import org.projectforge.framework.persistence.user.entities.TenantDO
import java.lang.reflect.AccessibleObject
import java.lang.reflect.Field
import java.util.*

/**
 * BaseHistorizableDTO is a DTO representation of a AbstractHistorizableBaseDO<Int>. It copies most fields automatically by name and type from
 * DTO to  AbstractHistorizableBaseDO<Int> and vice versa.
 */
open class BaseHistorizableDTO<T : AbstractHistorizableBaseDO<Int>>(var id: Int? = null,
                                                                    var created: Date? = null,
                                                                    var isDeleted: Boolean? = null,
                                                                    var lastUpdate: Date? = null,
                                                                    var tenantId: Int? = null) {
    /**
     * Full and deep copy of the object. Should be extended by inherited classes.
     */
    open fun copyFrom(src: T) {
        id = src.id
        created = src.created
        isDeleted = src.isDeleted
        lastUpdate = src.lastUpdate
        tenantId = src.tenantId
        copy(src, this)
    }

    /**
     * Full and deep copy of the object. Should be extended by inherited classes.
     */
    open fun copyTo(dest: T) {
        dest.id = id
        dest.created = created
        dest.isDeleted = isDeleted == true
        dest.lastUpdate = lastUpdate
        if (tenantId != null) {
            dest.tenant = TenantDO()
            dest.tenant.id = tenantId
        }
        copy(this, dest)
    }

    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(BaseHistorizableDTO::class.java)

        private fun copy(src: Any, dest: Any) {
            val destClazz = dest.javaClass
            val destFields = destClazz.getDeclaredFields()
            AccessibleObject.setAccessible(destFields, true)
            val srcClazz = src.javaClass
            destFields.forEach { destField ->
                val destType = destField.type
                var srcField: Field? = null
                if (destField.name != "log"
                        && destField.name != "serialVersionUID"
                        && destField.name != "Companion"
                        && !destField.name.startsWith("$")) {
                    // Fields log, serialVersionUID, Companion and $* may result in Exceptions and shouldn't be copied in any case.
                    try {
                        srcField = srcClazz.getDeclaredField(destField.name)
                    } catch (ex: Exception) {
                        log.debug("srcField named '${destField.name}' not found in class '$srcClazz'. Can't copy it to destination of type '$destClazz'. Ignoring...")
                    }
                    try {
                        if (srcField != null) {
                            if (srcField.type == destType) {
                                if (Collection::class.java.isAssignableFrom(destType)) {
                                    // Do not copy collections automatically (for now).
                                } else {
                                    srcField.setAccessible(true);
                                    destField.setAccessible(true);
                                    destField.set(dest, srcField.get(src))
                                }
                            } else {
                                if (BaseHistorizableDTO::class.java.isAssignableFrom(destType) && AbstractHistorizableBaseDO::class.java.isAssignableFrom(srcField.type)) {
                                    // Copy AbstractHistorizableBaseDO -> BaseObject
                                    srcField.setAccessible(true);
                                    val srcValue = srcField.get(src)
                                    if (srcValue != null) {
                                        val instance = destType.newInstance()
                                        (instance as BaseHistorizableDTO<*>)._copyFromMinimal(srcValue)
                                        destField.setAccessible(true)
                                        destField.set(dest, instance)
                                    }
                                } else if (AbstractHistorizableBaseDO::class.java.isAssignableFrom(destType) && BaseHistorizableDTO::class.java.isAssignableFrom(srcField.type)) {
                                    // Copy BaseObject -> AbstractHistorizableBaseDO
                                    srcField.setAccessible(true);
                                    val srcValue = srcField.get(src)
                                    if (srcValue != null) {
                                        val instance = destType.newInstance()
                                        (instance as AbstractHistorizableBaseDO<*>).id = (srcValue as BaseHistorizableDTO<*>).id
                                        destField.setAccessible(true)
                                        destField.set(dest, instance)
                                    }

                                } else {
                                    if (srcField.type.isPrimitive) { // boolean, ....
                                        var value: Any? = null
                                        if (srcField.type == kotlin.Boolean::class.java) {
                                            srcField.setAccessible(true);
                                            value = (srcField.get(src) == true)
                                        } else {
                                            log.error("Unsupported field to copy from '$srcClazz.${destField.name}' of type '${srcField.type.name}' to '$destClazz.${destField.name}' of type '${destType.name}'.")
                                        }
                                        if (value != null) {
                                            destField.setAccessible(true);
                                            destField.set(dest, value)
                                        }
                                    } else {
                                        log.debug("Unsupported field to copy from '$srcClazz.${destField.name}' of type '${srcField.type.name}' to '$destClazz.${destField.name}' of type '${destType.name}'.")
                                    }
                                }
                            }
                        } else {
                            // srcField not found. Can't copy.
                            log.debug("srcField named '${destField.name}' not found in class '$srcClazz'. Can't copy it to destination of type '$destClazz'.")
                        }
                    } catch (ex: Exception) {
                        log.error("Error while copiing field '${destField.name}' from $srcClazz to ${dest.javaClass}: ${ex.message}", ex)
                    }
                }
            }
        }
    }

    /**
     * Copy only minimal fields. Id at default, if not overridden. This method is usally used for embedded objects.
     */
    open fun copyFromMinimal(src: T) {
        id = src.id
    }

    private fun _copyFromMinimal(src: Any?) {
        if (src == null) {
            // Nothing to copy
            return
        }
        @Suppress("UNCHECKED_CAST")
        copyFromMinimal(src as T)
    }
}