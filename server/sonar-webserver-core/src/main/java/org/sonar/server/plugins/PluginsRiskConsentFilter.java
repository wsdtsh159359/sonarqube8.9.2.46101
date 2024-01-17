/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.plugins;

import com.google.common.collect.ImmutableSet;
import org.sonar.api.config.Configuration;
import org.sonar.api.web.ServletFilter;
import org.sonar.core.extension.PluginRiskConsent;
import org.sonar.server.user.ThreadLocalUserSession;

import static org.sonar.api.web.ServletFilter.UrlPattern.Builder.staticResourcePatterns;
import static org.sonar.core.config.CorePropertyDefinitions.PLUGINS_RISK_CONSENT;
import static org.sonar.core.extension.PluginRiskConsent.NOT_ACCEPTED;
import static org.sonar.core.extension.PluginRiskConsent.REQUIRED;
import static org.sonar.server.authentication.AuthenticationRedirection.redirectTo;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;

public class PluginsRiskConsentFilter extends ServletFilter {

  private static final String PLUGINS_RISK_CONSENT_PATH = "/admin/plugin_risk_consent"; //NOSONAR this path will be the same in every environment

  private static final Set<String> SKIPPED_URLS = ImmutableSet.of(
    PLUGINS_RISK_CONSENT_PATH,
    "/account/reset_password",
    "/admin/change_admin_password",
    "/batch/*", "/api/*");
  private final ThreadLocalUserSession userSession;
  private final Configuration config;

  public PluginsRiskConsentFilter(Configuration config, ThreadLocalUserSession userSession) {
    this.userSession = userSession;
    this.config = config;
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    //nothing to do
  }

  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {
    HttpServletRequest request = (HttpServletRequest) servletRequest;
    HttpServletResponse response = (HttpServletResponse) servletResponse;
    PluginRiskConsent riskConsent = PluginRiskConsent.valueOf(config.get(PLUGINS_RISK_CONSENT).orElse(NOT_ACCEPTED.name()));

    if (userSession.hasSession() && userSession.isLoggedIn()
      && userSession.isSystemAdministrator() && riskConsent == REQUIRED) {
      redirectTo(response, request.getContextPath() + PLUGINS_RISK_CONSENT_PATH);
    }

    chain.doFilter(request, response);
  }

  @Override
  public UrlPattern doGetPattern() {
    return UrlPattern.builder()
      .includes("/*")
      .excludes(staticResourcePatterns())
      .excludes(SKIPPED_URLS)
      .build();
  }

  @Override
  public void destroy() {
    //nothing to do
  }
}
