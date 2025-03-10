/*
 * Copyright 2015-2020 OpenCB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opencb.opencga.server.rest;

import io.swagger.annotations.*;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.opencb.commons.datastore.core.DataResult;
import org.opencb.commons.datastore.core.FacetField;
import org.opencb.commons.datastore.core.QueryOptions;
import org.opencb.opencga.catalog.db.api.StudyDBAdaptor;
import org.opencb.opencga.catalog.managers.StudyManager;
import org.opencb.opencga.catalog.utils.Constants;
import org.opencb.opencga.catalog.utils.ParamUtils;
import org.opencb.opencga.core.api.ParamConstants;
import org.opencb.opencga.core.exceptions.VersionException;
import org.opencb.opencga.core.models.common.Enums;
import org.opencb.opencga.core.models.study.*;
import org.opencb.opencga.core.response.OpenCGAResult;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Path("/{apiVersion}/studies")
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "Studies", description = "Methods for working with 'studies' endpoint")
public class StudyWSServer extends OpenCGAWSServer {

    private StudyManager studyManager;

    public StudyWSServer(@Context UriInfo uriInfo, @Context HttpServletRequest httpServletRequest, @Context HttpHeaders httpHeaders)
            throws IOException, VersionException {
        super(uriInfo, httpServletRequest, httpHeaders);
        studyManager = catalogManager.getStudyManager();
    }

    @POST
    @Path("/create")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Create a new study", response = Study.class)
    public Response createStudyPOST(
            @Deprecated @ApiParam(value = "Deprecated: Project id") @QueryParam("projectId") String projectId,
            @ApiParam(value = ParamConstants.PROJECT_DESCRIPTION) @QueryParam(ParamConstants.PROJECT_PARAM) String project,
            @ApiParam(value = ParamConstants.STUDY_PARAM, required = true) StudyCreateParams study) {
        try {
            study = ObjectUtils.defaultIfNull(study, new StudyCreateParams());

            if (StringUtils.isNotEmpty(projectId) && StringUtils.isEmpty(project)) {
                project = projectId;
            }

            String studyId = StringUtils.isEmpty(study.getId()) ? study.getAlias() : study.getId();
            return createOkResponse(catalogManager.getStudyManager().create(project, studyId, study.getAlias(), study.getName(),
                    study.getDescription(), study.getNotification(), null,
                    study.getStatus() != null ? study.getStatus().toCustomStatus() : null, study.getAttributes(), queryOptions, token));
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }

    @GET
    @Path("/search")
    @ApiOperation(value = "Search studies", response = Study.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = QueryOptions.INCLUDE, value = ParamConstants.INCLUDE_DESCRIPTION,
                    dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = QueryOptions.EXCLUDE, value = ParamConstants.EXCLUDE_DESCRIPTION,
                    dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = QueryOptions.LIMIT, value = ParamConstants.LIMIT_DESCRIPTION, dataType = "integer", paramType = "query"),
            @ApiImplicitParam(name = QueryOptions.SKIP, value = ParamConstants.SKIP_DESCRIPTION, dataType = "integer", paramType = "query"),
            @ApiImplicitParam(name = QueryOptions.COUNT, value = ParamConstants.COUNT_DESCRIPTION, dataType = "boolean", paramType = "query")
    })
    public Response getAllStudies(
            @Deprecated @ApiParam(value = "Project id or alias", hidden = true) @QueryParam("projectId") String projectId,
            @ApiParam(value = ParamConstants.PROJECT_DESCRIPTION, required = true) @QueryParam(ParamConstants.PROJECT_PARAM) String projectStr,
            @ApiParam(value = ParamConstants.STUDY_NAME_DESCRIPTION) @QueryParam("name") String name,
            @ApiParam(value = ParamConstants.STUDY_ID_DESCRIPTION) @QueryParam("id") String id,
            @ApiParam(value = ParamConstants.STUDY_ALIAS_DESCRIPTION) @QueryParam("alias") String alias,
            @ApiParam(value = ParamConstants.STUDY_FQN_DESCRIPTION) @QueryParam("fqn") String fqn,
            @ApiParam(value = ParamConstants.CREATION_DATE_DESCRIPTION)
            @QueryParam("creationDate") String creationDate,
            @ApiParam(value = ParamConstants.MODIFICATION_DATE_DESCRIPTION)
            @QueryParam("modificationDate") String modificationDate,
//            @ApiParam(value = "Boolean to retrieve deleted studies", defaultValue = "false") @QueryParam("deleted") boolean deleted,
            @ApiParam(value = ParamConstants.INTERNAL_STATUS_DESCRIPTION) @QueryParam(ParamConstants.INTERNAL_STATUS_PARAM) String internalStatus,
            @ApiParam(value = ParamConstants.STATUS_DESCRIPTION) @QueryParam(ParamConstants.STATUS_PARAM) String status,
            @ApiParam(value = "Attributes") @QueryParam("attributes") String attributes,
            @ApiParam(value = "Release value") @QueryParam("release") String release) {
        try {
            if (StringUtils.isNotEmpty(projectId) && StringUtils.isEmpty(projectStr)) {
                projectStr = projectId;
                query.remove(StudyDBAdaptor.QueryParams.PROJECT_ID.key());
            }
            query.remove(ParamConstants.PROJECT_PARAM);

            DataResult<Study> queryResult = catalogManager.getStudyManager().search(projectStr, query, queryOptions, token);
            return createOkResponse(queryResult);
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }

    @POST
    @Path("/{study}/update")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Update some study attributes", response = Study.class)
    public Response updateByPost(@ApiParam(value = ParamConstants.STUDY_DESCRIPTION,
            required = true) @PathParam(ParamConstants.STUDY_PARAM) String studyStr,
                                 @ApiParam(value = "JSON containing the params to be updated.", required = true) StudyUpdateParams updateParams) {
        try {
            ObjectUtils.defaultIfNull(updateParams, new StudyUpdateParams());
            DataResult queryResult = catalogManager.getStudyManager().update(studyStr, updateParams, null, token);
            return createOkResponse(queryResult);
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }

    @GET
    @Path("/{studies}/info")
    @ApiOperation(value = "Fetch study information", response = Study.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = QueryOptions.INCLUDE, value = ParamConstants.INCLUDE_DESCRIPTION,
                    dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = QueryOptions.EXCLUDE, value = ParamConstants.EXCLUDE_DESCRIPTION,
                    dataType = "string", paramType = "query")
    })
    public Response info(
            @ApiParam(value = ParamConstants.STUDIES_DESCRIPTION,
                    required = true) @PathParam(ParamConstants.STUDIES_PARAM) String studies) {
        try {
            List<String> idList = getIdList(studies);
            return createOkResponse(studyManager.get(idList, queryOptions, true, token));
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }

    @GET
    @Path("/{studies}/summary")
    @ApiOperation(value = "Fetch study information plus some basic stats", hidden = true,
            notes = "Fetch study information plus some basic stats such as the number of files, samples, cohorts...")
    public Response summary(@ApiParam(value = ParamConstants.STUDIES_DESCRIPTION, required = true)
                            @PathParam(ParamConstants.STUDIES_PARAM) String studies,
                            @ApiParam(value = ParamConstants.SILENT_DESCRIPTION,
                                    defaultValue = "false") @QueryParam(Constants.SILENT) boolean silent) {
        try {
            List<String> idList = getIdList(studies);
            return createOkResponse(studyManager.getSummary(idList, queryOptions, silent, token));
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }

    @GET
    @Path("/{study}/groups")
    @ApiOperation(value = "Return the groups present in the study", response = Group.class)
    public Response getGroups(
            @ApiParam(value = ParamConstants.STUDY_DESCRIPTION, required = true)
                @PathParam(ParamConstants.STUDY_PARAM) String studyStr,
            @ApiParam(value = "Group id. If provided, it will only fetch information for the provided group.") @QueryParam("id") String groupId,
            @ApiParam(value = "[DEPRECATED] Replaced by id.") @QueryParam("name") String groupName,
            @ApiParam(value = ParamConstants.SILENT_DESCRIPTION,
                    defaultValue = "false") @QueryParam(Constants.SILENT) boolean silent) {
        try {
            if (StringUtils.isNotEmpty(groupName)) {
                groupId = groupName;
            }
            return createOkResponse(catalogManager.getStudyManager().getGroup(studyStr, groupId, token));
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }

    @POST
    @Path("/{study}/groups/update")
    @ApiOperation(value = "Add or remove a group", response = Group.class)
    public Response updateGroupPOST(
            @ApiParam(value = ParamConstants.STUDY_DESCRIPTION) @PathParam(ParamConstants.STUDY_PARAM) String studyStr,
            @ApiParam(value = "Action to be performed: ADD or REMOVE a group", allowableValues = "ADD,REMOVE", defaultValue = "ADD")
                @QueryParam("action") ParamUtils.AddRemoveAction action,
            @ApiParam(value = "JSON containing the parameters", required = true) GroupCreateParams params) {
        try {
            if (action == null) {
                action = ParamUtils.AddRemoveAction.ADD;
            }
            OpenCGAResult<Group> group;
            if (action == ParamUtils.AddRemoveAction.ADD) {
                group = catalogManager.getStudyManager().createGroup(studyStr, params.getId(), params.getUsers(), token);
            } else {
                group = catalogManager.getStudyManager().deleteGroup(studyStr, params.getId(), token);
            }
            return createOkResponse(group);
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }

    @POST
    @Path("/{study}/groups/{group}/users/update")
    @ApiOperation(value = "Add, set or remove users from an existing group", response = Group.class)
    public Response updateUsersFromGroupPOST(
            @ApiParam(value = ParamConstants.STUDY_DESCRIPTION) @PathParam(ParamConstants.STUDY_PARAM) String studyStr,
            @ApiParam(value = "Group name") @PathParam("group") String groupId,
            @ApiParam(value = "Action to be performed: ADD, SET or REMOVE users to/from a group", allowableValues = "ADD,SET,REMOVE",
                    defaultValue = "ADD") @QueryParam("action") ParamUtils.BasicUpdateAction action,
            @ApiParam(value = "JSON containing the parameters", required = true) GroupUpdateParams updateParams) {
        try {
            if (action == null) {
                action = ParamUtils.BasicUpdateAction.ADD;
            }

            return createOkResponse(catalogManager.getStudyManager().updateGroup(studyStr, groupId, action, updateParams, token));

        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }

    @GET
    @Path("/{study}/permissionRules")
    @ApiOperation(value = "Fetch permission rules", response = PermissionRule.class)
    public Response getPermissionRules(
            @ApiParam(value = ParamConstants.STUDY_DESCRIPTION, required = true) @PathParam(ParamConstants.STUDY_PARAM) String studyStr,
            @ApiParam(value = "Entity where the permission rules should be applied to", required = true)
                @QueryParam("entity") Enums.Entity entity) {
        try {
            ParamUtils.checkIsSingleID(studyStr);
            return createOkResponse(catalogManager.getStudyManager().getPermissionRules(studyStr, entity, token));
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }

    @POST
    @Path("/{study}/permissionRules/update")
    @ApiOperation(value = "Add or remove a permission rule", response = PermissionRule.class)
    public Response updatePermissionRules(
            @ApiParam(value = ParamConstants.STUDY_DESCRIPTION) @PathParam(ParamConstants.STUDY_PARAM) String studyStr,
            @ApiParam(value = "Entity where the permission rules should be applied to", required = true) @QueryParam("entity")
                    Enums.Entity entity,
            @ApiParam(value = "Action to be performed: ADD to add a new permission rule; REMOVE to remove all permissions assigned by an "
                    + "existing permission rule (even if it overlaps any manual permission); REVERT to remove all permissions assigned by"
                    + " an existing permission rule (keep manual overlaps); NONE to remove an existing permission rule without removing "
                    + "any permissions that could have been assigned already by the permission rule.", allowableValues = "ADD,REMOVE,REVERT,NONE", defaultValue = "ADD")
            @QueryParam("action") Enums.PermissionRuleAction action,
            @ApiParam(value = "JSON containing the permission rule to be created or removed.", required = true) PermissionRule params) {
        try {
            if (action == null) {
                action = Enums.PermissionRuleAction.ADD;
            }
            if (action == Enums.PermissionRuleAction.ADD) {
                return createOkResponse(catalogManager.getStudyManager().createPermissionRule(studyStr, entity, params, token));
            } else {
                PermissionRule.DeleteAction deleteAction;
                switch (action) {
                    case REVERT:
                        deleteAction = PermissionRule.DeleteAction.REVERT;
                        break;
                    case NONE:
                        deleteAction = PermissionRule.DeleteAction.NONE;
                        break;
                    case REMOVE:
                    default:
                        deleteAction = PermissionRule.DeleteAction.REMOVE;
                        break;
                }
                catalogManager.getStudyManager().markDeletedPermissionRule(studyStr, entity, params.getId(), deleteAction, token);
                return createOkResponse(DataResult.empty());
            }
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }

    @GET
    @Path("/{studies}/acl")
    @ApiOperation(value = "Return the acl of the study. If member is provided, it will only return the acl for the member.", response = Map.class)
    public Response getAcls(
            @ApiParam(value = ParamConstants.STUDIES_DESCRIPTION, required = true) @PathParam(ParamConstants.STUDIES_PARAM) String studiesStr,
            @ApiParam(value = "User or group id") @QueryParam("member") String member,
            @ApiParam(value = ParamConstants.SILENT_DESCRIPTION, defaultValue = "false") @QueryParam(Constants.SILENT) boolean silent) {

        return run(() -> {
            List<String> idList = getIdList(studiesStr);
            return studyManager.getAcls(idList, member, silent, token);
        });
    }

    @POST
    @Path("/acl/{members}/update")
    @ApiOperation(value = "Update the set of permissions granted for the member", response = Map.class)
    public Response updateAcl(
            @ApiParam(value = "Comma separated list of user or group ids", required = true) @PathParam("members") String memberId,
            @ApiParam(value = ParamConstants.ACL_ACTION_DESCRIPTION, required = true, defaultValue = "ADD") @QueryParam(ParamConstants.ACL_ACTION_PARAM) ParamUtils.AclAction action,
            @ApiParam(value = "JSON containing the parameters to modify ACLs. 'template' could be either 'admin', 'analyst' or 'view_only'",
                    required = true) StudyAclUpdateParams params) {
        try {
            ObjectUtils.defaultIfNull(params, new StudyAclUpdateParams());
            StudyAclParams aclParams = new StudyAclParams(params.getPermissions(), params.getTemplate());
            List<String> idList = getIdList(params.getStudy(), false);
            return createOkResponse(studyManager.updateAcl(idList, memberId, aclParams, action, token));
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }

    @GET
    @Path("/{study}/variableSets")
    @ApiOperation(value = "Fetch variableSets from a study", response = VariableSet.class)
    public Response getVariableSets(
            @ApiParam(value = ParamConstants.STUDY_DESCRIPTION, required = true)
            @PathParam(ParamConstants.STUDY_PARAM) String studyStr,
            @ApiParam(value = "Id of the variableSet to be retrieved. If no id is passed, it will show all the variableSets of the study")
            @QueryParam("id") String variableSetId) {
        try {
            DataResult<VariableSet> queryResult;
            if (StringUtils.isEmpty(variableSetId)) {
                QueryOptions options = new QueryOptions(QueryOptions.INCLUDE, StudyDBAdaptor.QueryParams.VARIABLE_SET.key());
                DataResult<Study> studyQueryResult = catalogManager.getStudyManager().get(studyStr, options, token);

                if (studyQueryResult.getNumResults() == 1) {
                    queryResult = new DataResult<>(studyQueryResult.getTime(), studyQueryResult.getEvents(),
                            studyQueryResult.first().getVariableSets().size(), studyQueryResult.first().getVariableSets(),
                            studyQueryResult.first().getVariableSets().size());
                } else {
                    queryResult = DataResult.empty();
                }
            } else {
                queryResult = catalogManager.getStudyManager().getVariableSet(studyStr, variableSetId, queryOptions, token);
            }
            return createOkResponse(queryResult);
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }

    @GET
    @Path("/{studies}/aggregationStats")
    @ApiOperation(value = "Fetch catalog study stats", response = FacetField.class)
    public Response getAggregationStats(
            @ApiParam(value = "Comma separated list of studies [[user@]project:]study up to a maximum of 100", required = true)
            @PathParam(ParamConstants.STUDIES_PARAM) String studies,
            @ApiParam(value = "Calculate default stats", defaultValue = "true") @QueryParam("default") Boolean defaultStats,
            @ApiParam(value = "List of file fields separated by semicolons, e.g.: studies;type. For nested fields use >>, e.g.: "
                    + "studies>>biotype;type") @QueryParam("fileFields") String fileFields,
            @ApiParam(value = "List of individual fields separated by semicolons, e.g.: studies;type. For nested fields use >>, e.g.: "
                    + "studies>>biotype;type") @QueryParam("individualFields") String individualFields,
            @ApiParam(value = "List of family fields separated by semicolons, e.g.: studies;type. For nested fields use >>, e.g.: "
                    + "studies>>biotype;type") @QueryParam("familyFields") String familyFields,
            @ApiParam(value = "List of sample fields separated by semicolons, e.g.: studies;type. For nested fields use >>, e.g.: "
                    + "studies>>biotype;type") @QueryParam("sampleFields") String sampleFields,
            @ApiParam(value = "List of cohort fields separated by semicolons, e.g.: studies;type. For nested fields use >>, e.g.: "
                    + "studies>>biotype;type") @QueryParam("cohortFields") String cohortFields,
            @ApiParam(value = "List of job fields separated by semicolons, e.g.: studies;type. For nested fields use >>, e.g.: "
                    + "studies>>biotype;type") @QueryParam("jobFields") String jobFields) {
        try {
            if (defaultStats == null) {
                defaultStats = true;
            }
            List<String> idList = getIdList(studies);
            Map<String, Object> result = new HashMap<>();
            for (String study : idList) {
                result.put(study, catalogManager.getStudyManager().facet(study, fileFields, sampleFields, individualFields, cohortFields,
                        familyFields, jobFields, defaultStats, token));
            }
            return createOkResponse(result);
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }

    @POST
    @Path("/{study}/variableSets/update")
    @ApiOperation(value = "Add or remove a variableSet", response = VariableSet.class)
    public Response createOrRemoveVariableSets(
            @ApiParam(value = ParamConstants.STUDY_DESCRIPTION) @PathParam(ParamConstants.STUDY_PARAM) String studyStr,
            @ApiParam(value = "Action to be performed: ADD or REMOVE a variableSet", allowableValues = "ADD,REMOVE", defaultValue = "ADD")
                @QueryParam("action") ParamUtils.AddRemoveAction action,
            @ApiParam(value = "JSON containing the VariableSet to be created or removed.", required = true) VariableSetCreateParams params) {
        try {
            if (action == null) {
                action = ParamUtils.AddRemoveAction.ADD;
            }

            DataResult<VariableSet> queryResult;
            if (action == ParamUtils.AddRemoveAction.ADD) {
                // Fix variable set params to support 1.3.x
                // TODO: Remove in version 2.1.0-SNAPSHOT
                params.setId(StringUtils.isNotEmpty(params.getId()) ? params.getId() : params.getName());
                for (Variable variable : params.getVariables()) {
                    fixVariable(variable);
                }

                queryResult = catalogManager.getStudyManager().createVariableSet(studyStr, params.getId(), params.getName(), params.getUnique(),
                        params.getConfidential(), params.getDescription(), null, params.getVariables(), params.getEntities(), token);
            } else {
                queryResult = catalogManager.getStudyManager().deleteVariableSet(studyStr, params.getId(), token);
            }
            return createOkResponse(queryResult);
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }

//    @POST
//    @Path("/{study}/variableSets/{variableSet}/update")
//    @ApiOperation(value = "Update fields of an existing VariableSet [PENDING]", hidden = true)
//    public Response updateVariableSets(
//            @ApiParam(value = ParamConstants.STUDY_DESCRIPTION) @PathParam(ParamConstants.STUDY_PARAM) String studyStr,
//            @ApiParam(value = "VariableSet id of the VariableSet to be updated") @PathParam("variableSet") String variableSetId,
//            @ApiParam(value = "JSON containing the fields of the VariableSet to update.", required = true) UpdateableVariableSetParameters params) {
//        return createErrorResponse(new NotImplementedException("Pending of implementation"));
//    }

    @POST
    @Path("/{study}/variableSets/{variableSet}/variables/update")
    @ApiOperation(value = "Add or remove variables to a VariableSet", response = VariableSet.class)
    public Response updateVariablesFromVariableSet(
            @ApiParam(value = ParamConstants.STUDY_DESCRIPTION) @PathParam(ParamConstants.STUDY_PARAM) String studyStr,
            @ApiParam(value = "VariableSet id of the VariableSet to be updated") @PathParam("variableSet") String variableSetId,
            @ApiParam(value = "Action to be performed: ADD or REMOVE a variable", allowableValues = "ADD,REMOVE", defaultValue = "ADD")
                @QueryParam("action") ParamUtils.AddRemoveAction action,
            @ApiParam(value = "JSON containing the variable to be added or removed. For removing, only the variable id will be needed.", required = true) Variable variable) {
        try {
            if (action == null) {
                action = ParamUtils.AddRemoveAction.ADD;
            }

            DataResult<VariableSet> queryResult;
            if (action == ParamUtils.AddRemoveAction.ADD) {
                queryResult = catalogManager.getStudyManager().addFieldToVariableSet(studyStr, variableSetId, variable, token);
            } else {
                queryResult = catalogManager.getStudyManager().removeFieldFromVariableSet(studyStr, variableSetId, variable.getId(),
                        token);
            }
            return createOkResponse(queryResult);
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }

    private void fixVariable(Variable variable) {
        variable.setId(StringUtils.isNotEmpty(variable.getId()) ? variable.getId() : variable.getName());
        if (variable.getVariableSet() != null && variable.getVariableSet().size() > 0) {
            for (Variable variable1 : variable.getVariableSet()) {
                fixVariable(variable1);
            }
        }
    }

}
