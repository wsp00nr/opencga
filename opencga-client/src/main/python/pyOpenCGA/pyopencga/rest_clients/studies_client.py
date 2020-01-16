from pyopencga.rest_clients._parent_rest_clients import _ParentRestClient


class Studies(_ParentRestClient):
    """
    This class contains methods for the 'Studies' webservices
    Client version: 2.0.0
    PATH: /{apiVersion}/studies
    """

    def __init__(self, configuration, token=None, login_handler=None, *args, **kwargs):
        _category = 'studies'
        super(Studies, self).__init__(configuration, _category, token, login_handler, *args, **kwargs)

    def update(self, study, data=None, **options):
        """
        Update some study attributes.
        PATH: /{apiVersion}/studies/{study}/update

        :param str study: Study [[user@]project:]study where study and project can be either the ID or UUID.
        :param dict data: JSON containing the params to be updated.
        """

        return self._post('update', query_id=study, data=data, **options)

    def permission_rules(self, study, entity, **options):
        """
        Fetch permission rules.
        PATH: /{apiVersion}/studies/{study}/permissionRules

        :param str study: Study [[user@]project:]study where study and project can be either the ID or UUID.
        :param str entity: Entity where the permission rules should be applied to.
        """

        options['entity'] = entity
        return self._get('permissionRules', query_id=study, **options)

    def update_permission_rules(self, study, entity, data=None, **options):
        """
        Add or remove a permission rule.
        PATH: /{apiVersion}/studies/{study}/permissionRules/update

        :param str study: Study [[user@]project:]study where study and project can be either the ID or UUID.
        :param str entity: Entity where the permission rules should be applied to.
        :param str action: Action to be performed: ADD to add a new permission rule; REMOVE to remove all permissions assigned by an existing permission rule (even if it overlaps any manual permission); REVERT to remove all permissions assigned by an existing permission rule (keep manual overlaps); NONE to remove an existing permission rule without removing any permissions that could have been assigned already by the permission rule. Allowed values: ['ADD', 'REMOVE', 'REVERT', 'NONE']
        :param dict data: JSON containing the permission rule to be created or removed.
        """

        options['entity'] = entity
        return self._post('update', query_id=study, subcategory='permissionRules', data=data, **options)

    def variable_sets(self, study, **options):
        """
        Fetch variableSets from a study.
        PATH: /{apiVersion}/studies/{study}/variableSets

        :param str study: Study [[user@]project:]study where study and project can be either the ID or UUID.
        :param str id: Id of the variableSet to be retrieved. If no id is passed, it will show all the variableSets of the study.
        """

        return self._get('variableSets', query_id=study, **options)

    def update_variable_sets(self, study, data=None, **options):
        """
        Add or remove a variableSet.
        PATH: /{apiVersion}/studies/{study}/variableSets/update

        :param str study: Study [[user@]project:]study where study and project can be either the ID or UUID.
        :param str action: Action to be performed: ADD or REMOVE a variableSet. Allowed values: ['ADD', 'REMOVE']
        :param dict data: JSON containing the VariableSet to be created or removed.
        """

        return self._post('update', query_id=study, subcategory='variableSets', data=data, **options)

    def update_variables(self, study, variable_set, data=None, **options):
        """
        Add or remove variables to a VariableSet.
        PATH: /{apiVersion}/studies/{study}/variableSets/{variableSet}/variables/update

        :param str study: Study [[user@]project:]study where study and project can be either the ID or UUID.
        :param str variable_set: VariableSet id of the VariableSet to be updated.
        :param str action: Action to be performed: ADD or REMOVE a variable. Allowed values: ['ADD', 'REMOVE']
        :param dict data: JSON containing the variable to be added or removed. For removing, only the variable id will be needed.
        """

        return self._post('variables/update', query_id=study, subcategory='variableSets', second_query_id=variable_set, data=data, **options)

    def groups(self, study, **options):
        """
        Return the groups present in the study.
        PATH: /{apiVersion}/studies/{study}/groups

        :param str study: Study [[user@]project:]study where study and project can be either the ID or UUID.
        :param str id: Group id. If provided, it will only fetch information for the provided group.
        :param str name: [DEPRECATED] Replaced by id.
        :param bool silent: Boolean to retrieve all possible entries that are queried for, false to raise an exception whenever one of the entries looked for cannot be shown for whichever reason.
        """

        return self._get('groups', query_id=study, **options)

    def update_groups(self, study, data=None, **options):
        """
        Add or remove a group.
        PATH: /{apiVersion}/studies/{study}/groups/update

        :param str study: Study [[user@]project:]study where study and project can be either the ID or UUID.
        :param str action: Action to be performed: ADD or REMOVE a group. Allowed values: ['ADD', 'REMOVE']
        :param dict data: JSON containing the parameters.
        """

        return self._post('update', query_id=study, subcategory='groups', data=data, **options)

    def update_users(self, study, group, data=None, **options):
        """
        Add, set or remove users from an existing group.
        PATH: /{apiVersion}/studies/{study}/groups/{group}/users/update

        :param str study: Study [[user@]project:]study where study and project can be either the ID or UUID.
        :param str group: Group name.
        :param str action: Action to be performed: ADD, SET or REMOVE users to/from a group. Allowed values: ['ADD', 'SET', 'REMOVE']
        :param dict data: JSON containing the parameters.
        """

        return self._post('users/update', query_id=study, subcategory='groups', second_query_id=group, data=data, **options)

    def create(self, data=None, **options):
        """
        Create a new study.
        PATH: /{apiVersion}/studies/create

        :param str project_id: Deprecated: Project id.
        :param str project: Project [user@]project where project can be either the ID or the alias.
        :param dict data: study.
        """

        return self._post('create', data=data, **options)

    def search(self, project, **options):
        """
        Search studies.
        PATH: /{apiVersion}/studies/search

        :param str include: Fields included in the response, whole JSON path must be provided.
        :param str exclude: Fields excluded in the response, whole JSON path must be provided.
        :param int limit: Number of results to be returned.
        :param int skip: Number of results to skip.
        :param bool count: Get the total number of results matching the query. Deactivated by default.
        :param str project: Project [user@]project where project can be either the ID or the alias.
        :param str name: Study name.
        :param str id: Study id.
        :param str alias: Study alias.
        :param str fqn: Study full qualified name.
        :param str type: Type of study: CASE_CONTROL, CASE_SET...
        :param str creation_date: Creation date. Format: yyyyMMddHHmmss. Examples: >2018, 2017-2018, <201805.
        :param str modification_date: Modification date. Format: yyyyMMddHHmmss. Examples: >2018, 2017-2018, <201805.
        :param str status: Status.
        :param str attributes: Attributes.
        :param str nattributes: Numerical attributes.
        :param bool battributes: Boolean attributes.
        :param str release: Release value.
        """

        options['project'] = project
        return self._get('search', **options)

    def acl(self, studies, **options):
        """
        Return the acl of the study. If member is provided, it will only return the acl for the member.
        PATH: /{apiVersion}/studies/{studies}/acl

        :param str studies: Comma separated list of Studies [[user@]project:]study where study and project can be either the ID or UUID up to a maximum of 100.
        :param str member: User or group id.
        :param bool silent: Boolean to retrieve all possible entries that are queried for, false to raise an exception whenever one of the entries looked for cannot be shown for whichever reason.
        """

        return self._get('acl', query_id=studies, **options)

    def update_acl(self, members, data=None, **options):
        """
        Update the set of permissions granted for the member.
        PATH: /{apiVersion}/studies/acl/{members}/update

        :param str members: Comma separated list of user or group ids.
        :param dict data: JSON containing the parameters to modify ACLs. 'template' could be either 'admin', 'analyst' or 'view_only'.
        """

        return self._post('update', query_id=members, data=data, **options)

    def aggregation_stats(self, studies, **options):
        """
        Fetch catalog study stats.
        PATH: /{apiVersion}/studies/{studies}/aggregationStats

        :param str studies: Comma separated list of studies [[user@]project:]study up to a maximum of 100.
        :param bool default: Calculate default stats.
        :param str file_fields: List of file fields separated by semicolons, e.g.: studies;type. For nested fields use >>, e.g.: studies>>biotype;type.
        :param str individual_fields: List of individual fields separated by semicolons, e.g.: studies;type. For nested fields use >>, e.g.: studies>>biotype;type.
        :param str family_fields: List of family fields separated by semicolons, e.g.: studies;type. For nested fields use >>, e.g.: studies>>biotype;type.
        :param str sample_fields: List of sample fields separated by semicolons, e.g.: studies;type. For nested fields use >>, e.g.: studies>>biotype;type.
        :param str cohort_fields: List of cohort fields separated by semicolons, e.g.: studies;type. For nested fields use >>, e.g.: studies>>biotype;type.
        """

        return self._get('aggregationStats', query_id=studies, **options)

    def info(self, studies, **options):
        """
        Fetch study information.
        PATH: /{apiVersion}/studies/{studies}/info

        :param str include: Fields included in the response, whole JSON path must be provided.
        :param str exclude: Fields excluded in the response, whole JSON path must be provided.
        :param str studies: Comma separated list of Studies [[user@]project:]study where study and project can be either the ID or UUID up to a maximum of 100.
        """

        return self._get('info', query_id=studies, **options)

