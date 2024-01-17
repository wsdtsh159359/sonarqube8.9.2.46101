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
package org.sonar.server.permission.ws.template;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.core.i18n.I18n;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.server.permission.RequestValidator;
import org.sonar.server.permission.ws.PermissionWsSupport;
import org.sonar.server.permission.ws.PermissionsWsAction;
import org.sonar.server.permission.ws.WsParameters;
import org.sonar.server.property.InternalProperties;
import org.sonar.server.user.UserSession;

import static java.lang.String.format;
import static org.sonar.server.permission.PermissionPrivilegeChecker.checkGlobalAdmin;
import static org.sonar.server.permission.ws.template.WsTemplateRef.newTemplateRef;
import static org.sonar.server.ws.WsParameterBuilder.createDefaultTemplateQualifierParameter;
import static org.sonar.server.ws.WsParameterBuilder.QualifierParameterContext.newQualifierParameterContext;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_QUALIFIER;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_NAME;

public class SetDefaultTemplateAction implements PermissionsWsAction {
  private final DbClient dbClient;
  private final PermissionWsSupport wsSupport;
  private final ResourceTypes resourceTypes;
  private final UserSession userSession;
  private final I18n i18n;

  public SetDefaultTemplateAction(DbClient dbClient, PermissionWsSupport wsSupport, ResourceTypes resourceTypes,
    UserSession userSession, I18n i18n) {
    this.dbClient = dbClient;
    this.wsSupport = wsSupport;
    this.resourceTypes = resourceTypes;
    this.userSession = userSession;
    this.i18n = i18n;
  }

  private static SetDefaultTemplateRequest toSetDefaultTemplateWsRequest(Request request) {
    return new SetDefaultTemplateRequest()
      .setQualifier(request.mandatoryParam(PARAM_QUALIFIER))
      .setTemplateId(request.param(PARAM_TEMPLATE_ID))
      .setTemplateName(request.param(PARAM_TEMPLATE_NAME));
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("set_default_template")
      .setDescription("Set a permission template as default.<br />" +
        "Requires the following permission: 'Administer System'.")
      .setPost(true)
      .setSince("5.2")
      .setHandler(this);

    WsParameters.createTemplateParameters(action);
    createDefaultTemplateQualifierParameter(action, newQualifierParameterContext(i18n, resourceTypes))
      .setDefaultValue(Qualifiers.PROJECT);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    doHandle(toSetDefaultTemplateWsRequest(request));
    response.noContent();
  }

  private void doHandle(SetDefaultTemplateRequest request) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      String qualifier = request.getQualifier();
      PermissionTemplateDto template = findTemplate(dbSession, request);
      checkGlobalAdmin(userSession);
      RequestValidator.validateQualifier(qualifier, resourceTypes);
      setDefaultTemplateUuid(dbSession, template, qualifier);
      dbSession.commit();
    }
  }

  private PermissionTemplateDto findTemplate(DbSession dbSession, SetDefaultTemplateRequest request) {
    return wsSupport.findTemplate(dbSession, newTemplateRef(request.getTemplateId(), request.getTemplateName()));
  }

  private void setDefaultTemplateUuid(DbSession dbSession, PermissionTemplateDto permissionTemplateDto, String qualifier) {
    switch (qualifier) {
      case Qualifiers.PROJECT:
        dbClient.internalPropertiesDao().save(dbSession, InternalProperties.DEFAULT_PROJECT_TEMPLATE, permissionTemplateDto.getUuid());
        break;
      case Qualifiers.VIEW:
        dbClient.internalPropertiesDao().save(dbSession, InternalProperties.DEFAULT_PORTFOLIO_TEMPLATE, permissionTemplateDto.getUuid());
        break;
      case Qualifiers.APP:
        dbClient.internalPropertiesDao().save(dbSession, InternalProperties.DEFAULT_APPLICATION_TEMPLATE, permissionTemplateDto.getUuid());
        break;
      default:
        throw new IllegalStateException(format("Unsupported qualifier : %s", qualifier));
    }
  }

  private static class SetDefaultTemplateRequest {
    private String qualifier;
    private String templateId;
    private String templateName;

    public String getQualifier() {
      return qualifier;
    }

    public SetDefaultTemplateRequest setQualifier(String qualifier) {
      this.qualifier = qualifier;
      return this;
    }

    @CheckForNull
    public String getTemplateId() {
      return templateId;
    }

    public SetDefaultTemplateRequest setTemplateId(@Nullable String templateId) {
      this.templateId = templateId;
      return this;
    }

    @CheckForNull
    public String getTemplateName() {
      return templateName;
    }

    public SetDefaultTemplateRequest setTemplateName(@Nullable String templateName) {
      this.templateName = templateName;
      return this;
    }
  }
}
