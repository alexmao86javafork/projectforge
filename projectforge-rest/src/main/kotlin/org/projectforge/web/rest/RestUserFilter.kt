/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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
package org.projectforge.web.rest

import org.projectforge.business.user.filter.CookieService
import org.projectforge.business.user.filter.UserFilter
import org.projectforge.rest.Authentication
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

class RestUserFilter : AbstractRestUserFilter() {
    @Autowired
    lateinit var cookieService: CookieService

    override fun authenticate(authInfo: RestAuthenticationInfo) {
        restAuthenticationUtils.authenticationByRequestParameter(authInfo, RequestParamsDef.USERNAME_AND_PASSWORD) { user, password ->
            userService.authenticateUser(user, password)
        }
        if (authInfo.success) {
            return
        }
        restAuthenticationUtils.basicAuthentication(authInfo) { user, password ->
            userService.authenticateUser(user, password)
        }
        if (authInfo.success) {
            return
        }
        // Try to get the user by session id:
        authInfo.user = UserFilter.getUser(authInfo.request)
        if (authInfo.success) {
            return
        }
        val userContext = cookieService.checkStayLoggedIn(authInfo.request, authInfo.response)
        if (userContext != null) {
            if (log.isDebugEnabled) {
                log.debug("User's stay logged-in cookie found: ${authInfo.request.requestURI}")
            }
            RestAuthenticationUtils.executeLogin(authInfo.request, userContext)
            authInfo.user = userContext.user
        }
        if (authInfo.success) {
            return
        }
        val requestURI = authInfo.request.requestURI
        // Don't log error for userStatus (used by React client for checking weather the user is logged in or not).
        if (requestURI == null || requestURI != "/rs/userStatus") {
            log.error("Neither "
                    + Authentication.AUTHENTICATION_USER_ID
                    + " nor "
                    + Authentication.AUTHENTICATION_USERNAME
                    + "/"
                    + Authentication.AUTHENTICATION_PASSWORD
                    + " is given for rest call: " + requestURI + " . Rest call forbidden.")
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(RestUserFilter::class.java)
    }
}
