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
package org.sonar.server.project.ws;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.component.ComponentUpdater;
import org.sonar.server.project.ProjectDefaultVisibility;
import org.sonar.server.project.Visibility;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Projects.CreateWsResponse;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang.StringUtils.abbreviate;
import static org.sonar.api.resources.Qualifiers.PROJECT;
import static org.sonar.core.component.ComponentKeys.MAX_COMPONENT_KEY_LENGTH;
import static org.sonar.db.component.ComponentValidator.MAX_COMPONENT_NAME_LENGTH;
import static org.sonar.db.permission.GlobalPermission.PROVISION_PROJECTS;
import static org.sonar.server.component.NewComponent.newComponentBuilder;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.ACTION_CREATE;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_NAME;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_PROJECT;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_VISIBILITY;

public class CreateAction implements ProjectsWsAction {

  private final DbClient dbClient;
  private final UserSession userSession;
  private final ComponentUpdater componentUpdater;
  private final ProjectDefaultVisibility projectDefaultVisibility;

  public CreateAction(DbClient dbClient, UserSession userSession, ComponentUpdater componentUpdater,
    ProjectDefaultVisibility projectDefaultVisibility) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.componentUpdater = componentUpdater;
    this.projectDefaultVisibility = projectDefaultVisibility;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ACTION_CREATE)
      .setDescription("Create a project.<br/>" +
        "Requires 'Create Projects' permission")
      .setSince("4.0")
      .setPost(true)
      .setResponseExample(getClass().getResource("create-example.json"))
      .setHandler(this);

    action.setChangelog(
      new Change("7.1", "The 'visibility' parameter is public"));

    action.createParam(PARAM_PROJECT)
      .setDescription("Key of the project")
      .setRequired(true)
      .setMaximumLength(MAX_COMPONENT_KEY_LENGTH)
      .setExampleValue(KEY_PROJECT_EXAMPLE_001);

    action.createParam(PARAM_NAME)
      .setDescription("Name of the project. If name is longer than %d, it is abbreviated.", MAX_COMPONENT_NAME_LENGTH)
      .setRequired(true)
      .setExampleValue("SonarQube");

    action.createParam(PARAM_VISIBILITY)
      .setDescription("Whether the created project should be visible to everyone, or only specific user/groups.<br/>" +
        "If no visibility is specified, the default project visibility will be used.")
      .setRequired(false)
      .setSince("6.4")
      .setPossibleValues(Visibility.getLabels());

  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    CreateRequest createRequest = toCreateRequest(request);
    writeProtobuf(doHandle(createRequest), request, response);
  }

  private CreateWsResponse doHandle(CreateRequest request) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      userSession.checkPermission(PROVISION_PROJECTS);
      String visibility = request.getVisibility();
      boolean changeToPrivate = visibility == null ? projectDefaultVisibility.get(dbSession).isPrivate() : "private".equals(visibility);

      ComponentDto componentDto = componentUpdater.create(dbSession, newComponentBuilder()
        .setKey(request.getProjectKey())
        .setName(request.getName())
        .setPrivate(changeToPrivate)
        .setQualifier(PROJECT)
        .build(),
        userSession.isLoggedIn() ? userSession.getUuid() : null);
      return toCreateResponse(componentDto);
    }
  }

  private static CreateRequest toCreateRequest(Request request) {
    return CreateRequest.builder()
      .setProjectKey(request.mandatoryParam(PARAM_PROJECT))
      .setName(abbreviate(request.mandatoryParam(PARAM_NAME), MAX_COMPONENT_NAME_LENGTH))
      .setVisibility(request.param(PARAM_VISIBILITY))
      .build();
  }

  private static CreateWsResponse toCreateResponse(ComponentDto componentDto) {
    return CreateWsResponse.newBuilder()
      .setProject(CreateWsResponse.Project.newBuilder()
        .setKey(componentDto.getDbKey())
        .setName(componentDto.name())
        .setQualifier(componentDto.qualifier())
        .setVisibility(Visibility.getLabel(componentDto.isPrivate())))
      .build();
  }

  static class CreateRequest {
    private final String projectKey;
    private final String name;
    @CheckForNull
    private final String visibility;

    private CreateRequest(Builder builder) {
      this.projectKey = builder.projectKey;
      this.name = builder.name;
      this.visibility = builder.visibility;
    }

    public String getProjectKey() {
      return projectKey;
    }

    public String getName() {
      return name;
    }

    @CheckForNull
    public String getVisibility() {
      return visibility;
    }

    public static Builder builder() {
      return new Builder();
    }
  }

  static class Builder {
    private String projectKey;
    private String name;
    @CheckForNull
    private String visibility;

    private Builder() {
    }

    public Builder setProjectKey(String projectKey) {
      requireNonNull(projectKey);
      this.projectKey = projectKey;
      return this;
    }

    public Builder setName(String name) {
      requireNonNull(name);
      this.name = name;
      return this;
    }

    public Builder setVisibility(@Nullable String visibility) {
      this.visibility = visibility;
      return this;
    }

    public CreateRequest build() {
      requireNonNull(projectKey);
      requireNonNull(name);
      return new CreateRequest(this);
    }
  }
}
